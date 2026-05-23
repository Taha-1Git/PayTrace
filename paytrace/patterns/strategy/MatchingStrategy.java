package com.paytrace.patterns.strategy;

import com.paytrace.models.ReconciliationMatch;

import java.util.List;

/**
 * Abstract base for all reconciliation matching strategies.
 * Concrete subclasses (mirroring the UC-04 design specification 1:1):
 *   ExactMatchStrategy, VendorReferenceStrategy, TolerantMatchStrategy, PartialPaymentStrategy.
 *
 * Demonstrates: abstraction (cannot instantiate directly),
 * polymorphism (each subclass overrides match() differently),
 * template method (execute() is shared, match() varies).
 */
public abstract class MatchingStrategy {

    /** Display name shown in the engine log. Subclasses must provide. */
    public abstract String getStrategyName();

    /**
     * Run the strategy against the provided context.
     * Returns the list of matches this strategy was confident about.
     *
     * Template method — subclasses override match() to provide their own logic.
     */
    public final List<ReconciliationMatch> execute(ReconciliationContext ctx) {
        ctx.log("─── Running " + getStrategyName() + " ───");
        List<ReconciliationMatch> results = match(ctx);
        ctx.log(getStrategyName() + " complete: " + results.size()
                + " match(es) produced.");
        return results;
    }

    /** The variable part — each subclass implements its own matching algorithm. */
    protected abstract List<ReconciliationMatch> match(ReconciliationContext ctx);
}