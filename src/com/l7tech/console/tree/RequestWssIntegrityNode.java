package com.l7tech.console.tree;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.xmlsec.RequestWssIntegrity;

/**
 * The class represents a node element in the TreeModel.
 * It represents the XML encryption of the request node.
 *
 * @author flascell
 * @version 1.0
 */
public class RequestWssIntegrityNode extends AbstractLeafPaletteNode {
    private static final String DEFAULT_NAME = "Sign Request Element";

    public RequestWssIntegrityNode(String name) {
        super(name, "com/l7tech/console/resources/xmlencryption.gif");
    }

    public RequestWssIntegrityNode() {
        super(DEFAULT_NAME, "com/l7tech/console/resources/xmlencryption.gif");
    }

    /**
     * Return assertion representation of the node
     * or <b>null</b> if the node cannot be an assertion
     *
     * @return the assertion this node represnts
     */
    public Assertion asAssertion() {
        return new RequestWssIntegrity();
    }
}
