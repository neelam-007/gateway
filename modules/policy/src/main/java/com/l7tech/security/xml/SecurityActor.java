/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Mar 1, 2005<br/>
 */
package com.l7tech.security.xml;

/**
 * Type expressing a soap security header's actor attribute value.
 *
 * @author flascelles@layer7-tech.com
 */
public enum SecurityActor {

    /**
     * An actor that explicitly identifies a header should be processed by Layer 7.
     */
    L7ACTOR,

    /**
     * No actor, or an actor that specifies processing by the "next" soap node.
     */
    NOACTOR
}
