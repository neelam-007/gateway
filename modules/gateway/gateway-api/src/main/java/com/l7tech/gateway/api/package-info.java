/**
 * This package contains the Layer 7 SecureSpan Gateway Management API.
 *
 * <p>The {@link com.l7tech.gateway.api.ClientFactory ClientFactory} class is used to create instances of the
 * management client. The {@link com.l7tech.gateway.api.Client Client} provides access to managed objects on
 * a SecureSpan Gateway via {@link com.l7tech.gateway.api.Accessor Accessor}'s.</p>
 *
 * <p>Some types of managed object support additional operations using
 * {@link com.l7tech.gateway.api.Accessor Accessor} subclasses.</p>
 *
 * <p>The {@link com.l7tech.gateway.api.ManagedObjectFactory ManagedObjectFactory} class is used to create objects
 * for use with {@link com.l7tech.gateway.api.Accessor Accessor}s and for persistence of managed objects.</p>
 *
 * @see com.l7tech.gateway.api.ClientFactory#createClient(String)
 * @see com.l7tech.gateway.api.Client#getAccessor(Class)
 * @see com.l7tech.gateway.api.ManagedObjectFactory
 */
@XmlSchema(
        namespace = "http://ns.l7tech.com/2010/04/gateway-management",
        elementFormDefault = XmlNsForm.QUALIFIED,
        xmlns = { @XmlNs(prefix = "l7", namespaceURI="http://ns.l7tech.com/2010/04/gateway-management") }
)
@XmlAccessorType(value=XmlAccessType.NONE)
package com.l7tech.gateway.api;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlNs;
import javax.xml.bind.annotation.XmlNsForm;
import javax.xml.bind.annotation.XmlSchema;
