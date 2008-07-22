package com.l7tech.console.tree;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.credential.XpathCredentialSource;


/**
 * The class represents the WS-Trust Credential Exchange node element in the
 * assertion palette.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class XpathCredentialSourcePaletteNode extends AbstractLeafPaletteNode {
    public XpathCredentialSourcePaletteNode(){
        super("XPath Credentials", "com/l7tech/console/resources/xmlWithCert16.gif");
    }

    /**
     * Return assertion representation of the node
     * or <b>null</b> if the node cannot be an assertion
     *
     * @return the assertion this node represnts
     */
    public Assertion asAssertion() {
        return new XpathCredentialSource();
    }
}