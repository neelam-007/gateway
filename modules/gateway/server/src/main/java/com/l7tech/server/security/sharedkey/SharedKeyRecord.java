package com.l7tech.server.security.sharedkey;

import org.hibernate.annotations.Proxy;

import javax.persistence.Table;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Column;
import java.io.Serializable;

/**
 * Hibernate abstraction of a row in the shared_keys table.
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: May 18, 2007<br/>
 */
@Entity
@Proxy(lazy=false)
@Table(name="shared_keys")
public class SharedKeyRecord implements Serializable {
    private String encodingID;
    private String b64edKey;

    public SharedKeyRecord() {
    }

    public SharedKeyRecord(String encodingID, String b64edKey) {
        this.encodingID = encodingID;
        this.b64edKey = b64edKey;
    }

    /**
     * A unique ID for the public key used to encrypt this shared key
     * @see com.l7tech.util.EncryptionUtil
     * @return the id
     */
    @Id
    @Column(name="encodingid", nullable=false, length=32)
    public String getEncodingID() {
        return encodingID;
    }

    /**
     * A unique ID for the public key used to encrypt this shared key
     * @param encodingID the id
     */
    public void setEncodingID(String encodingID) {
        this.encodingID = encodingID;
    }

    @Column(name="b64edval", nullable=false, length=255)
    public String getB64edKey() {
        return b64edKey;
    }

    public void setB64edKey(String b64edKey) {
        this.b64edKey = b64edKey;
    }
}
