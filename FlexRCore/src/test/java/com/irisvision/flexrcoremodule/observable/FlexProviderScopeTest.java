package com.irisvision.flexrcoremodule.observable;

import static org.junit.Assert.assertEquals;

import com.irisvision.flexrcoremodule.core.ScopeIdentity;
import com.irisvision.flexrcoremodule.enums.FlexScope;

import org.junit.Test;

/**
 * The <b>subscription release</b> invariant, the one row recycling relies on in
 * {@code #ui/#list}.
 *
 * <p>What the library promises: a list row that subscribes to a {@code FlexProvider} is
 * released <b>before every new binding</b> and on every recycle, without touching the other rows of
 * the same screen. These tests check the release mechanism itself — {@code unSubscribe(...)} and the
 * per-scope purge — that is, exactly what the adapter calls.
 *
 * <p><b>Known limit</b>: real scrolling (10 000 rows, recycled holders) needs a device or an
 * emulator, because a {@code FlexItem} is a {@code RecyclerView.ViewHolder}. What is verifiable here
 * is that the scope counter returns to zero — so no subscription can accumulate.
 */
public class FlexProviderScopeTest {

    private static final String FRAGMENT_A = "fragmentA";
    private static final String FRAGMENT_B = "fragmentB";
    private static final String ACTIVITY_A = "activityA";

    private static FlexProvider provider(){
        return new FlexProvider() {
            @Override
            protected String providerFlag() {
                return "test";
            }
        };
    }

    /**
     * Test consumer: identity {@code "frag:activity"}, <b>synchronous</b> notification (to count
     * without waiting for the executor), and a call counter.
     */
    private static final class TestConsumer implements FlexConsumer {

        private final String fragment;
        private final String activity;
        private int notifications;

        TestConsumer(String fragment, String activity) {
            this.fragment = fragment;
            this.activity = activity;
        }

        @Override
        public void notifyDataChanged(FlexProvider flexProvider) {
            this.notifications++;
        }

        @Override
        public void synchronizeNotifyDataChanged(FlexProvider flexProvider) {
            notifyDataChanged(flexProvider);            // synchronous: this is what is under test
        }

        @Override
        public ScopeIdentity identity() {
            return new ScopeIdentity() {
                @Override
                public void identify() {
                    // identity frozen at construction time
                }

                @Override
                public String ref(String scope) {
                    if (FlexScope.FlexFragment.name().equals(scope)) {
                        return fragment;
                    }
                    if (FlexScope.FlexActivity.name().equals(scope)) {
                        return activity;
                    }
                    return "";
                }
            };
        }
    }

    @Test
    public void uneSouscriptionEstVisibleDansSonScope() {
        FlexProvider provider = provider();
        TestConsumer consumer = new TestConsumer(FRAGMENT_A, ACTIVITY_A);

        assertEquals(0, provider.size(FlexScope.FlexFragment));

        provider.subscribeFragScope(consumer);

        assertEquals("The consumer must be counted in the Fragment scope.",
                1, provider.size(FlexScope.FlexFragment));
        assertEquals("The Activity scope must not be touched.",
                0, provider.size(FlexScope.FlexActivity));
    }

    @Test
    public void libererUnConsommateurVideLeScope() {
        FlexProvider provider = provider();
        TestConsumer consumer = new TestConsumer(FRAGMENT_A, ACTIVITY_A);

        provider.subscribeFragScope(consumer);
        provider.unSubscribe(consumer);

        assertEquals("After release, no subscription may remain.",
                0, provider.size(FlexScope.FlexFragment));
    }

    @Test
    public void libererUnConsommateurNeTouchePasLesAutres() {
        FlexProvider provider = provider();
        TestConsumer ligne1 = new TestConsumer(FRAGMENT_A, ACTIVITY_A);
        TestConsumer ligne2 = new TestConsumer(FRAGMENT_A, ACTIVITY_A);

        provider.subscribeFragScope(ligne1);
        provider.subscribeFragScope(ligne2);

        provider.unSubscribe(ligne1);

        assertEquals("Only the released row may disappear from the scope.",
                1, provider.size(FlexScope.FlexFragment));

        provider.notifyConsumers();

        assertEquals("The released row must no longer be notified.", 0, ligne1.notifications);
        assertEquals("The other row must keep being notified.", 1, ligne2.notifications);
    }

    @Test
    public void uneDoubleLiberationEstSansEffet() {
        FlexProvider provider = provider();
        TestConsumer consumer = new TestConsumer(FRAGMENT_A, ACTIVITY_A);

        provider.subscribeFragScope(consumer);
        provider.unSubscribe(consumer);
        provider.unSubscribe(consumer);             // la seconde ne doit rien casser

        assertEquals(0, provider.size(FlexScope.FlexFragment));
    }

    @Test
    public void uneDoubleSouscriptionNeCreePasDeDoublon() {
        FlexProvider provider = provider();
        TestConsumer consumer = new TestConsumer(FRAGMENT_A, ACTIVITY_A);

        provider.subscribeFragScope(consumer);
        provider.subscribeFragScope(consumer);

        assertEquals("Idempotent subscription: the row must not be notified twice.",
                1, provider.size(FlexScope.FlexFragment));
    }

    @Test
    public void laPurgeParScopeIdLibereToutLeFragment() {
        FlexProvider provider = provider();
        TestConsumer ligneA1 = new TestConsumer(FRAGMENT_A, ACTIVITY_A);
        TestConsumer ligneA2 = new TestConsumer(FRAGMENT_A, ACTIVITY_A);
        TestConsumer ligneB = new TestConsumer(FRAGMENT_B, ACTIVITY_A);

        provider.subscribeFragScope(ligneA1);
        provider.subscribeFragScope(ligneA2);
        provider.subscribeFragScope(ligneB);

        // C'est cette purge qu'appelle FlexFragment.onDestroyView().
        provider.unSubscribeFragScope(FRAGMENT_A);

        assertEquals("The destroyed fragment must have no subscriber left.",
                1, provider.size(FlexScope.FlexFragment));
        assertEquals("The Activity scope is not touched by a Fragment purge.",
                0, provider.size(FlexScope.FlexActivity));

        provider.notifyConsumers();

        assertEquals(0, ligneA1.notifications);
        assertEquals(0, ligneA2.notifications);
        assertEquals("The neighbouring fragment must stay subscribed.", 1, ligneB.notifications);
    }

    @Test
    public void laNotificationAtteintTousLesAbonnes() {
        FlexProvider provider = provider();
        TestConsumer consumer = new TestConsumer(FRAGMENT_A, ACTIVITY_A);
        TestConsumer consumerActivity = new TestConsumer(FRAGMENT_A, ACTIVITY_A);

        provider.subscribeFragScope(consumer);
        provider.subscribeActivityScope(consumerActivity);

        provider.notifyConsumers();

        assertEquals(1, consumer.notifications);
        assertEquals(1, consumerActivity.notifications);
    }
}
