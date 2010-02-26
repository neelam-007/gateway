/**
 * This package contains the Layer 7 SecureSpan Gateway Management API.
 *
 * <p>The {@link ClientFactory} class is used to create instances of the
 * management client. The {@link Client} provides access to managed objects on
 * a SecureSpan Gateway via {@link Accessor}'s.</p>
 *
 * <p>Some types of managed object support additional operations using
 * {@link Accessor} subclasses.</p>
 *
 * <p>The {@link ManagedObjectFactory} class is used to create objects
 * for use with {@link Accessor}s and for persistence of managed objects.</p>
 *
 * @see ClientFactory#createClient(String)
 * @see Client#getAccessor(Class)
 * @see ManagedObjectFactory
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
