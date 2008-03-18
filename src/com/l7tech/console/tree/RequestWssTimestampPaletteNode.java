/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.console.tree;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.xmlsec.RequestWssTimestamp;

public class RequestWssTimestampPaletteNode extends AbstractLeafPaletteNode {
    private static final String DEFAULT_NAME = "Require Timestamp in Message";

    public RequestWssTimestampPaletteNode(String name) {
        super(name, "com/l7tech/console/resources/xmlencryption.gif");
    }

    public RequestWssTimestampPaletteNode() {
        super(RequestWssTimestampPaletteNode.DEFAULT_NAME, "com/l7tech/console/resources/xmlencryption.gif");
    }

    /**
     * Return assertion representation of the node
     * or <b>null</b> if the node cannot be an assertion
     *
     * @return the assertion this node represnts
     */
    public Assertion asAssertion() {
        return RequestWssTimestamp.newInstance();
    }
}
