/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.wsp;



/**
 * Typesafe enum holding known policy versions.
 */
class WspVersionImpl implements WspVersion {
    public static final WspVersion VERSION_3_0 = new WspVersionImpl("3.0");
    public static final WspVersion VERSION_2_1 = new WspVersionImpl("2.1");

    private final String representation;

    private WspVersionImpl(String representation) {
        this.representation = representation;
    }
}
