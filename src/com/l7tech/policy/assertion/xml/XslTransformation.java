package com.l7tech.policy.assertion.xml;

import com.l7tech.policy.assertion.Assertion;

/**
 * An assertion for XSLT transformations of soap messages.
 *
 * XSL transformation can be applied either to request messages or response messages.
 * The actual xsl containing the transformation is held by the assertion and the source of
 * the transformation is the soap message. The output of the transformation replaces the source
 * message once the transformation is complete.
 *
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Feb 10, 2004<br/>
 * $Id$<br/>
 *
 */
public class XslTransformation extends Assertion {

    /**
     * the actual transformation xsl
     */
    public String getXslSrc() {
        return xslSrc;
    }

    /**
     * the actual transformation xsl
     */
    public void setXslSrc(String xslSrc) {
        this.xslSrc = xslSrc;
    }

    /**
     * Whether this assertion applies to requests or responses soap messages.
     * Typed as an int for serialization purposes.
     * @return APPLY_TO_REQUEST OR APPLY_TO_RESPONSE
     */
    public int getDirection() {
        return direction;
    }

    /**
     * Whether this assertion applies to requests or responses soap messages.
     * Typed as an int for serialization purposes.
     * @param direction APPLY_TO_REQUEST OR APPLY_TO_RESPONSE
     */
    public void setDirection(int direction) {
        this.direction = direction;
    }

    public String getTransformName() {
        return transformName;
    }

    public void setTransformName(String name) {
        transformName = name;
    }

    private int direction;
    private String xslSrc;
    private String transformName;
    public static final int APPLY_TO_REQUEST = 1;
    public static final int APPLY_TO_RESPONSE = 2;
}
