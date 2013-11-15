/**
 * This package is here to set the namespaces used by the rest-only resources
 */
@XmlSchema(
        namespace = "http://ns.l7tech.com/2010/04/gateway-management",
        elementFormDefault = XmlNsForm.QUALIFIED,
        xmlns = {
                @XmlNs(prefix = "l7", namespaceURI="http://ns.l7tech.com/2010/04/gateway-management"),
                @XmlNs(prefix = "xlink", namespaceURI="http://www.w3.org/1999/xlink")}
)
@XmlAccessorType(value=XmlAccessType.NONE)
package com.l7tech.external.assertions.gatewaymanagement.server.rest.entities;

import javax.xml.bind.annotation.*;
