/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.policy.assertion.ConfidentialityAssertion;

/**
 * The policy assertion describes the XML encryption requirements.
 *
 * <p>
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class XmlEncAssertion extends ConfidentialityAssertion
  implements DirectionConstants {
    private int direction = IN;
    /**
     * default constructor, required by XML serializers
     */
    public XmlEncAssertion() {
    }

    /**
     * Return the direction to which the assertion applies to,
     * IN specifies the incoming messaage requires encryption, OUT
     * specifies that the outgoing message requires encryption and
     * INOUT specifies that the encryption is required both ways.
     *
     * @return the direction where the assertions applies
     * @see DirectionConstants
     */
    public int getDirection() {
        return direction;
    }

    /**
     * Set the encryption direction value
     *
     * @param direction the new direction
     */
    public void setDirection(int direction) {
        this.direction = direction;
    }
}
