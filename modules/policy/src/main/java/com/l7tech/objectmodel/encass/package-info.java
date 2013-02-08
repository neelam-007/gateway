@XmlSchema(
        namespace = "http://ns.l7tech.com/secureSpan/1.0/encass",
        elementFormDefault = XmlNsForm.QUALIFIED,
        xmlns = {@XmlNs(prefix = "enc", namespaceURI = "http://ns.l7tech.com/secureSpan/1.0/encass"),
                @XmlNs(prefix = "L7", namespaceURI = "http://ns.l7tech.com/secureSpan/1.0/core"),
                @XmlNs(prefix = "xsi", namespaceURI = "http://www.w3.org/2001/XMLSchema-instance"),
                @XmlNs(prefix = "xs", namespaceURI = "http://www.w3.org/2001/XMLSchema")}
)
@XmlAccessorType(value = XmlAccessType.NONE) package com.l7tech.objectmodel.encass;

import javax.xml.bind.annotation.*;
