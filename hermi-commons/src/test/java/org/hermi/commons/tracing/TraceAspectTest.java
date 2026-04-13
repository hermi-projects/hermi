package org.hermi.commons.tracing;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import java.util.ArrayList;
import java.util.List;
import org.hermi.commons.tracing.aop.TraceAspect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/** Unit test for {@link TraceAspect} and {@link Trace} annotation. */
class TraceAspectTest {

  private MockAppender mockAppender;
  private TracedService service;

  @BeforeEach
  void setUp() {
    service = new TracedService();
    MDC.clear();

    // Setup Logback interception
    LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
    mockAppender = new MockAppender();
    mockAppender.setContext(context);
    mockAppender.start();

    Logger rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME);
    rootLogger.addAppender(mockAppender);
  }

  @AfterEach
  void tearDown() {
    mockAppender.stop();
    ((LoggerContext) LoggerFactory.getILoggerFactory())
        .getLogger(Logger.ROOT_LOGGER_NAME)
        .detachAppender(mockAppender);
    MDC.clear();
  }

  /**
   * Tests basic tracing functionality. Verifies that two JSON logs (START and SUCCESS) are produced
   * with correct severity, message content, and arguments.
   */
  @Test
  void testBasicTracing() {
    service.doSomething("hello");

    List<ILoggingEvent> events = mockAppender.getEvents();
    assertThat(events).hasSize(2);

    // Verify Start Message: checks for INFO severity, method signature, and argument type/value.
    assertThat(events.get(0).getMessage()).contains("\"severity\":\"INFO\"");
    assertThat(events.get(0).getMessage())
        .contains("\"message\":\"Execution of TracedService.doSomething() started\"");
    assertThat(events.get(0).getMessage()).contains("\"args\":\"[String:hello]\"");

    // Verify Success Message: checks for INFO severity, completion message, and duration field.
    assertThat(events.get(1).getMessage()).contains("\"severity\":\"INFO\"");
    assertThat(events.get(1).getMessage())
        .contains("\"message\":\"Execution of TracedService.doSomething() finished");
    assertThat(events.get(1).getMessage()).contains("\"duration_ms\":");
  }

  /**
   * Tests privacy masking functionality. Verifies that when @Trace is configured with excludeArgs
   * or excludeResult set to true, sensitive data is replaced by "[MASKED]" in the logs.
   */
  @Test
  void testMasking() {
    service.sensitiveMethod("secret123");

    List<ILoggingEvent> events = mockAppender.getEvents();
    // Verify parameters are masked.
    assertThat(events.get(0).getMessage()).contains("\"args\":\"[MASKED]\"");
    // Verify result is masked.
    assertThat(events.get(1).getMessage()).contains("\"result\":\"[MASKED]\"");
  }

  /**
   * Tests automatic performance threshold alerting. Verifies that when execution time exceeds the
   * slowThresholdMs setting, the log severity is automatically upgraded from INFO to WARN.
   */
  @Test
  void testSlowThreshold() throws InterruptedException {
    service.slowMethod();

    List<ILoggingEvent> events = mockAppender.getEvents();
    // Start log should remain INFO.
    assertThat(events.get(0).getMessage()).contains("\"severity\":\"INFO\"");
    // Finish log should be WARN because duration (110ms+) exceeds threshold (50ms).
    assertThat(events.get(1).getMessage()).contains("\"severity\":\"WARN\"");
  }

  /**
   * Tests business event propagation across method calls. Verifies that the event identifier
   * defined at the top-level is automatically propagated to nested method logs within the same
   * thread.
   */
  @Test
  void testEventPropagation() {
    service.topLevelMethod();

    List<ILoggingEvent> events = mockAppender.getEvents();
    // Verify that both top-level and nested logs share the same "GlobalEvent" identifier.
    for (ILoggingEvent event : events) {
      assertThat(event.getMessage()).contains("\"event\":\"GlobalEvent\"");
    }
  }

  /**
   * Tests tracing for nested Executor executions. Verifies: 1. Inheritance-based tracing from the
   * Executor base class. 2. Event identifiers in MDC are preserved across multiple layers of Use
   * Case calls. 3. Internal protected methods like doExecute are correctly captured.
   */
  @Test
  void testNestedExecutors() {
    ChildUseCase child = new ChildUseCase();
    ParentUseCase parent = new ParentUseCase(child);

    // Manually set initial context event.
    MDC.put("event", "ParentProcess");
    parent.execute("start");

    List<ILoggingEvent> events = mockAppender.getEvents();
    assertThat(events).isNotEmpty();

    // Verify the entire chain (Parent & Child) shares the same "ParentProcess" event.
    for (ILoggingEvent event : events) {
      assertThat(event.getMessage()).contains("\"event\":\"ParentProcess\"");
    }

    // Specifically verify the Child's core logic (doExecute) was captured.
    assertThat(events.stream().anyMatch(e -> e.getMessage().contains("ChildUseCase.doExecute")))
        .isTrue();
  }

  /** Helper Service trimmed with @Trace. */
  @Trace
  public static class TracedService {

    public void doSomething(String input) {
      // Normal tracing
    }

    @Trace(excludeArgs = true, excludeResult = true)
    public String sensitiveMethod(String password) {
      return "done";
    }

    @Trace(slowThresholdMs = 50)
    public void slowMethod() throws InterruptedException {
      Thread.sleep(110);
    }

    @Trace(event = "GlobalEvent")
    public void topLevelMethod() {
      nestedMethod();
    }

    @Trace
    public void nestedMethod() {}
  }

  /** Logback appender to capture logs in memory. */
  private static class MockAppender extends AppenderBase<ILoggingEvent> {
    private final List<ILoggingEvent> events = new ArrayList<>();

    @Override
    protected void append(ILoggingEvent event) {
      events.add(event);
    }

    public List<ILoggingEvent> getEvents() {
      return events;
    }
  }
}

/**
 * A mock parent use case. Moved outside to avoid AspectJ weaver crash on nested inherited
 * annotations.
 */
class ParentUseCase extends org.hermi.commons.execution.Executor<String, String> {
  private final ChildUseCase child;

  public ParentUseCase(ChildUseCase child) {
    this.child = child;
  }

  @Override
  protected String doExecute(String context) {
    return child.execute("child-task");
  }
}

/**
 * A mock child use case. Moved outside to avoid AspectJ weaver crash on nested inherited
 * annotations.
 */
class ChildUseCase extends org.hermi.commons.execution.Executor<String, String> {
  @Override
  protected String doExecute(String context) {
    return "child-done";
  }
}
