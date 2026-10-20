// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrcoremodule.ui.list.flexadaptor;

import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.irisvision.flexrcoremodule.core.FlexContextInterface;
import com.irisvision.flexrcoremodule.core.FlexView;
import com.irisvision.flexrcoremodule.core.ScopeIdentity;
import com.irisvision.flexrcoremodule.enums.FlexScope;
import com.irisvision.flexrcoremodule.observable.FlexConsumer;
import com.irisvision.flexrcoremodule.observable.FlexProvider;
import com.irisvision.flexrconcurrency.FlexThreadMain;

import java.util.ArrayList;
import java.util.List;

/**
 * One row of a list, on the FlexR side: a <b>mini {@code FlexStructure}</b> whose view tree is
 * built by {@link #dynamicBuild()}.
 *
 * <p>The vocabulary is deliberately the one of a screen ({@code dynamicBuild} ->
 * {@code attachEvents} -> {@code initialBusinessLogic}), plus two methods specific to a row:
 * {@link #bindData(FlexItemData)} (receive the datum) and {@link #forgettingFlexConsumers()}
 * (release whatever was acquired for the previous datum).
 *
 * <h2>Contract on the view</h2>
 *
 * <p>The view passed to the constructor <b>must</b> be a {@link FrameLayout}: it receives the root
 * produced by {@code dynamicBuild()}. The expected pattern is a static factory, the only way to
 * satisfy the {@code ViewHolder(View)} constructor imposed by {@code RecyclerView}:
 *
 * <pre>{@code
 * public class UserItem extends FlexItem {
 *     public UserItem(@NonNull View itemView) { super(itemView); }
 *
 *     public static FlexItem instance(FlexContextInterface f) {
 *         return new UserItem(new FrameLayout(f.getFlexContext()));
 *     }
 * }
 * }</pre>
 *
 * <h2>Observers: a row is a state consumer</h2>
 *
 * <p>A row may subscribe to a {@link FlexProvider} with {@link #observe(FlexProvider)}. It is then
 * notified <b>on the main thread</b> through {@link #onProviderChanged(FlexProvider)}: since a row
 * writes into views, the library performs the hop itself. Direct consequence: changing an item
 * repaints <b>only the affected row</b>, with neither {@code notifyDataSetChanged} nor
 * {@code DiffUtil}.
 *
 * <p>Release is automatic: the adapter calls {@link #forget()} <b>before each rebinding</b> and on
 * every recycle, which detaches the row from its providers. A recycled holder therefore cannot keep
 * the subscriptions of the datum it was bound to.
 *
 * <p>The scope used is the <b>Fragment</b> one ({@code fragScope}): a row lives as long as its
 * screen. The {@code Back} scope is reserved for background work that may outlive the Activity (see
 * {@code Provider}).
 *
 * <p><b>Threading.</b> {@code dynamicBuild()}, {@code attachEvents()},
 * {@code initialBusinessLogic()}, {@code bindData(...)} and {@link #forget()} are invoked by the
 * adapter on the main thread, and {@link #onProviderChanged(FlexProvider)} is always delivered
 * there. Never touch the views of a row from another thread.
 */
public abstract class FlexItem extends RecyclerView.ViewHolder {

    protected FlexContextInterface f;

    /** Providers this row subscribed to for the current item; released by {@link #forget()}. */
    private final List<FlexProvider> observedProviders = new ArrayList<>();

    /** The consumer actually subscribed: it relays every notification to {@link #onProviderChanged}. */
    private final FlexConsumer consumer = new FlexConsumer() {
        @Override
        public void notifyDataChanged(FlexProvider flexProvider) {
            FlexItem.this.onProviderChanged(flexProvider);
        }

        @Override
        public ScopeIdentity identity() {
            return FlexItem.this.identity();
        }

        @Override
        public void synchronizeNotifyDataChanged(FlexProvider flexProvider) {
            // A row writes into views: delivery is brought back to the main thread so that the
            // developer does not have to deal with it (see the FlexConsumer javadoc).
            FlexThreadMain.runOnMain(() -> notifyDataChanged(flexProvider));
        }
    };

    private ScopeIdentity identity;

    /**
     * Wraps the view that hosts the row content.
     *
     * <p>Called by {@code RecyclerView} with the view returned by your static factory; the row is
     * not built yet at this point, {@link #init(FlexContextInterface)} performs the build.
     *
     * @param itemView the row container; it must be a {@link FrameLayout}, otherwise
     *                 {@link #init(FlexContextInterface)} throws
     */
    public FlexItem(@NonNull View itemView) {
        super(itemView);
    }

    /**
     * Builds the row: stores the FlexR context, runs {@code dynamicBuild()}, attaches the resulting
     * view to the row container, then runs {@code attachEvents()} and
     * {@code initialBusinessLogic()}.
     *
     * <p>Called once per holder by the adapter, before the first
     * {@link #bindData(FlexItemData)}.
     *
     * @param f the FlexR context of the screen, used for the row scope and for the builders
     * @throws IllegalStateException if the row container is not a {@link FrameLayout}, or if
     *                               {@code dynamicBuild()} returned {@code null}
     */
    protected final void init( FlexContextInterface f){
        this.f = f;

        if (!(this.itemView instanceof FrameLayout)) {
            throw new IllegalStateException(getClass().getName()
                    + ": the view of a row must be a FrameLayout, because it receives the FlexR tree"
                    + " built by dynamicBuild(). Expected pattern: a static factory passing"
                    + " new FrameLayout(f.getFlexContext()). Received view: "
                    + this.itemView.getClass().getName());
        }

        View built = dynamicBuild();
        if (built == null) {
            throw new IllegalStateException(getClass().getName()
                    + ": dynamicBuild() returned null. A row must produce its root view"
                    + " (for a FlexView, go through nativeView(...)).");
        }

        ((FrameLayout)this.itemView).addView(built);
        attachEvents();
        initialBusinessLogic();
    }

    /**
     * Returns the native view of a {@code FlexView}, ready to be returned by {@link #dynamicBuild()}.
     *
     * <pre>{@code
     * @Override protected View dynamicBuild() {
     *     FlexText label = new FlexText(f);
     *     label.text("Hello");
     *     return nativeView(label);
     * }
     * }</pre>
     *
     * <p>Design note: {@code FlexView.content(...)} only accepts a closed set of bridges
     * (controlled extensibility). This method therefore reuses the bridge of the adapter, which the
     * core already recognizes, instead of introducing a new one, which would be rejected.
     *
     * @param flexView the FlexR view to expose as an Android view
     * @return the native view, ready to be attached to the row container
     */
    protected final View nativeView(FlexView flexView){
        return FlexAdapter.nativeView(flexView);
    }

    /* *** Observers *** */

    /**
     * Subscribes this row to the provider, for the lifetime of the current item.
     *
     * <p>The subscription is recorded: {@link #forget()} releases it on rebinding and on recycling.
     * Idempotent, a same provider is never subscribed twice.
     *
     * <pre>{@code
     * observe(userProvider); // released at the next binding of this row
     * }</pre>
     *
     * @param provider the provider to observe; {@code null} is ignored
     */
    protected final void observe(FlexProvider provider){
        if (provider == null) {
            return;
        }
        if (!observedProviders.contains(provider)) {
            provider.subscribeFragScope(this.consumer);
            observedProviders.add(provider);
        }
    }

    /**
     * Detaches this row from one specific provider, immediately.
     *
     * <p>The provider is always unsubscribed, even when this row had not subscribed to it.
     *
     * @param provider the provider to detach from; {@code null} is ignored
     */
    protected final void unObserve(FlexProvider provider){
        if (provider == null) {
            return;
        }
        provider.unSubscribe(this.consumer);
        observedProviders.remove(provider);
    }

    /**
     * Releases what the row had acquired for the current item: all its subscriptions, then
     * {@link #forgettingFlexConsumers()} for whatever the developer added there.
     *
     * <p>Called by the adapter <b>before each</b> {@code bindData(...)} and on every recycle.
     * Idempotent.
     */
    public final void forget(){
        for (FlexProvider provider : new ArrayList<>(observedProviders)) {
            provider.unSubscribe(this.consumer);
        }
        observedProviders.clear();
        forgettingFlexConsumers();
    }

    /**
     * Called when one of the observed providers reports a change, <b>on the main thread</b>.
     * Override it to update the views of the row.
     *
     * @param flexProvider the provider that changed
     */
    protected void onProviderChanged(FlexProvider flexProvider){ }

    /**
     * Override it to release what the row acquired outside {@link #observe(FlexProvider)}
     * (listeners, resources). Called by {@link #forget()}.
     *
     * <p>It is the counterpart of the method with the same name in {@code FlexStructure}: a row
     * uses the vocabulary of a screen.
     */
    protected void forgettingFlexConsumers(){ }

    /** Identity of the row, used by scope-based purging (same shape as the one of a {@code FlexView}). */
    private ScopeIdentity identity(){
        if (this.identity == null) {
            this.identity = new RowScopeIdentity(this.f);
        }
        return this.identity;
    }

    private static final class RowScopeIdentity implements ScopeIdentity {

        private final String scopeId;

        RowScopeIdentity(FlexContextInterface f){
            this.scopeId = (f == null) ? "" : f.getFlexFragment() + ":" + f.getFlexActivity();
        }

        @Override
        public void identify() {
            // The scopeId is computed once, at construction time.
        }

        @Override
        public String ref(String scope) {
            int separator = this.scopeId.indexOf(":");
            if (separator < 0) {
            return "";      // incomplete identity: no scope-based purge may throw
            }
            if (FlexScope.FlexFragment.name().equals(scope)) {
                return this.scopeId.substring(0, separator);
            }
            if (FlexScope.FlexActivity.name().equals(scope)) {
                return this.scopeId.substring(separator + 1);
            }
            return "";
        }
    }

    /**
     * Builds the content view of the row.
     *
     * <p>Called once, from {@link #init(FlexContextInterface)}, on the main thread.
     *
     * @return the root view to attach to the row container; return
     *         {@link #nativeView(FlexView)} for a {@code FlexView}. Must not be {@code null}
     */
    protected abstract View dynamicBuild();
    /**
     * Registers the listeners of the row views. Called once, right after {@code dynamicBuild()}.
     */
    protected abstract void attachEvents();
    /**
     * Runs the initial business logic of the row, once the view tree and its listeners exist.
     */
    protected abstract void initialBusinessLogic();
    /**
     * Receives the datum to display in this row.
     *
     * <p>Called on the main thread after {@link #forget()}, so the subscriptions taken for the
     * previous datum are already released.
     *
     * @param itemData the datum bound to this row
     */
    public abstract void bindData(FlexItemData itemData);
}
