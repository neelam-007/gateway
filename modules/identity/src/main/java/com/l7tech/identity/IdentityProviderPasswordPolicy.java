package com.l7tech.identity;

import com.l7tech.common.io.NonCloseableOutputStream;
import com.l7tech.objectmodel.imp.PersistentEntityImp;
import com.l7tech.util.Charsets;
import com.l7tech.util.HexUtils;
import com.l7tech.util.PoolByteArrayOutputStream;
import com.l7tech.util.ResourceUtils;
import org.hibernate.annotations.Proxy;

import javax.persistence.*;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.ByteArrayInputStream;
import java.text.MessageFormat;
import java.util.Map;
import java.util.TreeMap;

/**
 * This entity stores Identity Provider password policy
 *
 * @author wlui
 */

@XmlRootElement
@Entity
@Proxy(lazy=false)
@Table(name="password_policy")
public class IdentityProviderPasswordPolicy extends PersistentEntityImp {
    private String propertiesXml;
    private Map<String, Object> properties = new TreeMap<String, Object>();
    private long internalIdentityProviderOid;

    public static String FORCE_PWD_CHANGE = "forcePasswordChangeNewUser";
    public static String NO_REPEAT_CHARS = "noRepeatingCharacters";
    public static String MIN_PASSWORD_LENGTH = "minPasswordLength";
    public static String MAX_PASSWORD_LENGTH = "maxPasswordLength";
    public static String UPPER_MIN = "upperMinimum";
    public static String LOWER_MIN = "lowerMinimum";
    public static String NUMBER_MIN = "numberMinimum";
    public static String SYMBOL_MIN = "symbolMinimum";
    public static String NON_NUMERIC_MIN = "nonNumericMinimum";
    public static String CHARACTER_DIFF_MIN = "charDiffMinimum";
    public static String REPEAT_FREQUENCY = "repeatFrequency";
    public static String PASSWORD_EXPIRY = "passwordExpiry";
    public static String ALLOWABLE_CHANGES = "allowableChangesPerDay";

    public IdentityProviderPasswordPolicy() {
    }

    public IdentityProviderPasswordPolicy(long oid) {
        setOid(oid);
    }

    @Column(name="internal_identity_provider_oid")
    public Long getInternalIdentityProviderOid() {
        return internalIdentityProviderOid;
    }

    public void setInternalIdentityProviderOid(Long oid) {
        this.internalIdentityProviderOid = oid;
    }

    @Override
    @Version
    @Column(name = "version")
    public int getVersion() {
        return super.getVersion();
    }
    
    /**
     * for serialization by axis and hibernate only.
     * to get the properties, call getProperty
     */
    @Deprecated
    @Column(name="properties", length=Integer.MAX_VALUE)
    @Lob
    public String getSerializedProps() throws java.io.IOException {
        if (propertiesXml == null) {
            // if no additionalProps, return empty string
            if (properties.size() < 1) {
                propertiesXml = "";
            } else {
                PoolByteArrayOutputStream output = null;
                java.beans.XMLEncoder encoder = null;
                try {
                    output = new PoolByteArrayOutputStream();
                    encoder = new java.beans.XMLEncoder(new NonCloseableOutputStream(output));
                    encoder.writeObject(properties);
                    encoder.close(); // writes closing XML tag
                    encoder = null;
                    propertiesXml = output.toString(Charsets.UTF8);
                }
                finally {
                    if(encoder!=null) encoder.close();
                    ResourceUtils.closeQuietly(output);
                }
            }
        }

        return propertiesXml;
    }

    /**
     * for serialization by axis and hibernate only.
     * to set the properties, call setProperty
     */
    public void setSerializedProps(String serializedProps) {
        propertiesXml = serializedProps;
        if (serializedProps == null || serializedProps.length() < 2) {
            properties.clear();
        } else {
            ByteArrayInputStream in = new ByteArrayInputStream(HexUtils.encodeUtf8(serializedProps));
            java.beans.XMLDecoder decoder = new java.beans.XMLDecoder(in);
            //noinspection unchecked
            properties = (Map<String, Object>) decoder.readObject();
        }
    }
    @Transient
    public Object getPropertyValue(String propName) {
        return properties.get(propName);
    }

    @Transient
    public Boolean getBooleanProperty( String prop){
        Object val = properties.get(prop);
        return val instanceof Boolean ? (Boolean) val : false ;
    }

    @Transient
    public Integer getIntegerProperty( String prop){
        Object val = properties.get(prop);
        return val instanceof Integer ? (Integer) val : -1 ;
    }


    public void setProperty(String name, Object value) {
        if ( value == null ) {
            properties.remove( name );
        } else {
            properties.put(name, value);
        }
        propertiesXml = null;
    }

    @Transient
    public Map<String, Object> getProperties() {
        return properties;
    }

    @Transient
    public String getDescription(){

        StringBuilder desc = new StringBuilder();
        desc.append("<html><body>Password changes must comply all of the following:<ul>");
        if (getIntegerProperty(MIN_PASSWORD_LENGTH)>0){desc.append("<li>"); desc.append(MessageFormat.format("at least {0} characters",getIntegerProperty(MIN_PASSWORD_LENGTH))); desc.append("</li>");}
        if (getIntegerProperty(MAX_PASSWORD_LENGTH)>0){desc.append("<li>"); desc.append(MessageFormat.format("less than {0} characters",getIntegerProperty(MAX_PASSWORD_LENGTH))); desc.append("</li>");}
        if (getIntegerProperty(UPPER_MIN)>0){desc.append("<li>"); desc.append(MessageFormat.format("at least {0} upper case characters",getIntegerProperty(UPPER_MIN))); desc.append("</li>");}
        if (getIntegerProperty(LOWER_MIN)>0){desc.append("<li>"); desc.append(MessageFormat.format("at least {0} lower case characters",getIntegerProperty(LOWER_MIN))); desc.append("</li>");}
        if (getIntegerProperty(NUMBER_MIN)>0){desc.append("<li>"); desc.append(MessageFormat.format("at least {0} numbers",getIntegerProperty(NUMBER_MIN))); desc.append("</li>");}
        if (getIntegerProperty(SYMBOL_MIN)>0){desc.append("<li>"); desc.append(MessageFormat.format("at least {0} special characters",getIntegerProperty(SYMBOL_MIN))); desc.append("</li>");}
        if (getIntegerProperty(NON_NUMERIC_MIN)>0){desc.append("<li>"); desc.append(MessageFormat.format("at least {0} non-numeric characters",getIntegerProperty(NON_NUMERIC_MIN))); desc.append("</li>");}
        if (getIntegerProperty(CHARACTER_DIFF_MIN)>0){desc.append("<li>"); desc.append(MessageFormat.format("at least {0} unique characters",getIntegerProperty(CHARACTER_DIFF_MIN))); desc.append("</li>");}
        if (getBooleanProperty(NO_REPEAT_CHARS)){desc.append("<li>"); desc.append("no consecutive repeating characters"); desc.append("</li>");}
        if (getIntegerProperty(REPEAT_FREQUENCY)>0){desc.append("<li>"); desc.append(MessageFormat.format("passwords are not reused within {0} password changes",getIntegerProperty(REPEAT_FREQUENCY))); desc.append("</li>");}
        if (getBooleanProperty(ALLOWABLE_CHANGES)){desc.append("<li>"); desc.append("allow one password change per 24 hours"); desc.append("</li>");}

        desc.append("</ul> </body></html>");
        return desc.toString();
    }
}

// STIG defaults:
//<?xml version="1.0" encoding="UTF-8"?><java version="1.6.0_21" class="java.beans.XMLDecoder"> <object class="java.util.TreeMap">  <void method="put">   <string>allowableChangesPerDay</string>   <boolean>true</boolean>  </void>  <void method="put">   <string>charDiffMinimum</string>   <int>4</int>  </void>  <void method="put">   <string>forcePasswordChangeNewUser</string>   <boolean>true</boolean>  </void>  <void method="put">   <string>lowerMinimum</string>   <int>1</int>  </void>  <void method="put">   <string>maxPasswordLength</string>   <int>32</int>  </void>  <void method="put">   <string>minPasswordLength</string>   <int>8</int>  </void>  <void method="put">   <string>noRepeatingCharacters</string>   <boolean>true</boolean>  </void>  <void method="put">   <string>numberMinimum</string>   <int>1</int>  </void>  <void method="put">   <string>passwordExpiry</string>   <int>90</int>  </void>  <void method="put">   <string>repeatFrequency</string>   <int>10</int>  </void>  <void method="put">   <string>symbolMinimum</string>   <int>1</int>  </void>  <void method="put">   <string>upperMinimum</string>   <int>1</int>  </void> </object></java>