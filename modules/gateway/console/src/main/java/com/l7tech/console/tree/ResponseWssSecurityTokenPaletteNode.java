package com.l7tech.console.tree;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.xmlsec.ResponseWssSecurityToken;

public class ResponseWssSecurityTokenPaletteNode extends AbstractLeafPaletteNode {
    private static final String DEFAULT_NAME = "Add Signed Security Token to Response";

    public ResponseWssSecurityTokenPaletteNode(String name) {
        super(name, "com/l7tech/console/resources/xmlencryption.gif");
    }

    public ResponseWssSecurityTokenPaletteNode() {
        super(ResponseWssSecurityTokenPaletteNode.DEFAULT_NAME, "com/l7tech/console/resources/xmlencryption.gif");
    }

    /**
     * Return assertion representation of the node
     * or <b>null</b> if the node cannot be an assertion
     *
     * @return the assertion this node represnts
     */
    public Assertion asAssertion() {
        return new ResponseWssSecurityToken();
    }
}
