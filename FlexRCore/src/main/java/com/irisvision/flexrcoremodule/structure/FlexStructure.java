// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrcoremodule.structure;

public interface FlexStructure {
    default void initializer_() {
        dynamicBuild();
        attachEvents();
        forgettingFlexConsumers();
        initialBusinessLogic();
    }

    void baseInit();

    //This method will being called always inside their parent to avoid multiple creation of FlexAwait object anywhere the
    //onFlexContainerIsVisible is used, but the child can instead choose tho add himself in the flexStructureLooper
    default void onFlexContainerIsVisible(){
        attachSyncEvents();
        initialSyncBusinessLogic();
    };

    //In this method all definition of FlexView must be done here, if FlexView contains some other view that are building
    //inside, use...-> for EX see the MyCard class in the testPackage ................. NB : THE dynamicBuild() method is used, useful only in
    //the case that dev extend one FlexView element that can be FlexBox class, FlexInput class, FlexButton class, and want to
    //perform some custom intern definition
    void dynamicBuild();
    void attachEvents();
    void initialBusinessLogic();

    /* ** These three methods aren't abstract because they are optional for developer
     *  ** 1. attachSyncEvents(){};
     *  ** 2. initialSyncBusinessLogic(){};
     *  ** 3. forgettingFlexConsumers(){};
     */

    //The events that needs view been visible to attache Events, one parent container is visible
    void attachSyncEvents();
    void initialSyncBusinessLogic();

    /* ** Declare, Create All the FlexConsumers that will be recreate each time the FlexFragment
     * ** is called, for ex : Login, Register... these flexConsumers can be declared in the LoginBody etc...
     */
    void forgettingFlexConsumers();
}
