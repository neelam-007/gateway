package com.l7tech.common.security;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;

import com.l7tech.objectmodel.imp.NamedEntityImp;
import com.l7tech.common.io.BufferPoolByteArrayOutputStream;
import com.l7tech.common.io.NonCloseableOutputStream;

/**
 * A Policy for Certificate Revocation Checking.
 *
 * @author Steve Jones
 */
public class RevocationCheckPolicy extends NamedEntityImp implements Cloneable {

    //- PUBLIC

    /**
     * Create an uninitialized instance. 
     */
    public RevocationCheckPolicy() {
        revocationCheckItems = Collections.unmodifiableList(new ArrayList());
    }

    /**
     * Is this the default policy.
     *
     * @return true if default
     */
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

    
    public List<RevocationCheckPolicyItem> getRevocationCheckItems() {
        return revocationCheckItems;
    }

    public void setRevocationCheckItems(List<RevocationCheckPolicyItem> revocationCheckItems) {
        // invalidate cached revocation policy
        revocationCheckPolicyXml = null;

        if ( revocationCheckItems == null ) {
            this.revocationCheckItems = Collections.unmodifiableList(new ArrayList());            
        } else {
            this.revocationCheckItems = Collections.unmodifiableList(new ArrayList(revocationCheckItems));
        }
    }

    /**
     * Value based equality check.
     */
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        RevocationCheckPolicy that = (RevocationCheckPolicy) o;

        if (defaultPolicy != that.defaultPolicy) return false;
        if (defaultSuccess != that.defaultSuccess) return false;
        if (!revocationCheckItems.equals(that.revocationCheckItems)) return false;

        return true;
    }

    /**
     * Value based hashcode.
     */
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + revocationCheckItems.hashCode();
        result = 31 * result + (defaultPolicy ? 1 : 0);
        result = 31 * result + (defaultSuccess ? 1 : 0);
        return result;
    }

    /**
     * Create a copy of this object.
     * 
     * @return an equal but distinct copy
     */
    public RevocationCheckPolicy clone() {
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

    protected String getRevocationCheckPolicyXml() {
        if ( revocationCheckPolicyXml == null ) {
            List<RevocationCheckPolicyItem> policyItems = this.revocationCheckItems;
            if ( policyItems == null || policyItems.isEmpty())
                return null;

            BufferPoolByteArrayOutputStream baos = new BufferPoolByteArrayOutputStream();
            try {
                XMLEncoder xe = new XMLEncoder(new NonCloseableOutputStream(baos));
                xe.writeObject(new ArrayList(policyItems));
                xe.close();
                revocationCheckPolicyXml = baos.toString(PROPERTIES_ENCODING);
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e); // Can't happen
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
            try {
                XMLDecoder xd = new XMLDecoder(new ByteArrayInputStream(xml.getBytes(PROPERTIES_ENCODING)));
                //noinspection unchecked
                this.revocationCheckItems = Collections.unmodifiableList(new ArrayList((List<RevocationCheckPolicyItem>)xd.readObject()));
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e); // Can't happen
            }
        }
    }

    //- PRIVATE

    private static final String PROPERTIES_ENCODING = "UTF-8";

    private transient String revocationCheckPolicyXml;
    private List<RevocationCheckPolicyItem> revocationCheckItems;
    private boolean defaultPolicy;
    private boolean defaultSuccess;

}
