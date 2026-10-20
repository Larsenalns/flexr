// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrstarter.template;

import com.irisvision.flexrconcurrency.FlexThreadMain;
import com.irisvision.flexrcoremodule.core.FlexView;
import com.irisvision.flexrcoremodule.core.ScopeIdentity;
import com.irisvision.flexrcoremodule.observable.FlexConsumer;
import com.irisvision.flexrcoremodule.observable.FlexProvider;
import com.irisvision.flexrcoremodule.observable.FlexReactive;
import com.irisvision.flexrcoremodule.rooting.NavigatorFlexFragment;
import com.irisvision.flexrcoremodule.structure.FlexAppbar;
import com.irisvision.flexrcoremodule.ui.container.FlexBox;
import com.irisvision.flexrcoremodule.ui.layout.FlexText;
import com.irisvision.flexrcoremodule.valueconverter.Val;

public class TemplateFlexAppBar extends FlexAppbar implements FlexConsumer {

    private FlexText homeText;
    private FlexText counterText;

    @Override
    public void init() {
        FlexReactive.get("home").subscribeFragScope(this);
        FlexReactive.get("config").subscribeFragScope(this);
    }

    @Override
    protected FlexView dynamicBuild() {
        FlexBox appBarContainer = new FlexBox(this);
        appBarContainer.background(1);

        this.counterText = new FlexText(this);
        this.counterText
                .textSize(17)
                .text("Counter")
                .textColor(1, "light")
                .autoApply(true)
                .boxParams().marginOnly("6%T", null)
                .outerGravity("right, top")
                .Sizer("65", "30");

        appBarContainer.add(counterText);

        FlexBox titleContainer = new FlexBox(this);
        titleContainer
                .autoApply(true)
                .boxParams().marginOnly("6%T", null)
                .outerGravity("center, top")
                .Sizer("80", "30");

        this.homeText = new FlexText(this);
        this.homeText
                .textSize(17)
                .text("Genesis ...")
                .textColor(1, "light")
                .boxParams().Sizer(Val.Wrap).apply();

        titleContainer.add(this.homeText);
        appBarContainer.add(titleContainer);

        return appBarContainer;
    }

    @Override
    protected void attachEvents() {
    }

    @Override
    protected void initialBusinessLogic() {
    }

    @Override
    public ScopeIdentity identity() {
        return this;
    }

    @Override
    public void notifyDataChanged(FlexProvider flexProvider) {

        // Important: this method is called from the ScheduledExecutorService of
        // FlexConsumer, so never on the UI thread. Every write to a view must
        // go back through FlexThreadMain.post, otherwise Android throws
        // CalledFromWrongThreadException.
        if (flexProvider instanceof TemplateFlexProviderHome) {
            TemplateFlexProviderHome home = (TemplateFlexProviderHome) flexProvider;

            FlexThreadMain.post(() -> {
                this.homeText.text(home.appBarText());
                this.counterText.text("" + home.counter);
            });
        }
        // Navigation, however, no longer needs wrapping: NavigatorFlexFragment
        // brings it back to the main thread by itself.
        else if (flexProvider instanceof TemplateFlexConfigNotify
                && getFlexFragment() instanceof TemplateFlexFragmentHome) {
            NavigatorFlexFragment.navigate(new TemplateFlexFragmentAbout(), false, this);
        }
        else if (flexProvider instanceof TemplateFlexConfigNotify
                && getFlexFragment() instanceof TemplateFlexFragmentAbout) {
            NavigatorFlexFragment.navigate(new TemplateFlexFragmentHome(), false, this);
        }
    }
}
