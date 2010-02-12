/**
 * TODO [steve] javadoc for package
 */
@XmlSchema(
        namespace = "http://ns.l7tech.com/2010/01/gateway-management",
        elementFormDefault = XmlNsForm.QUALIFIED,
        xmlns = { @XmlNs(prefix = "l7", namespaceURI="http://ns.l7tech.com/2010/01/gateway-management") }
)
@XmlAccessorType(value=XmlAccessType.PROPERTY)
package com.l7tech.gateway.api;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlNs;
import javax.xml.bind.annotation.XmlNsForm;
import javax.xml.bind.annotation.XmlSchema;
