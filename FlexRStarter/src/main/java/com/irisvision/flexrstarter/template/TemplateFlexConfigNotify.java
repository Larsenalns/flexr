// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrstarter.template;

import com.irisvision.flexrcoremodule.observable.FlexProvider;

public class TemplateFlexConfigNotify extends FlexProvider {
    @Override
    protected String providerFlag() {
        return "config";
    }

    public final void pushConfig(){
        notifyConsumers();
    }
}
