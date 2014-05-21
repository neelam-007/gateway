package com.l7tech.gateway.api.impl;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import static com.l7tech.gateway.api.impl.AttributeExtensibleType.get;
import static com.l7tech.gateway.api.impl.AttributeExtensibleType.set;

@XmlRootElement(name="PrivateKeySignCsrResult")
@XmlType(name="PrivateKeySignCertResultType", propOrder={"certDataValue"})
public class PrivateKeySignCsrResult {

    public String getCertData() {
        return get(certData);
    }

    public void setCertData( final String certData ) {
        this.certData = set(this.certData, certData);
    }

    //- PROTECTED

    @XmlElement(name="CertData", required=true)
    protected AttributeExtensibleType.AttributeExtensibleString getCertDataValue() {
        return certData;
    }

    protected void setCertDataValue( final AttributeExtensibleType.AttributeExtensibleString csrData ) {
        this.certData = csrData;
    }

    //- PRIVATE

    private AttributeExtensibleType.AttributeExtensibleString certData;
}
