/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.util;

/**
 * Interface for objects handed out by classes that want to easily serialize and deserialize as Java-1.4-style enums.
 */
public interface EnumTranslator {
    /** Convert the specified String (ie, "OPTION_REQUIRED") into an Object (ie, SslAssertion.OPTION_REQUIRED). */
    Object stringToObject(String s) throws IllegalArgumentException;

    /** Convert the specified Object (ie, SslAssertion.OPTION_REQUIRED) into a String (ie, "OPTION_REQUIRED"). */
    String objectToString(Object o) throws ClassCastException;
}
