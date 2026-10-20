// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrcoremodule.layoutParams;

import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.GridLayout;

import androidx.appcompat.widget.LinearLayoutCompat;

import com.google.android.material.chip.ChipGroup;
import com.irisvision.flexrcoremodule.enums.FlexDirection;
import com.irisvision.flexrcoremodule.valueconverter.FlexValue;
import com.irisvision.flexrfxmodule.FlexParamsAnimate;

public abstract class ExtendedParams extends FlexParams {
    //To performs in this class all common methods for BoxParams and LineParams

    private FrameLayout.LayoutParams box;
    private LinearLayoutCompat.LayoutParams line;
    private GridLayout.LayoutParams grid;
    private ChipGroup.LayoutParams chip;

    protected final void get_(ParamsUnWrapper paramsUnWrapper){

        //Testing the instance type and by the same way ensure that only FrameWork class can get the reference of
        //params in the FlexParam class
        paramsUnWrapper.extendedBridge(new Flasher());

    }

    public ExtendedParams outerGravity(String s){
        //Only these, because ChipLayoutParam doesn't contain gravity properties, just margin
        if (this instanceof BoxParams) this.box.gravity = FlexValue.gravity(s);
        else if (this instanceof LineParams) this.line.gravity = FlexValue.gravity(s);
        else if (this instanceof GridParams) this.grid.setGravity(FlexValue.gravity(s));

        applyParams();
        return this;
    }

    public ExtendedParams margins(String values){
        ViewGroup.MarginLayoutParams currentMargins = currentMargins();

        if(currentMargins != null) {
            int[] ints = FlexMargins.processAll(f, values, false, currentMargins.leftMargin, currentMargins.topMargin, currentMargins.rightMargin, currentMargins.bottomMargin);
            assignMargins(currentMargins, ints[0], ints[1], ints[2], ints[3]);
        }
        return this;
    }

    public ExtendedParams marginsTo(String values){
        ViewGroup.MarginLayoutParams currentMargins = currentMargins();

        if(currentMargins != null) {
            int[] ints = FlexMargins.processAll(f, values, true, currentMargins.leftMargin, currentMargins.topMargin, currentMargins.rightMargin, currentMargins.bottomMargin);
            assignMargins(currentMargins, ints[0], ints[1], ints[2], ints[3]);
        }
        return this;
    }

    public ExtendedParams marginOnly(String values, String strategy){
        ViewGroup.MarginLayoutParams currentMargins = currentMargins();

        if(currentMargins != null){
            int[] ints = FlexMargins.processOnly(f, values, strategy, false, currentMargins.leftMargin, currentMargins.topMargin, currentMargins.rightMargin, currentMargins.bottomMargin);
            assignMargins(currentMargins, ints[0], ints[1], ints[2], ints[3]);
        }
        return this;
    }
    public ExtendedParams marginOnlyTo(String values, String strategy){
        ViewGroup.MarginLayoutParams currentMargins = currentMargins();

        if(currentMargins != null){
            int[] ints = FlexMargins.processOnly(f, values, strategy, true, currentMargins.leftMargin, currentMargins.topMargin, currentMargins.rightMargin, currentMargins.bottomMargin);
            assignMargins(currentMargins, ints[0], ints[1], ints[2], ints[3]);
        }
        return this;
    }

    protected final FlexParams column(int colIndex, int colSpan){
        grid.columnSpec = GridLayout.spec(colIndex, colSpan);
        applyParams();
        return this;
    }

    protected final FlexParams row(int rowIndex, int rowSpan){
        grid.rowSpec = GridLayout.spec(rowIndex, rowSpan);
        applyParams();
        return this;
    }

    //FlexAnimate Methods

    @Override
    public FlexParamsAnimate setFlexAnimateMargins(int value) {

        setFlexAnimateMarginLeft(value);
        setFlexAnimateMarginTop(value);
        setFlexAnimateMarginRight(value);
        setFlexAnimateMarginBottom(value);

        return this;
    }

    @Override
    public FlexParamsAnimate setFlexAnimateMarginLeft(int leftValue) {
        ml = leftValue;
        return this;
    }

    @Override
    public FlexParamsAnimate setFlexAnimateMarginTop(int topValue) {
        mt = topValue;
        return this;
    }

    @Override
    public FlexParamsAnimate setFlexAnimateMarginRight(int rightValue) {
        mr = rightValue;
        return this;
    }

    @Override
    public FlexParamsAnimate setFlexAnimateMarginBottom(int bottomValue) {
        mb = bottomValue;
        return this;
    }

    @Override
    protected final void applyAnimateMargins() {
        if (need(mlTemp) || need(mtTemp) || need(mrTemp) || need(mbTemp)) {
            ViewGroup.MarginLayoutParams margins = currentMargins();

            if (margins != null){
                int left = margins.leftMargin, top = margins.topMargin;
                int right = margins.rightMargin, bottom = margins.bottomMargin;

                if (need(mlTemp) && mlTemp != left) left = mlTemp;
                if (need(mtTemp) && mtTemp != top) top = mtTemp;
                if (need(mrTemp) && mrTemp != right) right = mrTemp;
                if (need(mbTemp) && mbTemp != bottom) bottom = mbTemp;

                assignMargins(margins, left, top, right, bottom);
            }
            initMarginsAnimate();
        }
    }

    private ViewGroup.MarginLayoutParams currentMargins(){
        if (this instanceof BoxParams)
           return this.box;

        else if (this instanceof LineParams)
            return this.line;

        else if (this instanceof GridParams)
            return this.grid;

        else if (this instanceof ChipParam)
            return this.chip;

        return null;
    }

    private void assignMargins(ViewGroup.MarginLayoutParams params, int left, int top, int right, int bottom){
        params.leftMargin = left;
        params.topMargin = top;
        params.rightMargin = right;
        params.bottomMargin = bottom;
    }

    private void initMarginsAnimate(){
        mlTemp = BaseState;
        mtTemp = BaseState;
        mrTemp = BaseState;
        mbTemp = BaseState;
    }

    public final void marginsState(){
        ViewGroup.MarginLayoutParams params = currentMargins();
        System.out.println("MARGINS = LEFT->"+ params.leftMargin+"   TOP->"+params.topMargin+"    RIGHT->"+ params.rightMargin+"   BOTTOM->"+params.bottomMargin);
    }

    /**
     * Applies the native layout params to the view, casting them according to the container's family.
     *
     * <p>Each family has its own native params type — {@code FrameLayout.LayoutParams} for a Box,
     * {@code LinearLayout.LayoutParams} for a Line, {@code GridLayout.LayoutParams} for a Grid — and the
     * cast must match the container that received the child, or the layout silently comes out wrong.
     */
    public final class Flasher {
        public void flash(ViewGroup.LayoutParams params){
            if (ExtendedParams.this instanceof BoxParams){
                box = (FrameLayout.LayoutParams) params;
            }

            if (ExtendedParams.this instanceof LineParams){
                line = (LinearLayoutCompat.LayoutParams) params;
            }

            if (ExtendedParams.this instanceof GridParams){
                grid = (GridLayout.LayoutParams) params;
            }

            if (ExtendedParams.this instanceof ChipParam){
                chip = (ChipGroup.LayoutParams) params;
            }
        }
    }
}
