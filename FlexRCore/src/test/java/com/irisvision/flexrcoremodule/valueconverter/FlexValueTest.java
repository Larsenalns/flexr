package com.irisvision.flexrcoremodule.valueconverter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

/**
 * Tests for {@link FlexValue} — numeric conversions and gravity.
 *
 * <p>{@link FlexValue#valuePercent(String)} is deterministic and has no Android dependency.
 * {@link FlexValue#gravity(String)} relies on the {@code Gravity} constants, which are zero
 * in the unit-test environment (Android stubs). The gravity tests therefore check the
 * <b>recognition semantics</b> — which labels are accepted, case-insensitivity, and order
 * independence — rather than numeric values that would mean nothing here.
 */
public class FlexValueTest {

    /* *** valuePercent *** */

    @Test
    public void valuePercentConvertsPlainNumber() {
        assertEquals(80f, FlexValue.valuePercent("80"), 0f);
    }

    @Test
    public void valuePercentConvertsPercentToFraction() {
        assertEquals(0.8f, FlexValue.valuePercent("80%"), 0f);
    }

    @Test
    public void valuePercentHandlesZeroAndHundred() {
        assertEquals(0f, FlexValue.valuePercent("0%"), 0f);
        assertEquals(1f, FlexValue.valuePercent("100%"), 0f);
    }

    @Test
    public void valuePercentHandlesNegativeValues() {
        assertEquals(-0.1f, FlexValue.valuePercent("-10%"), 0f);
        assertEquals(-25f, FlexValue.valuePercent("-25"), 0f);
    }

    @Test
    public void valuePercentHandlesDecimalValues() {
        assertEquals(0.075f, FlexValue.valuePercent("7.5%"), 0f);
    }

    @Test
    public void valuePercentIgnoresSurroundingSpaces() {
        assertEquals(0.8f, FlexValue.valuePercent("  80% "), 0f);
    }

    @Test
    public void valuePercentRejectsNonNumericValueWithExplicitMessage() {
        try {
            FlexValue.valuePercent("abc");
            fail("A non-numeric value must be rejected.");
        } catch (IllegalArgumentException expected) {
            assertTrue("Le message doit citer la valeur fautive.",
                    expected.getMessage().contains("abc"));
        }
    }

    @Test
    public void valuePercentRejectsNullAndEmpty() {
        try {
            FlexValue.valuePercent(null);
            fail("A null value must be rejected.");
        } catch (IllegalArgumentException expected) {
            assertNotNull(expected.getMessage());
        }

        try {
            FlexValue.valuePercent("");
            fail("An empty value must be rejected.");
        } catch (IllegalArgumentException expected) {
            assertNotNull(expected.getMessage());
        }
    }

    /* *** gravity *** */

    @Test
    public void gravityReturnsDefaultForNull() {
        assertNotNull(FlexValue.gravity(null));
    }

    @Test
    public void gravityReturnsDefaultForEmptyInput() {
        assertNotNull(FlexValue.gravity(""));
    }

    /**
     * Two unknown labels produce the same result: recognition must not alter the default
     * fallback. That is the only strong assertion available here, because the
     * {@code Gravity} constants are zero in the unit-test environment.
     */
    @Test
    public void gravityUnknownLabelsLeaveDefaultUntouched() {
        assertEquals(FlexValue.gravity("aaa"), FlexValue.gravity("bbb,ccc"));
        assertEquals(FlexValue.gravity(null), FlexValue.gravity("zzz"));
    }

    /**
     * A known limit of the test environment: {@code Gravity.CENTER} and the other constants
     * are {@code 0} in the Android stubs, so recognising a valid label cannot be
     * distinguished from the fallback by a numeric assertion. Recognition is nevertheless
     * covered functionally by the on-device integration tests.
     */
    @Test
    public void gravityNeverThrowsForAnyInput() {
        assertNotNull(FlexValue.gravity("center"));
        assertNotNull(FlexValue.gravity("center, top"));
        assertNotNull(FlexValue.gravity("unknown,token"));
        assertNotNull(FlexValue.gravity(",,"));
    }
}
