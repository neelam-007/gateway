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
    public void show(PrintStream out) {
        out.println(getName() + " - " + getDesc() + "\n");        
    }
}
