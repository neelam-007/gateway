package com.l7tech.console.tree;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.xml.XslTransformation;

/**
 * The tree node in the assertion palette corresponding to the XSL Transformation Assertion.
 *
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Feb 11, 2004<br/>
 * $Id$<br/>
 *
 */
public class XslTransformationPaletteNode extends AbstractLeafPaletteNode {
    public XslTransformationPaletteNode() {
        super("XSL Transformation", "com/l7tech/console/resources/xmlsignature.gif");
    }

    /**
     * Return assertion representation of the node
     * or <b>null</b> if the node cannot be an assertion
     *
     * @return the popup menu
     */
    public Assertion asAssertion() {
        return new XslTransformation();
    }
}
