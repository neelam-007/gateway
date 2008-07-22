package com.l7tech.console.tree;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.xmlsec.RequestWssSaml;


/**
 * The class represents the SAML WSS constraint node element in the
 * assertion palette.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class RequestWssSamlNode extends AbstractLeafPaletteNode {
    public RequestWssSamlNode(){
        super("SAML Assertion", "com/l7tech/console/resources/xmlWithCert16.gif");
    }

    /**
     * Return assertion representation of the node
     * The default is the <code>RequestWssSaml</code>
     * configured as holder of key.
     *
     * @return the assertion this node represents
     */
    public Assertion asAssertion() {
        return RequestWssSaml.newHolderOfKey();
    }
}