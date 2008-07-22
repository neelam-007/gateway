package com.l7tech.console.tree;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.WsiSamlAssertion;

/**
 * Tree node in the assertion palette corresponding to the WsiSaml assertion type.
 *
 * @author $Author$
 * @version $Revision$
 */
public class WsiSamlPaletteNode extends AbstractLeafPaletteNode {

    public WsiSamlPaletteNode() {
        super("WSI-SAML Compliance", "com/l7tech/console/resources/policy16.gif");
    }

    public Assertion asAssertion() {
        return new WsiSamlAssertion();
    }
}
