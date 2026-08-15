package org.hermi.shell;

import org.hermi.commons.Executor;
import org.hermi.commons.audit.Auditor;
import org.hermi.commons.validation.JakartaValidator;

/**
 * [AI ARCHITECTURAL CONTRACT]
 *
 * @apiNote
 *     <p>ROLE: Inbound Event Gateway — receives events from external message brokers and routes
 *     them into the Use Case core through the full {@link Executor} lifecycle.
 *     <p>DESIGN INTENT: Decouple messaging infrastructure (Kafka, JMS, SQS) from business logic.
 *     The Consumer is a protocol bridge — it translates inbound events to Use Case contexts and
 *     delegates. It owns NOTHING of the business logic.
 *     <p>PURPOSE: Enforce boundary validation on every inbound event before it reaches domain code.
 *     <p>Phase: 2 (Shell/Protocol)
 *     <p>Priority: 3
 * @implSpec GENERATION RULES FOR AI AGENTS:
 *     <ul>
 *       <li>1. STATELESSNESS: Implementations MUST be strictly stateless. No request-specific
 *           instance variables. Only final, immutable dependencies (via constructor injection) are
 *           allowed.
 *       <li>2. NO BUSINESS LOGIC: The {@code doExecute} method MUST only convert the event to a Use
 *           Case context and delegate. All business rules, orchestration, and conditional logic
 *           live in the Use Case.
 *       <li>3. ONLY override {@code doExecute}. The {@code consume(E)} method is the framework
 *           entry point — annotate it with the broker-specific listener (e.g.
 *           {@code @KafkaListener}) and call {@link Executor#execute(Object)} to trigger the
 *           lifecycle. Never override {@code consume} with business logic.
 *       <li>4. NAMING PROPHECY: Follow the {@code {Action}{Resource}Consumer} pattern (e.g. {@code
 *           FindUserConsumer}).
 *     </ul>
 *
 * @implNote FORBIDDEN PATTERNS:
 *     <ul>
 *       <li>NEVER add business logic, orchestration, or conditional branching inside {@code
 *           doExecute} — delegate to a Use Case or Service.
 *       <li>DO NOT add try-catch blocks — the {@link Executor} lifecycle handles exception auditing
 *           and propagation.
 *       <li>DO NOT add data mapping or transformation logic — that belongs to a {@code Mapper}.
 *       <li>DO NOT call {@code doExecute} directly — always go through {@link #consume(Object)} or
 *           {@link Executor#execute(Object)}.
 *     </ul>
 *     <p><b>AI INSTRUCTION:</b> SCOPE: Protocol bridge only. Convert event → context, delegate,
 *     return. No business logic. LIFECYCLE: {@code consume(E)} is called by the broker. Call {@code
 *     execute(event)} inside it to trigger audit → validate → doExecute → validate → audit.
 *     <p>DO NOT add: - try-catch (exception boundary is in Executor.execute()) - log statements
 *     (handled by Auditor) - data mapping (belongs to Mapper) - null checks on context (handled by
 *     Validatable validation)
 *     <p><b>Example AI Generation:</b>
 *     <pre>{@code
 * // CORRECT: implements consume as broker entry point, doExecute as protocol bridge
 * &#64;Component
 * public class FindUserConsumer extends Consumer<Event, String> {
 *   private final FindUserService findUserService;
 *
 *   &#64;Autowired
 *   public FindUserConsumer(FindUserService findUserService) {
 *     this.findUserService = findUserService;
 *   }
 *
 *   &#64;KafkaListener(topics = "user.find.requests", groupId = "user-service-group")
 *   public void consume(Event event) {
 *     execute(event);
 *   }
 *
 *   &#64;Override
 *   protected String doExecute(Event event) {
 *     FindUserUseCase.Context context = new FindUserUseCase.Context(event.ssn);
 *     FindUserUseCase.Result result = findUserService.findUser(context);
 *     return result.id;
 *   }
 * }
 *
 * // WRONG: FindUserConsumerImpl, FindUserConsumerService — do NOT use these names
 * // WRONG: do NOT add business logic, try-catch, or logging in doExecute
 * }</pre>
 */

/** Inbound event consumer that routes broker-delivered events into the Use Case core. */

/**
 * Base class for all inbound event consumers in the Hermi Shell (Protocol layer).
 *
 * <p>A Consumer receives events from external message brokers (e.g., Kafka, JMS, SQS) and routes
 * them into the Use Case core through the full {@link Executor} lifecycle. It is the inbound
 * counterpart to {@link Messenger} — Messenger publishes outbound, Consumer receives inbound.
 *
 * <p>The event type {@code <E>} MUST implement {@link Validatable} because the event payload
 * crosses the system boundary from an untrusted external source. The framework validates the event
 * before {@code doExecute} runs, guaranteeing well-formed input.
 *
 * <p>Concrete implementations annotate {@link #consume(Object)} with the broker-specific listener
 * (e.g. {@code @KafkaListener}) and implement {@code doExecute} as a pure protocol bridge: convert
 * the event to a Use Case context, delegate to the Use Case, and return the result.
 *
 * @param <E> the inbound event type — MUST implement {@link Validatable}
 * @param <R> the result type returned after processing
 */
public abstract class Consumer<E, R> extends Executor<E, R> {

  protected Consumer(Auditor<E, R> auditor) {
    setAuditor(auditor);
    setResultValidator(new JakartaValidator());
  }

  /**
   * Entry point invoked by the messaging framework when an event arrives.
   *
   * <p>Annotate this method with the broker-specific listener (e.g. {@code @KafkaListener},
   * {@code @JmsListener}) and call {@link Executor#execute(Object)} to route the event through the
   * full auditing and validation lifecycle.
   *
   * @param event the inbound event received from the external message broker
   */
  public abstract void consume(E event);
}
