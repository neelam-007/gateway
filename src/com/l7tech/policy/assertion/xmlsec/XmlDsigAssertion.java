/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.policy.assertion.ConfidentialityAssertion;

/**
 * The policy assertion describes the XML message signing requirements.
 * This currently works at the whole message level.
 * <p>
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 *
 * todo, remove this
 */
public class XmlDsigAssertion extends ConfidentialityAssertion // ?! Dsig does not provide confidentiality
  implements DirectionConstants {
    private int direction = IN;
    /**
     * default constructor, required by XML serializers
     */
    public XmlDsigAssertion() {
    }

    /**
     * Return the direction to which the assertion applies to,
     * IN specifies the incoming message is expected signed, OUT
     * specifies that the outgoing message expects signed message and
     * INOUT specifies that the signing is required both ways.
     *
     * @return the direction where the assertions applies
     * @see DirectionConstants
     */
    public int getDirection() {
        return direction;
    }

    /**
     * Set the signing direction value
     *
     * @param direction the new direction
     */
    public void setDirection(int direction) {
        this.direction = direction;
    }
}
