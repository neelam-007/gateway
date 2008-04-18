package com.l7tech.identity;

import com.l7tech.common.io.BufferPoolByteArrayOutputStream;
import com.l7tech.common.io.NonCloseableOutputStream;
import com.l7tech.common.security.CertificateValidationType;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.ResourceUtils;
import com.l7tech.objectmodel.imp.NamedEntityImp;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration of an Identity Provider object.
 *
 * The properties depend on the type of identity provider and are defined in the
 * IdentityProvider type implementations.
 *
 * <br/><br/>
 * Layer 7 Technologies, inc.<br/>
 * User: flascelles<br/>
 * Date: Jun 23, 2003
 *
 */
@XmlRootElement
public class IdentityProviderConfig extends NamedEntityImp {

    public IdentityProviderConfig(IdentityProviderType type) {
        super();
        this.type = type;
    }

    public IdentityProviderConfig() {
        super();
        // only default value which makes sense for now.
        type = IdentityProviderType.LDAP;
    }

    /**
     * the type is set in constructor
     */
    public IdentityProviderType type() {return type;}

    public boolean isWritable() {
        return true; // Internal is writable and there's no InternalIdentityProviderConfig
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Object getProperty(String name) {
        return props.get(name);
    }

    protected void setProperty(String name, Object value) {
        props.put(name, value);
    }

    /**
     * for serialization by axis and hibernate only.
     * to get the properties, call getProperty
     */
    public String getSerializedProps() throws java.io.IOException {
        if (propsXml == null) {
            // if no props, return empty string
            if (props.size() < 1) {
                propsXml = "";
            } else {
                BufferPoolByteArrayOutputStream output = null;
                java.beans.XMLEncoder encoder = null;
                try {
                    output = new BufferPoolByteArrayOutputStream();
                    encoder = new java.beans.XMLEncoder(new NonCloseableOutputStream(output));
                    encoder.writeObject(props);
                    encoder.close(); // writes closing XML tag
                    encoder = null;
                    propsXml = output.toString("UTF-8");
                }
                finally {
                    if(encoder!=null) encoder.close();
                    ResourceUtils.closeQuietly(output);
                }
            }
        }

        return propsXml;
    }

    /*For JAXB processing. Needed a way to get this identity provider to recreate it's internal
    * propsXml after setting a property after it has been unmarshalled*/
    public void recreateSerializedProps() throws java.io.IOException{
        propsXml = null;
        this.getSerializedProps();
        return;
    }

    /**
     * for serialization by axis and hibernate only.
     * to set the properties, call setProperty
     */
    public void setSerializedProps(String serializedProps) {
        propsXml = serializedProps;
        if (serializedProps == null || serializedProps.length() < 2) {
            props.clear();
        } else {
            ByteArrayInputStream in = new ByteArrayInputStream(HexUtils.encodeUtf8(serializedProps));
            java.beans.XMLDecoder decoder = new java.beans.XMLDecoder(in);
            //noinspection unchecked
            props = (Map<String, Object>) decoder.readObject();
        }
    }

    /**
     * for serialization by axis and hibernate only.
     */
    public int getTypeVal() {
        return type.toVal();
    }

    /**
     * for serialization by axis and hibernate only.
     */
    public void setTypeVal(int val) {
        type = IdentityProviderType.fromVal(val);
    }

    public boolean isAdminEnabled() {
        Boolean b = (Boolean) getProperty(ADMIN_ENABLED);
        return b != null && b.booleanValue();
    }


    public void setAdminEnabled(boolean adminEnabled) {
        setProperty(ADMIN_ENABLED, adminEnabled);
    }

    public CertificateValidationType getCertificateValidationType() {
        return (CertificateValidationType) props.get(PROP_CERTIFICATE_VALIDATION_TYPE);
    }

    public void setCertificateValidationType(CertificateValidationType validationType) {
        if ( validationType == null ) {
            props.remove(PROP_CERTIFICATE_VALIDATION_TYPE);
        } else {
            props.put(PROP_CERTIFICATE_VALIDATION_TYPE, validationType);
        }
    }

    /**
     * allows to set all properties from another object
     */
    public void copyFrom(IdentityProviderConfig objToCopy) {
        setDescription(objToCopy.getDescription());
        setName(objToCopy.getName());
        type = objToCopy.type();
        props = objToCopy.props;
        propsXml = null;
    }

    protected boolean getBooleanProperty(String prop, boolean dflt) {
        Boolean val = (Boolean)props.get(prop);
        return val == null ? dflt : val.booleanValue();
    }

    // ************************************************
    // PRIVATES
    // ************************************************

    private static final String ADMIN_ENABLED = "adminEnabled";
    private static final String PROP_CERTIFICATE_VALIDATION_TYPE = "certificateValidationType";

    protected String description;
    private String propsXml;
    protected IdentityProviderType type;
    private Map<String, Object> props = new HashMap<String, Object>();
}
