package org.hermi.shell;

import org.hermi.commons.Executor;
import org.hermi.constraint.validation.Validatable;

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
 *   public LexisNexisUserClient(PersistentAuditor<LexisNexisRequest, LexisNexisResponse> auditor) {
 *     super(auditor);
 *     this.restTemplate = restTemplate;
 *   }
 *   protected LexisNexisResponse doExchange(LexisNexisRequest req) {
 *     return restTemplate.postForObject("/api/users", req, LexisNexisResponse.class);
 *   }
 * }
 *
 *
 * /**
 * Base class for vendor-specific technical clients (Protocol layer).
 *
 * <p><b>AI-Friendly Architecture</b>: In Phase 2 implementation, a concrete client (e.g.,
 * LexisNexisFindUserClient) composes a VendorClient (inheriting from this class for protocol and
 * auditing) with a {@link Mapper} for domain-vendor translation to fulfill the Use Case contract.
 *
 * @param <P> payload type sent to the external system
 * @param <R> result type received from the external system
 */
public abstract class Client<P, R extends Validatable> extends Executor<P, R> {
  protected Client() {
    super();
  }

  /**
   * Implementation hook for executing the underlying external protocol (e.g., REST, SOAP, gRPC).
   *
   * @param payload the payload to send to the external system
   * @return the result received from the external system
   */
  protected abstract R doExchange(P payload);

  public final R exchange(P payload) {
    return execute(payload);
  }

  /**
   * Executes the exchange with full auditing lifecycle protection.
   *
   * @param payload the payload to send to the external system
   * @return the result received from the external system
   */
  @Override
  protected final R doExecute(P payload) {
    return this.doExchange(payload);
  }
}
