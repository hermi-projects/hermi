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
 * <p><b>AI-Friendly Architecture (Rule of Three)</b>: In Phase 2 implementation, this component
 * should follow the decoupled pattern:
 *
 * <ol>
 *   <li><b>VendorClient</b>: Inherits from this class, handles protocol (REST, gRPC) and auditing.
 *   <li><b>Mapper</b>: Handles semantic translation between Domain and Vendor types.
 *   <li><b>Adapter</b>: Wires the Client and Mapper together to fulfill the Use Case contract.
 * </ol>
 *
 * @param <Req> Vendor request type (e.g., LexisNexisRequest)
 * @param <Res> Vendor response type (e.g., LexisNexisResponse)
 */
public abstract class Client<Req, Res> extends AuditedExecutor<Req, Res> {

  /**
   * Constructs a Client with the required auditor.
   *
   * @param auditor the auditor to wrap all exchange operations
   */
  protected Client(Auditor<Req, Res> auditor) {
    super(auditor);
  }

  /**
   * Implementation hook for executing the underlying vendor-specific protocol (e.g., REST, SOAP,
   * gRPC).
   *
   * @param request the vendor-specific request payload
   * @return the native vendor-specific response payload
   */
  protected abstract Res doExchange(Req request);

  /**
   * Executes the vendor request with full auditing lifecycle protection.
   *
   * <p>This method guarantees that all interactions are mechanically wrapped by the {@link
   * Auditor}.
   *
   * @param request the vendor input payload
   * @return the vendor output payload
   */
  public final Res exchange(Req request) {
    return this.execute(request);
  }

  @Override
  protected Res doExecute(Req request) {
    return this.doExchange(request);
  }
}
