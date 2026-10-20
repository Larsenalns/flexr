// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrcoremodule.core;

import android.view.View;
import android.view.ViewTreeObserver;

import com.google.android.material.shape.MaterialShapeDrawable;
import com.irisvision.flexrcoremodule.enums.FlexDirection;
import com.irisvision.flexrcoremodule.enums.FlexScope;
import com.irisvision.flexrcoremodule.layoutParams.CallParamsFieldLike;
import com.irisvision.flexrcoremodule.layoutParams.FlexParams;
import com.irisvision.flexrcoremodule.layoutParams.FlexMargins;
import com.irisvision.flexrcoremodule.structure.FlexStructure;
import com.irisvision.flexrcoremodule.structure.FlexStructureImpl;
import com.irisvision.flexrcoremodule.ui.component.FlexShape;
import com.irisvision.flexrcoremodule.ui.layout.TextBase_;
import com.irisvision.flexrcoremodule.ui.list.flexadaptor.FlexAdapter;
import com.irisvision.flexrfxmodule.AnimateFragmentScope;
import com.irisvision.flexrfxmodule.FlexAnimate;
import com.irisvision.flexrfxmodule.FlexPropertiesAnimate;
import com.irisvision.flexrthememodule.FlexTheme;
import com.irisvision.flexrthememodule.Thematic;

public abstract class FlexView extends CallParamsFieldLike implements FlexViewWrapper, FlexStructure, Thematic, FlexPropertiesAnimate, ScopeIdentity, AnimateFragmentScope {

    /** No palette index memorised for the property it qualifies. */
    private static final int NO_THEME = -1;

    private String scopeId;
    protected FlexContextInterface f;
    private final FlexViewWrapperBridge innerBridge = new FlexViewWrapperBridge();
    private final int BaseState = 99887;
    protected int pl,pt,pr,pb, xScroll, yScroll;
    protected int plTemp,ptTemp,prTemp,pbTemp, xScrollTemp, yScrollTemp;

    /**
     * Palette index of the background colour set by {@code background(index[, themeName])}.
     *
     * <p>Stays at {@link #NO_THEME} until a colour background is actually requested. The distinction
     * matters: {@code NO_THEME} means "this view never asked for a coloured background, so a theme switch
     * must leave it alone", while index {@code 0} is a real palette entry that some view may well want.
     * Conflating the two would repaint every view on the first switch.
     */
    protected int backColor = NO_THEME;

    /** Named theme the background colour targets; {@code null} means the current theme. */
    private String backColorThemeName;

    /** Shape set as background by {@code background(FlexShape)}, to be recoloured on a switch. */
    private FlexShape themedShape;

    private boolean dynamiqueApplyTheme = true;

    public FlexView(FlexContextInterface flexContext) {
        this.f = flexContext;
        initPddAnimate();
        initScrollAnimate();
        initAnimate();
    }

    /**
     * Repaints exactly the theme elements this view really owns: the colour background set by
     * {@code background(index[, themeName])}, and the shape set by {@code background(FlexShape)}.
     *
     * <p>The decision is made from what the view <b>recorded</b>, never from what its native background
     * happens to be. Inspecting the background would be wrong twice over:
     * <ul>
     *   <li>a Material card owns a {@code MaterialShapeDrawable} without this view having set any colour,
     *       so a colour repaint there would override the colour chosen through {@code shapeColor(...)};</li>
     *   <li>a view that never asked for a coloured background would still receive palette entry 0.</li>
     * </ul>
     *
     * <p>Only what was explicitly given is therefore repainted, and a themed shape keeps control of its
     * own colours through {@link FlexShape#applyTheme()}.
     */
    @Override
    public void ifThemeChange() {
        if (backColor != NO_THEME) {
            int color = (backColorThemeName == null)
                    ? FlexTheme.load(backColor)
                    : FlexTheme.load(backColor, backColorThemeName);
            this.innerBridge.me().setBackgroundColor(color);
        }
        if (themedShape != null) {
            this.themedShape.applyTheme();
        }
    }

    @Override
    public final boolean content(FlexViewBridge flexViewBridge,View view) {

        if (flexViewBridge instanceof FlexParams.ParamsUnWrapper
            || flexViewBridge instanceof FlexViewWrapperBridge || flexViewBridge instanceof TextBase_.TextViewWrapperBridge
            || flexViewBridge instanceof FlexAdapter.FlexAdapterBridge || FlexWrapperChecker.check(flexViewBridge)) {

            flexViewBridge.bridge(view);

            return true;
        }

        return false;
    }

    public abstract boolean contentWrapper(FlexViewBridge flexViewBridge);

    protected final FlexContextInterface flexContext(){
        return this.f;
    }

    @Override
    public final void call(FlexParams params) {
        params.manageParam(this.f, this);
    }

    protected final void initInnerBridge(){
        // A subclass that bridges no native view (contentWrapper() returning false) would otherwise fail
        // with a bare NullPointerException at construction time, where nothing names the culprit. The
        // check below makes that failure explicit and says which class forgot its content(...) call.
        if (!this.contentWrapper(this.innerBridge) || this.innerBridge.me() == null) {
            throw new IllegalStateException(getClass().getName()
                    + ": contentWrapper() bridged no native view. A FlexView subclass must call "
                    + "content(...) with its facade view in its constructor (see FlexText, FlexBox)."
                    + " Without it, every operation would apply to a null view.");
        }

        /*  ** Attach the real Facade View inside the class to their FlexView, this for all FlexView
         *  ** (FlexText, FlexButton, FlexBox etc...), the power of this, is performing in the FlexViewGroup class, because,
         *  ** The FlexViewGroup, need to know the FlexView of view Facade, because the FlexViewGroup is an android native ViewGroup
         *  ** and need to manipulate also the android native View
        */
        this.innerBridge.me().setTag(this);
        //Affect directly the ref of the FlexFragment and the FlexActivity to this FlexView
        identify();

        registerToThemeScope();
    }

    /**
     * Registers this view with {@link FlexTheme}, so that it receives {@link #ifThemeChange()} on every
     * theme switch.
     *
     * <h4>Why the Fragment scope is the nominal case</h4>
     *
     * <p>FlexR imposes a standardised architecture, borrowed from JavaFX
     * ({@code Stage} / {@code Scene} / {@code Root}):
     *
     * <pre>
     *   FlexActivity            ~ Stage  — the host window, which carries no FlexView
     *   FlexFragment            ~ Scene  — the carrier of a screen's lifecycle
     *   FlexAppbar / FlexBody   ~ Root   — the FlexView tree of a screen
     * </pre>
     *
     * <p>Every {@code FlexView} is therefore born inside a <i>root</i> ({@code FlexAppbar} or
     * {@code FlexBody}), and that root is attached to its {@code FlexFragment} by
     * {@code FlexFragment.prepareScreen(...)} → {@code initFlexStarter(...)} →
     * {@code FlexContext.contextualize(...)} — <b>before</b> {@code dynamicBuild()} builds anything. When
     * this method runs, the context's Fragment is known, and the view registers on the scope of
     * <b>its own screen</b>.
     *
     * <p>That is the right scope because its purge is guaranteed: {@code FlexFragment.onDestroyView()}
     * calls {@code FlexTheme.clearObsFragScope("" + this)}. No view therefore survives its own screen in
     * the static observer list. Registration is idempotent — {@code FlexTheme} checks for presence before
     * adding.
     *
     * <h4>The Activity branch: a documented reserve, not a nominal path</h4>
     *
     * <p>The second branch — the Activity scope, purged by {@code FlexActivity.onDestroy()} →
     * {@code FlexTheme.clearObsActivityScope(...)} — is kept deliberately, for the day FlexR grows
     * elements carried by the <b>Activity</b> and surviving screens: a persistent app bar, an application
     * background, a side menu, a global overlay.
     *
     * <p>It is <b>not reachable today</b>, and that matters before anyone relies on it:
     * <ul>
     *   <li>{@code FlexActivity} does not implement {@code FlexContextInterface}, so no {@code FlexView}
     *       can be built "inside the Activity" — the Activity's container only receives Fragments;</li>
     *   <li>{@code identify()} — called just before this method — already resolves
     *       {@code f.getFlexActivity()} through {@code getFlexFragment().requireContext()}, so a context
     *       without a Fragment fails <b>earlier</b>, while the view is being built.</li>
     * </ul>
     *
     * <p>Making that branch usable needs more than a test: an Activity-side implementation of
     * {@code FlexContextInterface} ({@code getFlexFragment()} returning {@code null}), and an adjustment
     * of {@code FlexContext.getFlexContext()}, which currently requires a non-null Fragment.
     *
     * <p>{@code autoApplyTheme(false)} makes a view inert towards switches without unregistering it: the
     * {@code isThemeAutoApply()} test happens when {@code FlexTheme} notifies.
     */
    private void registerToThemeScope(){
        if (!this.dynamiqueApplyTheme || this.f == null) return;

        if (this.f.getFlexFragment() != null) {
            FlexTheme.subscribeFragScope(this);
        } else {
            FlexTheme.subscribeActivityScope(this);
        }
    }


    //heavy because each FlexView is creating an implementation of new ViewTreeObserver.OnPreDrawListener(){} Object
    //FlexStructureLooper is more concise because its use a static array and call on the reference of FlexView
    //if want to use lazyVisible() dev must call this method in the constructor
    protected final void lazyVisible(){
        this.innerBridge.me().getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                if (innerBridge.me().getVisibility() == View.VISIBLE) {
                    FlexView.this.onFlexContainerIsVisible();
                    innerBridge.me().getViewTreeObserver().removeOnPreDrawListener(this);
                }

                return true;
            }
        });
    }

    @Override
    public final void identify() {
        if(this.scopeId == null) this.scopeId = f.getFlexFragment() +":"+ f.getFlexActivity();
    }

    /**
     * Returns the part of this view's screen identity that corresponds to the requested scope.
     *
     * <p>The identity is computed on first use ({@link #identify()}), so a view can be asked for its
     * scope as soon as it is built.
     *
     * @param scope {@code FlexScope.FlexFragment.name()} or {@code FlexScope.FlexActivity.name()}
     * @return the requested part of the identity, or an empty string when it is not available
     */
    @Override
    public final String ref(String scope) {
        identify();

        String scopeResult = "";

        if (this.scopeId == null || this.scopeId.indexOf(":") < 0) {
            return scopeResult;
        }

        if(scope.equals(FlexScope.FlexFragment.name())) scopeResult =  this.scopeId.substring(0, this.scopeId.indexOf(":"));
        else if((scope.equals(FlexScope.FlexActivity.name()))) scopeResult =  this.scopeId.substring(this.scopeId.indexOf(":")+1);

        return scopeResult;
    }

    @Override
    public void baseInit() {

    }
    @Override
    public void dynamicBuild() {

    }
    @Override
    public void attachEvents() {

    }
    @Override
    public void initialBusinessLogic() {

    }
    @Override
    public void attachSyncEvents() {

    }
    @Override
    public void initialSyncBusinessLogic() {

    }
    @Override
    public void forgettingFlexConsumers() {

    }

    public final int paddingLeft(){
        return this.innerBridge.me().getPaddingLeft();
    }
    public final int paddingTop(){
        return this.innerBridge.me().getPaddingTop();       // Read on its own side; the left inset would be wrong here.
    }
    public final int paddingRight(){
        return this.innerBridge.me().getPaddingRight();     // Read on its own side; the left inset would be wrong here.
    }
    public final int paddingBottom(){
        return this.innerBridge.me().getPaddingBottom();
    }

    // All the following methods are the method of View(FlexView)... common method defined in the native View class
    // Like elevation, v.setActivated(boolean);    v.setBackground(Drawable);  v.setBackgroundResource(Resource);
    //Set the background of view with Color
    public final FlexView background(int themeColorIndex){
        // e.g. themeIndex = FlexTheme.load(1), then load(1) + 1 when an array skips a palette index.
        //confer flexActivity class, theme declaration
        this.innerBridge.me().setBackgroundColor(FlexTheme.load(themeColorIndex));
        backColor = themeColorIndex;
        backColorThemeName = null;
        themedShape = null;
        return this;
    }
    public final FlexView background(int themeColorIndex, String themeName){
        this.innerBridge.me().setBackgroundColor(FlexTheme.load(themeColorIndex, themeName));
        backColor = themeColorIndex;
        backColorThemeName = themeName;
        themedShape = null;
        return this;
    }

    //Set the background of view with Drawable
    public final FlexView background(String drawable){
        //can use int resource or Drawable directly
        this.innerBridge.me().setBackground(f.flexDrawable(drawable));
        //this.innerBridge.me().setBackgroundResource(f.flexAsset(In.drawable, drawable));
        // A Drawable does not depend on the palette: it replaces both the colour background and the shape.
        backColor = NO_THEME;
        backColorThemeName = null;
        themedShape = null;
        return this;
    }

    //Set the background of view with MaterialShapeDrawable
    public final FlexView background(FlexShape flexShape){
        flexShape.applyDrawable(new FlexShapeBridge());
        // The shape owns the background from now on: it is the one recoloured on every theme switch
        // (shapeColor / stroke), not the view.
        backColor = NO_THEME;
        backColorThemeName = null;
        themedShape = flexShape;
        return this;
    }
    public final int relativeLeft(){
        return this.innerBridge.me().getLeft();
    }
    public final int relativeRight(){
        return this.innerBridge.me().getRight();
    }
    public final int relativeTop(){
        return this.innerBridge.me().getTop();
    }
    public final int relativeBottom(){
        return this.innerBridge.me().getBottom();
    }
    public final int width(){
        return this.innerBridge.me().getWidth();
    }
    public final int height(){
        return this.innerBridge.me().getHeight();
    }
    public final FlexView elevation(int elevation){
        this.innerBridge.me().setElevation(elevation);
        return this;
    }
    public final FlexView clickable(boolean clickable){
        this.innerBridge.me().setClickable(clickable);
        return this;
    }
    public final FlexView translate(int value){

        this.innerBridge.me().setTranslationY(value);
        this.innerBridge.me().setTranslationX(value);
        this.innerBridge.me().setTranslationZ(value);

        return this;
    }

    /**
     * Shows or hides the view.
     *
     * <p>Hidden means {@code GONE}: the view leaves the layout pass, so its siblings move up. The
     * {@code INVISIBLE} state is not exposed on purpose — a view that keeps its slot but cannot be
     * touched is a state most screens express more clearly with alpha.
     *
     * <p>UI thread only.
     *
     * @param visible {@code true} to display the view, {@code false} to hide it
     * @return this view, for chaining
     */
    public final FlexView visible(boolean visible){
        this.innerBridge.me().setVisibility(visible ? View.VISIBLE : View.GONE);
        return this;
    }

    /**
     * Sets the description read by accessibility services (TalkBack) and by UI tests.
     *
     * <p>Give one to every view whose meaning is carried by an image or an icon only: a screen reader
     * reads the description instead of the pixels. Passing {@code null} clears it.
     *
     * <p>UI thread only.
     *
     * @param description text read aloud, or {@code null} to remove it
     * @return this view, for chaining
     */
    public final FlexView contentDescription(CharSequence description){
        this.innerBridge.me().setContentDescription(description);
        return this;
    }

    /**
     * Key under which {@link #testTag(String)} stores its value.
     *
     * <p>A keyed tag is used instead of the plain one because FlexR and the adapters already store
     * values in {@code View.getTag()}; a plain tag would collide with them. An Espresso matcher can read
     * it back with {@code view.getTag(FlexView.TEST_TAG_KEY)}.
     */
    public static final int TEST_TAG_KEY = 0x466C5231;

    /**
     * Tags the view for UI tests.
     *
     * <p>The value never reaches the user and is never translated: it is an identifier, so it stays
     * stable when the visible text changes or when the application language changes.
     *
     * <p>UI thread only.
     *
     * @param tag stable identifier of this view in the tests; {@code null} clears it
     * @return this view, for chaining
     */
    public final FlexView testTag(String tag){
        this.innerBridge.me().setTag(TEST_TAG_KEY, tag);
        return this;
    }

    /**
     * Returns the native view behind this FlexR view.
     *
     * <p><b>Escape hatch.</b> It exists for the few Android APIs that only accept a {@code View} —
     * {@code Snackbar.make(...)}, {@code InputMethodManager.showSoftInput(...)}, a third-party image
     * loader — and not for layout: a size, a margin or a colour set directly on the native view would
     * bypass the parameters, the theme and the animation engine, and the next {@code call(...)} would
     * overwrite it.
     *
     * <p>UI thread only.
     *
     * @return the view bridged to this FlexR view, never {@code null} once construction succeeded
     */
    public final View nativeView(){
        return this.innerBridge.me();
    }

    public FlexView padding(String values){
        int[] ints = FlexMargins.processAll(f, values, false, paddingLeft(), paddingTop(), paddingRight(), paddingBottom());
        this.innerBridge.me().setPadding(ints[0], ints[1], ints[2], ints[3]);
        return this;
    }

    public FlexView paddingTo(String values){
        int[] ints = FlexMargins.processAll(f, values, true, paddingLeft(), paddingTop(), paddingRight(), paddingBottom());
        this.innerBridge.me().setPadding(ints[0], ints[1], ints[2], ints[3]);
        return this;
    }
    public FlexView paddingOnly(String values, String strategy){
        int[] ints = FlexMargins.processOnly(f, values, strategy,false, paddingLeft(), paddingTop(), paddingRight(), paddingBottom());
        this.innerBridge.me().setPadding(ints[0], ints[1], ints[2], ints[3]);
        return this;
    }
    public FlexView paddingOnlyTo(String values, String strategy){
        int[] ints = FlexMargins.processOnly(f, values, strategy,true, paddingLeft(), paddingTop(), paddingRight(), paddingBottom());
        this.innerBridge.me().setPadding(ints[0], ints[1], ints[2], ints[3]);
        return this;
    }

    /**
     * Debugging aid: prints the dimensions and paddings to the standard output.
     *
     * <p>Never called by the library — that is the whole point: it only prints when a developer asks for
     * it, while diagnosing a layout. Nothing in the nominal path writes to the standard output.
     */
    public final void dimState(){
        System.out.println("SIZE = WIDTH->"+ width()+"   HEIGHT->"+height());
        System.out.println("PADDINGS = LEFT->"+ paddingLeft()+"   TOP->"+paddingTop()+"    RIGHT->"+ paddingRight()+"   BOTTOM->"+paddingBottom());
    }

    /** Debugging aid: logs the relative positions (left/top/right/bottom). */
    public final void relative(){
        System.out.println("RELATIVE = LEFT->"+ relativeLeft()+"   TOP->"+relativeTop()+"    RIGHT->"+ relativeRight()+"   BOTTOM->"+relativeBottom());
    }


    //The Way to add these all default Listener methods to the view class, the common listener methods for all
    //Views
    public final void onClick(View.OnClickListener clickListener){
        this.innerBridge.me().setOnClickListener(clickListener);
    }
    public final void onLongClick(View.OnLongClickListener longClickListener){
        this.innerBridge.me().setOnLongClickListener(longClickListener);
    }

    public final class FlexShapeBridge{
        public void bridge(MaterialShapeDrawable drawable){
            FlexView.this.innerBridge.me().setBackground(drawable);
        }
    }

    /**
     * Enables or disables this view's automatic reaction to theme switches.
     *
     * <p>Disabled, the view stays registered but {@code isThemeAutoApply()} returns {@code false}, so
     * {@code FlexTheme} stops sending it {@link #ifThemeChange()}. Enabled again, registration is replayed
     * — it is idempotent — in case it had been lost.
     *
     * @param autoApplyTheme {@code true} to follow theme switches, {@code false} to ignore them
     * @return this view, for chaining
     */
    public final FlexView autoApplyTheme(boolean autoApplyTheme){
        this.dynamiqueApplyTheme = autoApplyTheme;
        if (autoApplyTheme) registerToThemeScope();
        return this;
    }

    @Override
    public boolean isThemeAutoApply() {
        return this.dynamiqueApplyTheme;
    }

    //FlexAnimateInterfaceImpl

    @Override
    public FlexPropertiesAnimate setFlexAnimateTranslateX(float valueX) {
        this.innerBridge.me().setTranslationX(valueX);
        return this;
    }

    @Override
    public FlexPropertiesAnimate setFlexAnimateTranslateY(float valueY) {
        this.innerBridge.me().setTranslationY(valueY);
        return this;
    }

    @Override
    public FlexPropertiesAnimate setFlexAnimateTranslateZ(float valueZ) {
        this.innerBridge.me().setTranslationZ(valueZ);
        return this;
    }

    @Override
    public FlexPropertiesAnimate setFlexAnimateX(float valueX) {
        this.innerBridge.me().setX(valueX);
        return this;
    }

    @Override
    public FlexPropertiesAnimate setFlexAnimateY(float valueY) {
        this.innerBridge.me().setY(valueY);
        return this;
    }

    @Override
    public FlexPropertiesAnimate setFlexAnimateZ(float valueZ) {
        this.innerBridge.me().setZ(valueZ);
        return this;
    }

    @Override
    public FlexPropertiesAnimate setFlexAnimateRotationX(float valueX) {
        this.innerBridge.me().setRotationX(valueX);
        return this;
    }

    @Override
    public FlexPropertiesAnimate setFlexAnimateRotationY(float valueY) {
        this.innerBridge.me().setRotationY(valueY);
        return this;
    }

    @Override
    public FlexPropertiesAnimate setFlexAnimateScaleX(float valueX) {
        this.innerBridge.me().setScaleX(valueX);
        return this;
    }

    @Override
    public FlexPropertiesAnimate setFlexAnimateScaleY(float valueY) {
        this.innerBridge.me().setScaleY(valueY);
        // Returning null here would break every animation chain.
        // (.setFlexAnimateScaleX(..).setFlexAnimateScaleY(..) -> NullPointerException).
        return this;
    }

    @Override
    public FlexPropertiesAnimate setFlexAnimateAlpha(float alphaValue) {
        this.innerBridge.me().setAlpha(alphaValue);
        return this;
    }

    @Override
    public FlexPropertiesAnimate setFlexAnimateElevation(float elevationValue) {
        this.innerBridge.me().setElevation(elevationValue);
        return this;
    }

    @Override
    public FlexPropertiesAnimate setFlexAnimateScrollX(int scrollXValue) {
        xScroll = scrollXValue;
        return this;
    }

    @Override
    public FlexPropertiesAnimate setFlexAnimateScrollY(int scrollYValue) {
        yScroll = scrollYValue;
        return this;
    }

    @Override
    public FlexPropertiesAnimate setFlexAnimatePaddings(int value) {

        setFlexAnimatePaddingLeft(value);
        setFlexAnimatePaddingTop(value);
        setFlexAnimatePaddingRight(value);
        setFlexAnimatePaddingBottom(value);

        // Returning null here would break the chaining of animation properties.
        return this;
    }

    @Override
    public FlexPropertiesAnimate setFlexAnimatePaddingLeft(int leftValue) {
        pl = leftValue;
        return this;
    }

    @Override
    public FlexPropertiesAnimate setFlexAnimatePaddingTop(int topValue) {
        pt = topValue;
        return this;
    }

    @Override
    public FlexPropertiesAnimate setFlexAnimatePaddingRight(int rightValue) {
        pr = rightValue;
        return this;
    }

    @Override
    public FlexPropertiesAnimate setFlexAnimatePaddingBottom(int bottomValue) {
        pb = bottomValue;
        return this;
    }

    @Override
    public final void flexAnimateApply() {
        // applyAnimateScroll() used to exist without ever being called, while flexAnimateChecker()
        // does count scroll offsets among the animated properties: a scroll choreography was reported
        // as active without ever moving anything. It is now started when such a property is set.
        applyAnimatePadding();
        applyAnimateScroll();
    }

    @Override
    public final boolean flexAnimateChecker() {
        affectValue();
        return (need(plTemp) || need(ptTemp) || need(prTemp) || need(pbTemp) || need(xScrollTemp) || need(yScrollTemp));
    }

    //This method use the FlexAnimate Bridge inside the class to obtain the FlexParam (FlexParamsAnimate)
    @Override
    public final FlexAnimate.FlexParamsAnimateBridger affect_(FlexAnimate.FlexParamsAnimateBridger bridger){
        bridger.bridge(params());
        return bridger;
    }

    private void applyAnimatePadding(){
        if(need(plTemp) || need(ptTemp) || need(prTemp) || need(pbTemp)){
            int left = paddingLeft(), top = paddingTop(), right = paddingRight(), bottom = paddingBottom();

            //This test ensure that if even its only one value is modified, all these rest will keep their value and will be
            //this to prevent reset/erase the value that are not concerning
            //for ex, only padding left is modified, the rest gonna get their present value and reassign ti
            //this because all are modified by one call in choreo
            //this the same logic for all the values that are modified simultaneous (margins, padding, scroll(X and Y)

            if(need(plTemp) && plTemp != paddingLeft()) left = plTemp;
            if(need(ptTemp) && ptTemp != paddingTop()) top = ptTemp;
            if(need(prTemp) && prTemp != paddingRight()) right = prTemp;
            if(need(pbTemp) && pbTemp != paddingBottom()) bottom = pbTemp;

            this.innerBridge.me().setPadding(left, top, right, bottom);
            initPddAnimate();
        }
    }
    private void applyAnimateScroll(){

        if(need(xScrollTemp) || need(yScrollTemp)){

            int scrollX = this.innerBridge.me().getScrollX();
            int scrolly = this.innerBridge.me().getScrollY();

            if(need(xScrollTemp) && xScrollTemp != scrollX) scrollX = xScrollTemp;
            if(need(yScrollTemp) && yScrollTemp != scrolly) scrolly = yScrollTemp;

            this.innerBridge.me().setScrollX(scrollX);
            this.innerBridge.me().setScrollY(scrolly);

            initScrollAnimate();
        }
    }
    private boolean need(int value){
        return (value != 99887);
    }
    private void initPddAnimate(){
        plTemp = BaseState;
        ptTemp = BaseState;
        prTemp = BaseState;
        pbTemp = BaseState;
    }
    private void initScrollAnimate(){
        xScrollTemp = BaseState;
        yScrollTemp = BaseState;
    }
    private void initAnimate(){
        pl = BaseState;
        pt = BaseState;
        pr = BaseState;
        pb = BaseState;

        xScroll = BaseState;
        yScroll = BaseState;
    }
    private void affectValue(){
        plTemp = pl;
        ptTemp = pt;
        prTemp = pr;
        pbTemp = pb;

        xScrollTemp = xScroll;
        yScrollTemp = yScroll;
    }

    private static final class FlexViewWrapperBridge implements FlexViewBridge{
        private View view;

        @Override
        public void bridge(View view) {
            this.view = view;
        }
        private View me(){
            return this.view;
        }
    }
}
