package org.hermi.shell;

import java.util.Objects;
import java.util.UUID;
import org.hermi.commons.Executor;

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

  protected abstract Res doExchange(Req resuest);

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
