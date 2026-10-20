package com.irisvision.flexrcoremodule.ztest;

import android.view.View;

import com.irisvision.flexrcoremodule.core.FlexContextInterface;
import com.irisvision.flexrcoremodule.core.FlexView;
import com.irisvision.flexrcoremodule.observable.FlexConsumer;
import com.irisvision.flexrcoremodule.observable.FlexProvider;
import com.irisvision.flexrcoremodule.observable.FlexReactive;
import com.irisvision.flexrcoremodule.structure.FlexStructure;
import com.irisvision.flexrcoremodule.ui.container.FlexBox;

public class MyCard extends FlexBox {
    MyCard card1, card2, card3;
    public MyCard(FlexContextInterface flexContext) {
        super(flexContext);
        card1 = new MyCard(flexContext);
        card2 = new MyCard(flexContext);
        card3 = new MyCard(flexContext);
    }

    @Override
    public void baseInit() {
        card1.initializer_();
        card2.initializer_();
        card3.initializer_();

        //FlexReactive.get("login").subscribeFragScope(this);
    }

    @Override
    public void onFlexContainerIsVisible() {
        card1.onFlexContainerIsVisible();
        card2.onFlexContainerIsVisible();
        card3.onFlexContainerIsVisible();
        super.onFlexContainerIsVisible();
    }

    @Override
    public void dynamicBuild() {
        this.card1
                .background(2)
                .elevation(3);

        this.card2.background(2);
        this.card2.elevation(3);

        this.card3
                .background(2)
                .elevation(3);

        this.addAll(new FlexView[]{card1, card2, card3});
    }

    @Override
    public void attachEvents() {
    }

    @Override
    public void forgettingFlexConsumers() {

    }

    /*@Override
    public void notifyDataChanged(FlexProvider flexProvider) {
        LoginProvider login = (LoginProvider) flexProvider;
        if (login.getUsername.equalsIgnoreCase("name")){
            System.out.println("YESSSSSSSSSS");
        }
    }*/
}
