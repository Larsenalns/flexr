// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrcoremodule.ui.component;

import android.content.res.ColorStateList;

import com.google.android.material.shape.AbsoluteCornerSize;
import com.google.android.material.shape.CornerFamily;
import com.google.android.material.shape.CornerSize;
import com.google.android.material.shape.EdgeTreatment;
import com.google.android.material.shape.MarkerEdgeTreatment;
import com.google.android.material.shape.MaterialShapeDrawable;
import com.google.android.material.shape.OffsetEdgeTreatment;
import com.google.android.material.shape.RelativeCornerSize;
import com.google.android.material.shape.ShapeAppearanceModel;
import com.google.android.material.shape.TriangleEdgeTreatment;
import com.irisvision.flexrcoremodule.core.FlexContextInterface;
import com.irisvision.flexrcoremodule.core.FlexView;
import com.irisvision.flexrcoremodule.enums.FlexDirection;
import com.irisvision.flexrcoremodule.enums.FlexEdge;
import com.irisvision.flexrcoremodule.ui.container.FlexCard;
import com.irisvision.flexrcoremodule.ui.layout.FlexImg;
import com.irisvision.flexrcoremodule.valueconverter.FlexValue;
import com.irisvision.flexrthememodule.FlexTheme;

/**
 * Reusable Material shape: corners (square, rounded, cut), edges (triangle, marker, offset) and
 * colors, which are then applied to a view.
 *
 * <h2>The two application modes</h2>
 * <ul>
 *   <li>{@code applyDrawable(...)} hands out the shape's <b>internal</b> {@link MaterialShapeDrawable}:
 *       the view uses it as its background. This is the mode used by
 *       {@code FlexView.background(shape)} and {@code FlexCard.applyShape(shape)};</li>
 *   <li>{@code applyAppearance(...)} only passes the {@link ShapeAppearanceModel} (the model) to a
 *       view that already manages its own background ({@code ShapeableImageView},
 *       {@code MaterialCardView}). This is the mode used by {@code appearance(shape)}, the one
 *       that breaks neither the clip nor the ripple of the Material component.</li>
 * </ul>
 *
 * <h2>One instance = one single Drawable</h2>
 * A shape owns <b>exactly one</b> {@code MaterialShapeDrawable}. Calling {@code background(shape)}
 * on two different views would hand them the <b>same</b> Drawable, and a Drawable has only one
 * callback and one set of bounds: the second view would override the rendering of the first. To
 * style two views, create two {@code FlexShape} instances, or use {@code appearance(...)} only,
 * which shares an immutable model.
 *
 * <h2>Non-destructive composition</h2>
 * The setter methods ({@code toCornerXxx}, {@code toEdgeXxx}, {@code corners}, {@code circular},
 * {@code roundSquare}) all <b>start from the current model</b> ({@code toBuilder()}): chaining
 * {@code toCornerTopLeft(...)} then {@code circular()} does not lose the first setting.
 * {@link #newCompose()} remains the only explicit reset point.
 *
 * <h2>Units</h2>
 * Corner and edge sizes follow the selected mode:
 * <ul>
 *   <li>{@code relative... = true}: the value is a <b>fraction</b> ({@code 0.5} = 50% of the
 *       dimension), as defined by {@link RelativeCornerSize};</li>
 *   <li>{@code relative... = false}: the value is an <b>absolute dimension in dp</b>, converted to
 *       pixels by {@link FlexValue#fixed};</li>
 *   <li>note: the relative mode of <b>edges</b> is a fraction of the <b>screen</b>
 *       ({@code FlexValue.dynamicH/dynamicW}), not of the view.</li>
 * </ul>
 *
 * <p>Instances are mutable and not thread-safe. Configure a shape on the thread that owns the
 * target view, which is normally the main thread, and apply it before the view is drawn.
 */
public final class FlexShape {

    /** Sentinel meaning that no theme color index has been recorded. */
    private static final int NO_THEME = -1;

    private ShapeAppearanceModel shape;
    private final MaterialShapeDrawable materialShapeDrawable;
    private final FlexContextInterface f;
    private FlexEdge edge;
    private float cornerValue, edgeValue, offsetValue;
    private boolean cutCorner, relativeCorner, outSide, relativeEdge, relativeOffset, offset;

    /* Colors kept so they can be re-applied when the theme changes. */
    private int shapeColorTheme = NO_THEME;
    private int strokeTheme = NO_THEME;
    private float strokeWidthPx;

    /**
     * Creates an empty shape bound to the given FlexR context.
     *
     * <p>The shape starts with a plain {@link ShapeAppearanceModel} (square corners, straight
     * edges) and owns its own {@link MaterialShapeDrawable}. The context is only used to convert dp
     * values into pixels and to resolve theme colors, so it must outlive the shape.
     *
     * <pre>{@code
     * FlexShape shape = new FlexShape(flexContext).circular().shapeColor(primaryIndex);
     * }</pre>
     *
     * @param flexContext context used for dp conversion and theme color lookup
     */
    public FlexShape(FlexContextInterface flexContext) {
        this.f = flexContext;
        this.shape = new ShapeAppearanceModel.Builder().build();
        this.materialShapeDrawable = new MaterialShapeDrawable();
    }

    /**
     * Rounds every corner by half of the shape dimension, producing a pill or a circle.
     *
     * <p>The corner is a {@link RelativeCornerSize} of {@code 0.5f}, so it is resolved against the
     * bounds of the shape instead of being read as an absolute pixel radius.
     *
     * @return this instance, for chaining
     */
    public FlexShape circular(){
        this.shape = this.shape.toBuilder()
                .setAllCornerSizes(new RelativeCornerSize(0.5f))
                .build();
        return this;
    }

    /**
     * Slightly rounded square: every corner is rounded by 9% of the shape dimension.
     *
     * <p>Like {@link #circular()}, the value is a relative {@link CornerSize}, so it follows the
     * bounds of the shape.
     *
     * @return this instance, for chaining
     */
    public FlexShape roundSquare(){
        this.shape = this.shape.toBuilder()
                .setAllCornerSizes(new RelativeCornerSize(0.09f))
                .build();
        return this;
    }

    /**
     * Sets the same corner style and size on the four corners at once.
     *
     * @param cornerValue   fraction when {@code relativeValue} is {@code true}, otherwise dp
     * @param cutCorner     {@code true} for cut corners, {@code false} for rounded corners
     * @param relativeValue {@code true} to interpret {@code cornerValue} relative to the shape
     *                      dimension
     * @return this instance, for chaining
     */
    public FlexShape corners(float cornerValue, boolean cutCorner, boolean relativeValue){
        this.shape = this.shape.toBuilder()
                .setTopLeftCorner(cornerProcess(cutCorner), cornerSizeProcess(cornerValue, relativeValue))
                .setTopRightCorner(cornerProcess(cutCorner), cornerSizeProcess(cornerValue, relativeValue))
                .setBottomRightCorner(cornerProcess(cutCorner), cornerSizeProcess(cornerValue, relativeValue))
                .setBottomLeftCorner(cornerProcess(cutCorner), cornerSizeProcess(cornerValue, relativeValue))
                .build();
        return this;
    }

    /**
     * Resets the shape to its empty state: square corners, straight edges, no edge treatment.
     *
     * <p>This is the <b>only</b> reset point of the class: every other setter starts from the
     * current model, so corners and edges can be composed one by one without losing earlier
     * settings.
     *
     * @return this instance, for chaining
     */
    public FlexShape newCompose(){
        this.shape = new ShapeAppearanceModel.Builder().build();
        return this;
    }

    /**
     * Switches corners, edges and offsets to <b>relative</b> (fractional) values.
     *
     * <p>The setting applies to every subsequent setter call on this instance.
     *
     * @return this instance, for chaining
     */
    public FlexShape likeRelative(){
        this.relativeCorner = true;
        this.relativeEdge = true;
        this.relativeOffset = true;
        return this;
    }
    /**
     * Selects how corner sizes of the following calls are interpreted.
     *
     * @param relativeCorner {@code true} for a fraction of the shape dimension, {@code false} for dp
     * @return this instance, for chaining
     */
    public FlexShape relativeCorner(boolean relativeCorner){
        this.relativeCorner = relativeCorner;
        return this;
    }
    /**
     * Selects how edge sizes of the following calls are interpreted.
     *
     * @param relativeEdge {@code true} for a fraction of the screen, {@code false} for dp
     * @return this instance, for chaining
     */
    public FlexShape relativeEdge(boolean relativeEdge){
        this.relativeEdge = relativeEdge;
        return this;
    }
    /**
     * Selects how edge offsets of the following calls are interpreted.
     *
     * @param relativeOffset {@code true} for a fraction of the screen, {@code false} for dp
     * @return this instance, for chaining
     */
    public FlexShape relativeOffset(boolean relativeOffset){
        this.relativeOffset = relativeOffset;
        return this;
    }
    /**
     * Enables and sizes the offset applied to the edge treatments built afterwards.
     *
     * @param applyOffset {@code true} to wrap each edge treatment in an {@link OffsetEdgeTreatment}
     * @param offsetValue offset value, read as a fraction when relative offset mode is on, else dp
     * @return this instance, for chaining
     */
    public FlexShape edgeOffset(boolean applyOffset, float offsetValue){
        this.offsetValue = offsetValue;
        this.offset = applyOffset;
        return this;
    }

    /**
     * Sets the top-left corner, leaving the other three untouched.
     *
     * @param cornerValue fraction when relative corner mode is on, otherwise dp
     * @param cutCorner   {@code true} for a cut corner, {@code false} for a rounded one
     * @return this instance, for chaining
     */
    public FlexShape toCornerTopLeft(float cornerValue, boolean cutCorner){
        initCorner(cornerValue, cutCorner);
        this.shape = this.shape.toBuilder()
                .setTopLeftCorner(cornerProcess(this.cutCorner), cornerSizeProcess(this.cornerValue, this.relativeCorner))
                .build();
        return this;
    }
    /**
     * Sets the bottom-left corner, leaving the other three untouched.
     *
     * @param cornerValue fraction when relative corner mode is on, otherwise dp
     * @param cutCorner   {@code true} for a cut corner, {@code false} for a rounded one
     * @return this instance, for chaining
     */
    public FlexShape toCornerBottomLeft(float cornerValue, boolean cutCorner){
        initCorner(cornerValue, cutCorner);
        this.shape = this.shape.toBuilder()
                .setBottomLeftCorner(cornerProcess(this.cutCorner), cornerSizeProcess(this.cornerValue, this.relativeCorner))
                .build();
        return this;
    }
    /**
     * Sets the top-right corner, leaving the other three untouched.
     *
     * @param cornerValue fraction when relative corner mode is on, otherwise dp
     * @param cutCorner   {@code true} for a cut corner, {@code false} for a rounded one
     * @return this instance, for chaining
     */
    public FlexShape toCornerTopRight(float cornerValue, boolean cutCorner){
        initCorner(cornerValue, cutCorner);
        this.shape = this.shape.toBuilder()
                .setTopRightCorner(cornerProcess(this.cutCorner), cornerSizeProcess(this.cornerValue, this.relativeCorner))
                .build();
        return this;
    }
    /**
     * Sets the bottom-right corner, leaving the other three untouched.
     *
     * @param cornerValue fraction when relative corner mode is on, otherwise dp
     * @param cutCorner   {@code true} for a cut corner, {@code false} for a rounded one
     * @return this instance, for chaining
     */
    public FlexShape toCornerBottomRight(float cornerValue, boolean cutCorner){
        initCorner(cornerValue, cutCorner);
        this.shape = this.shape.toBuilder()
                .setBottomRightCorner(cornerProcess(this.cutCorner), cornerSizeProcess(this.cornerValue, this.relativeCorner))
                .build();
        return this;
    }
    /**
     * Sets the same corner style and size on the four corners, starting from the current model.
     *
     * @param cornerValue fraction when relative corner mode is on, otherwise dp
     * @param cutCorner   {@code true} for cut corners, {@code false} for rounded ones
     * @return this instance, for chaining
     */
    public FlexShape toAllCorners(float cornerValue, boolean cutCorner){
        initCorner(cornerValue, cutCorner);
        this.shape = this.shape.toBuilder()
                .setTopLeftCorner(cornerProcess(this.cutCorner), cornerSizeProcess(this.cornerValue, this.relativeCorner))
                .setTopRightCorner(cornerProcess(this.cutCorner), cornerSizeProcess(this.cornerValue, this.relativeCorner))
                .setBottomRightCorner(cornerProcess(this.cutCorner), cornerSizeProcess(this.cornerValue, this.relativeCorner))
                .setBottomLeftCorner(cornerProcess(this.cutCorner), cornerSizeProcess(this.cornerValue, this.relativeCorner))
                .build();
        return this;
    }

    /**
     * Sets the treatment of the top edge, leaving the other three untouched.
     *
     * @param edge      edge shape to build ({@link FlexEdge})
     * @param edgeValue fraction of the screen height when relative edge mode is on, otherwise dp
     * @param outSide   {@code true} to point the treatment outwards
     * @return this instance, for chaining
     */
    public FlexShape toEdgeTop(FlexEdge edge, float edgeValue, boolean outSide){
        edgeInit(edge, edgeValue, outSide);
        this.shape = this.shape.toBuilder()
                .setTopEdge(edgeTreatmentProcess(this.edge, FlexDirection.T, this.edgeValue, this.relativeEdge, this.outSide, this.offsetValue, this.relativeOffset, this.offset))
                .build();
        return this;
    }
    /**
     * Sets the treatment of the left edge, leaving the other three untouched.
     *
     * @param edge      edge shape to build ({@link FlexEdge})
     * @param edgeValue fraction of the screen width when relative edge mode is on, otherwise dp
     * @param outSide   {@code true} to point the treatment outwards
     * @return this instance, for chaining
     */
    public FlexShape toEdgeLeft(FlexEdge edge, float edgeValue, boolean outSide){
        edgeInit(edge, edgeValue, outSide);
        this.shape = this.shape.toBuilder()
                .setLeftEdge(edgeTreatmentProcess(this.edge, FlexDirection.L, this.edgeValue, this.relativeEdge, this.outSide, this.offsetValue, this.relativeOffset, this.offset))
                .build();
        return this;
    }
    /**
     * Sets the treatment of the right edge, leaving the other three untouched.
     *
     * @param edge      edge shape to build ({@link FlexEdge})
     * @param edgeValue fraction of the screen width when relative edge mode is on, otherwise dp
     * @param outSide   {@code true} to point the treatment outwards
     * @return this instance, for chaining
     */
    public FlexShape toEdgeRight(FlexEdge edge, float edgeValue, boolean outSide){
        edgeInit(edge, edgeValue, outSide);
        this.shape = this.shape.toBuilder()
                .setRightEdge(edgeTreatmentProcess(this.edge, FlexDirection.R, this.edgeValue, this.relativeEdge, this.outSide, this.offsetValue, this.relativeOffset, this.offset))
                .build();
        return this;
    }
    /**
     * Sets the treatment of the bottom edge, leaving the other three untouched.
     *
     * @param edge      edge shape to build ({@link FlexEdge})
     * @param edgeValue fraction of the screen height when relative edge mode is on, otherwise dp
     * @param outSide   {@code true} to point the treatment outwards
     * @return this instance, for chaining
     */
    public FlexShape toEdgeBottom(FlexEdge edge, float edgeValue, boolean outSide){
        edgeInit(edge, edgeValue, outSide);
        this.shape = this.shape.toBuilder()
                .setBottomEdge(edgeTreatmentProcess(this.edge, FlexDirection.B, this.edgeValue, this.relativeEdge, this.outSide, this.offsetValue, this.relativeOffset, this.offset))
                .build();
        return this;
    }

    /**
     * Applies the same edge treatment type to the four sides.
     *
     * <p>Each side is computed with its own {@link FlexDirection}, so in relative mode the left and
     * right edges are dimensioned against the <b>width</b> of the screen while the top and bottom
     * edges use its <b>height</b>.
     *
     * @param edge      edge shape to build ({@link FlexEdge})
     * @param edgeValue fraction when relative edge mode is on, otherwise dp
     * @param outSide   {@code true} to point the treatments outwards
     * @return this instance, for chaining
     */
    public FlexShape toAllEdges(FlexEdge edge, float edgeValue, boolean outSide){
        edgeInit(edge, edgeValue, outSide);
        this.shape = this.shape.toBuilder()
                .setTopEdge(edgeTreatmentProcess(this.edge, FlexDirection.T, this.edgeValue, this.relativeEdge, this.outSide, this.offsetValue, this.relativeOffset, this.offset))
                .setBottomEdge(edgeTreatmentProcess(this.edge, FlexDirection.B, this.edgeValue, this.relativeEdge, this.outSide, this.offsetValue, this.relativeOffset, this.offset))
                .setLeftEdge(edgeTreatmentProcess(this.edge, FlexDirection.L, this.edgeValue, this.relativeEdge, this.outSide, this.offsetValue, this.relativeOffset, this.offset))
                .setRightEdge(edgeTreatmentProcess(this.edge, FlexDirection.R, this.edgeValue, this.relativeEdge, this.outSide, this.offsetValue, this.relativeOffset, this.offset))
                .build();
        return this;
    }

    private void edgeInit(FlexEdge edge, float edgeValue, boolean outSide){
        this.edge = edge; this.edgeValue = edgeValue; this.outSide = outSide;
    }
    private void initCorner(float cornerValue, boolean cutCorner) {
        this.cornerValue = cornerValue; this.cutCorner = cutCorner;
    }

    /**
     * Sets the fill color of the shape.
     *
     * <p>The index is remembered so that {@link #applyTheme()} can resolve it again after a theme
     * change.
     *
     * @param themeColorIndex index in the palette of the current theme
     * @return this instance, for chaining
     */
    public FlexShape shapeColor(int themeColorIndex){
        this.shapeColorTheme = themeColorIndex;
        this.materialShapeDrawable.setFillColor(ColorStateList.valueOf(FlexTheme.load(themeColorIndex)));
        return this;
    }

    /**
     * Sets the outline width and color of the shape.
     *
     * <p>Both the index and the converted width are remembered so that {@link #applyTheme()} can
     * re-apply the stroke after a theme change.
     *
     * @param strokeWidth width in <b>dp</b>; it is converted with {@code FlexValue.fixed} like every
     *                    other dimension of the library
     * @param strokeColor index in the palette of the current theme
     * @return this instance, for chaining
     */
    public FlexShape stroke(float strokeWidth, int strokeColor){
        this.strokeTheme = strokeColor;
        this.strokeWidthPx = FlexValue.fixed(f, strokeWidth);
        this.materialShapeDrawable.setStroke(this.strokeWidthPx, ColorStateList.valueOf(FlexTheme.load(strokeColor)));
        return this;
    }

    /**
     * Re-applies to the internal Drawable the colors recorded by {@link #shapeColor(int)} and
     * {@link #stroke(float, int)}.
     *
     * <p>Called by the owner of the shape (view or card) when the theme changes; without it a shape
     * used as a background would keep the color of the previous theme.
     *
     * <p>The fill and the stroke are only touched when they were configured. Call it on the main
     * thread, together with the view the shape decorates.
     */
    public void applyTheme(){
        if (shapeColorTheme != NO_THEME) {
            this.materialShapeDrawable.setFillColor(ColorStateList.valueOf(FlexTheme.load(shapeColorTheme)));
        }
        if (strokeTheme != NO_THEME) {
            this.materialShapeDrawable.setStroke(this.strokeWidthPx, ColorStateList.valueOf(FlexTheme.load(strokeTheme)));
        }
    }

    private CornerSize cornerSizeProcess(float cornerValue, boolean relativeValue){
        // A CornerSize carries its own mode: RelativeCornerSize is genuinely relative (a fraction
        // of the shape dimension), whereas AbsoluteCornerSize expects a dimension in pixels.
        return relativeValue ? new RelativeCornerSize(cornerValue) : new AbsoluteCornerSize(cornerValue);
    }

    private float edgeValueProcess(FlexDirection direction, float edgeValue, boolean relativeValue){
        if (relativeValue) {

            if(direction.equals(FlexDirection.T) || direction.equals(FlexDirection.B))
                edgeValue = FlexValue.dynamicH(f, edgeValue);

            else if (direction.equals(FlexDirection.L) || direction.equals(FlexDirection.R))
                edgeValue = FlexValue.dynamicW(f, edgeValue);
        }

        else edgeValue = FlexValue.fixed(f, edgeValue);

        return edgeValue;
    }

    private EdgeTreatment edgeTreatmentProcess(FlexEdge edge, FlexDirection direction, float edgeValue, boolean relativeEdge, boolean outSide, float offsetValue, boolean relativeOffset, boolean offset){

        EdgeTreatment edgeTreatment;

        if (edge.equals(FlexEdge.Triangle))
            edgeTreatment = new TriangleEdgeTreatment(edgeValueProcess(direction, edgeValue, relativeEdge), outSide);
        else edgeTreatment = new MarkerEdgeTreatment(edgeValueProcess(direction, edgeValue, relativeEdge));

        if (offset){
            offsetValue = edgeValueProcess(direction, offsetValue, relativeOffset);
            edgeTreatment = new OffsetEdgeTreatment(edgeTreatment, offsetValue);
        }

        return edgeTreatment;
    }

    private int cornerProcess(boolean cutCorner){
        return (cutCorner)? CornerFamily.CUT :  CornerFamily.ROUNDED;
    }

    /**
     * Applies the shape to a {@link FlexView} through its bridge.
     *
     * <p>Dedicated to {@code FlexView}: the internal {@link MaterialShapeDrawable} is handed to the
     * view, which uses it as an ordinary background Drawable and therefore inherits every benefit
     * of the appearance.
     *
     * <p>The Drawable is shared, not copied: only one view may use this shape at a time.
     *
     * @param flexShapeBridge bridge provided by {@link FlexView} that receives the Drawable
     */
    public void applyDrawable(FlexView.FlexShapeBridge flexShapeBridge){
        this.materialShapeDrawable.setShapeAppearanceModel(this.shape);
        flexShapeBridge.bridge(this.materialShapeDrawable);
    }
    /**
     * Applies the shape to a {@link FlexCard} through its bridge.
     *
     * <p>Same contract as {@link #applyDrawable(FlexView.FlexShapeBridge)}: the internal Drawable is
     * shared, so a card and a view must not use the same shape instance.
     *
     * @param flexShapeBridge bridge provided by {@link FlexCard} that receives the Drawable
     */
    public void applyDrawable(FlexCard.FlexShapeBridge flexShapeBridge){
        this.materialShapeDrawable.setShapeAppearanceModel(this.shape);
        flexShapeBridge.bridge(this.materialShapeDrawable);
    }

    /**
     * Applies the shape model to a {@link FlexImg} that already implements the Shapeable contract.
     *
     * <p>Only the {@link ShapeAppearanceModel} is transmitted, so the default inner behavior, the
     * clip and the ripple of the Material component are preserved. Unlike the Drawable mode, the
     * model is immutable and can safely be shared between views.
     *
     * @param flexAppearanceBridge bridge provided by {@link FlexImg} that receives the model
     */
    public void applyAppearance(FlexImg.FlexAppearanceBridge flexAppearanceBridge){
        flexAppearanceBridge.bridge(this.shape);
    }

    /**
     * Applies the shape model to a {@link FlexCard} that already implements the Shapeable contract.
     *
     * @param flexAppearanceBridge bridge provided by {@link FlexCard} that receives the model
     */
    public void applyAppearance(FlexCard.FlexAppearanceBridge flexAppearanceBridge){
        flexAppearanceBridge.bridge(this.shape);
    }
}
