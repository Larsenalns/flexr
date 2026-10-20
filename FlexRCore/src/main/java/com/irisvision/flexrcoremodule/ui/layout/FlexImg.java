// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrcoremodule.ui.layout;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.shape.ShapeAppearanceModel;
import com.irisvision.flexrcoremodule.core.FlexContextInterface;
import com.irisvision.flexrcoremodule.core.FlexView;
import com.irisvision.flexrcoremodule.core.FlexViewBridge;
import com.irisvision.flexrcoremodule.enums.In;
import com.irisvision.flexrcoremodule.ui.component.FlexShape;

/**
 * An image, backed by a Material {@link ShapeableImageView}: the corner radii and the stroke come from a
 * {@link FlexShape}, the colour follows the palette when the shape is themed.
 *
 * <h2>Where the pixels come from</h2>
 *
 * <p>Three entry points, in the spirit of the library:
 * <ul>
 *   <li>{@link #load(String)} — a drawable designated <b>by its name</b>, resolved through
 *       {@code flexAsset(In.drawable, name)}. This is the FlexR way: no {@code R.drawable} reference, so
 *       the screen can be driven by data;</li>
 *   <li>{@link #loadBitmap(Bitmap)} and {@link #loadDrawable(Drawable)} — for images the application
 *       already holds;</li>
 *   <li>{@link #loadUri(String)} — for everything else (network, files, content:// URIs), delegated to the
 *       application-wide {@link Loader}.</li>
 * </ul>
 *
 * <h2>Asynchronous loading</h2>
 *
 * <p>FlexR does not embed an HTTP client or a disk cache: it declares a single seam instead. An
 * application registers its loader once, typically where the theme is declared:
 *
 * <pre>{@code
 * FlexImg.useLoader(new FlexImg.Loader() {
 *     @Override public void load(ImageView target, String source, FlexImg image) {
 *         Glide.with(target).load(source).into(target);      // or Coil, or a custom client
 *     }
 *     @Override public void cancel(ImageView target) {
 *         Glide.with(target).clear(target);
 *     }
 * });
 * }</pre>
 *
 * <p>The loader is called on the <b>UI thread</b>, with the target view, so it only has to start the
 * request; recycling a row must call {@link #loadUri(String)} again, and the adapter already does it.
 * Without a registered loader, {@code loadUri(...)} fails loudly rather than showing an empty image.
 *
 * <h2>Threading</h2>
 *
 * <p>Every method touches a view and must be called on the <b>UI thread</b>. The loader itself may do its
 * work on a background thread — that is its own business — but the call to it, and the final
 * {@code into(target)}, happen on the UI thread.
 */
public class FlexImg extends FlexView {

    /** Applies the loader to the native image view. */
    private static volatile Loader loader;

    private final ShapeableImageView imageView;

    /**
     * Creates an empty image and registers it in the theme scope of its screen.
     *
     * @param flexContext context of the screen that owns this image
     */
    public FlexImg(FlexContextInterface flexContext) {
        super(flexContext);
        this.imageView = new ShapeableImageView(f.getFlexContext());
        initInnerBridge();
    }

    /**
     * Bridges the native {@code ShapeableImageView} to FlexR.
     *
     * @param flexViewBridge bridge supplied by the framework
     * @return {@code true} when the bridge accepted the view
     */
    @Override
    public boolean contentWrapper(FlexViewBridge flexViewBridge) {
        return content(flexViewBridge, this.imageView);
    }

    /**
     * Adapter between FlexR and an asynchronous image library.
     *
     * <p>Implement it once per application, with the library of your choice (Glide, Coil, Picasso, a
     * custom client). FlexR calls {@link #load(ImageView, String, FlexImg)} for every
     * {@code loadUri(...)}, and {@link #cancel(ImageView)} when it must drop the pending request — a row
     * being recycled, for instance.
     */
    public interface Loader {

        /**
         * Starts loading {@code source} into {@code target}.
         *
         * <p>Called on the <b>UI thread</b>. The implementation may start background work, but it must
         * return quickly; the final image is set on {@code target} when it arrives.
         *
         * @param target native image view to fill
         * @param source address to load: URL, file path, {@code content://} URI, anything the library
         *               understands
         * @param image  the FlexR image that made the request, for a loader that needs its context
         */
        void load(ImageView target, String source, FlexImg image);

        /**
         * Drops any pending request for {@code target}, leaving the current image in place.
         *
         * <p>Called on the UI thread. The default implementation does nothing, which is correct for a
         * loader that only sets a bitmap.
         *
         * @param target native image view whose request must be forgotten
         */
        default void cancel(ImageView target) {
        }
    }

    /**
     * Registers the application-wide image loader used by {@link #loadUri(String)}.
     *
     * <p>Call it once, before the first {@code loadUri(...)}: a screen built before the registration
     * would fail. Passing {@code null} removes the loader, which is only useful in tests.
     *
     * @param imageLoader loader to use, or {@code null} to unregister the current one
     */
    public static void useLoader(Loader imageLoader) {
        FlexImg.loader = imageLoader;
    }

    /**
     * Returns the registered loader.
     *
     * @return the application-wide loader, or {@code null} when none was registered
     */
    public static Loader currentLoader() {
        return FlexImg.loader;
    }

    /**
     * Loads a drawable <b>by its name</b>, resolved against the application resources.
     *
     * <p>This is the FlexR addressing mode: the name can come from the data, which is what allows a list
     * of images to be described by a server response. The name must exist in the merged resources; see
     * the {@code keep.xml} advice of the guide when {@code shrinkResources} is enabled.
     *
     * <p>UI thread only.
     *
     * @param imageName name of the drawable, without extension nor {@code R.drawable.} prefix
     * @return this image, for chaining
     * @throws IllegalArgumentException if no drawable of that name exists in the application
     */
    public final FlexImg load(String imageName){
        this.imageView.setImageResource(f.flexAsset(In.drawable, imageName));
        return this;
    }

    /**
     * Loads an image from an address, through the registered {@link Loader}.
     *
     * <p>The address is opaque for FlexR: a URL, a file path or a {@code content://} URI are all the
     * loader's business. Call it again when a recycled row receives a new datum.
     *
     * <p>UI thread only.
     *
     * @param uri address understood by the registered loader
     * @return this image, for chaining
     * @throws IllegalStateException if no loader was registered with {@link #useLoader(Loader)}
     */
    public final FlexImg loadUri(String uri){
        Loader current = FlexImg.loader;
        if (current == null) {
            throw new IllegalStateException("loadUri(\"" + uri + "\") : no image loader registered. "
                    + "Call FlexImg.useLoader(...) once, for example with a Glide or Coil adapter.");
        }
        current.load(this.imageView, uri, this);
        return this;
    }

    /**
     * Shows an image the application already decoded.
     *
     * <p>UI thread only.
     *
     * @param bitmap image to display; {@code null} clears the current one
     * @return this image, for chaining
     */
    public final FlexImg loadBitmap(Bitmap bitmap){
        this.imageView.setImageBitmap(bitmap);
        return this;
    }

    /**
     * Shows a drawable the application already holds.
     *
     * <p>UI thread only.
     *
     * @param drawable drawable to display; {@code null} clears the current one
     * @return this image, for chaining
     */
    public final FlexImg loadDrawable(Drawable drawable){
        this.imageView.setImageDrawable(drawable);
        return this;
    }

    /**
     * Removes the current image.
     *
     * <p>Equivalent to {@code loadDrawable(null)}, kept as its own name because a clearing call reads
     * better than a null argument. A pending request is <b>not</b> cancelled by this method: call
     * {@link Loader#cancel(ImageView)} through your loader when that matters.
     *
     * <p>UI thread only.
     *
     * @return this image, for chaining
     */
    public final FlexImg clear(){
        this.imageView.setImageDrawable(null);
        return this;
    }

    /**
     * Sets how the image is scaled inside the view.
     *
     * <p>UI thread only.
     *
     * @param scale one of the {@code ImageView.ScaleType} values, for instance {@code CENTER_CROP}
     * @return this image, for chaining
     */
    public final FlexImg scale(ImageView.ScaleType scale){
        this.imageView.setScaleType(scale);
        return this;
    }

    /**
     * Gives the image its shape (corner radii, stroke, fill).
     *
     * <p>The shape is applied to the native view, so a themed shape keeps controlling the colours of the
     * image on every theme switch.
     *
     * <p>UI thread only.
     *
     * @param flexShape shape to apply
     * @return this image, for chaining
     */
    public final FlexImg appearance(FlexShape flexShape){
        flexShape.applyAppearance(new FlexAppearanceBridge());
        return this;
    }

    /** Bridge that receives the shape appearance and applies it to the image view. */
    public final class FlexAppearanceBridge{
        /**
         * Applies a shape appearance to the native image view.
         *
         * @param shapeAppearanceModel model produced by a {@code FlexShape}
         */
        public void bridge(ShapeAppearanceModel shapeAppearanceModel){
            FlexImg.this.imageView.setShapeAppearanceModel(shapeAppearanceModel);
        }
    }
}
