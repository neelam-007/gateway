/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.console.tree;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.RemoteIpRange;

/**
 * @author alex
 * @version $Revision$
 */
public class JmsTransportPaletteNode extends AbstractLeafPaletteNode {
    public JmsTransportPaletteNode() {
        super("Message received via JMS", "com/l7tech/console/resources/interface.gif");
    }

    public Assertion asAssertion() {
        return new RemoteIpRange();
    }
}
