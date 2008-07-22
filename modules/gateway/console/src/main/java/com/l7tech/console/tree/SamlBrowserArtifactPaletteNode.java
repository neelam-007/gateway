package com.l7tech.console.tree;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.xmlsec.SamlBrowserArtifact;


/**
 * The class represents the SAML Browser Artifact node element in the
 * assertion palette.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class SamlBrowserArtifactPaletteNode extends AbstractLeafPaletteNode {
    public SamlBrowserArtifactPaletteNode(){
        super("SAML Browser Artifact", "com/l7tech/console/resources/xmlWithCert16.gif");
    }

    /**
     * Return assertion representation of the node
     * or <b>null</b> if the node cannot be an assertion
     *
     * @return the assertion this node represnts
     */
    public Assertion asAssertion() {
        return new SamlBrowserArtifact();
    }
}