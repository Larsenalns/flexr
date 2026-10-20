// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrcoremodule.structure;

import android.view.View;

public abstract class FlexBody extends FlexStructureImpl{

    // Have to Use one fo my own Concurrency class in the initializer_ method when calling all Sync Methode like
    // attachSyncEvents, initialSyncBusinessLogic

    /*public FlexBody() {
    }

    @Override
    protected void onFlexContainerIsVisible() {
        // in the concurrency object, after checking if the container is visible ( isNotNull )
        *//*
         ** initialSyncBusinessLogic
         ** attachSyncEvents();
         * *//*
    }

    @Override
    protected abstract View dynamicBuild();

    @Override
    protected abstract void attachEvents();

    @Override
    protected void attachSyncEvents() {

    }

    @Override
    protected void initialSyncBusinessLogic() {

    }

    @Override
    protected void initialBusinessLogic() {

    }

    @Override
    public void forgettingFlexConsumers() {

    }*/
}
