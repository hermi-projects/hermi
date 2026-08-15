package org.hermi.commons.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedWriter;
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
 * <p>Local Phase 1 debug loop only — not a production auditor. The main shell attaches one instance
 * per executor to the same file, runs {@code execute()}, and the agent Reads, greps, or diffs that
 * file instead of parsing mixed console text. The file is the agent's only evidence of what the
 * code did, so it must be trustworthy: events are appended, never truncated (the caller deletes the
 * file for a fresh run), flushed after every write, and write failures are logged instead of
 * silently dropped. One JSON event per line:
 *
 * <pre>{@code
 * {"event":"STARTED","executionId":"...","executor":"org.hermi.example.FindUserMain","context":{"ssn":"***-**-6789"}}
 * {"event":"SUCCEEDED","executionId":"...","executor":"org.hermi.example.FindUserMain","result":{"name":"Alice"}}
 * {"event":"FAILED","executionId":"...","executor":"org.hermi.example.FindUserMain","exceptionClass":"...","exceptionMessage":"...","stackTrace":"..."}
 * }</pre>
 *
 * <p>The {@code executor} field is the fully-qualified class name, so equally named classes in
 * different packages stay distinguishable. Context and result values are masked through {@link
 * MaskMapper} — {@code @Mask}/{@code @SSN} fields never reach the file. No explicit close is
 * needed; the writer is released at JVM exit, matching the one-JVM-per-debug-run lifecycle.
 */
public class FileAuditor<C, R> extends Auditor<C, R> {

  private static final ObjectMapper JSON = new ObjectMapper();

  private final String executor;
  private final BufferedWriter out;

  /**
   * @param executorClass the class being audited; its fully-qualified name is written to the {@code
   *     executor} field
   * @param file the output file, created if absent; events are appended, never overwritten
   */
  public FileAuditor(Class<?> executorClass, Path file) throws IOException {
    this.executor = executorClass.getName();
    this.out =
        Files.newBufferedWriter(
            file, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
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

  private synchronized void write(Map<String, Object> event) {
    try {
      out.write(JSON.writeValueAsString(event));
      out.newLine();
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
