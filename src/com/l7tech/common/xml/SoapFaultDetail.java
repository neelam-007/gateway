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
    String getFaultCode();
    String getFaultString();
    Element getFaultDetails();
}
