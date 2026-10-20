// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrstarter.template;

import android.view.View;

import com.irisvision.flexrcoremodule.core.FlexView;
import com.irisvision.flexrcoremodule.observable.FlexReactive;
import com.irisvision.flexrcoremodule.rooting.NavigatorFlexFragment;
import com.irisvision.flexrcoremodule.structure.FlexBody;
import com.irisvision.flexrcoremodule.ui.component.FlexShape;
import com.irisvision.flexrcoremodule.ui.container.FlexBox;
import com.irisvision.flexrcoremodule.ui.container.FlexCard;
import com.irisvision.flexrcoremodule.valueconverter.Val;

public class TemplateBodyHome extends FlexBody {

    private FlexCard bodyCard;
    private FlexBox bodyContainer;
    private FlexBox sizer;
    private int clickCount = 0;

    @Override
    public void init() {
        super.init();
    }

    @Override
    protected FlexView dynamicBuild() {
        this.bodyContainer = new FlexBox(this);
        this.bodyContainer.background(2, "light");

        this.bodyCard = new FlexCard(this);

        FlexShape back = new FlexShape(this);
        // Relative mode means a fraction (0.6 = 60 % of the shortest side), see FlexShape.
        back.newCompose()
                .likeRelative()
                .toCornerTopLeft(0.6f, false)
                .toCornerBottomRight(0.6f, false)
                .shapeColor(1);

        bodyCard
                .appearance(back)
                .cardColor(3)
                .flexCardElevation(15)
                .autoApply(true)
                .boxParams().outerGravity("center")
                .Sizer("80%", "50%");

        this.sizer = new FlexBox(this);
        this.sizer
                .background(back)
                .boxParams()
                // The trailing comma is REQUIRED by FlexMargins.processAll, which expects four values
                // plus the (empty) strategy slot. Removing it raises a StringIndexOutOfBoundsException.
                .margins("20, 20, 20, 0,")
                .outerGravity("top")
                .Sizer("Match", "90").apply();

        this.bodyContainer.addAll(new FlexView[]{bodyCard, sizer});

        return bodyContainer;
    }

    @Override
    protected void attachEvents() {
        this.bodyCard.clickable(true)
                .onClick(v -> {
                    TemplateFlexProviderHome home = (TemplateFlexProviderHome) FlexReactive.get("home");
                    home.appBarText("Home...");

                    if (home.counter < 10) {
                        home.increaseValue();
                    } else {
                        home.decreaseValue();
                    }
                });

        this.sizer.clickable(true);
        this.sizer.onClick(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (TemplateBodyHome.this.clickCount < 5) {
                    TemplateBodyHome.this.bodyCard
                            .boxParams()
                            .SizerTo("0", "10%").apply();
                }

                if (TemplateBodyHome.this.clickCount > 5 && TemplateBodyHome.this.clickCount <= 10) {
                    TemplateBodyHome.this.bodyCard
                            .boxParams()
                            .SizerTo("0", "-10%").apply();
                }
                clickCount++;

                if (TemplateBodyHome.this.clickCount > 10) clickCount = 0;
            }
        });

        this.bodyCard.onLongClick(v -> {
            NavigatorFlexFragment.navigate(new TemplateFlexFragmentAbout(), false, this);
            return false;
        });
    }

    @Override
    protected void initialBusinessLogic() {
    }
}
