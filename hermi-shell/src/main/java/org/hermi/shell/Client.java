package org.hermi.shell;

import java.util.Objects;
import java.util.UUID;
import org.hermi.commons.Executor;

/**
 * <b>AI INSTRUCTION:</b> When subclassing, ONLY override {@code doExchange}. Do NOT add telemetry,
 * try-catch logging, or data translation logic that belongs to the Mapper or Auditor.
 *
 * <p><b>Example AI Generation:</b>
 *
 * <pre>{@code
 * @Component
 * public class StripeClient extends Client<StripeReq, StripeRes> {
 *   public StripeClient(Auditor<StripeReq, StripeRes> auditor) { super(auditor); }
 *   protected StripeRes doExchange(StripeReq req) { return restTemplate.postForObject(...); }
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
public abstract class Client<Req, Res> extends Executor<Req, Res> {
  private final Auditor<Req, Res> auditor;

  protected Client(Auditor<Req, Res> auditor) {
    this.auditor = Objects.requireNonNull(auditor, "Auditor is required for Client");
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
    UUID trackingId = auditor.save(request);
    try {
      Res response = super.execute(request);
      auditor.save(trackingId, response);
      return response;
    } catch (Exception e) {
      auditor.error(trackingId, e);
      throw e;
    }
  }

  public final Res execute(Req request) {
    return this.exchange(request);
  }

  @Override
  protected Res doExecute(Req request) {
    return this.doExchange(request);
  }
}
