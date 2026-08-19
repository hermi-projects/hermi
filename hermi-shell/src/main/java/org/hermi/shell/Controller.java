package org.hermi.shell;

import org.hermi.commons.Executor;
import org.hermi.commons.audit.Auditor;
import org.hermi.commons.validation.JakartaValidator;

/**
 * <b>AI INSTRUCTION:</b> WHAT: Base class for inbound request handlers (REST, RPC, ...) — enforces
 * the auditing/validation lifecycle around one inbound {@code Context}. WHY: Entry method
 * signatures are framework-specific and arbitrary ({@code @PostMapping} methods may take zero, one,
 * or many parameters of different types) — the Controller contract is Context → Result, NOT
 * parameter binding. WHO: Extended by concrete controllers in the Shell layer; invoked by the web
 * framework through the author's own annotated entry methods. WHEN: Each annotated entry method
 * assembles its parameters (path variables, body, headers, ...) into ONE {@code Context} and calls
 * {@link #handle(Object)}. WHERE: Shell layer — the inbound counterpart to {@link Client}. HOW:
 * Extend {@code Controller<Context, Result>} with ONE Context type per controller — one inbound
 * operation per class, like Client and Consumer. Implement {@code doHandle(Context)}. Write one
 * annotated entry method per endpoint: assemble parameters into the Context, delegate to {@code
 * handle(context)}.
 *
 * <p>DO NOT add: - business logic in entry methods (assemble Context, call handle, nothing else) -
 * try-catch or logging (handled by the Executor lifecycle) - conditional branching in doHandle (one
 * operation per controller; multiple operations mean multiple controllers) - technology types in
 * the Context (translate to plain Java types)
 *
 * <p><b>Example AI Generation:</b>
 *
 * <pre>{@code
 * // CORRECT: free-form entry signature — two parameters of different types assembled into ONE Context
 * @RestController
 * public class CreateUserController extends Controller<CreateUserContext, CreateUserUseCase.Result> {
 *   private final CreateUserUseCase createUserUseCase;
 *
 *   public CreateUserController(CreateUserUseCase createUserUseCase, LogAuditor<CreateUserContext, CreateUserUseCase.Result> auditor) {
 *     super(auditor);
 *     this.createUserUseCase = createUserUseCase;
 *   }
 *
 *   @PostMapping("/users")
 *   public CreateUserUseCase.Result createUser(
 *       @RequestBody CreateUserBody body, @RequestHeader("X-Token") String token) {
 *     return handle(new CreateUserContext(body, token));
 *   }
 *
 *   @Override
 *   protected CreateUserUseCase.Result doHandle(CreateUserContext context) {
 *     var useCaseContext = new CreateUserUseCase.Context(context.body(), context.token());
 *     return createUserUseCase.fulfill(useCaseContext);
 *   }
 * }
 * // WRONG: business logic in the entry method, or a doHandle that branches over multiple operations
 * }</pre>
 */

/** Inbound Request Gateway: the inbound counterpart to {@link Client}. */
public abstract class Controller<C, R> extends Executor<C, R> {
  protected Controller(Auditor<C, R> auditor) {
    setAuditor(auditor);
    setContextValidator(new JakartaValidator());
  }

  /**
   * Handles the inbound request through the full auditing and validation lifecycle.
   *
   * @param context the inbound request context
   * @return the result of handling the request
   */
  public final R handle(C context) {
    return execute(context);
  }

  /**
   * Seals the Executor hook: delegates to {@link #doHandle}.
   *
   * @param context the validated context
   * @return the handling result
   */
  @Override
  protected final R doExecute(C context) {
    return doHandle(context);
  }

  /**
   * Implements the inbound request handling.
   *
   * @param context the validated inbound request context
   * @return the handling result
   */
  protected abstract R doHandle(C context);
}
