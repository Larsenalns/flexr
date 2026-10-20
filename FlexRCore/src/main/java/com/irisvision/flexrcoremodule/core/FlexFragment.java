// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrcoremodule.core;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.irisvision.flexrcoremodule.enums.FlexScope;
import com.irisvision.flexrcoremodule.observable.FlexReactive;
import com.irisvision.flexrcoremodule.rooting.FragmentNavigate;
import com.irisvision.flexrcoremodule.structure.FlexAppbar;
import com.irisvision.flexrcoremodule.structure.FlexBaseContainer;
import com.irisvision.flexrcoremodule.structure.FlexBody;
import com.irisvision.flexrcoremodule.structure.FlexStructureImpl;
import com.irisvision.flexrfxmodule.AnimateFragmentScope;
import com.irisvision.flexrfxmodule.AnimateScopeOwner;
import com.irisvision.flexrfxmodule.FlexAnimateScopeManager;
import com.irisvision.flexrfxmodule.FlexChoreoScope;
import com.irisvision.flexrthememodule.FlexTheme;

public abstract class FlexFragment extends Fragment implements FragmentNavigate, AnimateFragmentScope {

    private final String navigationKey;
    private FlexContext flexAppbar;
    private FlexContext flexBody;
    private FlexBaseContainer baseContainer;
    private boolean canPrepare;

    private FlexFragmentContainerWrapper wrapper = new FlexFragmentContainerWrapper();

    public FlexFragment() {
        // A no-argument constructor is mandatory: screens are instantiated by reflection (see
        // prepareScreen).
        // Note: setNavigationKey() is called from the constructor, so it must return a constant and
        // never depend on a field of the subclass.
        String key = setNavigationKey();
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalStateException(
                    getClass().getName() + ".setNavigationKey() must return a non-empty key "
                            + "(for example \"home\"). A null key produces a nameless back stack entry "
                            + "and breaks back navigation.");
        }
        this.navigationKey = key;
    }



    @Nullable
    @Override
    public final View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        init_();
        canPrepare = true; // Opens the screen-preparation window.
        onFlexCreateView(savedInstanceState);
        canPrepare = false; // Closes it: no further screen instance may be built afterwards.

        return this.wrapper.load();
    }

    @Override
    public final void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Consumes only the structures pending for THIS Fragment — the scope is its identity — so a
        // destroyed Fragment never has its deferred work run by the next one.
        FlexStructureLooper.loop("" + this);
        onFlexViewCreated(view, savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }

    @Override
    public final void onDestroyView() {
        super.onDestroyView();

        // The view is gone, and so is the Fragment's Context: every observer registered for the
        // Fragment scope is released here. This is what keeps a destroyed screen from leaking into
        // the next one, in the state objects, in the theme and in the animation registries alike.
        FlexReactive.clean(FlexScope.FlexFragment, ""+this);
        FlexTheme.clearObsFragScope(""+this);
        FlexStructureLooper.clear(""+this);
        FlexAnimateScopeManager.clearFlexFragmentScope(""+this);
        FlexChoreoScope.clearFlexFragmentScope(""+this);
        onFlexonDestroyView();

        // Detach the FlexR structures from this Fragment: they hold a FlexContext, which holds the
        // Activity's Context (getFlexContext() -> requireContext()). A Fragment survives a rotation —
        // the Activity does not — so keeping these fields would hold the old Activity until the next
        // view creation, or for good when no further view is ever built. The views themselves are
        // already destroyed and unreferenced at this point.
        detachFlexContexts();
    }

    /**
     * Releases the references to the structures built for the destroyed view, so that neither the
     * {@code FlexContext} nor the native container keeps the Activity alive beyond the view.
     *
     * <p>The fields are rebuilt by the next {@code onCreateView()} (see {@code init_()} and
     * {@code prepareScreen(...)}), which creates a fresh {@code FlexBaseContainer} and new structures
     * on the current context.
     */
    private void detachFlexContexts(){
        this.flexAppbar = null;
        this.flexBody = null;
        this.baseContainer = null;
        this.wrapper.release();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    /**
     * The Fragment detaching from its Activity: the last opportunity to release the references to the
     * host context.
     *
     * <p>{@link #onDestroyView()} already does it; this call is idempotent and covers the detach case
     * explicitly (a removed Fragment), so that no {@code Activity} stays held by a {@code FlexContext}
     * after the view is gone.
     */
    @Override
    public void onDetach() {
        super.onDetach();
        detachFlexContexts();
    }

    /**
     * Returns this Fragment's identity for the requested scope.
     *
     * <p>It is the counterpart of {@code FlexActivity.ref(String)} and it exists so that a Fragment —
     * or any view living inside it — can be handed directly to the animation engine:
     * {@code new FlexAnimate(myFragment)}. The identifier is the very one the views of this screen
     * compute, which is what makes an animation and a state subscription share the same lifetime.
     *
     * <p>Declared {@code final}: a screen must never have to implement it.
     *
     * @param scope {@link AnimateScopeOwner#FRAGMENT_SCOPE} or {@link AnimateScopeOwner#ACTIVITY_SCOPE}
     * @return this Fragment's identifier for that scope, or an empty string when it is not yet attached
     */
    @Override
    public final String ref(String scope) {
        if (AnimateScopeOwner.FRAGMENT_SCOPE.equals(scope)) {
            return "" + this;
        }
        if (AnimateScopeOwner.ACTIVITY_SCOPE.equals(scope)) {
            return getActivity() == null ? "" : "" + getActivity();
        }
        return "";
    }

    protected void onFlexCreateView(@Nullable Bundle savedInstanceState) {
        /* Here is the place to call prepareScreen method in the sub class that will extends this FlexFragment Class
        ** prepareScreen(LoginAppbar.class, LoginBody.class);
        */
    }

    protected void onFlexViewCreated (@NonNull View view, @Nullable Bundle savedInstanceState) {

    }
    protected void onFlexonDestroyView(){}

    protected final void prepareScreen(Class<? extends FlexAppbar> flexAppbarClass, Class<? extends FlexBody> flexBodyClass) {
         /* Here is the place to pass the context parameter that is needed by these class
         ** for work, 'cause without that these two classes can't work, it's a dependency
         */

        FlexStarter flexStarter = new FlexStarter();

        //Check if the flexAppbarClass isn't null and if flexAppbar isn't initialized as well, to avoid to affect many instance and ensure that just
        // By one Call of the prepareScreen method instance will be created

        /*   These two lines add the flexAppbar and flexBody in the FrameLayout of this current FlexFragment via FlexFragmentContainerWrapper
        ** * and the load() method inside this inner class FlexFragmentContainerWrapper is called in the native
        ** * onCreateView() of Fragment
        ** * ((FlexAppbar)this.flexAppbar).container().content(this.wrapper,"c");
        ** * ((FlexBody)this.flexBody).container().content(this.wrapper, "c");
        */

        if (flexAppbarClass != null && canPrepare){
            this.flexAppbar = instantiateScreen(flexAppbarClass);
            this.flexAppbar.initFlexStarter(flexStarter);
            this.flexAppbar.initializer_();
            //Details of this line is in the comment just up
            ((FlexStructureImpl)this.flexAppbar).container().content(this.wrapper,"c");
        }

        /* Check if the flexBodyClass isn't null and if flexBody isn't initialized as well, to avoid to affect many instance and ensure that just
        ** By one Call of the prepareScreen method instance will be created
        ** then, for the flexBody some more test must be add, test if flexAppbar will be part of the render to put margin-top to the
        ** flexBody or not
        */

        if (flexBodyClass != null && canPrepare){
            this.flexBody = instantiateScreen(flexBodyClass);
            this.flexBody.initFlexStarter(flexStarter);
            this.flexBody.initializer_();
            //Details of this line is in the comment just up
            ((FlexStructureImpl)this.flexBody).container().content(this.wrapper, "c");
        }

    }

    /**
     * Instantiates a screen (an AppBar or a Body) by reflection.
     *
     * <p>Uses {@code getDeclaredConstructor().newInstance()} rather than the deprecated
     * {@code Class.newInstance()}, which failed on classes whose constructor is not public.
     *
     * <p>Failures are never silent: a screen that cannot be instantiated raises an exception naming the
     * class and the two realistic causes. A blank screen with no message is the worst possible symptom,
     * because there is nothing to search for.
     *
     * @param screenClass the screen class to instantiate
     * @return a new instance of that class
     * @throws IllegalStateException if the class has no accessible no-argument constructor, or if code
     *                               shrinking renamed it
     */
    private <T> T instantiateScreen(Class<T> screenClass) {
        try {
            return screenClass.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                    "Unable to instantiate the screen " + screenClass.getName()
                            + ". Possible causes: the class has no accessible no-argument constructor, "
                            + "or R8 renamed it — in that case, add its keep rules to the module's "
                            + "consumer-rules.pro.", e);
        }
    }

    private void init_(){
        this.baseContainer = new FlexBaseContainer(getContext());
        this.baseContainer.content(this.wrapper,"w");
    }

    public String getNavigationKey() {
        return navigationKey;
    }

    protected abstract String setNavigationKey();

    private boolean hasAppBar(){
        return this.flexAppbar != null;
    }
    private int appBarHeight(){
        int appBarHeight = 0;

        if(hasAppBar())
            appBarHeight = ((FlexAppbar)this.flexAppbar).extendedAppBar();

        return appBarHeight;
    }

    @Override
    public boolean onNextFlexFragment() {
        return true;
    }

    @Override
    public boolean onCloseFlexFragment() {
        return true;
    }

    public final class FlexStarter {
        public void start(FlexContext flexContext){
            /*   This method pass the current Fragment, the current instance of this FlexFragment class
             * ** This can be like, LoginFragment, RegisterFragment
             * */
            flexContext.contextualize(FlexFragment.this);
        }
        public boolean hasAppBar(){
            return FlexFragment.this.hasAppBar();
        }

        public int appBarHeight(){
            return FlexFragment.this.appBarHeight();
        }
    }

    public static final class FlexFragmentContainerWrapper implements BaseContainerBridge{
        private FrameLayout frameLayout;
        @Override
        public void bridge(FrameLayout frameLayout, String s) {
            if (s.equalsIgnoreCase("w"))
                this.frameLayout = frameLayout;
            else if (s.equalsIgnoreCase("c"))
                this.frameLayout.addView(frameLayout, 0);

            //In honer of the FlexFragment color used fo debug was this Lime
            this.frameLayout.setBackgroundColor(Color.parseColor("#ff005544"));
        }

        private FrameLayout load(){
            return this.frameLayout;
        }

        /**
         * Releases the reference to the native container.
         *
         * <p>The container was created with the Activity's {@code Context}: keeping it after the view is
         * destroyed would keep that Activity alive (see {@code FlexFragment.detachFlexContexts()}).
         */
        private void release(){
            this.frameLayout = null;
        }
    }
}
