package com.l7tech.console.tree;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.xml.SchemaValidation;

/**
 * The tree node in the assertion palette corresponding to the Schema Validation Assertion.
 *
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Feb 6, 2004<br/>
 * $Id$<br/>
 *
 */
public class SchemaValidationPaletteNode extends AbstractLeafPaletteNode {
    public SchemaValidationPaletteNode() {
        super("Validate XML Schema", "com/l7tech/console/resources/xmlsignature.gif");
    }

    /**
     * Return assertion representation of the node
     * or <b>null</b> if the node cannot be an assertion
     *
     * @return the popup menu
     */
    public Assertion asAssertion() {
        return new SchemaValidation();
    }
}
