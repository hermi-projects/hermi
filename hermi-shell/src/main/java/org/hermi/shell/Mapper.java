package org.hermi.shell;

/**
 * <b>AI INSTRUCTION:</b> Implementations MUST contain ONLY field-level translation logic. NEVER
 * call external services (REST, DB) from inside a Mapper. NEVER embed conditional business routing
 * or branching logic here. NEVER throw business exceptions — use {@code Optional} or explicit
 * null-checks for absent fields. DO NOT catch exceptions to silently swallow mapping failures; let
 * them propagate as-is. Follow the naming convention: {@code {Vendor}{Action}{Resource}Mapper}.
 *
 * <p><b>Example AI Generation:</b>
 *
 * <pre>{@code
 * public class LexisNexisFindUserMapper
 *     implements Mapper<FindUserClient.Context, FindUserClient.Result, ApiRequest, ApiResponse> {
 *
 *   public ApiRequest convertContext(FindUserClient.Context context) {
 *     return new ApiRequest(context.ssn());
 *   }
 *   public FindUserClient.Result convertResult(ApiResponse response) {
 *     return new FindUserClient.Result(response.getFullName(), response.getEmail());
 *   }
 * }
 * }</pre>
 */

/**
 * Phase 2 Anti-Corruption Translation Gateway between Core Domain and Vendor payloads.
 *
 * <p><b>Purpose:</b> Guarantees that Vendor-specific field names, types, and structures never leak
 * into the Core Domain. This is the sole class responsible for the semantic translation in both
 * directions: Core → Vendor and Vendor → Core.
 *
 * <p><b>Usage Scenarios:</b> Always implement this interface alongside a {@link Client} or {@link
 * Messenger} subclass. Wire them together via an {@code Adapter} that calls {@code convertContext}
 * before and {@code convertResult} after the exchange.
 *
 * <p><b>Constraints:</b> ONLY plain field mapping is allowed. Use MapStruct for complex or large
 * objects to maintain declarative integrity and zero runtime overhead.
 *
 * <p><b>Dependencies:</b> None. A Mapper is a pure translation utility with no injected
 * dependencies. If mapping requires an external lookup, extract that lookup into the Adapter.
 *
 * @param <C> Core Context type (domain input, from Use Case layer)
 * @param <R> Core Result type (domain output, returned to Use Case layer)
 * @param <I> Infrastructure Input type (vendor request payload)
 * @param <O> Infrastructure Output type (vendor response payload)
 */
public interface Mapper<C, R, I, O> {

  /**
   * Translates the pure domain Context object into the Vendor-specific Input payload.
   *
   * @param context the domain context defined securely within the Use Case layer
   * @return the vendor-specific input payload ready for transmission
   */
  I convertContext(C context);

  /**
   * Translates the Vendor-specific Output payload back into the pure domain Result object.
   *
   * @param output the native response or event received from the external system
   * @return the domain result interpreted from the vendor payload
   */
  R convertResult(O output);
}
