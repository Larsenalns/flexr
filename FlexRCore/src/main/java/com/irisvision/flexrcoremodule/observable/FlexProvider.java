// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrcoremodule.observable;

import com.irisvision.flexrcoremodule.enums.FlexScope;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Base class of every observable state object in FlexR.
 *
 * <h2>What a provider is</h2>
 *
 * <p>A provider holds a piece of application state — a session, a counter, a filter — and the list of
 * consumers that must be told when it changes. The application declares its providers once, in
 * {@link FlexReactive}, and any view subscribes to the ones it depends on.
 *
 * <h2>Three lifetimes, three lists</h2>
 *
 * <p>Consumers do not all live as long as each other, so a provider keeps one list per
 * {@link FlexScope}:
 *
 * <ul>
 *   <li>{@link FlexScope#FlexActivity} — released when the Activity is destroyed;</li>
 *   <li>{@link FlexScope#FlexFragment} — released when the screen's view is destroyed. This is the
 *       scope of everything <b>visual</b>: a view, a row of a list, a screen block;</li>
 *   <li>{@link FlexScope#FlexBackground} — for <b>background work</b> whose life is tied neither to the
 *       Activity nor to the Fragment, such as a service or a download. Released when that work ends. A
 *       row of a list does <b>not</b> belong here: its life is the screen's.</li>
 * </ul>
 *
 * <h2>Bulk release versus exact release</h2>
 *
 * <p>When a screen dies, the whole list is released at once —
 * {@link #unSubscribeFragScope(String)} and its siblings, which is what {@code FlexFragment} and
 * {@code FlexActivity} call. When a single row is recycled, only that consumer is released:
 * {@link #unSubscribe(FlexConsumer)} compares by object identity, so it never touches the others.
 *
 * <h2>Threading</h2>
 *
 * <p>The three lists are {@link CopyOnWriteArrayList}s, and that is not an optimisation.
 * Subscriptions arrive on the main thread while notifications are delivered from
 * {@link FlexNotificationExecutor}, and a consumer may unsubscribe from inside its own notification.
 * Copy-on-write makes an in-progress iteration immune to such a change, where a plain
 * {@code ArrayList} would raise {@code ConcurrentModificationException} — which is exactly what the
 * purge of a screen used to do.
 *
 * <h2>Writing a provider</h2>
 *
 * <p>A concrete provider only has to give itself a key and to notify when its state changes:
 *
 * <pre>{@code
 * public final class SessionProvider extends FlexProvider {
 *     private String user;
 *
 *     @Override protected String providerFlag() { return "session"; }
 *
 *     public void user(String value) {
 *         this.user = value;
 *         notifyConsumers();
 *     }
 * }
 * }</pre>
 */
public abstract class FlexProvider implements Provider {

    private List<FlexConsumer> fragScope;
    private List<FlexConsumer> activityScope;
    private List<FlexConsumer> backScope;

    public FlexProvider() {
    }

    /* *** Subscribing *** */

    // Created lazily: a provider that nobody observes costs nothing.
    @Override
    public void subscribeActivityScope(FlexConsumer flexConsumer) {
        if (flexConsumer == null) return;
        if (activityScope == null) activityScope = new CopyOnWriteArrayList<>();
        subscribeProcess(activityScope, flexConsumer);
    }

    @Override
    public void subscribeFragScope(FlexConsumer flexConsumer) {
        if (flexConsumer == null) return;
        if (fragScope == null) fragScope = new CopyOnWriteArrayList<>();
        subscribeProcess(fragScope, flexConsumer);
    }

    @Override
    public void subscribeBackScope(FlexConsumer flexConsumer) {
        if (flexConsumer == null) return;
        if (backScope == null) backScope = new CopyOnWriteArrayList<>();
        subscribeProcess(backScope, flexConsumer);
    }

    /* *** Releasing *** */

    @Override
    public void unSubscribeActivityScope(String scopeId) {
        unSubscribeProcess(activityScope, FlexScope.FlexActivity.name(), scopeId);
    }

    @Override
    public void unSubscribeFragScope(String scopeId) {
        unSubscribeProcess(fragScope, FlexScope.FlexFragment.name(), scopeId);
    }

    @Override
    public void unSubscribeBackgroundScope(String scopeId) {
        unSubscribeProcess(backScope, FlexScope.FlexBackground.name(), scopeId);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Implemented by removing the consumer from the three lists, by identity.
     */
    @Override
    public void unSubscribe(FlexConsumer flexConsumer) {
        if (flexConsumer == null) {
            return;
        }
        removeConsumer(activityScope, flexConsumer);
        removeConsumer(fragScope, flexConsumer);
        removeConsumer(backScope, flexConsumer);
    }

    private void removeConsumer(List<FlexConsumer> flexConsumerList, FlexConsumer flexConsumer) {
        if (flexConsumerList == null) {
            return;
        }
        // removeIf mutates the list in place, which stays safe even while a notification is being
        // iterated over, because the list is copy-on-write.
        flexConsumerList.removeIf(candidate -> candidate == flexConsumer);
    }

    @Override
    public final void notifyConsumers() {
        notifyProcess(activityScope);
        notifyProcess(fragScope);
        notifyProcess(backScope);
    }

    private void subscribeProcess(List<FlexConsumer> flexConsumerList, FlexConsumer flexConsumer) {
        // Deliberately idempotent: subscribing twice does not create a duplicate.
        if (!flexConsumerList.contains(flexConsumer)) {
            flexConsumerList.add(flexConsumer);
        }
    }

    /**
     * Removes the consumers whose scope identity matches {@code scopeId}.
     *
     * <p>The list is modified <b>in place</b>, never replaced: replacing the field would open a window
     * during which a concurrent notification could work on the previous list and miss an unsubscription.
     */
    private void unSubscribeProcess(List<FlexConsumer> flexConsumerList, String scope, String scopeId) {
        if (flexConsumerList == null || scopeId == null) {
            return;
        }
        flexConsumerList.removeIf(consumer -> scopeId.equals(consumer.identity().ref(scope)));
    }

    private void notifyProcess(List<FlexConsumer> flexConsumerList) {
        if (flexConsumerList == null) {
            return;
        }
        // Copy-on-write gives a stable snapshot: the loop is unaffected if a consumer subscribes or
        // unsubscribes during its own notification.
        for (FlexConsumer consumer : flexConsumerList) {
            consumer.synchronizeNotifyDataChanged(this);
        }
    }

    /**
     * Key under which this provider is registered in {@link FlexReactive}.
     *
     * @return a non-null, stable key, unique among the application's providers
     */
    protected abstract String providerFlag();

    /** {@inheritDoc} */
    public String name() {
        return providerFlag();
    }

    /**
     * Number of consumers subscribed to a scope, by name.
     *
     * <p>Mainly a diagnostic tool: it is how a test — or {@code FlexReactive.info()} — proves that a
     * screen released its subscriptions.
     *
     * @param scope one of the names of {@link FlexScope}; {@code null} yields {@code 0}
     * @return the number of subscribers of that scope
     * @throws IllegalArgumentException if {@code scope} is not one of the three known names
     */
    public final int size(String scope) {
        List<FlexConsumer> target;
        if (scope == null) {
            target = null;                                  // unknown perimeter: see below
        } else if (scope.equals(FlexScope.FlexFragment.name())) {
            target = fragScope;
        } else if (scope.equals(FlexScope.FlexActivity.name())) {
            target = activityScope;
        } else if (scope.equals(FlexScope.FlexBackground.name())) {
            target = backScope;
        } else {
            // An unknown name used to return the size of the background scope: a plausible and wrong
            // number. It now fails, and says what it expects.
            throw new IllegalArgumentException(
                    "Unknown scope: \"" + scope + "\". Expected one of: "
                            + FlexScope.FlexFragment.name() + ", " + FlexScope.FlexActivity.name()
                            + ", " + FlexScope.FlexBackground.name() + ".");
        }
        return target == null ? 0 : target.size();
    }

    /**
     * Number of consumers subscribed to a scope, by constant.
     *
     * <p>The safe variant of {@link #size(String)}: the compiler checks the value.
     *
     * @param scope the scope; {@code null} yields {@code 0}
     * @return the number of subscribers of that scope
     */
    public final int size(FlexScope scope) {
        if (scope == null) {
            return 0;
        }
        return size(scope.name());
    }
}
