package com.l7tech.common.xml;

import org.w3c.dom.Element;

/**
 * Some error or exception that can be translated into a SOAP fault.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Aug 16, 2004<br/>
 * $Id$
 */
public interface SoapFaultDetail {
    /** @return the short fault code.  Will not be null or empty. */
    String getFaultCode();

    /** @return the longer fault string.  Will not be null, but may be empty. */
    String getFaultString();

    /** @return the soap fault details element.  May be null. */
    Element getFaultDetail();

    /** @return the fault actor to use, or null to indicate that caller should provide one. */
    String getFaultActor();

    /**
     * Get the fault actor, with default.  If the existing actor is null this will set it to
     * the default.
     *
     * @param defaultActor the default actor.  May not be null or empty.
     * @return the actor to use, or the specified default.  Never null or empty.
     */
    String getFaultActor(String defaultActor);

    /**
     * Set the fault actor.  This is the only mutator since only the fault actor may not be known at the time
     * the SoapFaultDetail is generated (perhaps deep inside a common library).
     *
     * @param faultActor the new fault actor to use.  May be null or empty, in which case the end user of this
     *                   SoapFaultDetail will need to provide their own.
     */
    void setFaultActor(String faultActor);
}
