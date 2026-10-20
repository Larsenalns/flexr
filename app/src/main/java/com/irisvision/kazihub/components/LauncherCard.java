package com.irisvision.kazihub.components;

import android.util.Log;
import android.widget.ImageView;

import com.irisvision.flexrcoremodule.core.FlexContextInterface;
import com.irisvision.flexrcoremodule.ui.component.FlexShape;
import com.irisvision.flexrcoremodule.ui.container.FlexBox;
import com.irisvision.flexrcoremodule.ui.container.FlexCard;
import com.irisvision.flexrcoremodule.ui.layout.FlexImg;
import com.irisvision.flexrthememodule.FlexTheme;

public class LauncherCard extends FlexCard {
    public LauncherCard(FlexContextInterface flexContext) {
        super(flexContext);
        init();
    }

    private void init(){
        this.autoApply(true);
        this.autoApplyTheme(true);

        this.flexCardElevation(6)
                .applyShape(new FlexShape(flexContext())
                        .newCompose()
                        .likeRelative()
                        // Mode relatif = FRACTION (0.5 = 50 % de la dimension), comme RelativeCornerSize
                        // de Material. La valeur 50 d'origine était interprétée comme 50 pixels : le
                        // mode relatif des coins ne fonctionnait pas (correctif U8).
                        .toAllCorners(0.5f, false)
                        .shapeColor(1)).boxParams()
                .outerGravity("center")
                .Sizer("80%", "35%");

        FlexImg appLauncherIcon = new FlexImg(flexContext());
        appLauncherIcon
                .load("kazihub_logo")
                .appearance(new FlexShape(flexContext()).newCompose().shapeColor(1))
                .scale(ImageView.ScaleType.FIT_CENTER)
                .autoApply(true)
                .boxParams()
                .outerGravity("center")
                .Sizer("75%", "30%");

        add(appLauncherIcon);

        Log.w("ADD_SZ", ""+this.children().size());
    }
}
