package com.l7tech.console.tree;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.xmlsec.WssTimestamp;

/**
 * Paletted node for Wss Timestamp Assertion.
 *
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
public class WssTimestampPaletteNode extends AbstractLeafPaletteNode {
    private static final String DEFAULT_NAME = "Request and response timestamps";

    public WssTimestampPaletteNode(String name) {
        super(name, "com/l7tech/console/resources/xmlencryption.gif");
    }

    public WssTimestampPaletteNode() {
        super(WssTimestampPaletteNode.DEFAULT_NAME, "com/l7tech/console/resources/xmlencryption.gif");
    }

    /**
     * Return assertion representation of the node
     * or <b>null</b> if the node cannot be an assertion
     *
     * @return the assertion this node represnts
     */
    public Assertion asAssertion() {
        return new WssTimestamp();
    }
}
