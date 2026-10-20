// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrcoremodule.core;

import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.GridLayout;

import com.google.android.material.chip.ChipGroup;
import com.irisvision.flexrcoremodule.layoutParams.ParamType;
import com.irisvision.flexrcoremodule.ui.container.BoxImpl;
import com.irisvision.flexrcoremodule.ui.layout.FlexTagGroup;
import com.irisvision.flexrcoremodule.ui.list.flexlists.FlexGrid;
import com.irisvision.flexrcoremodule.ui.container.LineImpl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class FlexViewGroup extends FlexView{

    /*private ArrayList<FlexViewWrapper> flexViewWArrayList;
    private FlexViewUnWrapper flexViewUnWrapper = new FlexViewUnWrapper();
    private BoxUnWrapper boxUnWrapper = new BoxUnWrapper();
    private LineUnWrapper lineUnWrapper = new LineUnWrapper();*/

    private ArrayList<FlexView> flexViewWArrayList;
    private FlexViewUnWrapper flexViewUnWrapper;
    private BoxUnWrapper boxUnWrapper;
    private LineUnWrapper lineUnWrapper;
    private GridUnWrapper gridUnWrapper;
    private ChipUnWrapper chipUnWrapper;

    private ParamType paramType = ParamType.EMPTY;

    public FlexViewGroup(FlexContextInterface flexContextInterface){
        super(flexContextInterface);
        this.flexViewWArrayList = new ArrayList<>();
    }

    private void addFlexViewWrapper(FlexView flexView){
        this.flexViewWArrayList.add(flexView);
    }

    private void removeFlexViewWrapper(int index){
        this.flexViewWArrayList.remove(index);
    }
    public final void add(FlexView flexView){

        if (flexView == null) {
            throw new IllegalArgumentException("add(null): pass the FlexView to add.");
        }

        // The list is only updated once every check has passed, so it can never hold a child that was
        // refused by the native hierarchy.
        boolean canAddToFlexViewWArrayList = false;

        if(this.flexViewUnWrapper == null) this.flexViewUnWrapper = new FlexViewUnWrapper();
        flexView.contentWrapper(this.flexViewUnWrapper);

        if (this instanceof BoxImpl){
            /* ** The test is on the CAPACITY, not on a list of classes.
             ** Every container able to bridge itself as a Box (BoxImpl provides boxWrapper) is accepted:
             ** the framework's own containers, future ones, and the developer's subclasses
             ** (MyCard extends FlexCard). A container of another family is refused, because no bridge to
             ** its native view is known.
             */

            if(this.boxUnWrapper == null){
                this.boxUnWrapper = new BoxUnWrapper();
            }
            paramType = ParamType.BOX;

            if (((BoxImpl)this).boxWrapper(this.boxUnWrapper))
                this.boxUnWrapper.getV().addView(this.flexViewUnWrapper.getV());

            canAddToFlexViewWArrayList = true;
        }
        else if(this instanceof LineImpl) {
            /* ** Same rule for the Line family (the lineWrapper capacity). The bridge is a ViewGroup,
             ** because LinearLayoutCompat, LinearLayout and RadioGroup share no closer ancestor.
             */

            if(this.lineUnWrapper == null){
                this.lineUnWrapper = new LineUnWrapper();
            }
            paramType = ParamType.LINE;

            if(((LineImpl)this).lineWrapper(this.lineUnWrapper))
                this.lineUnWrapper.getV().addView(this.flexViewUnWrapper.getV());

            canAddToFlexViewWArrayList = true;
        }
        else if (this instanceof FlexGrid) {

            if(this.gridUnWrapper == null){
                this.gridUnWrapper = new GridUnWrapper();
            }
            paramType = ParamType.GRID;

            ((FlexGrid)this).gridWrapper(this.gridUnWrapper);
            this.gridUnWrapper.getV().addView(flexViewUnWrapper.getV());

            canAddToFlexViewWArrayList = true;
        }
        else if (this instanceof FlexTagGroup) {

            if(this.chipUnWrapper == null){
                this.chipUnWrapper = new ChipUnWrapper();
            }
            paramType = ParamType.CHIP;

            ((FlexTagGroup)this).chipWrapper(this.chipUnWrapper);
            this.chipUnWrapper.getV().addView(this.flexViewUnWrapper.getV());

            canAddToFlexViewWArrayList = true;
        }

        //Now can add cause all the test this instanceof ... are done
        if (canAddToFlexViewWArrayList){
            if (flexView.contentWrapper(this.flexViewUnWrapper)){
                addFlexViewWrapper(flexView);
            }
        }

    }

    public final void addAll(FlexView[] flexViews){
        for (FlexView flexView : flexViews) {
            add(flexView);
        }
    }

    public final void addAll(ArrayList<FlexView> flexViews){
        for (int i = 0; i < flexViews.size(); i++){
            add(flexViews.get(i));
        }
    }

    public final void remove(FlexView flexView){
        if(this.flexViewUnWrapper == null) this.flexViewUnWrapper = new FlexViewUnWrapper();

        if (flexView.contentWrapper(this.flexViewUnWrapper)){
            remove(this.flexViewWArrayList.indexOf(flexView));
        }
    }

    public final void remove(int index){
        if(this.flexViewUnWrapper == null) this.flexViewUnWrapper = new FlexViewUnWrapper();
        if (index > -1 && index < this.flexViewWArrayList.size()){
            this.flexViewWArrayList.remove(index);
            switch (paramType){
                case BOX: {
                    this.boxUnWrapper.getV().removeViewAt(index);
                }break;

                case LINE: {
                    this.lineUnWrapper.getV().removeViewAt(index);
                }break;

                case GRID: {
                    this.gridUnWrapper.getV().removeViewAt(index);
                }break;

                case CHIP:{
                    this.chipUnWrapper.getV().removeViewAt(index);
                }break;
            }
        }

    }

    public final void removeAll(FlexView[] flexViews){
        for(FlexView f: flexViews){
            remove(f);
        }
    }

    public final void removeAll(ArrayList<FlexView> flexView){
        flexView.forEach(f-> remove(f));
    }

    public final FlexView child(int index){
        return this.flexViewWArrayList.get(index);
    }

    /**
     * A read-only view of the children, in the order they were added.
     *
     * <p>The developer may iterate, count, search and index into it. What it does <b>not</b> allow is
     * mutation: clearing it, or adding to it directly, would desynchronise the Java list from the native
     * view hierarchy, and the indexes used by {@link #child(int)} and {@link #remove(int)} would become
     * wrong. To modify the container, use {@link #add(FlexView)}, {@link #remove(FlexView)} or
     * {@link #remove(int)}.
     *
     * @return an unmodifiable view of the children
     */
    public final List<FlexView> children() {
        return Collections.unmodifiableList(flexViewWArrayList);
    }

    public final int index(FlexView flexView){
        return this.flexViewWArrayList.indexOf(flexView);
    }

    public final int count(){
        return this.flexViewWArrayList.size();
    }

    public static boolean chekWrapper(FlexViewBridge flexViewBridge){
        return flexViewBridge instanceof FlexViewUnWrapper;
    }

    private class FlexViewUnWrapper implements FlexViewBridge{
        private View view;

        @Override
        public void bridge(View view) {
           this.view = view;
        }

        private View getV(){
            return this.view;
        }
    }

    public static final class BoxUnWrapper implements BoxBridge {
        private FrameLayout view;

        @Override
        public void bridge(FrameLayout view) {
            this.view = view;
        }

        private FrameLayout getV(){
            return this.view;
        }
    }


    public static final class LineUnWrapper implements LineBridge {

        //The LINE Process/Impl use ViewGroup like param for Bridge because, those classes are managed inside the Line Process

        /* ** public class LinearLayoutCompat extends ViewGroup
         * ** public class LinearLayout extends ViewGroup
         * ** LinearLayoutCompat and LinearLayout are all extend the ViewGroup class
         * ** And RadioGroup extend only LinearLayout, not LinearLayoutCompat
         * ** So in this case the only common ancestor is ViewGroup
        * */
        private ViewGroup view;

        @Override
        public void bridge(ViewGroup view) {
            this.view = view;
        }

        private ViewGroup getV(){
            return this.view;
        }
    }

    public static final class GridUnWrapper implements GridBridge {
        private GridLayout view;

        @Override
        public void bridge(GridLayout view) {
            this.view = view;
        }

        private GridLayout getV(){
            return this.view;
        }
    }

    public static final class ChipUnWrapper implements ChipBridge {
        private ChipGroup view;

        @Override
        public void bridge(ChipGroup view) {
            this.view = view;
        }

        private ChipGroup getV(){
            return this.view;
        }
    }
}
