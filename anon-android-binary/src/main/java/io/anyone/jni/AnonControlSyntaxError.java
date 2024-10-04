// Copyright 2005 Nick Mathewson, Roger Dingledine
// See LICENSE file for copying information
package io.anyone.jni;

import java.io.IOException;
import java.io.Serial;

/**
 * An exception raised when Anon behaves in an unexpected way.
 */
public class AnonControlSyntaxError extends IOException {

    @Serial
    private static final long serialVersionUID = 3;

    public AnonControlSyntaxError(String s) { super(s); }
}
