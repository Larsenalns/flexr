// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrcoremodule.ui.layout;

import android.net.Uri;

import androidx.annotation.OptIn;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import com.irisvision.flexrcoremodule.core.FlexContextInterface;
import com.irisvision.flexrcoremodule.core.FlexView;
import com.irisvision.flexrcoremodule.core.FlexViewBridge;
import com.irisvision.flexrcoremodule.enums.In;
import com.irisvision.flexrthememodule.FlexTheme;

import java.io.File;

/**
 * A video view, backed by ExoPlayer (media3).
 *
 * <p><b>Why it lives in its own module.</b> This class is the only one in FlexR that pulls ExoPlayer,
 * and it lives in {@code FlexRMedia} rather than in the core so that an application showing no video
 * carries no media3 artifact at all. It keeps the package it had in the core, so a consuming project
 * changes <b>only its Gradle dependency</b>, never an import.
 *
 * <p><b>Shrinking.</b> When the application enables code shrinking, the media code that is never used
 * is removed — measured at ≈ 30 KB for an application that includes this module but shows no video.
 * The rules needed by ExoPlayer itself are shipped by media3.
 */
public class FlexVideo extends FlexView {

    private ExoPlayer player;
    private PlayerView playerView;
    private boolean activeToggler = false;

    public FlexVideo(FlexContextInterface flexContext, BufferProfile bufferProfile) {
        super(flexContext);

        this.playerView = new PlayerView(f.getFlexContext());
        initExoPlayer(bufferProfile);
        this.playerView.setPlayer(this.player);
    }

    @Override
    public boolean contentWrapper(FlexViewBridge flexViewBridge) {
        return content(flexViewBridge, this.playerView);
    }


    //PlayerView Methods ...
    public final FlexVideo controlToggler(boolean controlToggleState){
        this.activeToggler = controlToggleState;

        if (activeToggler) this.playerView.setOnClickListener((v) -> hideAndShowControls());
        else this.playerView.setOnClickListener(null);

        return this;
    }

    @OptIn(markerClass = UnstableApi.class)
    public final FlexVideo controlShowTimeout(int timeOutMilli){

        this.playerView.setControllerShowTimeoutMs(timeOutMilli);

        return this;
    }

    public final FlexVideo artWorkFill(){
        return this.artWork(2);
    }
    public final FlexVideo artWorkFit(){
        return this.artWork(1);
    }
    public final FlexVideo artWorkNone(){
        return this.artWork(0);
    }

    @OptIn(markerClass = UnstableApi.class)
    private FlexVideo artWork(int artWorkMode){
        //0 = None, 1 = Fit, 2 = Fill;
        if(artWorkMode < 4 && artWorkMode > -1)
            this.playerView.setArtworkDisplayMode(artWorkMode);

        return this;
    }

    //Define manually the image when Audio will be playing
    //But if the audio has already their own image album, this will be used
    @OptIn(markerClass = UnstableApi.class)
    public final FlexVideo defineArtWork(String artWorkDrawable){
        this.playerView.setDefaultArtwork(f.flexDrawable(artWorkDrawable));
        return this;
    }

    public final FlexVideo contentBackGround(int themeColorIndex){

        this.playerView.setShutterBackgroundColor(FlexTheme.load(themeColorIndex));
        return this;
    }

    @OptIn(markerClass = UnstableApi.class)
    public final FlexVideo keepContentOnPlayReset(boolean keep){

        this.playerView.setKeepContentOnPlayerReset(keep);
        return this;
    }

    @OptIn(markerClass = UnstableApi.class)
    public final FlexVideo videoSize(VideoSize size){
        this.playerView.setResizeMode(size.ordinal());

        return this;
    }

    //When true, this methode ensure autoShowControl in these following context
    //When player is attached(setPlayer), When player goes on STATE_READY and playWhenReady is defined to false,
    //When state goes on STATE_ENDED or Pause.
    @OptIn(markerClass = UnstableApi.class)
    public final FlexVideo autoShowControlsBehavior(boolean activate){
        this.playerView.setControllerAutoShow(activate);

        return this;
    }

    /*  ** This methode can be added inside the setOnClickListener that user can add to this class
     *  ** Or attache call the clickListener by calling controlToggler, but if user define another callback setOnClickListener
     *  ** The controlToggler() will be remove
    */
    @OptIn(markerClass = UnstableApi.class)
    public final FlexVideo hideAndShowControls(){

        if (playerView.isControllerFullyVisible())playerView.hideController();
        else playerView.showController();

        return this;
    }

    //Manual decide if show control or not ... for ex user click on three dot at corner, this cas may be we will want to hide
    //controls if they are visible or show if they are hidden
    @OptIn(markerClass = UnstableApi.class)
    public final FlexVideo hideAndShowControls(boolean show){

        if (show)playerView.showController();
        else playerView.hideController();

        return this;
    }

    /**
     * Creates the player with the requested buffer profile.
     *
     * <p>The {@code switch} must cover every enum value, with a {@code break} in each branch: without
     * it, the requested profile would not be applied, and the player would silently keep the last one.
     * 
     */
    @OptIn(markerClass = UnstableApi.class)
    private void initExoPlayer(BufferProfile bufferProfile){
        switch (bufferProfile) {
            case Vod:
                player = new ExoPlayer.Builder(f.getFlexContext())
                        .setLoadControl(ExoPlayerBuffer.createDefaultBufferProfile())
                        .build();
                break;
            case Looping:
                player = new ExoPlayer.Builder(f.getFlexContext())
                        .setLoadControl(ExoPlayerBuffer.createLoopingBufferProfile())
                        .build();
                break;
            case Streaming:
                player = new ExoPlayer.Builder(f.getFlexContext())
                        .setLoadControl(ExoPlayerBuffer.createStreamingLiveBufferProfile())
                        .build();
                break;
            case LowRam:
                player = new ExoPlayer.Builder(f.getFlexContext())
                        .setLoadControl(ExoPlayerBuffer.createLowRamCoastBufferProfile())
                        .build();
                break;
            case UltraFluid:
                player = new ExoPlayer.Builder(f.getFlexContext())
                        .setLoadControl(ExoPlayerBuffer.createCinemaBufferProfile())
                        .build();
                break;
            default:
                throw new IllegalArgumentException("Unsupported buffer profile: " + bufferProfile);
        }
    }


    //ExoPlayer Methods...
    public final FlexVideo clearMediaItems(){
        this.player.clearMediaItems();
        return this;
    }

    //Load from res folder ...
    public void loadMediaFromRaw(String rawName) {
        Uri uri = Uri.parse("android.resource://" + f.getFlexContext().getPackageName() + "/" + f.flexAsset(In.raw, rawName));
        MediaItem item = MediaItem.fromUri(uri);
        player.setMediaItem(item);
        player.prepare();
    }

    //Provide http or https url fo media ...
    public void loadMediaFromHttp(String url) {
        Uri uri = Uri.parse(url);
        MediaItem item = MediaItem.fromUri(uri);
        player.setMediaItem(item);
        player.prepare();
    }

    //Provide File ...
    public void loadMediaFromFile(File file) {
        Uri uri = Uri.fromFile(file);
        MediaItem item = MediaItem.fromUri(uri);
        player.setMediaItem(item);
        player.prepare();
        player.play();
    }

    //For all purposes ... dev or user have to provide the Uri for the file that want to be played ...
    public void loadMedia(Uri uri) {
        MediaItem item = MediaItem.fromUri(uri);
        player.setMediaItem(item);
        player.prepare();
        player.play();
    }

    public final FlexVideo playWhenReady(boolean value){
        this.player.setPlayWhenReady(value);
        return this;
    }

    public final FlexVideo play(){
        this.player.play();
        return this;
    }
    public final FlexVideo pause(){
        this.player.pause();
        return this;
    }
    public final FlexVideo jumpTo(long millis){
        this.player.seekTo(millis);
        return this;
    }

    public final void playerListener(Player.Listener listener){
        this.player.addListener(listener);
    }


    /** Buffer profile to use, see {@link ExoPlayerBuffer}. */
    public enum BufferProfile {
        Vod, Looping, Streaming, LowRam, UltraFluid
    }

    /**
     * How the video image is sized inside the view: {@code Fit} letterboxes, {@code Fill} stretches,
     * {@code Zoom} crops the overflow, and the two {@code Fixed*} modes match one dimension.
     */
    public enum VideoSize{
        Fit, FixedWidth, FixedHeight, Fill, Zoom
    }
}
