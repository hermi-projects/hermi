package org.hermi.shell;

/**
 * [AI ARCHITECTURAL CONTRACT]
 *
 * @apiNote
 *     <p>ROLE: Anti-Corruption Translation Gateway.
 *     <p>DESIGN INTENT: Decouple domain-agnostic vendor payloads from the Core Domain layer.
 *     <p>PURPOSE: Ensure that vendor-specific structures never leak into business logic.
 *     <p>Phase: 2 (Refinement / Refactoring)
 *     <p>Priority: 3
 * @implSpec GENERATION RULES FOR AI AGENTS:
 *     <ul>
 *       <li>1. PURE TRANSLATION: Implementations MUST contain ONLY field-level translation logic.
 *       <li>2. NO I/O: NEVER call external services (REST, DB) from inside a Mapper.
 *       <li>3. NO BRANCHING: NEVER embed conditional business routing or complex branching logic
 *           here.
 *       <li>4. NO EXCEPTIONS: NEVER throw business exceptions; use Optional or explicit null-checks
 *           for absent fields.
 *       <li>5. NAMING PROPHECY: Follow the {@code {Vendor}{Action}{Resource}Mapper} pattern (e.g.,
 *           {@code LexisNexisFindUserMapper}).
 *     </ul>
 *
 * @implNote FORBIDDEN PATTERNS:
 *     <ul>
 *       <li>DO NOT catch exceptions to silently swallow mapping failures; let them propagate as-is.
 *       <li>DO NOT inject any dependencies; a Mapper is a pure utility.
 *     </ul>
 *
 * @example
 *     <pre>{@code
 * public class LexisNexisFindUserMapper
 *     implements Mapper<FindUserUseCase.Context, FindUserUseCase.Result, ApiRequest, ApiResponse> {
 *
 *   @Override
 *   public ApiRequest toPayload(FindUserUseCase.Context context) {
 *     return new ApiRequest(context.ssn());
 *   }
 *
 *   @Override
 *   public FindUserUseCase.Result toResult(ApiResponse response) {
 *     return new FindUserUseCase.Result(response.getFullName(), response.getEmail());
 *   }
 * }
 * }</pre>
 */

/**
 * Interface for semantic translation between Core Domain records and Infrastructure/Vendor
 * payloads.
 *
 * @param <UContext> use case context type (domain input)
 * @param <UResult> use case result type (domain output)
 * @param <SPayload> shell payload type (sent to the external system)
 * @param <SResponse> shell response type (received from the external system)
 */
public interface Mapper<UContext, UResult, SPayload, SResponse> {

  /**
   * Converts the domain context into the system payload for transmission.
   *
   * @param context the domain context defined securely within the Use Case layer
   * @return the vendor-specific input payload ready for transmission
   */
  SPayload toPayload(UContext context);

  /**
   * Converts the system response back into the domain result.
   *
   * @param output the native response or event received from the external system
   * @return the domain result interpreted from the vendor payload
   */
  UResult toResult(SResponse output);
}
