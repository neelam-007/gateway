/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.proxy.cli;

import java.io.PrintStream;

/**
 * Represents a configuration noun like "gateways" or "gateway57".
 */
abstract class Noun extends Word {
    protected Noun(String name, String desc) {
        super(name, desc);
    }

    // Display all relevant info about this noun to the specified output stream.

    /**
     * Display information about this noun, or the specific property of this noun, to the specified output stream.
     *
     * @param out  the output stream.  Must not be null.
     * @param args info about the property to display; if not provided, all relevant overview info should be displayed.
     *             May be null or empty.
     */
    public void show(PrintStream out, String[] args) {
        out.println(getName() + " - " + getDesc() + "\n");        
    }
}
