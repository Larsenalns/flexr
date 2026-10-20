// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrcoremodule.ui.component;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.irisvision.flexrcoremodule.core.FlexActivity;
import com.irisvision.flexrcoremodule.core.FlexContextInterface;
import com.irisvision.flexrcoremodule.core.FlexView;
import com.irisvision.flexrcoremodule.core.FlexViewBridge;
import com.irisvision.flexrcoremodule.structure.FlexStructure;

/**
 * Modal sheet (bottom sheet) whose content is a FlexR tree.
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 * public class MySheet extends FlexBottomSheet {
 *     public MySheet() { }                       // required by the FragmentManager
 *     public MySheet(FlexContextInterface f) { flexContext(f); }
 *
 *     @Override public void dynamicBuild() {
 *         FlexBox root = new FlexBox(flexContext());
 *         root.background(1).add(new FlexText(flexContext()).text("Hello"));
 *         content(root);                          // bridges the root into the sheet
 *     }
 *
 *     @Override public String getTags() { return "my-sheet"; }
 * }
 *
 * new MySheet(this).flexShowSheet();
 * }</pre>
 *
 * <h2>Build order</h2>
 *
 * <p>{@code onCreateView()} runs the whole construction chain before the view is returned to the
 * {@code FragmentManager}: {@code baseInit()}, then {@code initializer_()} (hence
 * {@code dynamicBuild()}, {@code attachEvents()}, {@code forgettingFlexConsumers()} and
 * {@code initialBusinessLogic()}). The bridged view is then wrapped in a {@link FrameLayout} that
 * the method returns, so the sheet is never displayed with a null view. If {@code dynamicBuild()}
 * did not bridge anything, an explicit exception says so instead of letting an empty sheet
 * through.
 *
 * <h2>System recreation</h2>
 *
 * <p>The {@code FragmentManager} recreates a {@code Fragment} <b>by reflection, with no
 * argument</b>: screen rotation, system theme change, process restore. The class therefore exposes
 * the no-argument constructor such a recreation requires, and
 * {@link #flexContext(FlexContextInterface)} lets the parent that recreates the sheet hand its
 * context back; without it {@link #flexContext()} fails with an explicit message instead of
 * propagating a {@code null}.
 *
 * <p>The context is deliberately <b>not</b> kept in a static field nor reloaded from the
 * {@code Bundle}: it is held by a strong instance reference and released together with the sheet,
 * so no static {@code Context} and therefore no {@code Activity} leak can occur.
 *
 * <p>Instances are not thread-safe. Every lifecycle callback and every accessor touches the view
 * hierarchy and must run on the main thread, which the fragment lifecycle already guarantees.
 */
public abstract class FlexBottomSheet extends BottomSheetDialogFragment implements FlexStructure {

    private FlexContextInterface f;

    /**
     * Facade bridge: it receives the native view of the FlexR root built in {@code dynamicBuild()}.
     * {@link #content(FlexView)} performs that call for you.
     */
    protected FlexBottomSheetBridge bridge = new FlexBottomSheetBridge();

    /**
     * Creates an empty sheet with no FlexR context.
     *
     * <p>Required by the {@code FragmentManager}, which recreates the fragment by reflection and
     * therefore has no argument to pass. Call {@code flexContext(...)} before using the sheet.
     */
    public FlexBottomSheet() {
    }

    /**
     * Creates a sheet bound to an existing FlexR context.
     *
     * @param flexContext context of the host activity
     */
    public FlexBottomSheet(FlexContextInterface flexContext) {
        this.f = flexContext;
    }

    /**
     * FlexR context of the sheet: the one given to the constructor, or the one re-injected by
     * {@link #flexContext(FlexContextInterface)} after a system recreation.
     *
     * @return the FlexR context, never {@code null}
     * @throws IllegalStateException when no context is available, which is the case for an
     *         automatic recreation in which the parent must call {@code flexContext(...)} again
     */
    protected final FlexContextInterface flexContext(){
        if (this.f == null) {
            throw new IllegalStateException(getClass().getName()
                    + " : no FlexR context. After a system recreation (rotation, process "
                    + "restore), the Fragment loses its fields: the parent must call "
                    + "flexContext(...) before showing the sheet.");
        }
        return this.f;
    }

    /**
     * Provides or replaces the FlexR context of the sheet.
     *
     * <p>Used by the parent that has to restore an existing sheet after a rotation, the system
     * having recreated the fragment without its arguments.
     *
     * @param flexContext context of the host activity
     */
    protected final void flexContext(FlexContextInterface flexContext){
        this.f = flexContext;
    }

    /**
     * Bridges the root of the FlexR tree into the sheet. Call it from {@code dynamicBuild()}.
     *
     * <p>Equivalent to {@code root.contentWrapper(bridge)}. Exactly one root must be bridged,
     * otherwise {@code onCreateView()} throws.
     *
     * @param root root of the FlexR tree to display
     * @throws IllegalArgumentException when {@code root} is {@code null}
     */
    protected final void content(FlexView root){
        if (root == null) {
            throw new IllegalArgumentException("content(null) : provide the FlexR root of the sheet.");
        }
        root.contentWrapper(this.bridge);
    }

    /**
     * Builds the FlexR tree and returns the container that hosts it.
     *
     * <p>The whole construction chain runs here, before the view is handed to the
     * {@code FragmentManager}, so the returned container always has a content. The bridged view is
     * added to it with match-parent bounds.
     *
     * @return a frame layout wrapping the bridged root view
     * @throws IllegalStateException when {@code dynamicBuild()} did not bridge any view
     */
    @Nullable
    @Override
    public final View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // The construction window opens BEFORE the view is returned to the FragmentManager.
        baseInit();
        initializer_();

        if (bridge.view == null) {
            throw new IllegalStateException(getClass().getName()
                    + " : no view was bridged. In dynamicBuild(), build the FlexR root "
                    + "then call content(root), otherwise the sheet has no content.");
        }

        FrameLayout sheetHost = new FrameLayout(requireContext());
        sheetHost.addView(bridge.view, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        return sheetHost;
    }

    /**
     * Notifies the sheet content that it is now visible, so children can wire their synchronous
     * events.
     *
     * @param view               the container returned by {@code onCreateView()}
     * @param savedInstanceState saved fragment state, may be {@code null}
     */
    @Override
    public final void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        /*  ** The children of the sheet call their own onFlexContainerIsVisible() from here: the
         *  ** sheet is visible, so the synchronous events can be wired.
         */
        onFlexContainerIsVisible();
    }

    /**
     * Start of the sheet. Overridable: this is where the sheet behavior is set up through
     * {@code BottomSheetBehavior.from(...)} (peek height, initial state, drag).
     *
     * <p>Call {@code super.onStart()} when overriding.
     */
    @Override
    public void onStart() {
        super.onStart();
    }

    /**
     * Identifier of the sheet type, used to recognize it in the fragment stack.
     *
     * @return a non-null, stable tag for this sheet
     */
    public abstract String getTags();

    /**
     * Shows this sheet on top of the host activity.
     *
     * <p>Resolves the FlexR context to its host {@code Context} and delegates to
     * {@code FlexActivity.launchSheet(...)}.
     *
     * @throws IllegalStateException when the context is missing or when its host is not a
     *         {@link FlexActivity}
     */
    public final void flexShowSheet(){
        // The FlexR context is not an Activity but a FlexContextInterface: its getFlexContext()
        // returns the host Context (the FlexActivity), which is what the cast below relies on.
        Context host = flexContext().getFlexContext();
        if (!(host instanceof FlexActivity)) {
            throw new IllegalStateException(getClass().getName() + ".flexShowSheet() : the sheet must be "
                    + "hosted by a FlexActivity (context received: " + host + ").");
        }
        ((FlexActivity) host).launchSheet(this);
    }

    /**
     * Dismisses the sheet, which closes the dialog fragment on the next main-thread loop.
     */
    public final void closeSheet(){
        this.dismiss();
    }


    /**
     * First hook of the construction chain: set up the state the build depends on.
     */
    @Override
    public void baseInit() {

    }

    /**
     * Builds the FlexR tree of the sheet and bridges its root with {@link #content(FlexView)}.
     */
    @Override
    public void dynamicBuild() {

    }

    /**
     * Wires the asynchronous events of the tree.
     */
    @Override
    public void attachEvents() {

    }

    /**
     * Runs the initial business logic, once the tree is built and its events are wired.
     */
    @Override
    public void initialBusinessLogic() {

    }

    /**
     * Wires the events that require the container to be visible.
     */
    @Override
    public void attachSyncEvents() {

    }

    /**
     * Runs the initial logic that requires the container to be visible.
     */
    @Override
    public void initialSyncBusinessLogic() {

    }

    /**
     * Drops the FlexConsumers created by this sheet, so they are built again on the next show.
     */
    @Override
    public void forgettingFlexConsumers() {

    }

    /**
     * Bridge that captures the native view of the FlexR root bridged by {@link #content(FlexView)}.
     */
    public static class FlexBottomSheetBridge implements FlexViewBridge{
        private View view;

        /**
         * Stores the view produced by the FlexR tree.
         *
         * @param view native view to display inside the sheet
         */
        @Override
        public void bridge(View view) {
            this.view = view;
        }
    }
}
