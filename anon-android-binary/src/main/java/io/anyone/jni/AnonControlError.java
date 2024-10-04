// Copyright 2005 Nick Mathewson, Roger Dingledine
// See LICENSE file for copying information
package io.anyone.jni;

import java.io.IOException;
import java.io.Serial;

/**
 * An exception raised when Anon tells us about an error.
 * @noinspection unused
 */
public class AnonControlError extends IOException {

    @Serial
    private static final long serialVersionUID = 3;

    private final int errorType;

    public AnonControlError(int type, String s) {
        super(s);
        errorType = type;
    }

    public AnonControlError(String s) {
        this(-1, s);
    }

    public int getErrorType() {
        return errorType;
    }

    public String getErrorMsg() {
        try {
            if (errorType == -1)
                return null;
            return AnonControlCommands.ERROR_MSGS[errorType];
        } catch (ArrayIndexOutOfBoundsException ex) {
            return "Unrecongized error #"+errorType;
        }
    }
}
