// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrstarter.template;

import com.irisvision.flexrcoremodule.core.FlexView;
import com.irisvision.flexrcoremodule.observable.FlexReactive;
import com.irisvision.flexrcoremodule.rooting.NavigatorFlexFragment;
import com.irisvision.flexrcoremodule.structure.FlexBody;
import com.irisvision.flexrcoremodule.ui.component.FlexShape;
import com.irisvision.flexrcoremodule.ui.container.FlexCard;

public class TemplateAboutBody extends FlexBody {

    private FlexCard bodyCard;

    @Override
    protected FlexView dynamicBuild() {
        bodyCard = new FlexCard(this);

        FlexShape back = new FlexShape(this);
        back.newCompose()
                .likeRelative()
                .toCornerTopLeft(60, true)
                .toCornerTopRight(60, true)
                .toCornerBottomLeft(60, false)
                .toCornerBottomRight(60, false)
                .shapeColor(1);

        bodyCard
                .appearance(back)
                .cardColor(1)
                .autoApply(true)
                .boxParams().outerGravity("center")
                .Sizer("80%", "80%");

        return bodyCard;
    }

    @Override
    protected void attachEvents() {
        this.bodyCard.clickable(true)
                .onClick(v -> {
                    TemplateFlexProviderHome home = (TemplateFlexProviderHome) FlexReactive.get("home");
                    home.appBarText("About...");

                    if (home.counter >= 10) {
                        home.decreaseValue();
                    } else {
                        home.increaseValue();
                    }
                });

        this.bodyCard.onLongClick(v -> {
            NavigatorFlexFragment.navigate(new TemplateFlexFragmentHome(), false, this);
            return false;
        });
    }

    @Override
    protected void initialBusinessLogic() {
    }
}
