package com.ecosystem.simulation.rules;

import com.ecosystem.simulation.entities.Entity;
import com.ecosystem.simulation.events.SchedulingContext;
import com.ecosystem.simulation.simulation.World;
import com.ecosystem.simulation.statistics.Statistics;

/**
 * Bundles everything a rule needs to evaluate a condition or execute an action
 * against one entity, without giving the evaluator or the AST nodes a dependency
 * on the whole simulation engine.
 *
 * @param entity      the entity the rule is being evaluated against
 * @param world       the simulation world (for {@code env.*} references and spatial actions)
 * @param statistics  the statistics recorder (for {@code stat.*} references)
 * @param scheduling  the scheduling seam used by domain commands that cause a
 *                    discrete event (e.g. {@code die}, {@code reproduce})
 */
public record EvaluationContext(Entity entity, World world, Statistics statistics, SchedulingContext scheduling) {
}
