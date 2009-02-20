/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Mar 1, 2005<br/>
 */
package com.l7tech.security.xml;

/**
 * Enum type expressing a soap security header's actor attribute value.
 *
 * @author flascelles@layer7-tech.com
 */
public class SecurityActor {
    /** This is the original Layer 7 actor value, recognized by every XVC and Gateway version since 2.0 as being addressed to Trogdor. */
    public static final SecurityActor L7ACTOR = new SecurityActor("secure_span");

    /** This is a URI actor/role value, sent by default by version 4.6 SSBs and SSGs.  See Bug #*/
    public static final SecurityActor L7ACTOR_URI = new SecurityActor("http://www.layer7tech.com/ws/policy");

    /** This represents the default Security header with no actor or role. */
    public static final SecurityActor NOACTOR = new SecurityActor(null);

    private String attributeValue;

    /**
     * @return the value of the actor attribute for this SecurityActor instance
     *  null being valid and representing the special case where there is no
     *  actor attribute
     */
    public String getValue() {
        return attributeValue;
    }

    private SecurityActor(String attributeValue) {
        this.attributeValue = attributeValue;
    }
}
