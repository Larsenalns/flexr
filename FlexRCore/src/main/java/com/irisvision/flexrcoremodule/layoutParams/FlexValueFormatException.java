// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 IrisVision

package com.irisvision.flexrcoremodule.layoutParams;

/**
 * Thrown when a layout value string — margins, paddings, dimensions — is malformed.
 *
 * <p>Layout values reach FlexR as text ({@code "10L, 20T"}, {@code "10, 20, 10, 20, TRL"}), and a
 * malformed one has to fail in a way a developer can act on. This exception exists for that: its message
 * names <b>which</b> string is wrong, <b>which</b> segment is at fault, and <b>which</b> format was
 * expected.
 *
 * <p>It extends {@link IllegalArgumentException} because that is what it is: the caller passed a value
 * the API cannot honour, and the fix is on the caller's side.
 */
public class FlexValueFormatException extends IllegalArgumentException {

    /**
     * Creates the exception with an explanatory message.
     *
     * @param message what is wrong, which segment is at fault, and the expected format
     */
    public FlexValueFormatException(String message){
        super(message);
    }

    /**
     * Creates the exception with an explanatory message and the underlying cause.
     *
     * @param message what is wrong, which segment is at fault, and the expected format
     * @param cause   the lower-level failure, kept for the stack trace
     */
    public FlexValueFormatException(String message, Throwable cause){
        super(message, cause);
    }
}
