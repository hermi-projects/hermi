# Code as Journalism: Mastering The Newspaper Metaphor in Java

If you open a random source file in a typical enterprise Java application, you are usually greeted by a chaotic wall of text. You might see a low-level SQL query, followed by a massive `try-catch` block handling API call issues, followed by 20 lines of string manipulation, and somewhere buried in the middle, the actual business logic.

Reading this code feels like riding a broken elevator—constantly scrolling up and down just to understand what a single function does.

There is a better way. In his book *Clean Code*, Robert Martin introduced **The Newspaper Metaphor**. It states that a source file should read like a well-written newspaper article: **top-down, high-level intent first, with dirty technical details pushed entirely to the bottom.**

Let’s look at how to implement this metaphor in production Java using an elegant, intent-driven architectural pattern.

---

## The Anatomy of a Newspaper Class

In journalism, articles follow the **Inverted Pyramid** structure. The most critical information is printed at the very top, while background details sit at the bottom.

When we map this to a Java class, it breaks down into four clean, vertical sections:

```text
┌───────────────────────────────────────────────────────────┐
│  Section 1: The Headline     │ Class Name (0.1s intent)   │
├──────────────────────────────┼────────────────────────────┤
│  Section 2: The Background   │ Fields & Core Collaborators│
├──────────────────────────────┼────────────────────────────┤
│  Section 3: The Lead Story   │ Executive Summary          │
├──────────────────────────────┼────────────────────────────┤
│  Section 4: Secondary Body   │ Protected Milestone Steps  │
└───────────────────────────────────────────────────────────┘

```

### Breaking Down the 4 Sections

* **Section 1: The Headline:** Just like a front-page headline, this section must be bold, definitive, and announce the single sovereign purpose of the entire file in less than a second.
* **Section 2: The Background:** This introduces the key characters, dependencies, or structural configurations required to make the upcoming story possible. It sets the baseline context before the narrative begins.
* **Section 3: The Lead Story:** The absolute peak of the pyramid. This is a highly scannable, top-level summary of the entire scenario. It explains *what* the system accomplishes, written like a sequence of plain English milestones completely free of technical noise.
* **Section 4: Secondary Body:** The lower-level milestone developments. This sits further down the file because it focuses on *how* those high-level milestones actually happen—handling individual step mechanics, parameter preparation, and routing downstream assignments.

---

Here is a complete, production-ready example of a Use Case class built entirely around this narrative flow:

```java
package org.hermi.user.find.usecase;

/**
 * =================================================================
 * SECTION 1: THE HEADLINE
 * The class name is your front-page headline. A reader should know 
 * the exact, sovereign business intent of this file in 0.1 seconds.
 * =================================================================
 */
public class DefaultFindUserUseCase extends FindUserUseCase {

    // =================================================================
    // SECTION 2: THE BACKGROUND CONTEXT
    // The background variables required to understand the narrative. 
    // This defines *who* our core orchestrator interacts with.
    // =================================================================
    private final FindUserClient findUserClient;
    private final SaveUserRepository saveUserRepository;
    private final NotifyUserFoundMessenger messenger;

    public DefaultFindUserUseCase(FindUserClient client, SaveUserRepository repo, NotifyUserFoundMessenger messenger) {
        this.findUserClient = client;
        this.saveUserRepository = repo;
        this.messenger = messenger;
    }

    // =================================================================
    // SECTION 3: THE LEAD STORY (Executive Summary)
    // The main orchestrator. It acts as a highly scannable summary 
    // of the business scenario. It reads like plain English sentences 
    // flowing downwards—mapping only the high-level milestones.
    // =================================================================
    @Override
    protected Result doExecute(Context context) {
        
        User user = fetchDomainUser(context.ssn());
        
        persistUserToSystem(user);
        
        notifyUserHasBeenFound(user);
        
        return new Result(user.name(), user.email());
    }

    // =================================================================
    // SECTION 4: SECONDARY BODY (The Chapter Milestones)
    // Follows the "Vertical Proximity Rule". These protected steps are 
    // placed directly below the Lead. They handle the mechanics of 
    // contract execution and context mapping, keeping the front page clean.
    // =================================================================
    
    protected User fetchDomainUser(String ssn) {
        var apiResult = findUserClient.execute(new FindUserClient.Context(ssn));
        return new User(ssn, apiResult.name(), apiResult.email());
    }

    protected void persistUserToSystem(User user) {
        saveUserRepository.execute(new SaveUserRepository.Context(user.name(), user.email()));
    }

    protected void notifyUserHasBeenFound(User user) {
        messenger.execute(new NotifyUserFoundMessenger.Context(
            user.email(), 
            "User registered successfully: " + user.name()
        ));
    }
}

```

---

## Why This Changes Everything for Your Team

### 1. The 10-Second Architecture Review

When a Tech Lead or Senior Architect opens this file to perform a Code Review, they only need to read **Section 3 (`doExecute`)**. Within 10 seconds, they can verify if the business logic is correct without being distracted by JSON mapping, database transactions, or API payloads. If the business story makes sense, the architecture passes.

### 2. Zero "Technical Flashbacks"

A common mistake developers make is writing "flashbacks" mid-story. For example, right between fetching and saving a user, they might write 15 lines of code to build an email template string. That is a technical flashback.

By isolating steps into dedicated, protected functions (`notifyUserHasBeenFound`), we keep the structural noise out of the main storyline.

### 3. Extensibility via OCP (Open-Closed Principle)

Notice that the milestone steps are marked as `protected` instead of `private`. This is a massive superpower for downstream developers. If a different business unit or a testing profile needs to change *only* how a user is notified (e.g., sending an SMS instead of an Email, or adding an audit log), they don't have to copy-paste your orchestration logic. They simply extend `DefaultFindUserUseCase` and override a single method:

```java
@Override
protected void notifyUserHasBeenFound(User user) {
    // Custom team-specific notification logic goes here
}

```

---

## Where Did the "Garbage Talk" Go?

You might be wondering: *What happens if the network blinks? Where do the try-catch blocks, the HTTP retries, and the SQL queries live?*

According to the Newspaper Metaphor, **computer instructions belong in the back-page classifieds, not on the front page.** The Use Case layer is sovereign—it only commands *what* should happen, not *how* infrastructure manages it.

Resilience mechanics like retries or error handling are handled entirely inside the implementation classes down in the **Infrastructure Shell** (e.g., `KafkaNotifyUserFoundMessenger`). The core contract implicitly guarantees execution or structural failure, keeping your domain layer beautifully pure.

## The Ultimate Golden Rule

Next time you are writing a Java class, apply the **Fold Test**: Press `Ctrl + Shift + -` in your IDE to collapse all methods to their signatures.

Look at what remains visible. If your top-level public or protected methods still tell a cohesive, unbroken story about your business domain, you’ve written a great piece of journalism. If it reads like a machine manual, it's time to push those details down to the back page.
