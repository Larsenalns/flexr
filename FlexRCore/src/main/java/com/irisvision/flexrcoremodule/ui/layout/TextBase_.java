// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrcoremodule.ui.layout;

import android.content.res.ColorStateList;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.irisvision.flexrcoremodule.core.FlexContextInterface;
import com.irisvision.flexrcoremodule.core.FlexView;
import com.irisvision.flexrcoremodule.core.FlexViewBridge;
import com.irisvision.flexrcoremodule.valueconverter.FlexValue;
import com.irisvision.flexrthememodule.FlexTheme;

/**
 * Common base for the FlexR text widgets: {@code FlexText}, {@code FlexButton},
 * {@code FlexCheck}, {@code FlexRadioButton}, {@code FlexSwitch}, and the internal fields of
 * {@code FlexInput} ({@code NormalInput}, {@code AutoCompleteInput}).
 *
 * <h2>Reaction to theme changes</h2>
 *
 * <p>Every color set through a <b>palette index</b> is remembered, and
 * {@link #ifThemeChange()} re-applies it in the current theme. The developer therefore sets the
 * index <b>once</b> and the library follows theme switches, instead of re-applying every property
 * by hand.
 *
 * <p>Two variants, two distinct intents, both honored:
 * <ul>
 *   <li>{@code textColor(index)} - the color follows the <b>current theme</b>: on every switch the
 *       index is looked up again in the new palette. This is the "automatic theme" variant;</li>
 *   <li>{@code textColor(index, "light")} - the color targets the <b>named theme</b>: on every
 *       switch the index is looked up in the named palette. This is the "color borrowed from a
 *       specific theme" variant, and it is deliberately insensitive to switches.</li>
 * </ul>
 *
 * <p>Invariant: only the <b>palette index</b> is stored, never a resolved ARGB color. The
 * index-to-color conversion happens in exactly one place per property, so a palette lookup can
 * never receive a color as its index.
 *
 * <p><b>Threading:</b> every setter writes to a live {@code TextView} and must be called on the UI
 * thread. {@link #ifThemeChange()} repaints on its calling thread only; the library invokes it from
 * the UI thread when a theme switch is applied.
 *
 * <pre>{@code
 * new FlexText(flexContext)
 *         .text("Title")
 *         .textColor(12)          // follows the current theme
 *         .textSize(18f);
 * }</pre>
 */
public abstract class TextBase_ extends FlexView {

    /** Sentinel meaning "no palette index is remembered" for a themed color property. */
    protected static final int NO_THEME = -1;

    private final TextViewWrapperBridge textViewBridge = new TextViewWrapperBridge();

    /** Remembered palette index for the text color, re-applied by {@link #ifThemeChange()}. */
    protected int textColorTheme = NO_THEME;

    /** Named theme targeted by the text color; {@code null} means the current theme. */
    private String textColorThemeName;

    /** Remembered palette index for the hint color, re-applied by {@link #ifThemeChange()}. */
    protected int hintColorTheme = NO_THEME;

    /**
     * Creates the text facade for a {@code FlexView}.
     *
     * @param flexContext context of the FlexR tree that owns this view
     */
    public TextBase_(FlexContextInterface flexContext) {
        super(flexContext);
    }

    /**
     * Registers this facade as the content of its {@code FlexView} and tags the host Android view
     * with {@code this}, so a lookup can walk back from the view to the FlexR object.
     *
     * <p>Subclasses call it once, right after the backing {@code TextView} has been created.
     */
    protected final void textViewBridge(){
        contentWrapper(this.textViewBridge);
        this.textViewBridge.me().setTag(this);
    }

    /**
     * Sets the displayed text.
     *
     * @param textValue text to display
     * @return this instance, for chaining
     */
    public final TextBase_ text(String textValue){
        this.textViewBridge.me().setText(textValue);
        return this;
    }

    /**
     * Returns the displayed text.
     *
     * @return the displayed text, never {@code null}; an empty view yields an empty string
     */
    public CharSequence text(){
        CharSequence value = this.textViewBridge.me().getText();
        return (value == null) ? "" : value;
    }

    /**
     * Sets the hint displayed when the text is empty.
     *
     * @param hintValue hint to display
     * @return this instance, for chaining
     */
    public final TextBase_ hint(String hintValue){
        this.textViewBridge.me().setHint(hintValue);
        return this;
    }

    /**
     * Sets the hint color from the current palette and remembers the index for theme switches.
     *
     * @param themeColorIndex index in the current palette
     * @return this instance, for chaining
     */
    public final TextBase_ hintColor(int themeColorIndex){
        this.hintColorTheme = themeColorIndex;
        this.textViewBridge.me().setHintTextColor(ColorStateList.valueOf(FlexTheme.load(themeColorIndex)));
        return this;
    }

    /**
     * Sets the text color from the palette of the <b>current</b> theme and remembers the index, so
     * {@link #ifThemeChange()} can re-apply it after a switch.
     *
     * @param themeColorIndex index in the current palette
     * @return this instance, for chaining
     */
    public final TextBase_ textColor(int themeColorIndex){
        this.textColorTheme = themeColorIndex;
        this.textColorThemeName = null;
        this.textViewBridge.me().setTextColor(FlexTheme.load(themeColorIndex));
        return this;
    }

    /**
     * Sets the text color from the palette of a <b>named</b> theme and remembers both the index and
     * the theme name. This color does not follow theme switches.
     *
     * @param themeColorIndex index in the palette of {@code themeName}
     * @param themeName       theme whose palette is read; {@code null} means the current theme
     * @return this instance, for chaining
     */
    public final TextBase_ textColor(int themeColorIndex, String themeName){
        this.textColorTheme = themeColorIndex;
        this.textColorThemeName = themeName;
        this.textViewBridge.me().setTextColor(FlexTheme.load(themeColorIndex, themeName));
        return this;
    }

    /**
     * Sets the text size in <b>sp</b>, honoring the user font-scale setting.
     *
     * @param textSize size in scaled pixels
     * @return this instance, for chaining
     */
    public final TextBase_ textSize(float textSize){
        FlexValue.textByDensity(f, this.textViewBridge.me(), textSize);
        return this;
    }

    /**
     * Sets the truncation behavior and where the ellipsis is drawn.
     *
     * @param truncateValue {@code "END"}, {@code "MIDDLE"} or {@code "START"} (case-insensitive);
     *                      {@code null} leaves the truncation unchanged
     * @return this instance, for chaining
     * @throws IllegalArgumentException if a non-null value is not one of the three keywords
     */
    public final TextBase_ truncate(String truncateValue){

        if (truncateValue == null) return this;

        if(truncateValue.equalsIgnoreCase("END"))
            this.textViewBridge.me().setEllipsize(TextUtils.TruncateAt.END);

        else if(truncateValue.equalsIgnoreCase("MIDDLE"))
            this.textViewBridge.me().setEllipsize(TextUtils.TruncateAt.MIDDLE);

        else if(truncateValue.equalsIgnoreCase("START"))
            this.textViewBridge.me().setEllipsize(TextUtils.TruncateAt.START);

        else throw new IllegalArgumentException(
                "truncate(\"" + truncateValue + "\") : unknown value. Use \"END\", \"MIDDLE\" or \"START\".");

        return this;
    }

    /**
     * Placeholder kept for API compatibility: <b>has no effect</b>. The call is accepted, but no
     * style is applied to the view.
     *
     * @return this instance, for chaining
     */
    public final TextBase_ style(){
        return this;
    }

    /**
     * Re-applies the remembered colors in the current theme, then lets {@link FlexView} handle the
     * properties it owns (background, shape).
     *
     * <p>Palette indices are read again here; a resolved color is never converted back into an
     * index.
     */
    @Override
    public void ifThemeChange() {
        super.ifThemeChange();
        applyThemedColors();
    }

    private void applyThemedColors(){
        TextView textView = this.textViewBridge.me();
        if (textView == null) return;   // facade not bridged yet: nothing to repaint

        if (textColorTheme != NO_THEME) {
            textView.setTextColor(this.textColorThemeName == null
                    ? FlexTheme.load(textColorTheme)
                    : FlexTheme.load(textColorTheme, textColorThemeName));
        }
        if (hintColorTheme != NO_THEME) {
            textView.setHintTextColor(ColorStateList.valueOf(FlexTheme.load(hintColorTheme)));
        }
    }

    /**
     * Bridge between the facade and the Android {@code TextView} rendered by the FlexR engine.
     *
     * <p>The view is injected through {@link FlexViewBridge#bridge(View)} when the layout is
     * inflated; {@code me()} then exposes it typed as a {@code TextView}.
     */
    public static final class TextViewWrapperBridge implements FlexViewBridge{
        private View view;

        @Override
        public void bridge(View view) {
            this.view = view;
        }

        private TextView me(){
            return (TextView) this.view;
        }
    }
}
