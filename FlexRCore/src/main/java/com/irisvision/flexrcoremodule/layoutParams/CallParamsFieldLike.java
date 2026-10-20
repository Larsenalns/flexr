// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrcoremodule.layoutParams;

import com.irisvision.flexrcoremodule.core.FlexView;
import com.irisvision.flexrfxmodule.FlexAnimate;

public abstract class CallParamsFieldLike implements CallParamsInterface{
    private FlexParams params;
    private ParamType paramType = ParamType.EMPTY;

    private boolean autoApplyParams = false;
    @Override
    public final FlexParams params() {
        if (isTypeDifferent(ParamType.LITE)){
            this.params = new FlexParams();
            flexSwitch();
        }
        return this.params;
    }

    @Override
    public final BoxParams boxParams() {
        if (isTypeDifferent(ParamType.BOX)){
            this.params = new BoxParams();
            flexSwitch();
            params.paramMirror();
        }
        return (BoxParams) this.params;
    }

    @Override
    public final LineParams lineParams() {
        if (isTypeDifferent(ParamType.LINE)){
            this.params = new LineParams();
            flexSwitch();
            params.paramMirror((LineParams)params);
        }
        return (LineParams) this.params;
    }

    @Override
    public final GridParams gridParams() {
        if (isTypeDifferent(ParamType.GRID)){
            this.params = new GridParams();
            flexSwitch();
            params.paramMirror((GridParams)params);
        }
        return (GridParams) this.params;
    }

    @Override
    public ChipParam chipParams() {
        if(isTypeDifferent(ParamType.CHIP)){
            this.params = new ChipParam();
            flexSwitch();
            params.paramMirror((ChipParam)params);
        }
        init();
        return (ChipParam) this.params;
    }

    public void resetParams(){
        this.paramType = ParamType.EMPTY;
    }

    public abstract void call(FlexParams params);

    private boolean isTypeDifferent(ParamType paramType){
        if (!this.paramType.equals(paramType)){

            this.paramType = paramType;
            return true;
        }
        return false;
    }

    private void flexSwitch(){
        /* Here params get casted to the paramType that passed in the compare method if the current paramType
         ** In this class is Different to the paramType passed like argument to compare method.
         ** This allow all the FlexView to switch between different type of LayoutParams anytime and everywhere
         ** during the program
         * */
        params.paramsTo(this.paramType);

        /* The call method is an abstract method that got implementation in the FlexView class to perform
         ** The setLayoutParams action to all the FlexView ( all the classes that extends FlexView )
         */
        call(this.params);
    }

    public final CallParamsFieldLike autoApply(boolean autoApplyParams){
        this.autoApplyParams = autoApplyParams;
        init();
        return this;
    }

    public final boolean status(){
        return this.autoApplyParams;
    }

    private void init(){
        //this method allow user to activate autoApplyParams or not
        if (this.params != null)
            this.params.initApply(this);
    }
}
