/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Mar 1, 2005<br/>
 */
package com.l7tech.common.security.xml;

/**
 * Enum type expressing a soap security header's actor attribute value.
 *
 * @author flascelles@layer7-tech.com
 */
public class SecurityActor {
    public static final SecurityActor L7ACTOR = new SecurityActor("l7");
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

    protected SecurityActor(String attributeValue) {
        this.attributeValue = attributeValue;
    }
}
