# Ecosystem Simulation

A customizable, genuinely discrete-event ecosystem simulation with a
runtime, user-authored rule DSL — implemented entirely in Java 21.

## Project Overview

A university OOP project simulating an ecosystem of predators, herbivores,
and plants interacting directly and decentrally (no central controller
decides outcomes — entities query the world and act on each other). The
simulation demonstrates encapsulation, information hiding, abstraction,
inheritance, composition, interface-based subtyping/multityping, inclusion
polymorphism, parametric polymorphism, overloading polymorphism, coercion
polymorphism, exception handling, and extensibility — see `docs/report/` for
a worked example of each, taken from the final code.

## What makes this a *genuine* discrete-event simulation

Unlike a fixed-timestep loop that polls every entity every tick, this engine
is driven entirely by a future-event list (FEL):

- Every `SimulationEvent` carries its own `scheduledTime` and a monotonic
  `sequenceNumber` used to break same-time ties deterministically.
- `SimulationEngine.advanceTo(targetTime)` pops the single earliest-scheduled
  event, sets its clock to *that event's own time*, and executes it — the
  clock is never incremented independently.
- No entity is ever polled from outside. Each living entity has exactly one
  pending `EntityActivityEvent` at a time, which performs the entity's
  routine behavior (movement, energy consumption, growth, rule evaluation)
  and reschedules its own successor — a dead or `pendingRemoval` entity's
  chain simply stops.
- Predation, reproduction, and death each have one dedicated, single-owner
  event class (`PredationEvent`, `ReproductionEvent`, `DeathEvent`) — see
  `Entity.scheduleRemoval()` and the events package Javadoc for the full
  "one owner per mutation" design. `MovementEvent` is documented as
  observational-only (it records a move that already happened; it does not
  cause it) rather than misleadingly presented as causal.
- Stale events (their subject died between scheduling and execution) are
  lazily invalidated — each event re-checks `isAlive()`/`isPendingRemoval()`
  at the top of its own `execute()`.

## Runtime rule DSL — customization without recompiling

Players author new ecosystem logic by editing `config/rules.txt` (by hand or
through the in-app free-text Rule Editor) — no Java source is touched or
recompiled. Rules support named targets, comparisons, `AND`/`OR`/`NOT`,
parentheses, arithmetic, entity/environment/statistics references, and
`=`/`+=`/`-=`/`*=`/`/=` mutations plus domain commands, with multiple
sequential actions per rule:

```
ColdStress | Herbivore | env.temperature < 5 AND energy < 40 | energy -= 8, speed *= 0.8
CrowdedForest | Plant | stat.plantPopulation > 100 OR env.foodLevel < 20 | growthRate *= 0.5
```

This is genuinely new logic, not preset selection: compound conditions,
statistics/environment references, and non-`energy` attribute mutations are
all things the old dropdown-based rule builder could not express at all.
Invalid or target-incompatible rules (e.g. `flee` on a `Plant`) are rejected
at load/validate time with a precise message — never silently ignored.

**Runtime vs. compile-time boundary, stated honestly**: composing *new rule
logic* from the existing readable/writable attributes and commands needs zero
Java changes. Exposing a genuinely *new* attribute or command (one not
already registered in `RuleVocabulary`) needs one `registerX(...)` line and a
recompile — this is not hidden or overclaimed anywhere in the project.

## Project Structure

```
oop|_sim/
├── pom.xml
├── config/                          (created at runtime, gitignored)
│   ├── rules.txt                    ← active, writable rules
│   └── simulation.properties        ← active, writable configuration
├── docs/
│   └── uml/
│       ├── class_diagram.puml
│       ├── sequence_simulation_step.puml
│       ├── activity_predator_hunt.puml
│       └── rule_processing.puml
├── src/
│   ├── main/
│   │   ├── java/com/ecosystem/simulation/
│   │   │   ├── Main.java, HeadlessRunner.java
│   │   │   ├── entities/            (Entity hierarchy, Movable, Reproducible, EntityFactory)
│   │   │   ├── environment/         (Environment: food, temperature)
│   │   │   ├── events/              (SimulationEvent hierarchy, EventQueue, SchedulingContext)
│   │   │   ├── rules/               (Rule, RuleEngine, RuleVocabulary, Evaluator, RuleRepository)
│   │   │   │   ├── ast/             (sealed ConditionNode / NumericNode / RuleAction)
│   │   │   │   └── lang/            (Lexer, Parser, Token)
│   │   │   ├── simulation/          (SimulationEngine, World, SimulationConfig)
│   │   │   ├── statistics/          (Statistics)
│   │   │   └── gui/                 (MainWindow, RuleEditorPanel, ...)
│   │   └── resources/               (read-only bundled defaults: rules.txt, simulation.properties)
│   └── test/java/com/ecosystem/simulation/   (see Testing section)
```

## User Guide

### Requirements

- Java 21
- Maven 3.6+

### How to Run

Both entry points must be run from the project root (they resolve
`config/rules.txt` and `config/simulation.properties` relative to the current
working directory, bootstrapping them from the bundled defaults on first run).

```bash
mvn compile
mvn exec:java -Dexec.mainClass="com.ecosystem.simulation.Main"
```

Or package and run the jar (same working-directory requirement applies):

```bash
mvn package
java -jar target/simulation-1.0-SNAPSHOT.jar
```

Headless validation / smoke test:

```bash
mvn exec:java -Dexec.mainClass="com.ecosystem.simulation.HeadlessRunner"
mvn exec:java -Dexec.mainClass="com.ecosystem.simulation.HeadlessRunner" -Dexec.args="batch 20"
```

### External Configuration

`config/simulation.properties` (bootstrapped from the bundled default on
first run) controls map size, initial populations, per-species stats (energy,
speed, attack/defense, vision range, max age, reproduction cooldown, energy
consumption, population cap), environment (food level/regen rate,
**temperature**), and the headless run's stopping condition. Every value is
genuinely consumed end to end via `EntityFactory` — including by reproduced
offspring, which use configured species defaults rather than inheriting from
their parent (a deliberately simpler policy than genetic inheritance). See
`SimulationConfig` for the full parameter list and defaults.

## Rules — full grammar

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
mutationOp  ::= "=" | "+=" | "-=" | "*=" | "/="
```

Readable references: `energy`, `age`, `x`, `y`, `speed`, `attackPower`,
`defensePower`, `growthRate`, `env.foodLevel`, `env.temperature`,
`env.width`, `env.height`, `stat.predatorPopulation`,
`stat.herbivorePopulation`, `stat.plantPopulation`.
Writable attributes (range-clamped): `energy`, `speed`, `attackPower`,
`defensePower`, `growthRate`. Domain commands: `die`, `move`, `flee`, `grow`,
`reproduce`. All of the above are registered in `RuleVocabulary` — `Rule`
itself contains no hardcoded field/action switch.

## Controls (GUI)

| Button / Control | Effect |
|---|---|
| Start / Pause / Reset | Simulation lifecycle |
| Speed (0.5×–5×) | Steps per second |
| Rule Editor: Validate / Save / Reload / Remove by name | Free-text rule authoring against `config/rules.txt` |

## Architecture

### Entity Hierarchy
```
Entity (abstract)
└── Organism (abstract)
    ├── Animal (abstract) ......implements Movable
    │   ├── Predator ...........implements Reproducible
    │   └── Herbivore ..........implements Reproducible
    └── Plant ...................implements Reproducible
```

### Composition
`SimulationEngine` owns `World`, `RuleEngine`, `Statistics`, `EntityFactory`,
and the FEL (`EventQueue<SimulationEvent>`); `World` owns `Environment`.

### UML Diagrams

| File | Content |
|---|---|
| `class_diagram.puml` | Full class diagram, including the DES event hierarchy and rule-DSL packages |
| `sequence_simulation_step.puml` | One `step()` call through the FEL, including a predation→death event chain |
| `activity_predator_hunt.puml` | `Predator.hunt()`/`attack()`, updated for event-scheduled predation |
| `rule_processing.puml` | Lexer → Parser → AST → validation → persistence → runtime evaluation pipeline |

Render with: `java -jar plantuml.jar docs/uml/*.puml`

## Testing

`mvn test` runs the full suite (unit + integration) covering: lexer/parser
tokenization, precedence and parentheses, valid/invalid rules, static
semantic validation, writable-attribute range clamping, NaN/Infinity
handling, external rule load/save/rollback, deterministic FEL ordering and
sequence-number tie-breaking, lazy invalidation, duplicate-death prevention,
offspring/activity-event scheduling, DES clock progression, rule evaluation
inside activity events, a compound temperature rule's measurable population
effect, configuration propagation, and full simulation integration.

## License

Educational project for university OOP course.
