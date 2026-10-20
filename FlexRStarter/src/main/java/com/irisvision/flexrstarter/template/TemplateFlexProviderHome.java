// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrstarter.template;

import com.irisvision.flexrcoremodule.observable.FlexProvider;

public class TemplateFlexProviderHome extends FlexProvider {

    public int counter = 0;
    private String templateAppBarText = "";

    @Override
    protected String providerFlag() {
        return "home";
    }

    public void increaseValue(){
        this.counter++;
        notifyConsumers();
    }

    public void decreaseValue(){
        this.counter--;
        notifyConsumers();
    }

    public int counterValue(){
        return this.counter;
    }

    public void appBarText(String text){
        this.templateAppBarText = text;
        notifyConsumers();
    }

    public String appBarText(){
        return this.templateAppBarText;
    }
}
