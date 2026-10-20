package com.irisvision.kazihub.screen;

import android.util.Log;
import android.widget.ImageView;

import com.irisvision.flexrcoremodule.core.FlexView;
import com.irisvision.flexrcoremodule.structure.FlexBody;
import com.irisvision.flexrcoremodule.ui.component.FlexShape;
import com.irisvision.flexrcoremodule.ui.container.FlexBox;
import com.irisvision.flexrcoremodule.ui.container.FlexCard;
import com.irisvision.flexrcoremodule.ui.container.FlexContainer;
import com.irisvision.flexrcoremodule.ui.layout.FlexImg;
import com.irisvision.flexrcoremodule.valueconverter.Val;
import com.irisvision.flexrthememodule.FlexTheme;
import com.irisvision.kazihub.components.LauncherCard;

public class LauncherBody extends FlexBody {
    @Override
    protected FlexView dynamicBuild() {
        FlexBox container = new FlexBox(this);
        container.background(new FlexShape(this).newCompose().shapeColor(0))
                .autoApply(true)
                .boxParams()
                .Sizer(Val.Match);

        FlexBox decorationOne = new FlexBox(this);
        decorationOne.background(0).boxParams().outerGravity("right, top").margins("40, 70, 0, 0,").Sizer("100%", "40%");

        FlexShape shapeDecorationOne = new FlexShape(this);
        // Mode relatif = fraction (0.5 = 50 %), voir le correctif U8 de FlexShape.
        shapeDecorationOne.newCompose().likeRelative().toAllCorners(0.5f, false).shapeColor(1);

        for (int i = 0; i < 3; i++){
            FlexCard card = new FlexCard(this);
            card.flexCardElevation(4)
                    .appearance(shapeDecorationOne)
                    .cardColor(1)
                    .autoApply(true)
                    .boxParams()
                    .outerGravity("center, top")
                    .marginOnly((i*40)+"T,"+(-i*15)+"L", null)
                    .Sizer("200", "35");

            decorationOne.add(card);
        }

        container.addAll(new FlexView[]{decorationOne, new LauncherCard(this)});

        return container;
    }

    @Override
    protected void attachEvents() {

    }

    @Override
    protected void initialBusinessLogic() {

    }
}
