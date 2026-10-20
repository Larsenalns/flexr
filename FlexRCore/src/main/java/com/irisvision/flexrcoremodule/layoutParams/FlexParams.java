// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrcoremodule.layoutParams;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.GridLayout;

import androidx.appcompat.widget.LinearLayoutCompat;

import com.google.android.material.chip.ChipGroup;
import com.irisvision.flexrcoremodule.core.FlexContextInterface;
import com.irisvision.flexrcoremodule.core.FlexView;
import com.irisvision.flexrcoremodule.core.FlexViewBridge;
import com.irisvision.flexrcoremodule.valueconverter.FlexStrategy;
import com.irisvision.flexrcoremodule.valueconverter.FlexValue;
import com.irisvision.flexrcoremodule.valueconverter.Val;
import com.irisvision.flexrfxmodule.FlexParamsAnimate;

public class FlexParams implements FlexParamsAnimate {
    private ViewGroup.LayoutParams params;
    protected ParamsUnWrapper paramsUnWrapper = new ParamsUnWrapper();
    private FlexStrategy strategy;

    protected FlexContextInterface f;

    protected final int BaseState = 99887;
    protected int ml,mt,mr,mb,sizerW,sizerH;
    protected int mlTemp,mtTemp,mrTemp,mbTemp, sizerWTemp,sizerHTemp;


    private boolean autoApplyParams = false;
    public FlexParams() {
        initAnimate();
    }

    public final void paramsTo(ParamType type){
        if(type.equals(ParamType.BOX)){
            params = new FrameLayout.LayoutParams(-1,-1);
        }

        else if(type.equals(ParamType.LINE))
            params = new LinearLayoutCompat.LayoutParams(-1, -1);

        else if(type.equals(ParamType.LITE))
            params = new ViewGroup.LayoutParams(-1, -1);

        else if(type.equals(ParamType.GRID)) {
            // GridLayout.LayoutParams extends MarginLayoutParams: set the dimensions after the margins.
            // construction, faute de constructeur (int, int).
            GridLayout.LayoutParams gridParams = new GridLayout.LayoutParams();
            gridParams.width = -1;
            gridParams.height = -1;
            params = gridParams;
        }

        else if (type.equals(ParamType.CHIP))
            params = new ChipGroup.LayoutParams(-1, -1);
    }

    /*   This method provide all use case of size definition...
    ** * No matter when size need to be calculate in percent ot fixed
    ** * That's why the size is provided like a string to allow prefixing percent (%)
    */
    private FlexParams processSizer(String add, String width, String height, FlexStrategy strategy){
        //this.strategy = strategy;
        boolean canProcess = false;
        int tempWidth = 0;
        int tempHeight = 0;

        boolean widthIsEnum = false;
        boolean heightIsEnum = false;

        if(width.equalsIgnoreCase(Val.Wrap.toString())){
            width = "Wrap"; widthIsEnum = true;

        } else if(width.equalsIgnoreCase(Val.Match.toString())){
            width = "Match"; widthIsEnum = true;
        }

        if(height.equalsIgnoreCase(Val.Wrap.toString())){
            height = "Wrap"; heightIsEnum = true;

        } else if(height.equalsIgnoreCase(Val.Match.toString())){
            height = "Match"; heightIsEnum = true;
        }

        if(widthIsEnum) testEnumWidth(Val.valueOf(width));
        if(heightIsEnum) testEnumHeight(Val.valueOf(height));

        //Width Process
        if(!widthIsEnum){
            try{

                float convertedWith = convert(width);

                if((width.contains("%") && strategy == null) || (width.contains("%") && strategy != null)){

                    if(strategy == null)
                        tempWidth = FlexValue.dynamicW(this.f, convertedWith);

                    else if (FlexStrategy.Inverted.equals(strategy) || FlexStrategy.OnlyW.equals(strategy))
                        tempWidth = FlexValue.dynamicH(this.f, convertedWith);

                }
                else if(!width.contains("%") && strategy == null){
                    tempWidth = FlexValue.fixed(this.f, convertedWith);
                }

                this.params.width = toExistingParams(add, this.params.width, tempWidth);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        //Height Process
        if(!heightIsEnum){
            try {

                float convertedHeight = convert(height);

                if((height.contains("%") && strategy == null) || (height.contains("%") && strategy != null)){

                    if(strategy == null)
                        tempHeight = FlexValue.dynamicH(this.f, convertedHeight);

                    else if (FlexStrategy.Inverted.equals(strategy) || FlexStrategy.OnlyH.equals(strategy))
                        tempHeight = FlexValue.dynamicW(this.f, convertedHeight);

                }
                else if(!height.contains("%") && strategy == null){
                    tempHeight = FlexValue.fixed(this.f, convertedHeight);
                }

                this.params.height = toExistingParams(add, this.params.height, tempHeight);
                Log.w("__HEIGHT_ ", ""+params.height);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        applyParams();

        return this;
    }

    private float convert(String v){
        return FlexValue.valuePercent(v);
    }

    public final FlexParams Sizer(String width, String height){
        return processSizer("null", width, height, null);
    }

    public final FlexParams SizerTo(String width, String height){
        return processSizer("existingValue", width, height, null);
    }

    public final FlexParams Sizer(Val width, Val height){
        testEnumWidth(width);
        testEnumHeight(height);
        applyParams();

        return this;
    }

    public final FlexParams Sizer(Val val){

        testEnumWidth(val);
        testEnumHeight(val);
        applyParams();

        return this;
    }

    public final FlexParams Sizer(String width, String height, FlexStrategy strategy){
        return processSizer("null", width, height, strategy);
    }

    public final FlexParams SizerTo(String width, String height, FlexStrategy strategy){
        return processSizer("existingValue", width, height, strategy);
    }

    private boolean  wilBeNeg(int mainValue, int verifyingValue){
        return (mainValue + (verifyingValue)) < 0;
    }

    private int toExistingParams(String s, int paramsValue, int calculatedValue){
        if (s.equalsIgnoreCase("existingValue")){
            if (!wilBeNeg(paramsValue, calculatedValue)){
                paramsValue += calculatedValue;
            }
        }else{
            //Test because here is the directly assign the calculated value to param, so, if the calculated value is < to Zero
            //for example -100, -1200, -800 etc so in that case the program return the current value of the parms without
            //doing anything
            if(calculatedValue > -1)
                paramsValue = calculatedValue;
        }

        return paramsValue;
    }

    public int width(){
        return this.params.width;
    }

    public int height(){
        return this.params.height;
    }

    private void OnlyMatchX(){
        this.params.width = -1;
    }

    private void OnlyMatchY(){
        this.params.height = -1;
    }

    private void OnlyWrapX(){
        this.params.width = -2;
    }

    private void OnlyWrapY(){
        this.params.height = -2;
    }

    private void testEnumWidth(Val val){
        if((Val.Wrap).equals(val)) this.OnlyWrapX();
        else this.OnlyMatchX();
    }
    private void testEnumHeight(Val val){
        if((Val.Wrap).equals(val)) this.OnlyWrapY();
        else this.OnlyMatchY();
    }

    public final void manageParam(FlexContextInterface flexContextInterface, FlexView flexView) {
        if (this.f == null){
            this.f = flexContextInterface;
            flexView.contentWrapper(this.paramsUnWrapper);
            apply();
        }
    }

    public final void initApply(CallParamsFieldLike callParamsFieldLike){
        this.autoApplyParams = callParamsFieldLike.status();
    }

    protected final void applyParams(){
        if (this.autoApplyParams)
            this.paramsUnWrapper.apply();
    }

    public final void apply(){
        this.paramsUnWrapper.apply();
    }

    public void paramMirror(ExtendedParams extendedParams){
        if (extendedParams instanceof BoxParams || extendedParams instanceof LineParams || extendedParams instanceof GridParams || extendedParams instanceof ChipParam){
            extendedParams.get_(this.paramsUnWrapper);
        }

    }

    public void paramMirror(){
        if (this instanceof BoxParams || this instanceof LineParams || this instanceof GridParams || this instanceof ChipParam){
            ((ExtendedParams) this).get_(this.paramsUnWrapper);
        }

    }

    //FlexParamsAnimate ...
    /*  ** These setters are what makes size animation possible.
     **  Each one stores the animated value in its field (sizerW / sizerH) and returns this, so that an
     **  ObjectAnimator resolves the property and sees a non-null value on every frame. A setter that
     **  returns null, or that does not write its field, breaks the animator at a point where nothing
     **  reports an error: the property is animated, but the value never reaches applyAnimateSizer().
     **  Margin setters are overridden in ExtendedParams; these keep the base class honest — it writes
     **  into its own animation fields instead of answering null.
     */
    @Override
    public FlexParamsAnimate setFlexAnimateSizerWidth(int width) {
        this.sizerW = width;
        return this;
    }

    @Override
    public FlexParamsAnimate setFlexAnimateSizerHeight(int height) {
        this.sizerH = height;
        return this;
    }

    @Override
    public FlexParamsAnimate setFlexAnimateMargins(int value) {
        this.ml = value;
        this.mt = value;
        this.mr = value;
        this.mb = value;
        return this;
    }

    @Override
    public FlexParamsAnimate setFlexAnimateMarginLeft(int leftValue) {
        this.ml = leftValue;
        return this;
    }

    @Override
    public FlexParamsAnimate setFlexAnimateMarginTop(int topValue) {
        this.mt = topValue;
        return this;
    }

    @Override
    public FlexParamsAnimate setFlexAnimateMarginRight(int rightValue) {
        this.mr = rightValue;
        return this;
    }

    @Override
    public FlexParamsAnimate setFlexAnimateMarginBottom(int bottomValue) {
        this.mb = bottomValue;
        return this;
    }

    @Override
    public final void flexAnimateApply() {
        applyAnimateMargins();
        applyAnimateSizer();
        apply();
    }

    @Override
    public final boolean flexAnimateChecker() {
        affectValue();
        return (need(mlTemp) || need(mtTemp) || need(mrTemp) || need(mbTemp) || need(sizerWTemp) || need(sizerHTemp));
    }
    protected void applyAnimateMargins(){

    }
    protected void applyAnimateSizer(){
        if(need(sizerWTemp) || need(sizerHTemp)){
            int width = width(), height = height();

            if(need(sizerWTemp) && sizerWTemp != width())  width  = sizerWTemp;
            if(need(sizerHTemp) && sizerHTemp != height()) height = sizerHTemp;   // writes the height: the width field would be wrong here

            params.width = width;
            params.height = height;

            initSizerAnimate();
        }
    }
    protected final boolean need(int value){
        return (value != 99887);
    }
    private void initSizerAnimate(){
        sizerWTemp = BaseState;
        sizerHTemp = BaseState;
    }
    private void initAnimate(){
        ml = BaseState;
        mt = BaseState;
        mr = BaseState;
        mb = BaseState;

        sizerW = BaseState;
        sizerH = BaseState;
    }
    private void affectValue(){
        mlTemp = ml;
        mtTemp = mt;
        mrTemp = mr;
        mbTemp = mb;

        sizerWTemp = sizerW;
        sizerHTemp = sizerH;
    }

    public final class ParamsUnWrapper implements FlexViewBridge{
        private View view;

        @Override
        public void bridge(View view) {
            this.view = view;
        }

        private void apply(){
            this.view.setLayoutParams(FlexParams.this.params);
        }

        public void extendedBridge(ExtendedParams.Flasher flasher){
            flasher.flash(FlexParams.this.params);
        }
    }
}
