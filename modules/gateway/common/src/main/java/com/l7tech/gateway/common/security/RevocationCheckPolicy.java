package com.l7tech.gateway.common.security;

import com.l7tech.common.io.NonCloseableOutputStream;
import com.l7tech.objectmodel.imp.ZoneableNamedGoidEntityImp;
import com.l7tech.util.Charsets;
import com.l7tech.util.PoolByteArrayOutputStream;
import org.hibernate.annotations.Proxy;

import javax.persistence.*;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A Policy for Certificate Revocation Checking.
 *
 * @author Steve Jones
 */
@Entity
@Proxy(lazy=false)
@Table(name="revocation_check_policy")
public class RevocationCheckPolicy extends ZoneableNamedGoidEntityImp implements Cloneable {

    //- PUBLIC

    /**
     * Create an uninitialized instance. 
     */
    public RevocationCheckPolicy() {
        revocationCheckItems = Collections.emptyList();
    }

    /**
     * Is this the default policy.
     *
     * @return true if default
     */
    @Column(name="default_policy")
    public boolean isDefaultPolicy() {
        return defaultPolicy;
    }

    /**
     * Set as the default policy.
     *
     * @param defaultPolicy true for default
     */
    public void setDefaultPolicy(boolean defaultPolicy) {
        this.defaultPolicy = defaultPolicy;
    }

    /**
     * Should this policy evaluate successfully for unknown status?
     *
     * @return true to succeed on unknown certificate status.
     */
    @Column(name="default_success")
    public boolean isDefaultSuccess() {
        return defaultSuccess;
    }

    /**
     * Set this policy to evaluate successfully for unknown status.
     *
     * @param defaultSuccess true to succeed on unknown certificate status.
     */
    public void setDefaultSuccess(boolean defaultSuccess) {
        this.defaultSuccess = defaultSuccess;
    }

    @Column(name="continue_server_unavailable")
    public boolean isContinueOnServerUnavailable() {
        return continueOnServerUnavailable;
    }

    public void setContinueOnServerUnavailable(boolean continueOnServerUnavailable) {
        this.continueOnServerUnavailable = continueOnServerUnavailable;
    }

    @Transient
    public List<RevocationCheckPolicyItem> getRevocationCheckItems() {
        return revocationCheckItems;
    }

    public void setRevocationCheckItems(List<RevocationCheckPolicyItem> revocationCheckItems) {
        // invalidate cached revocation policy
        revocationCheckPolicyXml = null;

        if ( revocationCheckItems == null ) {
            this.revocationCheckItems = Collections.emptyList();
        } else {
            this.revocationCheckItems = Collections.unmodifiableList(new ArrayList<RevocationCheckPolicyItem>(revocationCheckItems));
        }
    }

    /**
     * Value based equality check.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        RevocationCheckPolicy that = (RevocationCheckPolicy) o;

        return defaultPolicy == that.defaultPolicy &&
                defaultSuccess == that.defaultSuccess &&
                continueOnServerUnavailable == that.continueOnServerUnavailable &&
                revocationCheckItems.equals( that.revocationCheckItems ) &&
                (securityZone != null ? securityZone.equals(that.securityZone) : that.securityZone == null);

    }

    /**
     * Value based hashcode.
     */
    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + revocationCheckItems.hashCode();
        result = 31 * result + (defaultPolicy ? 1 : 0);
        result = 31 * result + (defaultSuccess ? 1 : 0);
        result = 31 * result + (continueOnServerUnavailable ? 1 : 0);
        result = 31 * result + (securityZone != null ? securityZone.hashCode() : 0);
        return result;
    }

    /**
     * Create a copy of this object.
     * 
     * @return an equal but distinct copy
     */
    @Override
    public final RevocationCheckPolicy clone() {
        try {
            RevocationCheckPolicy cloned = (RevocationCheckPolicy) super.clone();

            // deep copy item list
            cloned.setRevocationCheckPolicyXml(getRevocationCheckPolicyXml());

            return cloned;
        }
        catch(CloneNotSupportedException cnse) {
            throw new IllegalStateException(cnse);
        }
    }

    //- PROTECTED

    @Column(name="revocation_policy_xml", length=Integer.MAX_VALUE)
    @Lob
    protected String getRevocationCheckPolicyXml() {
        if ( revocationCheckPolicyXml == null ) {
            List<RevocationCheckPolicyItem> policyItems = this.revocationCheckItems;
            if ( policyItems == null || policyItems.isEmpty())
                return null;

            PoolByteArrayOutputStream baos = new PoolByteArrayOutputStream();
            try {
                XMLEncoder xe = new XMLEncoder(new NonCloseableOutputStream(baos));
                xe.writeObject(new ArrayList<RevocationCheckPolicyItem>(policyItems));
                xe.close();
                revocationCheckPolicyXml = baos.toString(PROPERTIES_ENCODING);
            } finally {
                baos.close();
            }
        }
        return revocationCheckPolicyXml;
    }

    protected void setRevocationCheckPolicyXml(String xml) {
        if (xml != null && xml.equals(revocationCheckPolicyXml))
            return;

        this.revocationCheckPolicyXml = xml;

        if ( xml != null && xml.length() > 0 ) {
            XMLDecoder xd = new XMLDecoder(new ByteArrayInputStream(xml.getBytes(PROPERTIES_ENCODING)));
            //noinspection unchecked
            this.revocationCheckItems = Collections.unmodifiableList(new ArrayList((List<RevocationCheckPolicyItem>)xd.readObject()));
        }
    }

    //- PRIVATE

    private static final Charset PROPERTIES_ENCODING = Charsets.UTF8;

    private transient String revocationCheckPolicyXml;
    private List<RevocationCheckPolicyItem> revocationCheckItems;
    private boolean defaultPolicy;
    private boolean defaultSuccess;
    private boolean continueOnServerUnavailable;  

}
