package com.paytrace.patterns.factory;

import com.paytrace.patterns.strategy.ExactMatchStrategy;
import com.paytrace.patterns.strategy.MatchingStrategy;
import com.paytrace.patterns.strategy.PartialPaymentStrategy;
import com.paytrace.patterns.strategy.TolerantMatchStrategy;
import com.paytrace.patterns.strategy.VendorReferenceStrategy;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory pattern (GoF Creational) — produces the correct {@link MatchingStrategy}
 * on demand. The Reconciliation Engine asks for a strategy by name and gets back
 * a polymorphic instance of the abstract base class — calling code never needs
 * to know which concrete subclass it received.
 *
 * Singleton — there's only ever one factory.
 *
 * The 4 strategies match the design-document UC-04 specification 1:1:
 *   ┌────────────────────────────┬────────────────────────────────────────────┐
 *   │  Design name               │  Concrete class                            │
 *   ├────────────────────────────┼────────────────────────────────────────────┤
 *   │  1. Exact Match            │  ExactMatchStrategy                        │
 *   │  2. Tolerant Amount+Date   │  TolerantMatchStrategy                     │
 *   │  3. Vendor Reference       │  VendorReferenceStrategy                   │
 *   │  4. Partial Payment        │  PartialPaymentStrategy                    │
 *   └────────────────────────────┴────────────────────────────────────────────┘
 *
 * Execution order in {@link #createAll()} runs strict → loose so that the
 * highest-confidence matches consume their payments first.
 */
public class StrategyFactory {

    private static final StrategyFactory INSTANCE = new StrategyFactory();

    public static StrategyFactory getInstance() {
        return INSTANCE;
    }

    private StrategyFactory() {}

    /** Produce a single strategy by name. Names are case-insensitive. */
    public MatchingStrategy create(String strategyName) {
        if (strategyName == null) {
            throw new IllegalArgumentException("Strategy name is required.");
        }
        switch (strategyName.trim().toUpperCase()) {
            case "EXACT":            return new ExactMatchStrategy();
            case "VENDOR_REFERENCE":
            case "VENDOR":
            case "ACCOUNT":          return new VendorReferenceStrategy();
            case "TOLERANT":
            case "DATE":             return new TolerantMatchStrategy();
            case "PARTIAL":          return new PartialPaymentStrategy();
            default:
                throw new IllegalArgumentException(
                        "Unknown strategy: " + strategyName);
        }
    }

    /** All four strategies in canonical execution order (strictest → loosest). */
    public List<MatchingStrategy> createAll() {
        List<MatchingStrategy> all = new ArrayList<>();
        all.add(new ExactMatchStrategy());
        all.add(new VendorReferenceStrategy());
        all.add(new TolerantMatchStrategy());
        all.add(new PartialPaymentStrategy());
        return all;
    }
}
