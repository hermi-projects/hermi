package org.hermi.shell;

import org.hermi.shell.audit.AuditedExecutor;
import org.hermi.shell.audit.Auditor;

/**
 * <b>AI INSTRUCTION:</b> When subclassing, ONLY override {@code doExchange}. Do NOT add telemetry,
 * try-catch logging, or data translation logic that belongs to the Mapper or Auditor.
 *
 * <p><b>Example AI Generation:</b>
 *
 * <pre>{@code
 * @Component
 * public class LexisNexisUserClient extends Client<LexisNexisRequest, LexisNexisResponse> {
 *   private final RestTemplate restTemplate;
 *
 *   public LexisNexisUserClient(Auditor<LexisNexisRequest, LexisNexisResponse> auditor, RestTemplate restTemplate) {
 *     super(auditor);
 *     this.restTemplate = restTemplate;
 *   }
 *   protected LexisNexisResponse doExchange(LexisNexisRequest req) {
 *     return restTemplate.postForObject("/api/users", req, LexisNexisResponse.class);
 *   }
 * }
 * }</pre>
 */

/**
 * Base class for vendor-specific technical clients (Protocol layer).
 *
 * <p><b>AI-Friendly Architecture</b>: In Phase 2 implementation, a concrete client (e.g.,
 * LexisNexisFindUserClient) composes a VendorClient (inheriting from this class for protocol and
 * auditing) with a {@link Mapper} for domain-vendor translation to fulfill the Use Case contract.
 *
 * @param <P> payload type sent to the external system
 * @param <R> result type received from the external system
 */
public abstract class Client<P, R> extends AuditedExecutor<P, R> {

  /**
   * Constructs a Client with the required auditor.
   *
   * @param auditor the auditor to wrap all exchange operations
   */
  protected Client(Auditor<P, R> auditor) {
    super(auditor);
  }

  /**
   * Implementation hook for executing the underlying external protocol (e.g., REST, SOAP, gRPC).
   *
   * @param payload the payload to send to the external system
   * @return the result received from the external system
   */
  protected abstract R doExchange(P payload);

  /**
   * Executes the exchange with full auditing lifecycle protection.
   *
   * <p>This method guarantees that all interactions are mechanically wrapped by the {@link
   * Auditor}.
   *
   * @param payload the payload to send to the external system
   * @return the result received from the external system
   */
  public final R exchange(P payload) {
    return this.execute(payload);
  }

  @Override
  protected final R doExecute(P payload) {
    return this.doExchange(payload);
  }
}
