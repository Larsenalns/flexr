// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrcoremodule.observable;

/**
 * Contract of an observable state object: it keeps consumers and knows how to notify them.
 *
 * <h2>The three scopes, and what they mean</h2>
 *
 * <p>The scope of a subscription answers one question: <b>who releases it, and when</b>?
 *
 * <ul>
 *   <li>{@link #subscribeActivityScope(FlexConsumer)} — lives as long as the Activity. Released by
 *       {@code FlexActivity.onDestroy()};</li>
 *   <li>{@link #subscribeFragScope(FlexConsumer)} — lives as long as the Fragment, hence as long as the
 *       screen. This is the scope of everything <b>visual</b>: a view, a row of a list, a screen block.
 *       Released by {@code FlexFragment.onDestroyView()};</li>
 *   <li>{@link #subscribeBackScope(FlexConsumer)} — for <b>background work</b> whose life is tied
 *       neither to the Activity nor to the Fragment: it may carry on after the Activity is destroyed,
 *       like a foreground or background service, a synchronisation or a download. See
 *       {@link #subscribeBackScope(FlexConsumer)} for what does <b>not</b> belong there.</li>
 * </ul>
 *
 * <h2>Releasing a single consumer</h2>
 *
 * <p>{@link #unSubscribe(FlexConsumer)} removes exactly one consumer, whichever scope it was
 * subscribed in. That is what a recycled list row needs: detach itself without waiting for its
 * Fragment to die, and without touching the other rows of the same screen.
 */
public interface Provider {

    /**
     * Subscribes until the Activity dies.
     *
     * @param flexConsumer the observer; {@code null} is ignored
     */
    void subscribeActivityScope(FlexConsumer flexConsumer);

    /**
     * Subscribes until the screen's view is destroyed — the nominal scope for anything visual.
     *
     * @param flexConsumer the observer; {@code null} is ignored
     */
    void subscribeFragScope(FlexConsumer flexConsumer);

    /**
     * Subscribes for the duration of a <b>background treatment</b>, which may outlive the Activity.
     *
     * <p>This scope is <b>not</b> meant for elements whose life follows a screen — a view, a row of a
     * list. Those subscribe through {@link #subscribeFragScope(FlexConsumer)}; otherwise they would be
     * released too late, or never, when the Activity outlives the screen.
     *
     * @param flexConsumer the observer; {@code null} is ignored
     */
    void subscribeBackScope(FlexConsumer flexConsumer);

    /**
     * Releases every consumer of the Activity scope whose identity matches {@code scopeId}.
     *
     * @param scopeId identity of the Activity; {@code null} does nothing
     */
    void unSubscribeActivityScope(String scopeId);

    /**
     * Releases every consumer of the screen scope whose identity matches {@code scopeId}.
     *
     * @param scopeId identity of the Fragment; {@code null} does nothing
     */
    void unSubscribeFragScope(String scopeId);

    /**
     * Releases every consumer of the background scope whose identity matches {@code scopeId}.
     *
     * @param scopeId identity of the background work; {@code null} does nothing
     */
    void unSubscribeBackgroundScope(String scopeId);

    /**
     * Releases <b>one</b> consumer, in all three scopes.
     *
     * <p>The comparison is made by <b>object identity</b>, not by scope identifier: this is the exact
     * release, usable when a single row must detach (recycling of a {@code RecyclerView.ViewHolder})
     * without disturbing the other subscribers of the same screen.
     *
     * @param flexConsumer the observer to release; {@code null} does nothing
     */
    void unSubscribe(FlexConsumer flexConsumer);

    /**
     * Notifies every consumer of every scope.
     *
     * <p>Each consumer receives {@link FlexConsumer#synchronizeNotifyDataChanged(FlexProvider)}, which
     * is free to schedule the work — the default implementation does, off the main thread.
     */
    void notifyConsumers();
}
