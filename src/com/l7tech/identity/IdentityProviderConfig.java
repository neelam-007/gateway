package com.l7tech.identity;

import com.l7tech.objectmodel.imp.NamedEntityImp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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
        props = new HashMap();
        this.type = type;
    }

    public IdentityProviderConfig() {
        super();
        props = new HashMap();
        // only default value which makes sense for now.
        type = IdentityProviderType.LDAP;
    }

    /**
     * the type is set in constructor
     */
    public IdentityProviderType type() {return type;}

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void putProperty(String name, String value) {
        props.put(name, value);
    }

    public String getProperty(String name) {
        return (String)props.get(name);
    }

    /**
     * for serialization by axis and hibernate only.
     * to get the properties, call getProperty
     */
    public String getSerializedProps() throws java.io.IOException {
        // if no props, return empty string
        Set keys = props.keySet();
        if (keys.size() < 1) return "";
        
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        java.beans.XMLEncoder encoder = new java.beans.XMLEncoder(output);
        encoder.writeObject(props);
        encoder.close();
        output.close();
        return output.toString();
    }

    /**
     * for serialization by axis and hibernate only.
     * to set the properties, call setProperty
     */
    public void setSerializedProps(String serializedProps) {
        if (serializedProps == null || serializedProps.length() < 2) props = new HashMap();
        else
        {
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
    protected Map props;
}
