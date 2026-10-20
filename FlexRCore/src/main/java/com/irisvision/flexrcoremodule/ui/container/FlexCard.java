// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrcoremodule.ui.container;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.shape.MaterialShapeDrawable;
import com.google.android.material.shape.ShapeAppearanceModel;
import com.irisvision.flexrcoremodule.core.BoxBridge;
import com.irisvision.flexrcoremodule.core.FlexContextInterface;
import com.irisvision.flexrcoremodule.core.FlexViewBridge;
import com.irisvision.flexrcoremodule.ui.component.FlexShape;
import com.irisvision.flexrcoremodule.valueconverter.FlexValue;
import com.irisvision.flexrthememodule.FlexTheme;

/**
 * Material card: a stacking container, like {@link FlexBox}, that also owns a corner radius, an
 * elevation, a stroke and a background colour.
 *
 * <h2>Two ways to apply a shape</h2>
 * <ul>
 *   <li>{@link #appearance(FlexShape)} hands over the shape <b>model</b> only: the card keeps its own
 *       background, hence its mask, its ripple and {@code cardColor}. This is the recommended
 *       path;</li>
 *   <li>{@link #applyShape(FlexShape)} installs the drawable of the shape <b>as the card
 *       background</b>: the Material mask and ripple are replaced, and {@code cardColor} no longer
 *       shows. Both are public so that the caller can choose, but they are not meant to be combined
 *       on the same card.</li>
 * </ul>
 *
 * <h2>Theme</h2>
 * <p>The background colour, the stroke and the shape are recorded as they are set, and re-applied on
 * every theme switch from {@link #ifThemeChange()}. Only what this card actually owns is repainted: a
 * card that never received {@link #cardColor(int)} keeps the colour of its Material theme, and a
 * shape installed through {@link #applyShape(FlexShape)} recolours itself through
 * {@link FlexShape#applyTheme()}.
 *
 * <h2>Units</h2>
 * <p>Every dimension is expressed in <b>dp</b> and converted by {@link FlexValue#fixed}, the stroke
 * width and {@link #maxElevation(float)} included.
 *
 * <p>Threading: every member must be used on the UI thread, since all of them drive a native view.
 *
 * <pre>{@code
 * FlexCard card = new FlexCard(this);
 * card.radius(8f).cardColor(1).stroke(2, 1f).flexCardElevation(4f);
 * }</pre>
 *
 * @see FlexShape
 */
public class FlexCard extends BoxImpl{

    /** No palette index recorded for the colour or the stroke it qualifies. */
    private static final int NO_THEME = -1;

    /** Native view this container drives; never {@code null} once constructed. */
    private final MaterialCardView cardView;

    /** Palette index of the background colour, or {@link #NO_THEME} when no colour was requested. */
    private int cardColorTheme = NO_THEME;
    /** Named theme the background index is read from; {@code null} means the current theme. */
    private String cardColorThemeName;
    /** Palette index of the stroke colour, or {@link #NO_THEME} when no stroke was requested. */
    private int strokeTheme = NO_THEME;
    /** Stroke width in pixels, already converted from dp: {@code setStrokeWidth} takes an int. */
    private int strokeWidthPx;
    /** Shape installed as the card background, recoloured on every theme switch. */
    private FlexShape themedShape;

    /**
     * Creates a card backed by a new native {@code MaterialCardView}, and registers it with the theme
     * scope of its screen.
     *
     * <p>The card starts with the default Material appearance: no radius, no stroke, no elevation and
     * no palette colour recorded, so the first theme switch leaves it untouched. Must be called on
     * the UI thread, since it instantiates a view.
     *
     * @param flexContext context of the screen that owns this card
     */
    public FlexCard(FlexContextInterface flexContext) {
        super(flexContext);
        this.cardView = new MaterialCardView(flexContext().getFlexContext());
        initInnerBridge();
    }

    /**
     * Bridges the native {@code MaterialCardView} to a {@code BoxBridge}: this is the path
     * {@code FlexViewGroup.add(FlexView)} uses to attach a child to this card.
     *
     * <p>UI thread only.
     *
     * @param boxBridge bridge to hand the native view to
     * @return {@code true} when the bridge accepted the view
     */
    @Override
    public final boolean boxWrapper(BoxBridge boxBridge) {
        return content(boxBridge, this.cardView);
    }

    /**
     * Bridges the native {@code MaterialCardView} to a {@code FlexViewBridge}: this is the generic
     * path through which the framework reaches the view of this card.
     *
     * <p>UI thread only.
     *
     * @param flexViewBridge bridge to hand the native view to
     * @return {@code true} when the bridge accepted the view
     */
    @Override
    public final boolean contentWrapper(FlexViewBridge flexViewBridge) {
        return content(flexViewBridge, this.cardView);
    }

    /**
     * Sets the corner radius of the four corners.
     *
     * @param radius corner radius in <b>dp</b>
     * @return this instance, for chaining
     */
    public final FlexCard radius(float radius){
        this.cardView.setRadius(FlexValue.fixed(f, radius));
        return this;
    }

    /**
     * Sets the background colour of the card from the palette of the current theme.
     *
     * <p>The index is recorded and resolved again on every theme switch, so the card keeps following
     * the active theme. It also clears any theme name set by {@link #cardColor(int, String)}.
     *
     * @param themeIndex index in the palette of the current theme
     * @return this instance, for chaining
     */
    public final FlexCard cardColor(int themeIndex){
        this.cardColorTheme = themeIndex;
        this.cardColorThemeName = null;
        this.cardView.setCardBackgroundColor(FlexTheme.load(themeIndex));
        return this;
    }

    /**
     * Sets the background colour of the card from the palette of a <b>named</b> theme.
     *
     * <p>Use this overload when the card must keep the colours of one specific theme even while
     * another theme is active. Both the index and the name are recorded, so the colour is resolved
     * again against the same theme on every switch.
     *
     * @param themeIndex index in the palette of {@code themeName}
     * @param themeName  name of the theme whose palette is read
     * @return this instance, for chaining
     * @see FlexTheme#load(int, String)
     */
    public final FlexCard cardColor(int themeIndex, String themeName){
        this.cardColorTheme = themeIndex;
        this.cardColorThemeName = themeName;
        this.cardView.setCardBackgroundColor(FlexTheme.load(themeIndex, themeName));
        return this;
    }

    /**
     * Installs the drawable of {@code shape} as the background of the card.
     *
     * <p>The shape then owns the background: it is recorded here so that {@link #ifThemeChange()} can
     * recolour it, and it masks the card colour, so {@code cardColor} no longer shows. The Material
     * mask and ripple of the card are replaced by the drawable. Prefer {@link #appearance(FlexShape)}
     * when the mask, the ripple and {@code cardColor} must stay intact.
     *
     * @param shape shape whose drawable becomes the card background
     * @return this instance, for chaining
     */
    public final FlexCard applyShape(FlexShape shape){
        shape.applyDrawable(new FlexShapeBridge());
        this.themedShape = shape;
        return this;
    }

    /**
     * Sets the elevation of the card, which drives the size of its shadow.
     *
     * @param elevation elevation in <b>dp</b>
     * @return this instance, for chaining
     */
    public final FlexCard flexCardElevation(float elevation){
        this.cardView.setCardElevation(FlexValue.fixed(f, elevation));
        return this;
    }

    /**
     * Sets the corner radius, the elevation and both compatibility options in a single call.
     *
     * <p>Useful when a card is configured from one place, for instance a style helper.
     *
     * @param radius        corner radius in <b>dp</b>
     * @param elevation     elevation in <b>dp</b>
     * @param overlap       value passed to {@code setPreventCornerOverlap}: {@code true} keeps the
     *                      content clear of the rounded corners
     * @param compatPadding {@code true} to reserve room for the shadow inside the measured bounds on
     *                      platforms older than Lollipop
     * @return this instance, for chaining
     */
    public final FlexCard radElev(float radius, float elevation, boolean overlap, boolean compatPadding){
        this.cardView.setRadius(FlexValue.fixed(f, radius));
        this.cardView.setCardElevation(FlexValue.fixed(f, elevation));
        this.cardView.setPreventCornerOverlap(overlap);
        this.cardView.setUseCompatPadding(compatPadding);
        return this;
    }

    /**
     * Sets the maximum elevation the card may reach, in <b>dp</b>.
     *
     * <p>The value bounds the elevation used by the shadow, including the elevation added by a
     * translation on the Z axis. Like every other dimension of this class it is converted through
     * {@link FlexValue#fixed}.
     *
     * @param maxElevation maximum elevation in <b>dp</b>
     * @return this instance, for chaining
     */
    public final FlexCard maxElevation(float maxElevation){
        this.cardView.setMaxCardElevation(FlexValue.fixed(f, maxElevation));
        return this;
    }

    /**
     * Sets the colour and the width of the stroke drawn around the card.
     *
     * <p>Both are recorded, so the stroke is restored on every theme switch. The colour is a palette
     * index of the current theme, not a colour value.
     *
     * @param themeColorIndex index in the palette of the current theme, recorded for theme switches
     * @param strokeWidth     stroke width in <b>dp</b>
     * @return this instance, for chaining
     */
    public final FlexCard stroke(int themeColorIndex, float strokeWidth){
        this.strokeTheme = themeColorIndex;
        this.strokeWidthPx = FlexValue.fixed(f, strokeWidth);
        this.cardView.setStrokeColor(FlexTheme.load(themeColorIndex));
        this.cardView.setStrokeWidth(this.strokeWidthPx);
        return this;
    }

    /**
     * Applies the shape <b>model</b>, corners and edges, to the card without touching its background.
     *
     * <p>This is the clean path: the {@code MaterialCardView} keeps its own drawable, hence its mask,
     * its ripple and its card colour, and only the geometry of the shape changes.
     *
     * @param flexShape shape whose appearance model is copied onto the card
     * @return this instance, for chaining
     */
    public final FlexCard appearance(FlexShape flexShape){
        flexShape.applyAppearance(new FlexAppearanceBridge());
        return this;
    }

    /**
     * Re-applies the theme colours this card owns: the background set by {@link #cardColor}, the
     * stroke set by {@link #stroke} and, when a shape was installed as background, the colours of
     * that shape.
     *
     * <p>Only what was actually set is repainted, so a card that relies on the default Material
     * appearance is left alone. Call {@code super.ifThemeChange()} first, as this override does.
     *
     * <p>Invoked by the theme dispatcher on the UI thread; it touches native views.
     */
    @Override
    public void ifThemeChange() {
        super.ifThemeChange();

        if (cardColorTheme != NO_THEME) {
            this.cardView.setCardBackgroundColor(this.cardColorThemeName == null
                    ? FlexTheme.load(cardColorTheme)
                    : FlexTheme.load(cardColorTheme, cardColorThemeName));
        }
        if (strokeTheme != NO_THEME) {
            this.cardView.setStrokeColor(FlexTheme.load(strokeTheme));
            this.cardView.setStrokeWidth(this.strokeWidthPx);
        }
        if (themedShape != null) {
            this.themedShape.applyTheme();
        }
    }

    /**
     * Bridge that receives the shape appearance model to install on the card.
     *
     * <p>Handed to {@code FlexShape.applyAppearance(...)}, which calls
     * {@code bridge(ShapeAppearanceModel)} once the model has been built.
     */
    public final class FlexAppearanceBridge {
        /**
         * Installs {@code shapeAppearanceModel} as the shape of the card.
         *
         * <p>UI thread only.
         *
         * @param shapeAppearanceModel model to apply to the native card view
         */
        public void bridge(ShapeAppearanceModel shapeAppearanceModel) {
            FlexCard.this.cardView.setShapeAppearanceModel(shapeAppearanceModel);
        }
    }

    /**
     * Bridge that receives the shape drawable to install as the card background.
     *
     * <p>Handed to {@code FlexShape.applyDrawable(...)}, which calls
     * {@code bridge(MaterialShapeDrawable)} once the drawable has been built.
     */
    public final class FlexShapeBridge {
        /**
         * Installs {@code shapeDrawable} as the background of the card.
         *
         * <p>UI thread only.
         *
         * @param shapeDrawable drawable to install as the card background
         */
        public void bridge(MaterialShapeDrawable shapeDrawable) {
            FlexCard.this.cardView.setBackgroundDrawable(shapeDrawable);
        }
    }
}
