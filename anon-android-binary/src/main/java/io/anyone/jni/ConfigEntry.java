// Copyright 2005 Nick Mathewson, Roger Dingledine
// See LICENSE file for copying information
package io.anyone.jni;

/** A single key-value pair from Anon's configuration. */
public class ConfigEntry {

    public ConfigEntry(String k, String v) {
        key = k;
        value = v;
        is_default = false;
    }

    public ConfigEntry(String k) {
        key = k;
        value = "";
        is_default = true;
    }

    public final String key;

    public final String value;

    public final boolean is_default;
}