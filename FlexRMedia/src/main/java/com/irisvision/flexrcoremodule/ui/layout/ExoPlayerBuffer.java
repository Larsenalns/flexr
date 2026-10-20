// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrcoremodule.ui.layout;

import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.LoadControl;

/**
 * ExoPlayer buffer profiles.
 *
 * <p>This class used to be a nested type inside {@code FlexVideo}, where the
 * {@link UnstableApi} annotation sat on the class instead of its methods, so it did not apply.
 * It is now public and reusable, and each of its methods is genuinely annotated.
 *
 * <p>Each profile sets four durations (in milliseconds), in the order ExoPlayer expects:
 * {@link DefaultLoadControl.Builder#setBufferDurationsMs} :
 * <ol>
 *   <li>minimum duration before playback may start;</li>
 *   <li>maximum buffer duration;</li>
 *   <li>duration to keep after a playback;</li>
 *   <li>duration to keep after a seek.</li>
 * </ol>
 */
public final class ExoPlayerBuffer {

    private ExoPlayerBuffer() {
        // Utility class: no instances.
    }

    /** Balanced profile: ExoPlayer's default settings. */
    @UnstableApi
    public static LoadControl createDefaultBufferProfile(){
        return new DefaultLoadControl.Builder().build();
    }

    /** Short loop: reacts fast when a looping video restarts. */
    @UnstableApi
    public static LoadControl createLoopingBufferProfile() {
        return new DefaultLoadControl.Builder()
                .setBufferDurationsMs(1500, 10000, 1000, 1500)
                .setTargetBufferBytes(50 * 1024 * 1024)
                .setPrioritizeTimeOverSizeThresholds(true)
                .build();
    }

    /** Live streaming: minimal buffer to stay as close to live as possible. */
    @UnstableApi
    public static LoadControl createStreamingLiveBufferProfile(){
        return new DefaultLoadControl.Builder()
                .setBufferDurationsMs(1000, 3000, 500, 1000)
                .build();
    }

    /** Low-memory devices: modest buffer, released quickly. */
    @UnstableApi
    public static LoadControl createLowRamCoastBufferProfile(){
        return new DefaultLoadControl.Builder()
                .setBufferDurationsMs(1500, 5000, 1000, 1500)
                .build();
    }

    /** Cinema playback: wide buffer to absorb network variation. */
    @UnstableApi
    public static LoadControl createCinemaBufferProfile(){
        return new DefaultLoadControl.Builder()
                .setBufferDurationsMs(5000, 60000, 3000, 4000)
                .build();
    }
}
