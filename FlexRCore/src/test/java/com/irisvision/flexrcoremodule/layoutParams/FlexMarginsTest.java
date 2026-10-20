package com.irisvision.flexrcoremodule.layoutParams;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.irisvision.flexrcoremodule.core.FlexContextInterface;

import org.junit.Test;

/**
 * Tests de {@link FlexMargins}.
 *
 * <p>These methods are <b>pure string-to-integer functions</b>: they need neither
 * a screen, nor an emulator, only a context providing known metrics. That is why this
 * package concentre le meilleur retour sur investissement en tests de toute la librairie.
 *
 * <p>Coverage: both accepted formats, the optional trailing comma, the strategy,
 * the targeted tokens of {@code processOnly}, and above all the error cases
 * des {@code StringIndexOutOfBoundsException} sans message.
 */
public class FlexMarginsTest {

    private static final FlexContextInterface CONTEXT = TestFlexContext.standard();

    /* *** processAll *** */

    @Test
    public void processAllWithoutStrategyUsesFourValues() {
        int[] result = FlexMargins.processAll(CONTEXT, "10, 20, 30, 40", false, 0, 0, 0, 0);

        assertArrayEquals(new int[]{10, 20, 30, 40}, result);
    }

    /**
     * The format must accept both spellings: with or without the trailing comma.
     * They must produce the same result.
     */
    @Test
    public void processAllTrailingCommaIsOptional() {
        int[] without = FlexMargins.processAll(CONTEXT, "10, 20, 30, 40", false, 0, 0, 0, 0);
        int[] with = FlexMargins.processAll(CONTEXT, "10, 20, 30, 40,", false, 0, 0, 0, 0);

        assertArrayEquals("La virgule finale ne doit plus rien changer.", without, with);
        assertArrayEquals(new int[]{10, 20, 30, 40}, with);
    }

    @Test
    public void processAllIgnoresSpaces() {
        int[] result = FlexMargins.processAll(CONTEXT, "  10 ,  20 ,30,  40  ", false, 0, 0, 0, 0);

        assertArrayEquals(new int[]{10, 20, 30, 40}, result);
    }

    /** A strategy in the fifth position inverts the computation's polarity (see FlexMargins). */
    @Test
    public void processAllAcceptsOptionalStrategyAsFifthSegment() {
        int[] result = FlexMargins.processAll(CONTEXT, "10, 20, 30, 40, Inverted", false, 0, 0, 0, 0);

        assertArrayEquals(new int[]{10, 20, 30, 40}, result);
    }

    @Test
    public void processAllAddsToCurrentValuesWhenRequested() {
        int[] result = FlexMargins.processAll(CONTEXT, "10, 20, 30, 40", true, 100, 100, 100, 100);

        assertArrayEquals(new int[]{110, 120, 130, 140}, result);
    }

    @Test
    public void processAllReplacesCurrentValuesByDefault() {
        int[] result = FlexMargins.processAll(CONTEXT, "10, 20, 30, 40", false, 100, 100, 100, 100);

        assertArrayEquals(new int[]{10, 20, 30, 40}, result);
    }

    @Test
    public void processAllEmptySegmentLeavesValueUnchanged() {
        int[] result = FlexMargins.processAll(CONTEXT, "10, , 30, 40", false, 0, 55, 0, 0);

        assertEquals("Un segment vide doit conserver la valeur courante.",
                55, result[1]);
        assertEquals(10, result[0]);
        assertEquals(30, result[2]);
        assertEquals(40, result[3]);
    }

    @Test
    public void processAllRejectsFewerThanFourValuesWithExplicitMessage() {
        try {
            FlexMargins.processAll(CONTEXT, "10, 20, 30", false, 0, 0, 0, 0);
            fail("A three-value format must be rejected.");
        } catch (FlexValueFormatException expected) {
            String message = expected.getMessage();
            assertTrue("The message must state how many values were received (3).",
                    message.contains("3"));
            assertTrue("The message must recall the expected format.",
                    message.contains("left"));
        }
    }

    /**
     * An extra segment after the strategy is ignored: only the first four segments
     * and the fifth (the strategy) are read.
     */
    @Test
    public void processAllIgnoresSegmentsBeyondTheStrategy() {
        int[] result = FlexMargins.processAll(CONTEXT, "10, 20, 30, 40, Inverted, extra", false, 0, 0, 0, 0);

        assertArrayEquals(new int[]{10, 20, 30, 40}, result);
    }

    @Test
    public void processAllRejectsNullValues() {
        try {
            FlexMargins.processAll(CONTEXT, null, false, 0, 0, 0, 0);
            fail("A null string must be rejected.");
        } catch (FlexValueFormatException expected) {
            assertTrue(expected.getMessage().contains("null"));
        }
    }

    @Test
    public void processAllRejectsNonNumericValueWithExplicitMessage() {
        try {
            FlexMargins.processAll(CONTEXT, "abc, 20, 30, 40", false, 0, 0, 0, 0);
            fail("A non-numeric value must be rejected.");
        } catch (FlexValueFormatException expected) {
            assertTrue("Le message doit citer la valeur fautive.",
                    expected.getMessage().contains("abc"));
        }
    }

    /** A percentage value is relative to the dimension supplied by the context. */
    @Test
    public void processAllPercentValuesAreRelativeToContextDimension() {
        // Test context: 1000 × 2000, density 1.0.
        // The "L" token (left side) refers to the width: 50% of 1000 = 500.
        int[] result = FlexMargins.processAll(CONTEXT, "50%L, 0, 0, 0", false, 0, 0, 0, 0);

        assertEquals(500, result[0]);
    }

    /* *** Per-side strategy — the multi-state behaviour to protect *** */

    /**
     * The strategy may name a <b>subset</b> of sides ({"TRL"}, length ≤ 4). A side
     * that is absent is not inverted, while the others are. Each side therefore has its
     * own state, not a global one.
     */
    @Test
    public void strategyAppliesPerSideNotGlobally() {
        int[] noStrategy = FlexMargins.processAll(CONTEXT, "0, 10%T, 0, 10%B", false, 0, 0, 0, 0);
        int[] partialStrategy = FlexMargins.processAll(CONTEXT, "0, 10%T, 0, 10%B, TRL", false, 0, 0, 0, 0);

        // Without a strategy: T and B refer to the height (2000) → 200.
        assertEquals(200, noStrategy[1]);
        assertEquals(200, noStrategy[3]);

        // "TRL" names T but not B:
        //   - T is inverted and refers to the width (1000) → 100;
        //   - B is absent from the strategy, hence not inverted → it stays on the height → 200.
        assertEquals("Side T, present in the strategy, must be inverted.",
                100, partialStrategy[1]);
        assertEquals("Side B, absent from the strategy, must not be inverted.",
                200, partialStrategy[3]);
    }

    /** Symmetric case: "TBL" names B and inverts it, leaving R untouched. */
    @Test
    public void strategyWithThreeLettersInvertsOnlyTheNamedSides() {
        int[] result = FlexMargins.processAll(CONTEXT, "0, 0, 10%R, 10%B, TBL", false, 0, 0, 0, 0);

        // R is absent from "TBL" → not inverted, refers to the width (1000) → 100.
        assertEquals(100, result[2]);
        // B is present → inverted, refers to the width (1000) → 100.
        assertEquals(100, result[3]);
    }

    /**
     * <b>Both inversion directions, across all four sides.</b>
     *
     * <p>This test covers the token → dimension mapping, when the strategy applies
     * (branche {@code WITH_PERCENT_MR} d'origine) :
     * <ul>
     *   <li>a <b>horizontal</b> side (L) moves to the <b>height</b>;</li>
     *   <li>a <b>vertical</b> side (T) moves to the <b>width</b>.</li>
     * </ul>
     *
     * <p>That double check is exactly what was missing: earlier tests
     * never exercised a <b>horizontal</b> token with an applied strategy, which let
     * a naming ambiguity about polarity slip through ({@code strategyApplies} that actually
     * returned {@code !emptyStrategy}).
     *
     * <p>Contexte de test : 1000 × 2000 px.
     */
    @Test
    public void strategyInversionSwapsReferenceDimensionForBothDirections() {
        int[] result = FlexMargins.processAll(CONTEXT, "10%L, 10%T, 0, 0, TRL", false, 0, 0, 0, 0);

        // WITH_PERCENT_MR branch (strategy applied for those two sides):
        //   - jeton L (horizontal) → dynamicH → 10 % de 2000 = 200 ;
        //   - jeton T (vertical)   → dynamicW → 10 % de 1000 = 100.
        assertEquals("With a strategy applied, a horizontal side uses the height.",
                200, result[0]);
        assertEquals("With a strategy applied, a vertical side uses the width.",
                100, result[1]);
    }

    /**
     * <b>Both directions without a strategy</b>: the normal mapping, opposite to
     * {@link #strategyInversionSwapsReferenceDimensionForBothDirections()}.
     */
    @Test
    public void withoutStrategyBothDirectionsUseTheirOwnDimension() {
        int[] result = FlexMargins.processAll(CONTEXT, "10%L, 10%T, 0, 0", false, 0, 0, 0, 0);

        // Without a strategy: L → width (1000) and T → height (2000).
        assertEquals("A horizontal side uses the width.", 100, result[0]);
        assertEquals("A vertical side uses the height.", 200, result[1]);
    }

    /**
     * The {@code Inverted} keyword inverts <b>all four</b> sides, horizontal and vertical — which
     * is what distinguishes its state from a partial strategy's.
     */
    @Test
    public void invertedKeywordInvertsBothDirections() {
        int[] result = FlexMargins.processAll(CONTEXT, "10%L, 10%T, 0, 0, Inverted", false, 0, 0, 0, 0);

        // Same expectations as the partial strategy, reached through the other path.
        assertEquals("Inverted turns the horizontal side onto the height.", 200, result[0]);
        assertEquals("Inverted turns the vertical side onto the width.", 100, result[1]);
    }

    /** The Inverted keyword inverts all four sides, whatever the token length. */
    @Test
    public void invertedKeywordInvertsEverySide() {
        int[] result = FlexMargins.processAll(CONTEXT, "10%L, 10%T, 10%R, 10%B, Inverted", false, 0, 0, 0, 0);

        // Without inversion, L and R would refer to the width (100), T and B to the height (200).
        // With "Inverted", the references are swapped.
        assertEquals(200, result[0]);
        assertEquals(100, result[1]);
        assertEquals(200, result[2]);
        assertEquals(100, result[3]);
    }

    /* *** processOnly *** */

    @Test
    public void processOnlyTargetsOnlyTheTokensPresent() {
        // Only the top (T) and the right (R) are targeted.
        int[] result = FlexMargins.processOnly(CONTEXT, "10T, 20R", null, false, 1, 2, 3, 4);

        assertArrayEquals(new int[]{1, 10, 20, 4}, result);
    }

    @Test
    public void processOnlyWithSingleSegmentTargetsThatSideOnly() {
        int[] result = FlexMargins.processOnly(CONTEXT, "15B", null, false, 1, 2, 3, 4);

        assertArrayEquals(new int[]{1, 2, 3, 15}, result);
    }

    @Test
    public void processOnlyWithNoMatchingTokenLeavesEverythingUnchanged() {
        int[] result = FlexMargins.processOnly(CONTEXT, "10X, 20Y", null, false, 1, 2, 3, 4);

        assertArrayEquals("Aucun jeton reconnu : rien ne doit changer.", new int[]{1, 2, 3, 4}, result);
    }

    @Test
    public void processOnlyAddsToCurrentValuesWhenRequested() {
        int[] result = FlexMargins.processOnly(CONTEXT, "10T, 20R", null, true, 0, 100, 100, 0);

        assertArrayEquals(new int[]{0, 110, 120, 0}, result);
    }

    /** Both tokens may target the same side: the second segment wins. */
    @Test
    public void processOnlySecondSegmentWinsForSameToken() {
        int[] result = FlexMargins.processOnly(CONTEXT, "10T, 99T", null, false, 0, 0, 0, 0);

        assertEquals(10, result[1]);
    }

    /* *** process *** */

    @Test
    public void processWithoutPercentReturnsFixedValueAtDensityOne() {
        int value = FlexMargins.process(CONTEXT, 0, "80", null, "L", false);

        assertEquals(80, value);
    }

    @Test
    public void processStripsLetterSuffixBeforeParsing() {
        int value = FlexMargins.process(CONTEXT, 0, "80L", null, "L", false);

        assertEquals(80, value);
    }

    @Test
    public void processOnEmptyValueReturnsCurrent() {
        assertEquals(42, FlexMargins.process(CONTEXT, 42, "", null, "L", false));
        assertEquals(42, FlexMargins.process(CONTEXT, 42, null, null, "L", false));
    }
}
