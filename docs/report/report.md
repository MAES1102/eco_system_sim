# Ecosystem Simulation — Project Report

## 1. Project Objective

An agent-based ecosystem simulation (predators, herbivores, plants) implemented
entirely in Java 21, built to satisfy two requirements set by the course:

1. **A technically defensible discrete-event simulation**, not a fixed-timestep
   loop dressed up with the phrase "discrete-event."
2. **Genuine runtime rule customizability** — players author new ecosystem
   logic (not just parameter tweaks or preset selection) without touching or
   recompiling Java source.

The system also demonstrates every OOP principle required by the course
(§8), with real, load-bearing examples rather than manufactured ones.

## 2. The Discrete-Event Model

### 2.1 What "genuine" means here

A discrete-event simulation (DES) advances its clock to the time of the next
scheduled event, not by a fixed increment, and every state change happens
inside an event's execution. This project satisfies both properties:

- `SimulationEvent` carries a `scheduledTime` and a monotonic
  `sequenceNumber` (deterministic tie-break — `java.util.PriorityQueue` does
  **not** guarantee FIFO order for equal-priority elements, so relying on
  insertion order alone would have been a latent bug).
- `SimulationEngine.advanceTo(targetTime)` pops the single earliest event,
  **adopts that event's own `scheduledTime` as the clock**, and executes it —
  repeating until the future-event list (FEL) is empty or the next event
  exceeds `targetTime`. The clock is never incremented on its own initiative.
- No entity is ever polled by an external loop. Each living entity has
  exactly one pending `EntityActivityEvent`, which performs the entity's
  behavior and reschedules its own successor — the classic DES idiom for
  periodic recurring processes. `EnvironmentRegenerationEvent` does the same
  for food regeneration.

```java
public void advanceTo(int targetTime) {
    while (eventQueue.peek() != null && eventQueue.peek().getScheduledTime() <= targetTime) {
        SimulationEvent event = eventQueue.dequeue();
        clock = event.getScheduledTime();
        event.execute(this);
    }
    if (clock < targetTime) clock = targetTime;
}
```

### 2.2 One owner per mutation

| Mutation | Owner |
|---|---|
| Aging, energy consumption, growth, movement decision, rule evaluation | `EntityActivityEvent.execute()` — legitimate because this event *is* the causal event |
| Predator-side kill effects (energy, satiation cooldown, hunt stat) | `PredationEvent.execute()` |
| Offspring creation, birth stat, first activity event | `ReproductionEvent.execute()` |
| `alive` flag, world removal, death stat — **for every cause** | `DeathEvent.execute()` — the *only* call site of `Entity.die()` |
| Position change record | `MovementEvent` — **observational only**, documented as non-causal (the move already happened inside `EntityActivityEvent`) |

Nothing mutates state and then enqueues a second event claiming to have
caused the same mutation. `Predator.attack()` only decides success/failure
and schedules `PredationEvent`; `PredationEvent` applies the predator's
effects and calls `prey.scheduleRemoval("predation")` rather than killing the
prey directly — so death always flows through exactly one path, regardless
of cause (starvation, old age, predation, grazing, or a rule's `die`
command).

### 2.3 Lazy invalidation and `pendingRemoval`

An entity can die between when an event referencing it was scheduled and
when that event executes. Rather than actively searching and cancelling
entries in the FEL (`PriorityQueue` doesn't support efficient arbitrary
removal), every event re-validates its preconditions at the top of its own
`execute()` and silently no-ops if stale — the standard, simplest DES
technique for this problem.

`Entity.pendingRemoval` is a single boolean guard set synchronously the
moment removal is *requested* (before the `DeathEvent` even executes),
preventing duplicate `DeathEvent`s and excluding the entity from being
targeted, moving, reproducing, or being rule-evaluated in the interim:

```java
public void scheduleRemoval(String cause) {
    if (!alive || pendingRemoval) return;   // idempotent guard
    pendingRemoval = true;
    if (schedulingContext != null) {
        schedulingContext.schedule(new DeathEvent(schedulingContext.getClock(), this, cause));
    } else {
        die();   // testability fallback outside a running engine
    }
}
```

## 3. Ecosystem Logic and Entity Interactions

Entities interact **directly and decentrally** — there is no central
arbiter deciding outcomes. `Predator.hunt()` queries `World.getNeighbors()`
and calls methods on a specific `Herbivore` object; `Herbivore.graze()` does
the same against `Plant` objects. The `World`/`Environment` provide spatial
queries and shared resources, not decision-making authority.

Each species' `update()` (called once per its own `EntityActivityEvent`):
searches for relevant neighbors within `visionRange`, moves or acts, consumes
energy, ages, and — if eligible — reproduces (which now schedules a
`ReproductionEvent` rather than constructing an offspring directly).

## 4. Rule DSL and Runtime Customization

### 4.1 Grammar

```
rule        ::= name "|" targetType "|" condition "|" actionList
condition   ::= orExpr
orExpr      ::= andExpr ( "OR" andExpr )*
andExpr     ::= unary ( "AND" unary )*
unary       ::= "NOT" unary | comparison | "(" orExpr ")"
comparison  ::= numExpr compareOp numExpr
numExpr     ::= term ( ("+"|"-") term )*
term        ::= factor ( ("*"|"/") factor )*
factor      ::= NUMBER | IDENTIFIER | "-" factor | "(" numExpr ")"
actionList  ::= action ( "," action )*
action      ::= IDENTIFIER ( mutationOp numExpr )?
```

Precedence, low to high: `OR` < `AND` < `NOT` < comparison < `+ -` < `* /`.

### 4.2 Pipeline

Free text (GUI editor or hand-edited `config/rules.txt`) → **Lexer**
(tokens) → **Parser** (recursive descent, hand-written — no ANTLR) →
immutable AST (`ConditionNode`/`NumericNode`/`RuleAction`, Java 21 sealed
interfaces + records) → **static validation** against `RuleVocabulary`
(target-type applicability, checked once at load time) → **Evaluator**
(exhaustive pattern-matching `switch`, invoked from inside every entity's
`EntityActivityEvent`). See `docs/uml/rule_processing.puml`.

### 4.3 Example rules (increasing complexity)

```
StarvingPredator | Predator | energy < 10 | die
ColdPenalty      | Predator | age > 30 AND env.temperature < 10 | energy -= 5
ColdStress       | Herbivore | env.temperature < 5 AND energy < 40 | energy -= 8, speed *= 0.8
CrowdedForest    | Plant | stat.plantPopulation > 100 OR env.foodLevel < 20 | growthRate *= 0.5
```

`CrowdedForest` is provably impossible to express in the project's earlier
dropdown-based rule builder: no `OR`, no `stat.*`/`env.*` reference, and no
non-`energy` attribute mutation existed in that design at all.

### 4.4 Runtime semantics

- Evaluated once per entity, inside that entity's own `EntityActivityEvent`,
  right after its own behavior runs.
- **Level-triggered**: fires again next tick if still true (matches the
  original design, avoids extra per-rule-per-entity state).
- Rules fire in **file/definition order**; actions within one rule execute
  **left to right, sequentially** (a later action observes an earlier
  action's effect — no rollback/transaction machinery).
- **Static rejection vs. runtime no-op, one consistent principle**: anything
  detectable without live simulation state (unknown attribute, target/command
  mismatch, division by a literal zero) is a hard `RuleParseException` at
  load time. Anything only discoverable from changing runtime state (a stale
  scheduled event, a dynamically-zero divisor) is a graceful, silent no-op —
  never a crash.
- **Numeric safety**: all arithmetic uses `double`; a runtime division by a
  dynamic zero yields `NaN`/`Infinity`, which is treated as a false condition
  / skipped mutation, never propagated into entity state.
- **Range clamping**: every writable attribute (`energy`, `speed`,
  `attackPower`, `defensePower`, `growthRate`) has a registered `[min, max]`
  range, so a repeated `speed *= 1.1` cannot grow without bound — no cooldown
  mechanism is needed for this, clamping alone is sufficient and simpler.

### 4.5 External persistence

`config/rules.txt` is bootstrapped from the bundled classpath default
(`src/main/resources/rules.txt`, read-only) on first run; every subsequent
load/save touches only the active file. Loading validates the **entire**
file before it can replace the active rule set — a single bad line leaves
the previous, still-valid rules untouched. Saving writes to a sibling
`.tmp` file, then attempts `ATOMIC_MOVE`, falling back to a plain
`REPLACE_EXISTING` move if the filesystem doesn't support atomic moves — the
active file is never opened for writing directly, so a failed save can only
ever leave an incomplete `.tmp` behind.

### 4.6 The honest customization boundary

Composing **new rule logic** from the existing readable/writable attributes,
operators, and domain commands needs **zero Java changes or recompilation**.
Exposing a genuinely **new** attribute or command (one not already in
`RuleVocabulary`) needs one `registerX(...)` line and a recompile. Both
statements are true and neither is hidden anywhere in this report or the
code's own Javadoc.

## 5. Configuration

`config/simulation.properties` (same bootstrap-from-seed pattern) controls
map size, initial populations, per-species stats (energy, speed,
attack/defense, vision range, max age, reproduction cooldown, energy
consumption, population cap), environment (food level/regen rate,
**temperature** — deliberately configuration-controlled only, no random
drift/seasons/weather engine), and the headless run's stopping condition.
Every value flows end to end through `EntityFactory`, used for **both** the
initial population and every reproduced offspring — offspring use configured
species defaults, not genetic inheritance from the parent (a deliberately
simpler, explicit policy).

## 6. Metrics

`Statistics` tracks population by species, births, deaths (with per-species
breakdown), successful/failed hunts, and plants consumed — all genuinely
displayed via `StatisticsPanel` (plain numeric counters, **not** graphs —
this report does not claim otherwise) and `EventLogPanel` (a live scrolling
log of rule triggers and entity events). The DES refactor also fixed a
pre-existing bug: `Herbivore.graze()` used to remove the eaten plant
directly, bypassing `Statistics.recordDeath` entirely, so the statistics
population count silently drifted from the real world count over time
("phantom plants"). After the refactor, grazing schedules a `DeathEvent`
like every other death, so this is structurally impossible now — confirmed
live: a smoke-test run showed `Plants consumed: 80` and `Plant deaths: 80` in
the same reporting window, an exact match.

## 7. Testing

107 tests (32 original + 75 new), unit and integration, covering: lexer
tokenization; parser precedence/parentheses/unary-minus; valid and invalid
rules; static target-type validation; writable-attribute range clamping;
NaN/Infinity handling; external file load/save/rollback with atomic-move
fallback; deterministic FEL ordering and sequence-number tie-breaking; lazy
invalidation; duplicate-death prevention; offspring/activity-event
scheduling; DES clock progression (adopts event time, never self-increments);
rule evaluation happening inside activity events; a compound
temperature rule's measurable population effect; configuration propagation
end to end (including to reproduced offspring); and full engine integration.

## 8. Limitations

- Time is integer-tick-valued and, in this specific ecosystem model, every
  entity needs an activity roughly every tick — so visually the cadence
  resembles fixed-Δt. The distinguishing, load-bearing property is
  architectural: the clock is FEL-driven (adopts popped events' own times)
  and state only changes inside `execute()`, not that inter-event gaps are
  large in this particular model.
- The rule DSL is deliberately not Turing-complete: no loops, no
  user-defined functions, no recursion — closed by the sealed AST at the
  type level, not just by convention.
- `Math.random()` is used directly (not a shared seeded `Random`), so exact
  run-to-run reproduction isn't guaranteed; tests that depend on outcomes use
  batch-averaged or structurally deterministic assertions instead of exact
  single-run values.
- Offspring use configured defaults, not inherited/varied traits — no
  genetic algorithm or evolutionary pressure is modeled.

## 9. Extension Points

Adding a new attribute or command: one line in `RuleVocabulary`. Adding a new
event type: extend `SimulationEvent`, implement `execute(SchedulingContext)`
— no existing code changes (Open/Closed). Adding a new species: extend
`Organism`/`Animal`, implement `Reproducible`/`Movable` as applicable, add
one `EntityFactory.createX(...)` method and matching `SimulationConfig`
keys.

## 10. OOP Principles — one section each, with real code

Each principle below is demonstrated by a genuinely load-bearing example from
the final implementation — not a manufactured one added just to tick a box.
Where a design decision could have gone another way, the reasoning is
included, since that is what an oral examiner will probe.

### 10.1 Encapsulation

`World` never lets a caller obtain a live reference to its internal entity
list — every read returns a defensive copy, so external code cannot corrupt
the world's bookkeeping by mutating what it thinks is a private structure:

```java
// World.java
public List<Entity> getEntities() {
    return new ArrayList<>(this.entities);  // Return copy to preserve encapsulation
}
```

Without the copy, `Predator.hunt()` iterating `world.getEntities()` while
`World.addEntity()`/`removeEntity()` run elsewhere in the same tick could
throw a `ConcurrentModificationException` or silently corrupt iteration —
the copy is not decorative, it is what makes the rest of the entity-update
loop safe to reason about.

### 10.2 Information hiding

`Rule`'s public contract (`matches`, `evaluateCondition`, `executeActions`,
the getters) did not change at all when the entire parsing strategy was
replaced — the old hand-rolled `switch`-based parser was deleted and
replaced by `Lexer`/`Parser`/`RuleVocabulary`/`Evaluator`, and every external
caller (`RuleEngine`, the GUI, all pre-existing tests) needed zero changes:

```java
// Rule.java — constructor is the only thing that changed internally
public Rule(String name, String targetType, String conditionStr, String actionStr,
            RuleVocabulary vocabulary, int lineNumber) throws RuleParseException {
    // ... trivial field assignments (name, targetType, conditionStr, actionStr, vocabulary) omitted
    this.targetClass = RuleVocabulary.resolveTargetType(targetType, lineNumber);
    this.condition   = Parser.parseCondition(conditionStr, lineNumber);
    this.actions     = Parser.parseActions(actionStr, lineNumber);
    validateCondition(condition, lineNumber);
    validateActions(lineNumber);
}
```

This is the practical value of information hiding, not just the textbook
definition: a caller who only ever used `rule.matches(entity)` and
`rule.evaluateCondition(ctx)` was completely insulated from a rewrite of how
"condition" is represented internally (string → AST).

### 10.3 Abstraction

`Entity` declares `update()` as abstract — it defines *that* every entity
must be updatable each tick, not *how*:

```java
// Entity.java
public abstract void update();
```

`SimulationEvent` does the same for `execute(SchedulingContext)`. Both are
genuine abstractions because the base class cannot know the concrete
behavior in advance: a `Plant` grows, a `Predator` hunts, a `DeathEvent`
records a death, a `MovementEvent` does nothing at all — the contract is
shared, the implementation is not.

### 10.4 Inheritance

```
Entity (abstract)
 └─ Organism (abstract)      adds energy, age, maxEnergy
     ├─ Animal (abstract)    adds speed, visionRange; implements Movable
     │   ├─ Predator         adds attackPower, fedCooldown
     │   └─ Herbivore        adds defensePower, grazeCooldown
     └─ Plant                adds growthRate
```

Every level adds genuinely new, non-redundant state — this was a deliberate
design check: `Plant` is **not** placed under `Animal` even though both are
organisms, because `Animal implements Movable` and a plant cannot move;
forcing it under `Animal` would have violated Liskov substitution (code
holding an `Animal` reference could legitimately call `move()` and expect it
to do something).

### 10.5 Composition

```java
// SimulationEngine.java
private final World world;
private final RuleVocabulary vocabulary;
private final RuleEngine ruleEngine;
private final Statistics statistics;
private final EntityFactory entityFactory;
private final EventQueue<SimulationEvent> eventQueue;   // the future-event list
```

`SimulationEngine` **has-a** `World`, `RuleEngine`, `Statistics`,
`EntityFactory`, and the FEL — none of these is a kind of `SimulationEngine`,
and none of them can outlive a running simulation in any meaningful sense, so
composition (not inheritance, not a looser aggregation) is the correct
relationship. `World` similarly owns its `Environment`.

### 10.6 Interface-based subtyping and multityping

```java
public abstract class Animal extends Organism implements Movable { ... }
public class Predator extends Animal implements Reproducible { ... }
```

A `Predator` is simultaneously an `Animal` (by inheritance), a `Movable`
(via `Animal`), and a `Reproducible` (declared directly) — three independent
types satisfied by one object, which is what multityping means in practice.
The two interfaces are kept separate deliberately (Interface Segregation):
`Plant` is `Reproducible` but **not** `Movable`, so it is never forced to
implement a `move()` method that would have no sensible body.

### 10.7 Inclusion polymorphism

```java
// EntityActivityEvent.execute(), called through the FEL for every concrete event type
entity.update();   // entity's static type here is Entity; the actual method that
                    // runs is chosen at runtime from {Predator, Herbivore, Plant}.update()
```

and, at the event level:

```java
// SimulationEngine.advanceTo()
SimulationEvent event = eventQueue.dequeue();
clock = event.getScheduledTime();
event.execute(this);   // dispatches to DeathEvent/PredationEvent/ReproductionEvent/
                        // MovementEvent/EntityActivityEvent/EnvironmentRegenerationEvent
```

This is the real mechanism the entire simulation loop runs on: the engine
never asks "what kind of event is this?" — it just calls `execute()` through
the base type and lets the JVM's dynamic dispatch pick the right override.
This is deliberately **not** the same thing as the sealed-`switch` pattern
used in the rule evaluator (see the note at the end of this section).

### 10.8 Parametric polymorphism

```java
// EventQueue.java
public class EventQueue<T extends SimulationEvent> {
    private final PriorityQueue<T> heap;
    public void enqueue(T event) { ... }
    public T dequeue() { ... }
}
```

The bound `T extends SimulationEvent` is what makes this a meaningful
generic rather than decoration: the compiler rejects enqueueing anything
that isn't a `SimulationEvent` at the call site, while the class itself
never needs to know which concrete event subtype it holds. Only
`EventQueue<SimulationEvent>` is instantiated in this project, but the bound
still does real compile-time work, and any future, more specific event
queue (e.g. one restricted to a narrower event family) would work with this
class unmodified.

### 10.9 Overloading polymorphism

```java
// Rule.java
public boolean evaluateCondition(Entity entity) { ... }               // overload 1
public boolean evaluateCondition(EvaluationContext ctx) { ... }       // overload 2

// Statistics.java
public void recordBirth(String entityType) { recordBirth(entityType, 1); }   // overload 1
public void recordBirth(String entityType, int count) { ... }                // overload 2

// SimulationException.java
public SimulationException(String message) { super(message); }                    // overload 1
public SimulationException(String message, Throwable cause) { super(message, cause); }  // overload 2
public SimulationException(Code code) { super(buildMessage(code)); }              // overload 3
```

Each pair/triple is resolved at **compile time** by the compiler matching
argument types to a signature — this is not the same mechanism as
overriding (`update()` above), which is resolved at **runtime** from the
object's actual class. Being able to state that distinction precisely is a
common examiner probe.

### 10.10 Coercion polymorphism

```java
// Predator.java, attack()
double successChance = (double) this.attackPower / (this.attackPower + herbivore.getDefensePower());
```

`this.attackPower` and `herbivore.getDefensePower()` are both `int`. The
explicit cast on the left operand forces Java's binary numeric promotion
rule to widen the *other*, uncast `int` operand to `double` as well before
the division runs — that implicit widening is the coercion. This is
deliberately **not** illustrated with `Integer.parseInt(...)` (used, for
example, in `SimulationConfig.getInt()` to read a configuration value) —
that is an explicit method call performing conversion, not a language-level
coercion, and conflating the two is a labeling mistake worth avoiding out
loud in the exam.

### 10.11 Exception handling

Two custom exceptions, deliberately different in kind:

```java
// RuleParseException.java — checked, recoverable: a malformed rule is expected,
// user-facing input, not a programming error.
public class RuleParseException extends Exception {
    public RuleParseException(String message, int lineNumber, int column) {
        super(message + " (line " + lineNumber + ", column " + column + ")");
        ...
    }
}

// SimulationException.java — unchecked, a programming-contract violation.
public SimulationException(SimulationException.Code code) { super(buildMessage(code)); }
```

`RuleParseException` is checked because callers (`RuleRepository`,
`RuleEngine.loadRules`) are expected to catch it and recover — the whole
"invalid file must not destroy the last valid rules" guarantee is built on
this being a checked, always-handled exception, verified by
`RuleRepositoryTest.reloadingCorruptedFile_preservesLastValidRules`.
`SimulationException` is unchecked because its causes (e.g. negative world
dimensions) indicate a bug in the caller, not a condition the program should
routinely recover from.

### 10.12 Extensibility (Open/Closed)

```java
// RuleVocabulary.java — adding a new rule-language attribute needs exactly this,
// nothing in Lexer/Parser/Rule/RuleEngine changes.
registerReadable("attackPower", Predator.class, ctx -> ((Predator) ctx.entity()).getAttackPower());
registerWritable("attackPower", Predator.class, (ctx, newValue) ->
        ((Predator) ctx.entity()).setAttackPower((int) clamp(newValue, 1, 50)));
```

This is verified, not just claimed: `RuleVocabularyValidationTest` and
`ParserTest` add coverage against the *existing* registered vocabulary
without ever touching `Rule.java`, `RuleEngine.java`, or the parser — proof
that the extension point is real. The same pattern holds for
`SimulationEvent` subclassing (a new event type needs only
`execute(SchedulingContext)` implemented, nothing upstream changes) and for
adding a new species via `EntityFactory`.

---

**A deliberate non-example, stated for honesty**: the rule AST's sealed
interfaces + pattern-matching `switch` (`Evaluator`) are a modern,
type-safe *alternative* to inclusion polymorphism, not an instance of it —
there is no virtual dispatch there; the compiler exhaustively checks a fixed,
closed set of cases instead. It is cited under abstraction/closed-set
exhaustiveness (§10.3), not §10.7, and this distinction — *why* a sealed
`switch` was chosen over a polymorphic `evaluate()` method on each AST node —
is itself good oral-exam material (see §11): the AST's node set is fixed and
closed by the grammar, whereas `Entity`/`SimulationEvent` are open extension
points, which is exactly why one uses dispatch and the other doesn't.

## 11. Likely Oral-Exam Questions

**"Is this truly discrete-event?"** Show `advanceTo()`: the clock is set from
`event.getScheduledTime()`, never incremented independently; show the
sequence-number tie-break; show `LazyInvalidationTest`.

**"Are the rules genuinely new, or still presets?"** Show `CrowdedForest` —
`OR`, a `stat.*` reference, a `growthRate` mutation — none of which the old
dropdown UI could express, and point at `RuleVocabularyValidationTest`
proving unknown/mismatched references are rejected, not silently ignored.

**"Why are domain commands still registered rather than fully generic?"**
`die`/`move`/`flee`/`grow`/`reproduce` invoke real, multi-step, stateful
behavior (spatial search, cooldowns, population caps) that isn't expressible
as one attribute assignment.

**"Why does a new entity attribute need Java code?"** It genuinely does —
stated explicitly in §4.6, not hidden.

**"Why isn't the DSL Turing-complete?"** No loops/functions/recursion by
design — every rule terminates in bounded time and can't touch state outside
the registered, clamped writable set.

**"Difference between inclusion, parametric, overloading, coercion
polymorphism?"** Use §10's table — and be ready to explain why the AST's
`switch` is deliberately *not* cited as the inclusion-polymorphism example.

**"Show me a live extension without touching the parser."** Add one
`RuleVocabulary.registerReadable(...)` line (e.g. expose `maxEnergy`),
recompile, write a rule using it — parser/lexer/AST untouched.

## 12. Demonstration Procedure

1. `mvn exec:java -Dexec.mainClass="com.ecosystem.simulation.Main"` — baseline
   simulation running with the bootstrapped `config/rules.txt`.
2. Pause; in the Rule Editor, type a new compound rule (e.g. `ColdStress` or
   a variant); click Validate — show a deliberate typo first to see the exact
   parser error, then the corrected version validating successfully.
3. Save; `ActiveRulesPanel`/`EventLogPanel` update immediately.
4. Resume; point at `StatisticsPanel` counters and `EventLogPanel` entries
   changing.
5. For a statistically clean version of the same effect: run
   `HeadlessRunner batch 25` at `environment.temperature=20.0` vs `=2.0` in
   `config/simulation.properties` — cold showed 92% predator extinction vs
   56% at mild temperature, and 8% vs 44% three-species survival, across 25
   trials each (see verification log).
6. Open `Rule.java`/`Parser.java`/`RuleVocabulary.java`/`EntityActivityEvent.java`
   side by side and narrate the pipeline.
7. Live extension: add one `RuleVocabulary` line, recompile, demonstrate.
