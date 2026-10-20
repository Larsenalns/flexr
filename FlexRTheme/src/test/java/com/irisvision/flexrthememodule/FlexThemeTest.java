package com.irisvision.flexrthememodule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests of the theme <b>mechanism</b>: registration, notification, the {@code isThemeAutoApply}
 * filter, per-scope unsubscription, unknown theme, and re-entrance protection.
 *
 * <h2>Pourquoi ces tests tournent sans Android</h2>
 *
 * <p>Le module active {@code returnDefaultValues = true} : dans une JVM de test,
 * {@code Looper.getMainLooper()} rend {@code null}. {@code FlexTheme.appChangeTheme(...)} applique
 * the switch happens <b>in place</b> (see the method's javadoc), which makes the behaviour
 * <b>synchronous and deterministic</b>: these tests check the real mechanism, with no screen, no
 * emulator and no Robolectric.
 *
 * <p>What is <b>not</b> covered here, and cannot be without a device: delivery
 * effective sur le thread principal via {@code FlexThreadMain.runOnMain(...)}, et le rendu des
 * couleurs ({@code Color.parseColor} rend {@code 0} dans ce mode).
 */
public class FlexThemeTest {

    /** Test observer: counts its notifications and may replay a theme (re-entrance). */
    private static final class FakeThematic implements Thematic {

        private final String scopeId;
        private final boolean autoApply;
        private int changes;
        private String themeOnChange;

        FakeThematic(String scopeId, boolean autoApply) {
            this.scopeId = scopeId;
            this.autoApply = autoApply;
        }

        @Override public void identify() { }

        @Override public String ref(String scope) { return scopeId; }

        @Override public boolean isThemeAutoApply() { return autoApply; }

        @Override public void ifThemeChange() {
            changes++;
            if (themeOnChange != null) {
                // Deliberately pathological: an observer that switches theme while
                // a theme is being applied to it.
                FlexTheme.appChangeTheme(themeOnChange);
            }
        }
    }

    private final List<FakeThematic> registered = new ArrayList<>();
    private String initialTheme;

    @Before
    public void setUp() {
        this.initialTheme = FlexTheme.currentTheme;
    }

    @After
    public void tearDown() {
        // Les listes d'observateurs et la palette sont STATIQUES : on rend le terrain propre pour
        // other tests, exercising per-scope unsubscription on the way.
        for (FakeThematic thematic : registered) {
            FlexTheme.clearObsFragScope(thematic.ref("FlexFragment"));
            FlexTheme.clearObsActivityScope(thematic.ref("FlexActivity"));
        }
        registered.clear();
        FlexTheme.currentTheme = initialTheme;
    }

    private FakeThematic registerOnFragmentScope(String themeKey, boolean autoApply) {
        FlexTheme.pushTheme(themeKey, List.of("#FF112233", "#FF445566"));
        FakeThematic thematic = new FakeThematic("frag:" + themeKey, autoApply);
        FlexTheme.subscribeFragScope(thematic);
        registered.add(thematic);
        return thematic;
    }

    @Test
    public void unObservateurInscritEstNotifie() {
        FakeThematic thematic = registerOnFragmentScope("t_informe", true);

        FlexTheme.appChangeTheme("t_informe");

        // Sans thread principal (JVM de test), l'application est faite sur place : l'observateur est
        // so already notified when the call returns.
        assertEquals("the registered observer must be notified once", 1, thematic.changes);
        assertEquals("the current theme must have switched", "t_informe", FlexTheme.currentTheme);
    }

    @Test
    public void unObservateurSansAutoApplyNEstPasNotifie() {
        FakeThematic thematic = registerOnFragmentScope("t_inerte", false);

        FlexTheme.appChangeTheme("t_inerte");

        assertEquals("isThemeAutoApply() == false doit rendre la vue inerte", 0, thematic.changes);
        assertEquals("the current theme switches anyway", "t_inerte", FlexTheme.currentTheme);
    }

    @Test
    public void unThemeInconnuNeNotifiePasEtNeBasculePas() {
        FakeThematic thematic = registerOnFragmentScope("t_connu", true);
        String avant = FlexTheme.currentTheme;

        FlexTheme.appChangeTheme("t_inexistant");

        assertEquals("no notification for an undeclared theme", 0, thematic.changes);
        assertEquals("the current theme must not change", avant, FlexTheme.currentTheme);
    }

    @Test
    public void unObservateurDesinscritNEstPlusNotifie() {
        FakeThematic thematic = registerOnFragmentScope("t_desinscrit", true);

        // C'est exactement ce que fait FlexFragment.onDestroyView() : la purge par scope.
        FlexTheme.clearObsFragScope(thematic.ref("FlexFragment"));

        FlexTheme.appChangeTheme("t_desinscrit");

        assertEquals("after the purge, the view must receive nothing more", 0, thematic.changes);
    }

    @Test
    public void laReentranceNeProvoquePasDeCascade() {
        FakeThematic thematic = registerOnFragmentScope("t_externe", true);
        FlexTheme.pushTheme("t_interne", List.of("#FF000000"));
        thematic.themeOnChange = "t_interne";

        FlexTheme.appChangeTheme("t_externe");

        assertEquals("the observer must be notified only once", 1, thematic.changes);
        assertEquals("the nested switch theme must be ignored",
                "t_externe", FlexTheme.currentTheme);
        assertNotEquals("the nested switch must not apply",
                "t_interne", FlexTheme.currentTheme);
    }

    @Test
    public void leThemeCourantEstLisibleApresBascule() {
        registerOnFragmentScope("t_lecture", true);

        FlexTheme.appChangeTheme("t_lecture");

        // load() on a declared palette does not throw: that is what guarantees the indexes used by
        // views stay valid after a switch (2 colours declared above).
        FlexTheme.load(0);
        FlexTheme.load(1);
        assertTrue("the current theme is the requested one", "t_lecture".equals(FlexTheme.currentTheme));
        assertFalse("another theme's palette must not be confused with it",
                "un_autre".equals(FlexTheme.currentTheme));
    }
}
