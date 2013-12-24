package com.l7tech.gateway.api;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Private Key export.
 *
 * <p>The result of a key export.</p>
 */
@XmlRootElement(name="PrivateKeyExport")
@XmlType(name = "PrivateKeyExportType", propOrder={"alias","keystoreID","pkcs12Data","password"})
public class PrivateKeyRestExport {

    PrivateKeyRestExport(){}

    //- PUBLIC

    @XmlElement(name = "alias", required = true)
    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    @XmlElement(name = "keystoreID", required = true)
    public String getKeystoreID() {
        return keystoreID;
    }

    public void setKeystoreID(String keystoreID) {
        this.keystoreID = keystoreID;
    }

    @XmlElement(name = "pkcs12Data", required = true)
    public byte[] getPkcs12Data() {
        return pkcs12Data;
    }

    public void setPkcs12Data(byte[] pkcs12Data) {
        this.pkcs12Data = pkcs12Data;
    }

    @XmlElement(name = "password", required = true)
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    //- PRIVATE

    private byte[] pkcs12Data;
    private String alias;
    private String keystoreID;
    private String password;
}
