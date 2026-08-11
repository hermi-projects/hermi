package org.hermi.commons.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.hermi.constraint.mask.MaskMapper;

/**
 * {@link Auditor} that writes JSONL execution lifecycle events to a file for AI-agent consumption.
 *
 * <p>Designed for the Phase 1 debug loop: a main shell passes this auditor to the use case, runs
 * {@code execute()}, and the agent Reads, greps, or diffs the output file instead of parsing mixed
 * console text. Each run overwrites the file, one JSON event per line:
 *
 * <pre>{@code
 * {"event":"STARTED","executionId":"...","executor":"FindUserMain","context":{"ssn":"***-**-6789"}}
 * {"event":"SUCCEEDED","executionId":"...","executor":"FindUserMain","result":{"name":"Alice"}}
 * {"event":"FAILED","executionId":"...","executor":"FindUserMain","exceptionClass":"...","exceptionMessage":"...","stackTrace":"..."}
 * }</pre>
 *
 * <p>Context and result values are masked through {@link MaskMapper}, so sensitive fields annotated
 * with {@code @Mask} or {@code @SSN} never reach the file. Events are flushed after every write, so
 * output survives a crash mid-run. The file is released when the JVM exits — no explicit close is
 * needed.
 */
public class FileAuditor<C, R> extends PersistentAuditor<C, R> {

  private static final ObjectMapper JSON = new ObjectMapper();

  private final String executor;
  private final PrintWriter out;

  /**
   * @param executorClass the class being audited, used to name the {@code executor} field
   * @param file the output file, truncated at the start of each run
   */
  public FileAuditor(Class<?> executorClass, Path file) throws IOException {
    this.executor = executorClass.getSimpleName();
    this.out =
        new PrintWriter(
            Files.newBufferedWriter(
                file,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING));
  }

  @Override
  protected UUID doRecordContext(C context) {
    UUID uuid = UUID.randomUUID();
    Map<String, Object> event = new LinkedHashMap<>();
    event.put("event", "STARTED");
    event.put("executionId", uuid);
    event.put("executor", executor);
    event.put("context", MaskMapper.mask(context));
    write(event);
    return uuid;
  }

  @Override
  protected void doRecordResult(UUID trackingId, R result) {
    Map<String, Object> event = new LinkedHashMap<>();
    event.put("event", "SUCCEEDED");
    event.put("executionId", trackingId);
    event.put("executor", executor);
    event.put("result", MaskMapper.mask(result));
    write(event);
  }

  @Override
  protected void doRecordError(UUID trackingId, C context, Exception exception) {
    Map<String, Object> event = new LinkedHashMap<>();
    event.put("event", "FAILED");
    event.put("executionId", trackingId);
    event.put("executor", executor);
    event.put("context", MaskMapper.mask(context));
    event.put("exceptionClass", exception.getClass().getName());
    event.put("exceptionMessage", exception.getMessage());
    event.put("stackTrace", stackTrace(exception));
    write(event);
  }

  private void write(Map<String, Object> event) {
    try {
      out.println(JSON.writeValueAsString(event));
      out.flush();
    } catch (IOException e) {
      throw new UncheckedIOException("FileAuditor failed to write event", e);
    }
  }

  private static String stackTrace(Throwable throwable) {
    StringWriter writer = new StringWriter();
    throwable.printStackTrace(new PrintWriter(writer));
    return writer.toString();
  }
}
