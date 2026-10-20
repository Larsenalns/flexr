// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrcoremodule.ui.layout;

import android.content.res.ColorStateList;
import android.os.Build;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.irisvision.flexrcoremodule.core.FlexContextInterface;
import com.irisvision.flexrcoremodule.core.FlexView;
import com.irisvision.flexrcoremodule.core.FlexViewBridge;
import com.irisvision.flexrcoremodule.enums.FlexFieldFlag;
import com.irisvision.flexrcoremodule.enums.FlexFieldType;
import com.irisvision.flexrcoremodule.enums.In;
import com.irisvision.flexrcoremodule.ui.list.flexadaptor.FlexAdapterFilter;
import com.irisvision.flexrcoremodule.valueconverter.FlexValue;
import com.irisvision.flexrthememodule.FlexTheme;

/**
 * Text field widget, backed by a Material {@code TextInputLayout} that hosts either a
 * {@code TextInputEditText} (plain field) or an {@code AutoCompleteTextView} (drop-down field).
 *
 * <p>The layout is the view added to the FlexR tree; the editable view is its single child,
 * chosen once at construction time from the {@code isAutoComplete} flag. Every mutator therefore
 * forwards either to the plain input or to the auto-complete one, never to both.
 *
 * <p><b>Two layers of configuration.</b> Methods prefixed with {@code box} configure the
 * surrounding {@code TextInputLayout} (hint, helper text, error message, counter, background,
 * stroke); the other methods configure the editable view itself (text, colors, size, input type,
 * line count, icons).
 *
 * <p><b>Theme.</b> Colors are given as <b>palette indices</b>, never as ARGB values. Each index is
 * remembered and re-applied by {@link #ifThemeChange()} on a theme switch. Sizes and stroke widths
 * are in <b>dp</b> and converted by {@link FlexValue#fixed}.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * new FlexInput(flexContext, false)
 *         .boxHint("Email", 12)
 *         .inputVariation(FlexFieldType.email, null)
 *         .textSize(16f)
 *         .endIconMode(FlexInput.clearText);
 * }</pre>
 *
 * <p><b>Threading:</b> every method mutates live Android views and must run on the UI thread. The
 * watcher registered with {@link #whenTextChange(TextWatcher)} and the {@code flexTextChanged}
 * hooks run on the UI thread while the user types.
 */
public class FlexInput extends FlexView {

    private TextInputLayout inputLayout; private NormalInput normalInput; private AutoCompleteInput autoCompleteInput;

    private final boolean isAutoComplete;

    /** End-icon modes accepted by {@link #endIconMode(int)}: none, clear text, password toggle, drop-down, custom. */
    public static final int iconNone = 0, clearText = 1, passwordToggle = 2, dropDown = 3, iconCustom = 4;
    /** Scroll-bounce modes accepted by {@link #lineDetails(int, String, int)}: never, always, on content overflow. */
    public static final int NeverScroll = -2, ScrollAlways = -1, IfContentScroll = 5;

    /** Sentinel meaning "no palette index is remembered" (see {@link #ifThemeChange()}). */
    private static final int NO_THEME = -1;

    /** Remembered palette indices, replayed on every theme switch; {@link #NO_THEME} means unset. */
    private int hintThemeColorIndex = NO_THEME;
    private int helperThemeColorIndex = NO_THEME;
    private int boxThemeColorIndex = NO_THEME;
    private int strokeThemeColorIndex = NO_THEME;
    private int strokeWidthValue = NO_THEME;
    private int strokeFocusWidthValue = NO_THEME;

    /**
     * Creates the field, its layout and the editable view matching {@code isAutoComplete}.
     *
     * @param flexContext    context of the FlexR tree that owns this view
     * @param isAutoComplete {@code true} to host an {@code AutoCompleteTextView} with a drop-down,
     *                       {@code false} to host a plain {@code TextInputEditText}
     */
    public FlexInput(FlexContextInterface flexContext, boolean isAutoComplete) {
        super(flexContext);
        this.inputLayout = new TextInputLayout(f.getFlexContext());
        this.isAutoComplete = isAutoComplete;
        initInnerBridge();
        build();
    }
    /**
     * Attaches the given bridge to the input layout, which is the view added to the FlexR tree.
     *
     * @param flexViewBridge bridge supplied by the FlexR engine
     * @return whatever {@code content(FlexViewBridge, View)} reports
     */
    @Override
    public final boolean contentWrapper(FlexViewBridge flexViewBridge) {
        //return the inputLayout view because this is the container of the input, so its this that must be used view when its
        //about to call this class or to put this class in the FLexBox, Line Container etc ...
        return content(flexViewBridge, this.inputLayout);
    }

    //This method add the Input(TextInputEditText) to their TextInputLayout
    private void build(){
        if(isAutoComplete){
            this.autoCompleteInput = new AutoCompleteInput(f);
            this.inputLayout.addView(autoCompleteInput._get());
        }
        else {
            this.normalInput = new NormalInput(f);
            this.inputLayout.addView(this.normalInput._get());
        }

        attachTextWatcherEvents();
    }

    /**
     * Sets the hint of the box, leaving its color untouched.
     *
     * @param hintValue hint to display
     * @return this instance, for chaining
     */
    public final FlexInput boxHint(String hintValue){
        this.inputLayout.setHint(hintValue);
        return this;
    }
    /**
     * Sets the hint of the box and its color, taken from the current palette.
     *
     * <p>The index is remembered, so the color follows the current theme
     * (see {@link #ifThemeChange()}).
     *
     * @param hintValue       hint to display
     * @param themeColorIndex index in the current palette
     * @return this instance, for chaining
     */
    public final FlexInput boxHint(String hintValue, int themeColorIndex){
        this.hintThemeColorIndex = themeColorIndex;
        this.inputLayout.setHint(hintValue);
        this.inputLayout.setHintTextColor(ColorStateList.valueOf(FlexTheme.load(themeColorIndex)));
        return this;
    }
    /**
     * Returns the hint currently set on the box.
     *
     * @return the hint, or {@code null} when no hint is set
     */
    public final CharSequence boxHint(){
        return this.inputLayout.getHint();
    }

    /**
     * Sets the helper text and its color, taken from the current palette.
     *
     * <p>The index is remembered and re-applied on theme switches.
     *
     * @param helperText      helper text to display
     * @param helperTextColor palette index of the helper text color
     * @return this instance, for chaining
     */
    public final FlexInput boxTextHelper(String helperText, int helperTextColor){
        this.helperThemeColorIndex = helperTextColor;
        this.inputLayout.setHelperText(helperText);
        this.inputLayout.setHelperTextColor(ColorStateList.valueOf(FlexTheme.load(helperTextColor)));
        return this;
    }
    /**
     * Enables or disables the helper text area.
     *
     * @param value {@code true} to show the helper text, {@code false} to hide it
     * @return this instance, for chaining
     */
    public final FlexInput boxEnableHelper(boolean value){
        this.inputLayout.setHelperTextEnabled(value);
        return this;
    }
    /**
     * Sets the error message shown under the field; {@code null} clears the error state.
     *
     * @param errorMessage message to display
     * @return this instance, for chaining
     */
    public final FlexInput boxErrorMessage(String errorMessage){
        this.inputLayout.setError(errorMessage);
        return this;
    }

    //FlexInput.clearText, FlexInput.passwordToggle, FlexInput.iconCustom, etc... the value can be passed to this method lke
    //param endIcon(FlexInput.clearText) ....
    /**
     * Selects the end icon behavior of the box.
     *
     * @param icon one of {@link #iconNone}, {@link #clearText}, {@link #passwordToggle},
     *             {@link #dropDown} or {@link #iconCustom}
     * @return this instance, for chaining
     * @throws IllegalArgumentException if {@code icon} is not one of those constants
     */
    public final FlexInput endIconMode(int icon){
        if (icon == FlexInput.iconNone) this.inputLayout.setEndIconMode(TextInputLayout.END_ICON_NONE);
        else if (icon == FlexInput.clearText) this.inputLayout.setEndIconMode(TextInputLayout.END_ICON_CLEAR_TEXT);
        else if (icon == FlexInput.passwordToggle) this.inputLayout.setEndIconMode(TextInputLayout.END_ICON_PASSWORD_TOGGLE);
        else if (icon == FlexInput.iconCustom) this.inputLayout.setEndIconMode(TextInputLayout.END_ICON_CUSTOM);
        else if (icon == FlexInput.dropDown) this.inputLayout.setEndIconMode(TextInputLayout.END_ICON_DROPDOWN_MENU);
        else throw new IllegalArgumentException(
                "endIconMode(" + icon + ") : unknown mode. Use one of the FlexInput constants: "
                        + "iconNone, clearText, passwordToggle, dropDown, iconCustom.");
        return this;
    }
    /**
     * Shows a custom drawable as the end icon and switches the box to custom end-icon mode.
     *
     * @param icon drawable asset name
     * @return this instance, for chaining
     */
    public final FlexInput endIcon(String icon){
        this.inputLayout.setEndIconMode(TextInputLayout.END_ICON_CUSTOM);
        this.inputLayout.setEndIconDrawable(f.flexAsset(In.drawable, icon));
        return this;
    }
    /**
     * Shows a custom drawable as the start icon, leaving the end icon untouched.
     *
     * @param icon drawable asset name
     * @return this instance, for chaining
     */
    public final FlexInput startIcon(String icon){
        this.inputLayout.setStartIconDrawable(f.flexAsset(In.drawable, icon));
        return this;
    }
    /**
     * Enables the character counter under the box.
     *
     * @param value     {@code true} to show the counter, {@code false} to hide it
     * @param maxLength maximum length used by the counter; a non-positive value keeps the
     *                  Material default
     * @return this instance, for chaining
     */
    public final FlexInput counter(boolean value, int maxLength){
        this.inputLayout.setCounterEnabled(value);
        if(maxLength > 0) this.inputLayout.setCounterMaxLength(maxLength);
        return this;
    }
    /**
     * Gives the box the filled background mode used by default.
     *
     * @return this instance, for chaining
     */
    public final FlexInput defaultBox(){
        this.inputLayout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_FILLED);
        return this;
    }
    /*public final void boxHintSize(){

    }*/
    /**
     * Gives the box the outline background mode and sets its colors and stroke widths.
     *
     * <p>Colors are palette indices, widths are in <b>dp</b>. The indices and the widths are
     * remembered, so the decoration follows theme switches.
     *
     * @param boxColorThemeIndex    palette index of the box background
     * @param strokeWidth           stroke width in <b>dp</b>
     * @param strokeColorThemeIndex palette index of the stroke color
     * @param strokeFocusWidth      stroke width while the field has focus, in <b>dp</b>
     * @return this instance, for chaining
     */
    public final FlexInput boxDecoration(int boxColorThemeIndex, int strokeWidth, int strokeColorThemeIndex, int strokeFocusWidth){
        //The box background must be painted through setBoxBackgroundColor(), not through
        //setBackgroundColor(), which paints the host view. Stroke widths go through
        //FlexValue.fixed (dp -> px), as everywhere else in FlexR.
        this.inputLayout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);

        this.boxThemeColorIndex = boxColorThemeIndex;
        this.strokeThemeColorIndex = strokeColorThemeIndex;
        this.strokeWidthValue = strokeWidth;
        this.strokeFocusWidthValue = strokeFocusWidth;

        applyBoxDecoration();
        return this;
    }

    /**
     * Applies the box decoration from the remembered indices and widths.
     *
     * <p>Called by {@link #boxDecoration(int, int, int, int)} and by {@link #ifThemeChange()}, which
     * is what keeps the decoration in sync with the theme.
     */
    private void applyBoxDecoration(){
        if (this.boxThemeColorIndex != NO_THEME) {
            this.inputLayout.setBoxBackgroundColor(FlexTheme.load(this.boxThemeColorIndex));
        }
        if (this.strokeThemeColorIndex != NO_THEME) {
            this.inputLayout.setBoxStrokeColorStateList(
                    ColorStateList.valueOf(FlexTheme.load(this.strokeThemeColorIndex)));
        }
        if (this.strokeWidthValue != NO_THEME) {
            this.inputLayout.setBoxStrokeWidth(FlexValue.fixed(f, this.strokeWidthValue));
        }
        if (this.strokeFocusWidthValue != NO_THEME) {
            this.inputLayout.setBoxStrokeWidthFocused(FlexValue.fixed(f, this.strokeFocusWidthValue));
        }
    }

    /**
     * Re-applies the remembered palette colors of this field in the current theme: hint color,
     * helper text color and box decoration.
     */
    @Override
    public void ifThemeChange() {
        if (this.hintThemeColorIndex != NO_THEME) {
            this.inputLayout.setHintTextColor(
                    ColorStateList.valueOf(FlexTheme.load(this.hintThemeColorIndex)));
        }
        if (this.helperThemeColorIndex != NO_THEME) {
            this.inputLayout.setHelperTextColor(
                    ColorStateList.valueOf(FlexTheme.load(this.helperThemeColorIndex)));
        }
        if (this.boxThemeColorIndex != NO_THEME || this.strokeThemeColorIndex != NO_THEME) {
            applyBoxDecoration();
        }
    }

    /**
     * Sets the text of the editable view.
     *
     * @param textValue text to display
     * @return this instance, for chaining
     */
    public final FlexInput text(String textValue){
        if(isAutoComplete) this.autoCompleteInput.text(textValue);
        else this.normalInput.text(textValue);

        return this;
    }
    /**
     * Returns the text currently entered.
     *
     * @return the field text, or an empty string when nothing has been entered; never
     *         {@code null}
     */
    public final CharSequence text(){
        CharSequence value = (isAutoComplete) ? this.autoCompleteInput._get().getText()
                                              : this.normalInput._get().getText();
        return (value == null) ? "" : value;
    }
    /**
     * Returns the live editable buffer of the field.
     *
     * @return the {@code Editable} held by the editable view
     */
    public final Editable editable(){
       return  (isAutoComplete) ?this.autoCompleteInput._get().getText() :this.normalInput._get().getText();
    }
    /**
     * Sets the hint of the editable view.
     *
     * @param hintValue hint to display
     * @return this instance, for chaining
     */
    public final FlexInput hint(String hintValue){
        if (isAutoComplete) this.autoCompleteInput.hint(hintValue);
        else this.normalInput.hint(hintValue);

        return this;
    }
    /**
     * Sets the hint color from the current palette.
     *
     * @param themeIndex index in the current palette
     * @return this instance, for chaining
     */
    public final FlexInput hintColor(int themeIndex){
        if (isAutoComplete) this.autoCompleteInput.hintColor(themeIndex);
        else this.normalInput.hintColor(themeIndex);

        return this;
    }
    /**
     * Sets the text color from the current palette.
     *
     * @param themIndex index in the current palette
     * @return this instance, for chaining
     */
    public final FlexInput textColor(int themIndex){
        if (isAutoComplete) this.autoCompleteInput.textColor(themIndex);
        else this.normalInput.textColor(themIndex);

        return this;
    }
    /**
     * Sets the text size in sp, honoring the user font-scale setting.
     *
     * @param textSize size in scaled pixels
     * @return this instance, for chaining
     */
    public final FlexInput textSize(float textSize){
        if (isAutoComplete) this.autoCompleteInput.textSize(textSize);
        else this.normalInput.textSize(textSize);

        return this;
    }

    //This method is for the use of basic, quick and default dropDown with only just String item, perform by android default layout
    /**
     * Installs the default simple drop-down over plain strings, using the Android one-line item
     * layout. Has no effect on a field that is not in auto-complete mode.
     *
     * @param data suggestions shown while typing
     * @return this instance, for chaining
     */
    public final FlexInput dropDownDefault(String[] data){
        if (isAutoComplete){
            this.autoCompleteInput._get().setAdapter(new ArrayAdapter<>(
                    f.getFlexContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    data
            ));
            this.autoCompleteInput._get().setThreshold(1);
        }
        return this;
    }

    private void buildDropDownPrecess(int width, int height,int sugBy){
        this.autoCompleteInput._get().setDropDownWidth(width);
        this.autoCompleteInput._get().setDropDownHeight(height);
        this.autoCompleteInput._get().setThreshold(sugBy);
    }

    //This method is for custom, advanced look and feel and all custom dropDown look
    /**
     * Configures a custom drop-down: size, suggestion threshold and adapter. Has no effect on a
     * field that is not in auto-complete mode.
     *
     * @param width   drop-down width in pixels
     * @param height  drop-down height in pixels
     * @param adapter adapter supplying the suggestions
     * @param sugBy   number of characters typed before suggestions appear
     * @return this instance, for chaining
     */
    public final FlexInput buildDropDown(int width, int height, FlexAdapterFilter adapter, int sugBy){
        if (isAutoComplete){
            buildDropDownPrecess(width, height,sugBy);
            dropDownAdapter(adapter);
        }
        return this;
    }
    /**
     * Configures a custom drop-down without touching its adapter. Has no effect on a field that is
     * not in auto-complete mode.
     *
     * @param width  drop-down width in pixels
     * @param height drop-down height in pixels
     * @param sugBy  number of characters typed before suggestions appear
     * @return this instance, for chaining
     */
    public final FlexInput buildDropDown(int width, int height, int sugBy){
        if (isAutoComplete){
            buildDropDownPrecess(width, height,sugBy);
        }
        return this;
    }

    //This method is used when dev compose their dropDown separately with the adapter or when dev want to attach another adapter
    //this is the setter (setAdapter)
    /**
     * Sets the adapter of the drop-down, for callers that build their own or reuse one from
     * elsewhere. Has no effect on a field that is not in auto-complete mode.
     *
     * @param adapter adapter supplying the suggestions
     * @return this instance, for chaining
     */
    public final FlexInput dropDownAdapter(FlexAdapterFilter adapter){
        if (isAutoComplete){
            this.autoCompleteInput._get().setAdapter(adapter);
        }
        return this;
    }
    /**
     * Makes the field clickable or not.
     *
     * @param clickable {@code true} to accept clicks, {@code false} to ignore them
     * @return this instance, for chaining
     */
    public final FlexInput canClickable(boolean clickable){
        if (isAutoComplete) this.autoCompleteInput._get().setClickable(clickable);
        else this.normalInput._get().setClickable(clickable);

        return this;
    }

    /**
     * Makes the field focusable or not.
     *
     * @param focusable {@code true} to accept focus, {@code false} to refuse it
     * @return this instance, for chaining
     */
    public final FlexInput focusable(boolean focusable){
        if (isAutoComplete) this.autoCompleteInput._get().setFocusable(focusable);
        else this.normalInput._get().setFocusable(focusable);

        return this;
    }
    /**
     * Sets the keyboard input type from a field type plus optional flags.
     *
     * <p>Flags are OR-ed into the input type. {@link FlexFieldFlag#TextConversionSug} is ignored
     * below API 33, where the underlying input-type flag does not exist.
     *
     * @param fieldType     field type to apply; must not be {@code null}
     * @param flexFieldFlag flags to OR in, or {@code null} for none
     * @return this instance, for chaining
     * @throws IllegalArgumentException if {@code fieldType} is {@code null} or has no mapping
     */
    public final FlexInput inputVariation(FlexFieldType fieldType, FlexFieldFlag[] flexFieldFlag){
        if (fieldType == null)
            throw new IllegalArgumentException(
                    "fieldType == null : provide a FlexFieldType value (for example FlexFieldType.text).");

        int inputType = InputType.TYPE_NULL;
        switch (fieldType){
            //Deliberately neutral: no input type is imposed, so the field stays non-editable
            //until an explicit type is set.
            case defaultType:     inputType = InputType.TYPE_NULL;
                break;
            case text:            inputType = InputType.TYPE_CLASS_TEXT;
                break;
            case numeric:         inputType = InputType.TYPE_CLASS_NUMBER;
                break;
            case tel:             inputType = InputType.TYPE_CLASS_PHONE;
                break;

            case showPassText:    inputType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD;
                break;
            case hidePassText:    inputType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD;
                break;
            case showPassNumeric: inputType = InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_NORMAL;
                break;
            case hidePassNumeric: inputType = InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD;
                break;
            case decimal:         inputType = InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL;
                break;
            //A masked variant of decimal would combine TYPE_NUMBER_FLAG_DECIMAL with
            //TYPE_NUMBER_VARIATION_PASSWORD.
            case date:            inputType = InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_DATE;
                break;
            case time:            inputType = InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_TIME;
                break;

            case email:           inputType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS;
                break;
            case uri:             inputType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI;
                break;
            case personName:      inputType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PERSON_NAME;
                break;
            case webEmail:        inputType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS;
                break;
            case webEditText:     inputType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_WEB_EDIT_TEXT;
                break;
            case webPass:         inputType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD;
                break;
            case shortMessage:    inputType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE;
                break;
            case longMessage:     inputType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_LONG_MESSAGE;
                break;
            case filter:          inputType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_FILTER;
                break;
            case phonetic:        inputType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PHONETIC;
                break;
            case postalAddress:   inputType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_POSTAL_ADDRESS;
                break;

            default:
                throw new IllegalArgumentException(
                        "Unsupported FlexFieldType: " + fieldType
                                + ". Add the mapping in FlexInput.inputVariation().");
        }

        if (flexFieldFlag != null) {
            for (FlexFieldFlag fieldFlag : flexFieldFlag) {
                if (fieldFlag == null) continue;

                /*  ** This check avoids undefined behavior: TYPE_TEXT_FLAG_ENABLE_TEXT_CONVERSION_SUGGESTIONS
                 *  ** only exists from API 33 (Android 13, TIRAMISU). Below it the flag is ignored -
                 *  ** the user gets no conversion suggestion, but nothing breaks.
                 */
                if (fieldFlag == FlexFieldFlag.TextConversionSug
                        && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    continue;
                }
                inputType |= flagValue(fieldFlag);
            }
        }

        if(isAutoComplete) this.autoCompleteInput._get().setInputType(inputType);
        else this.normalInput._get().setInputType(inputType);

        return this;
    }

    /*  Hide Content of Field, Should be used only on the setInputType that don't have their own hide Variation
     ** for ex : TYPE_TEXT_VARIATION_POSTAL_ADDRESS, TYPE_TEXT_VARIATION_PHONETIC, TYPE_TEXT_VARIATION_URI ect...
     **  But most of case the only field that need to be hidden is just password field
    */
    /**
     * Masks or unmasks the field content by installing a password transformation.
     *
     * <p>Useful for input types that have no secret variation of their own, such as a postal
     * address, a phonetic name or a URI.
     *
     * @param value {@code true} to mask the content, {@code false} to show it
     * @return this instance, for chaining
     */
    public final FlexInput hideContent(boolean value){
        if (value){
            if(isAutoComplete) this.autoCompleteInput._get().setTransformationMethod(PasswordTransformationMethod.getInstance());
            else this.normalInput._get().setTransformationMethod(PasswordTransformationMethod.getInstance());
        }
        else{
            if(isAutoComplete) this.autoCompleteInput._get().setTransformationMethod(null);
            else this.normalInput._get().setTransformationMethod(null);
        }
        return this;
    }

    //Define number of line for input, more than 2 normally the input become visually a texArea
    /**
     * Sets the number of visible lines and the scroll behavior of the field.
     *
     * <p>Above two lines the field starts to look like a text area.
     *
     * @param line            number of visible lines; a non-positive value means one
     * @param scrollDirection {@code "H"} for a horizontal scroll bar, anything else for a vertical
     *                        one; {@code null} leaves the scroll bars unchanged
     * @param scrollBounce    one of {@link #ScrollAlways}, {@link #NeverScroll} or
     *                        {@link #IfContentScroll}
     * @return this instance, for chaining
     */
    public final FlexInput lineDetails(int line, String scrollDirection, int scrollBounce){
        int value =  (line > 0) ? line : 1;
        if(isAutoComplete) this.autoCompleteInput._get().setLines(value);
        else this.normalInput._get().setLines(value);

        if(scrollDirection != null){
            //Each direction enables the scroll BAR of its own axis; a vertical scroll is not
            //expressed through a fading edge.
            if (scrollDirection.equalsIgnoreCase("H")){
                if(isAutoComplete) this.autoCompleteInput._get().setHorizontalScrollBarEnabled(true);
                else this.normalInput._get().setHorizontalScrollBarEnabled(true);
            }
            else{
                if(isAutoComplete) this.autoCompleteInput._get().setVerticalScrollBarEnabled(true);
                else this.normalInput._get().setVerticalScrollBarEnabled(true);
            }
        }

        if(scrollBounce == FlexInput.ScrollAlways){
            if(isAutoComplete) this.autoCompleteInput._get().setOverScrollMode(View.OVER_SCROLL_ALWAYS);
            else this.normalInput._get().setOverScrollMode(View.OVER_SCROLL_ALWAYS);
        }
        else if (scrollBounce == FlexInput.NeverScroll){
            if(isAutoComplete) this.autoCompleteInput._get().setOverScrollMode(View.OVER_SCROLL_NEVER);
            else this.normalInput._get().setOverScrollMode(View.OVER_SCROLL_NEVER);
        }
        else if (scrollBounce == FlexInput.IfContentScroll){
            if(isAutoComplete) this.autoCompleteInput._get().setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
            else this.normalInput._get().setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
        }

        return this;
    }
    /**
     * Placeholder kept for API compatibility: <b>has no effect</b>. The method exists for the
     * named-style API to come, not for immediate use.
     */
    public final void inputStyle(){

    }

    /**
     * Placeholder kept for API compatibility: <b>has no effect</b>. It is meant to walk a table of
     * property names and set the corresponding styles of the {@code EditText}, one property at a
     * time; no property is applied yet.
     *
     * @param textStyleProperties property names to apply
     */
    public final void textStyle(String[] textStyleProperties){
        for (int i = 0; i < textStyleProperties.length; i++){
            //No property is applied yet.
        }
    }

    //Events
    /**
     * Registers a click listener on the end icon.
     *
     * @param clickListener listener to notify
     */
    public final void endIconClick(View.OnClickListener clickListener){
        this.inputLayout.setEndIconOnClickListener(clickListener);
    }
    /**
     * Registers a long-click listener on the end icon.
     *
     * @param longClickListener listener to notify
     */
    public final void endIconLongClick(View.OnLongClickListener longClickListener){
        this.inputLayout.setEndIconOnLongClickListener(longClickListener);
    }

    /**
     * Registers a click listener on the start icon.
     *
     * @param clickListener listener to notify
     */
    public final void startIconClick(View.OnClickListener clickListener){
        this.inputLayout.setStartIconOnClickListener(clickListener);
    }
    /**
     * Registers a long-click listener on the start icon.
     *
     * @param longClickListener listener to notify
     */
    public final void startIconLongClick(View.OnLongClickListener longClickListener){
        this.inputLayout.setStartIconOnLongClickListener(longClickListener);
    }

    //This method isn't final because dev can want to redefine this in their own class (custom class extend FlexView)
    //So in this cas dev must call the super.whenTextChange(TextWatcher textWatcher); when Overriding this method
    /**
     * Adds a text watcher on the editable view.
     *
     * <p>Not final on purpose: a subclass may override it, in which case it must call
     * {@code super.whenTextChange(...)} to keep the watcher registered.
     *
     * @param textWatcher watcher to add
     * @return this instance, for chaining
     */
    public FlexInput whenTextChange(TextWatcher textWatcher){

        if(isAutoComplete) this.autoCompleteInput._get().addTextChangedListener(textWatcher);
        else this.normalInput._get().addTextChangedListener(textWatcher);

        return this;
    }

    private void attachTextWatcherEvents(){
        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                flexTextChanged(s, start, before, count);
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                beforeFlexTextChanged(s, start, count, after);
            }

            @Override
            public void afterTextChanged(Editable s) {
                afterFlexTextChanged(s);
            }
        };

        if(isAutoComplete) this.autoCompleteInput._get().addTextChangedListener(textWatcher);
        else this.normalInput._get().addTextChangedListener(textWatcher);
    }

    //These three methods are to Override when dev extends this class (FlexInput) and want to define all inside the class
    //and they are called inside private method attachTextWatcherEvents();
    /**
     * Hook called while the text changes. Empty by default; override it in a subclass to react to
     * typing without registering a watcher.
     *
     * @param s     current text
     * @param start start offset of the changed range
     * @param before length of the replaced segment
     * @param count  length of the new segment
     */
    public void flexTextChanged(CharSequence s, int start, int before, int count) {}
    /**
     * Hook called before the text changes. Empty by default.
     *
     * @param s     text before the change
     * @param start start offset of the changed range
     * @param count length of the replaced segment
     * @param after length of the new segment
     */
    public void beforeFlexTextChanged(CharSequence s, int start, int count, int after) {}
    /**
     * Hook called after the text has changed. Empty by default.
     *
     * @param s the editable buffer after the change
     */
    public void afterFlexTextChanged(Editable s) {}

    private int flagValue(FlexFieldFlag fieldFlag){
        /*  ** A named switch keeps the mapping explicit: a flag can never receive another flag's
         *  ** constant, and a value added to FlexFieldFlag fails through an explicit error
         *  ** (default) instead of an ArrayIndexOutOfBoundsException at runtime.
         */
        switch (fieldFlag){
            case CapLetter:         return InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS;
            case CapWord:           return InputType.TYPE_TEXT_FLAG_CAP_WORDS;
            case CapSentence:       return InputType.TYPE_TEXT_FLAG_CAP_SENTENCES;
            case AutoCorrect:       return InputType.TYPE_TEXT_FLAG_AUTO_CORRECT;
            case AutoComplete:      return InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE;
            case MultiLine:         return InputType.TYPE_TEXT_FLAG_MULTI_LINE;
            case ImeMultiLine:      return InputType.TYPE_TEXT_FLAG_IME_MULTI_LINE;
            case NoSuggestion:      return InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS;
            case Signed:            return InputType.TYPE_NUMBER_FLAG_SIGNED;
            case Decimal:           return InputType.TYPE_NUMBER_FLAG_DECIMAL;
            case TextConversionSug: return InputType.TYPE_TEXT_FLAG_ENABLE_TEXT_CONVERSION_SUGGESTIONS;
            default:
                throw new IllegalArgumentException(
                        "Unsupported FlexFieldFlag: " + fieldFlag
                                + ". Add the mapping in FlexInput.flagValue().");
        }
    }

    /**
     * Plain editable view used when the field is not in auto-complete mode.
     */
    private static class NormalInput extends TextBase_{
        private TextInputEditText input;
        public NormalInput(FlexContextInterface flexContext) {
            super(flexContext);
            this.input = new TextInputEditText(f.getFlexContext());
            initInnerBridge();
            textViewBridge();
        }

        @Override
        public boolean contentWrapper(FlexViewBridge flexViewBridge) {
            return content(flexViewBridge, this.input);
        }

        private TextInputEditText _get(){
            return this.input;
        }
    }

    /**
     * Auto-complete editable view used when the field is in auto-complete mode.
     */
    private static class AutoCompleteInput extends TextBase_{
        private AutoCompleteTextView input;

        public AutoCompleteInput(FlexContextInterface flexContext) {
            super(flexContext);
            this.input = new AutoCompleteTextView(f.getFlexContext());
            initInnerBridge();
            textViewBridge();
        }

        @Override
        public boolean contentWrapper(FlexViewBridge flexViewBridge) {
            return content(flexViewBridge, this.input);
        }

        private AutoCompleteTextView _get(){
            return this.input;
        }
    }
}
