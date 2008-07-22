/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.console.tree;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.HttpTransportAssertion;

/**
 * @author alex
 * @version $Revision$
 */
public class HttpTransportPaletteNode extends AbstractLeafPaletteNode {
    public HttpTransportPaletteNode() {
        super("Message received via HTTP(S)", "com/l7tech/console/resources/server16.gif");
    }

    public Assertion asAssertion() {
        return new HttpTransportAssertion();
    }
}
