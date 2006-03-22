package com.l7tech.identity;

import com.l7tech.common.io.BufferPoolByteArrayOutputStream;
import com.l7tech.common.io.NonCloseableOutputStream;
import com.l7tech.common.util.ResourceUtils;
import com.l7tech.objectmodel.imp.NamedEntityImp;

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
                    propsXml = output.toString();
                }
                finally {
                    if(encoder!=null) encoder.close();
                    ResourceUtils.closeQuietly(output);
                }
            }
        }

        return propsXml;
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
            ByteArrayInputStream in = new ByteArrayInputStream(serializedProps.getBytes());
            java.beans.XMLDecoder decoder = new java.beans.XMLDecoder(in);
            props = (Map)decoder.readObject();
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
    protected String description;
    protected IdentityProviderType type;
    private Map props = new HashMap();
    private String propsXml;
}
