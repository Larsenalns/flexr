// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrcoremodule.structure;

import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.irisvision.flexrcoremodule.core.BaseContainerBridge;
import com.irisvision.flexrcoremodule.core.FlexContext;
import com.irisvision.flexrcoremodule.core.FlexStructureLooper;
import com.irisvision.flexrcoremodule.core.FlexView;
import com.irisvision.flexrcoremodule.core.FlexViewBridge;
import com.irisvision.flexrcoremodule.core.Init_;
import com.irisvision.flexrcoremodule.core.ScopeIdentity;
import com.irisvision.flexrcoremodule.enums.FlexScope;
import com.irisvision.flexrthememodule.FlexTheme;

public abstract class FlexStructureImpl extends FlexContext implements Init_, ScopeIdentity {
    private FlexBaseContainer flexBaseContainer;
    private FlexStructureContainerWrapper wrapper;
    private String scopeId;

    public FlexStructureImpl() {
    }

    /* ** The Means of this this class its to block access of all the method defined in the FlexStructure interface
     *  ** Doing like this way grunt access of these method only inside of the class that extends or implement the
     *  ** The FlexStructure interface
     *  ** All the classes that want to implement FlexStructure have to extends only this base Implementation.
     * */

    public final void initializer_() {
         /* The init methode for FlexStructure allow dev to perform all kind of init before any other operation, this can
          *  ** especially used to init subscribe this instance in some FlexProvider or doing top level init that not depend on sync or construction of FlexView/View,
          *  ** so, basically the initialBusinessLogic can bu used too in the place of init method, because initialBusinessLogic doesn't
          *  ** also depend on sync or construction of FlexView/View
          */
        init();
        initBaseContainer();
        attachEvents();
        forgettingFlexConsumers();
        initialBusinessLogic();

        /* ** The "run once the container is visible" logic is delegated to
         * ** FlexStructureLooper: it runs the work exactly once, during the visible pass of the
         * ** owning Fragment. Nothing polls and nothing sleeps, so deferred work costs nothing
         * ** until the screen is actually shown.
         */
        FlexStructureLooper.run(ref(FlexScope.FlexFragment.name()), this::onFlexContainerIsVisible);
    }

    @Override
    public void init() {

    }
    /*  This the default but dev can Override this method for doing is own operation and after or before that call
     ** this super onFlexContainerIsVisible();
     * ** protected void onFlexContainerIsVisible(){
     * **   initialSyncBusinessLogic();
     * **   attachEvents();
     * * };
     */
    protected void onFlexContainerIsVisible(){
        initialSyncBusinessLogic();
        attachEvents();
    };
    protected abstract FlexView dynamicBuild();
    protected abstract void attachEvents();
    protected abstract void initialBusinessLogic();


    /* ** These three methods aren't abstract because they are optional for developer
     *  ** 1. attachSyncEvents(){};
     *  ** 2. initialSyncBusinessLogic(){};
     *  ** 3. forgettingFlexConsumers(){};
     */

    //The events that needs view been visible to attache Events, one parent container is visible
    protected void attachSyncEvents(){};
    protected void initialSyncBusinessLogic(){};

    /* ** Declare, Create All the FlexConsumers that will be recreate each time the FlexFragment
     * ** is called, for ex : Login, Register... these flexConsumers can be declared in the LoginBody etc...
     */
    protected void forgettingFlexConsumers(){};

    @Override
    public final void identify() {
        if(this.scopeId == null)
            this.scopeId = signature();
    }

    @Override
    public final String ref(String scope) {
        identify();
        String scopeResult = "";

        if(scope.equals(FlexScope.FlexFragment.name())) scopeResult =  this.scopeId.substring(0, this.scopeId.indexOf(":"));
        else if((scope.equals(FlexScope.FlexActivity.name()))) scopeResult =  this.scopeId.substring(this.scopeId.indexOf(":")+1);

        return scopeResult;
    }

    private void initBaseContainer(){
        //Here, the flexContainer got initialize and make building of view by calling the dynamicBuild(); method
        // this.flexContainer.addView(dynamicBuild());

        this.flexBaseContainer = new FlexBaseContainer(this.getFlexContext());

        if (this.wrapper == null){
           this.wrapper = new FlexStructureContainerWrapper();
           this.flexBaseContainer.content(this.wrapper,"c");

           if (this instanceof FlexAppbar){

               this.wrapper.get().setBackgroundColor(FlexTheme.load(1));
               ViewGroup.LayoutParams params = this.wrapper.get().getLayoutParams();
               params.height = ((FlexAppbar) this).extendedAppBar();
               this.wrapper.get().setLayoutParams(params);

           }
           else if (this instanceof FlexBody) {
               if (hasAppBar())
                   this.wrapper.get().setPadding(0, appBarHeight(), 0, 0);
               this.wrapper.get().setBackgroundColor(FlexTheme.load(0));
           }
        }
    }

    public final FlexBaseContainer container(){
        return this.flexBaseContainer;
    }

    public static boolean chekWrapper(FlexViewBridge flexViewBridge){
        return flexViewBridge instanceof FlexStructureFlexView_Wrapper;
    }

    public final class FlexStructureContainerWrapper implements BaseContainerBridge{
        private FrameLayout frameLayout;
        @Override
        public void bridge(FrameLayout frameLayout, String s) {
            if (s.equalsIgnoreCase("c")){

                this.frameLayout = frameLayout;
                FlexStructureFlexView_Wrapper flexView_wrapper = new FlexStructureFlexView_Wrapper();
                FlexStructureImpl.this.dynamicBuild().contentWrapper(flexView_wrapper);
                frameLayout.addView(flexView_wrapper.view);
            }
        }

        private FrameLayout get(){
            return this.frameLayout;
        }
    }

    private static class FlexStructureFlexView_Wrapper implements FlexViewBridge{
        private View view;
        @Override
        public void bridge(View view) {
            this.view = view;
        }
    }
}
