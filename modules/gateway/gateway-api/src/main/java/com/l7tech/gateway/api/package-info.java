/**
 * This package contains the Layer 7 SecureSpan Gateway Management API.
 *
 * <p>The {@code ClientFactory} class is used to create instances of the
 * management client. The {@code Client} provides access to resources on a
 * SecureSpan Gateway via {@code Accessor}'s.</p>
 *
 * <p>Some resources support additional operations using {@code Accessor} 
 * subclasses.</p>
 *
 * <p>The {@code ManagedObjectFactory} class is used to create resource objects
 * for use with {@code Accessor}s.</p>
 *
 * @see ClientFactory#createClient
 * @see Client#getAccessor
 * @see ManagedObjectFactory
 *
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
