# Ecosystem Simulation

A customizable discrete-event ecosystem simulation demonstrating OOP principles.

## Project Overview

This is a university OOP project that simulates an ecosystem with predators, herbivores, and plants. The simulation demonstrates:
- Encapsulation
- Inheritance
- Polymorphism
- Abstraction
- Composition
- Single Responsibility Principle

## Project Structure

```
ecosystem-simulation/
├── pom.xml
├── docs/
│   └── uml/
│       ├── class_diagram.puml
│       ├── sequence_simulation_step.puml
│       └── activity_predator_hunt.puml
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── ecosystem/
│   │   │           └── simulation/
│   │   │               ├── Main.java
│   │   │               ├── HeadlessRunner.java
│   │   │               ├── entities/
│   │   │               │   ├── Entity.java
│   │   │               │   ├── Organism.java
│   │   │               │   ├── Animal.java
│   │   │               │   ├── Predator.java
│   │   │               │   ├── Herbivore.java
│   │   │               │   └── Plant.java
│   │   │               ├── environment/
│   │   │               │   └── Environment.java
│   │   │               ├── rules/
│   │   │               │   ├── Rule.java
│   │   │               │   ├── RuleEngine.java
│   │   │               │   └── RuleParseException.java
│   │   │               ├── simulation/
│   │   │               │   ├── SimulationEngine.java
│   │   │               │   └── World.java
│   │   │               ├── statistics/
│   │   │               │   └── Statistics.java
│   │   │               └── gui/
│   │   │                   ├── MainWindow.java
│   │   │                   ├── WorldPanel.java
│   │   │                   ├── ViewerPanel.java
│   │   │                   ├── ControlPanel.java
│   │   │                   ├── StatisticsPanel.java
│   │   │                   ├── EventLogPanel.java
│   │   │                   ├── RuleBuilderPanel.java
│   │   │                   └── ActiveRulesPanel.java
│   │   └── resources/
│   │       └── rules.txt
│   └── test/
│       └── java/
│           └── com/ecosystem/simulation/
│               ├── entities/
│               │   └── SimulationEntitiesTest.java
│               └── rules/
│                   └── RuleMatchesTest.java
```

## Requirements

- Java 21
- Maven 3.6+

## How to Run

### Using Maven

1. Compile the project:
```bash
mvn compile
```

2. Run the simulation:
```bash
mvn exec:java -Dexec.mainClass="com.ecosystem.simulation.Main"
```

Or package and run:
```bash
mvn package
java -jar target/simulation-1.0-SNAPSHOT.jar
```

### Using Java directly

1. Compile:
```bash
javac -d target/classes src/main/java/com/ecosystem/simulation/**/*.java
```

2. Run:
```bash
java -cp target/classes com.ecosystem.simulation.Main
```

## Simulation Details

### Initial Population

Both the GUI (`Main`) and the headless validation run (`HeadlessRunner`) start with the same population:

| Entity type | Count | Energy | Speed | Attack / Defense / Growth |
|-------------|-------|--------|-------|--------------------------|
| Predator    | 3     | 80     | 2.0   | attackPower = 7          |
| Herbivore   | 15    | 50     | 1.5   | defensePower = 12        |
| Plant       | 40    | 50     | —     | growthRate = 2.0         |

Grid size: 50 × 50 cells.

### Carrying Capacities

| Type      | Cap |
|-----------|-----|
| Predator  |   8 |
| Herbivore |  35 |
| Plant     |  80 |

### Rules

Rules are loaded from `src/main/resources/rules.txt` at startup and can be
edited without recompiling.  Format: `Name | TargetType | Condition | Action`

```
# Rule targeting a superclass — applies to ALL subclasses of Organism
MaximumAge       | Organism  | age > 100    | die

# Rules targeting concrete types
StarvingPredator | Predator  | energy < 10  | die
DesperatePredator| Predator  | energy < 20  | move
OldHerbivore     | Herbivore | age > 50     | flee
FastPlantGrowth  | Plant     | energy > 30  | grow
```

Supported conditions: `energy`, `age`, `x`, `y` with operators `<`, `>`, `<=`, `>=`, `==`, `!=`.
Supported actions: `die`, `move`, `grow`, `flee`.

### Controls (GUI)

| Button / Control | Effect                           |
|------------------|----------------------------------|
| Start            | Begin / resume the simulation    |
| Pause            | Freeze simulation at current step|
| Reset            | Restart with initial population  |
| Speed (0.5×–5×)  | Change steps per second          |

### Headless Validation Run

```bash
mvn exec:java -Dexec.mainClass="com.ecosystem.simulation.HeadlessRunner"
```

Runs 200 steps silently and prints a population + event report every 25 steps.

## Architecture

### Entity Hierarchy
```
Entity (abstract)
└── Organism (abstract)
    ├── Animal (abstract)
    │   ├── Predator
    │   └── Herbivore
    └── Plant
```

### Composition
- `SimulationEngine` owns `World`, `RuleEngine`, and `Statistics`
- `World` owns `Environment`

### Simulation Step Pipeline
1. **updateAllEntities()** — each entity's `update()` called polymorphically
2. **applyRulesToAllEntities()** — `RuleEngine` evaluates text-file rules
3. **removeDeadEntities()** — single authoritative death recording in `Statistics`
4. **updateEnvironment()** — `Environment.regenerateFood()`
5. `timeStep++`

### UML Diagrams

Formal diagrams are in `docs/uml/` (PlantUML source):

| File | Content |
|------|---------|
| `class_diagram.puml` | Full class diagram with all relationships and visibility |
| `sequence_simulation_step.puml` | Sequence diagram for one complete simulation step |
| `activity_predator_hunt.puml` | Activity diagram for `Predator.hunt()` |

Render with: `java -jar plantuml.jar docs/uml/*.puml`

## OOP Principles Demonstrated

1. **Encapsulation**: Private fields with public methods
2. **Inheritance**: Clear entity hierarchy
3. **Polymorphism**: Different entity behaviors through method overriding
4. **Abstraction**: Abstract classes define contracts
5. **Composition**: SimulationEngine contains World, RuleEngine, Statistics
6. **Single Responsibility**: Each class has one clear purpose

## License

Educational project for university OOP course.