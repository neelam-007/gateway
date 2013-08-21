package com.l7tech.identity;

import com.l7tech.common.io.NonCloseableOutputStream;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.imp.PersistentEntityImp;
import com.l7tech.util.Charsets;
import com.l7tech.util.HexUtils;
import com.l7tech.util.PoolByteArrayOutputStream;
import com.l7tech.util.ResourceUtils;
import org.hibernate.annotations.Proxy;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.ByteArrayInputStream;
import java.text.MessageFormat;
import java.util.Map;
import java.util.TreeMap;

//TODO change to use NamedEntityWithPropertiesImp
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
    private Goid internalIdentityProviderGoid;

    public static final String FORCE_PWD_CHANGE = "forcePasswordChangeNewUser";
    public static final String NO_REPEAT_CHARS = "noRepeatingCharacters";
    public static final String MIN_PASSWORD_LENGTH = "minPasswordLength";
    public static final String MAX_PASSWORD_LENGTH = "maxPasswordLength";
    public static final String UPPER_MIN = "upperMinimum";
    public static final String LOWER_MIN = "lowerMinimum";
    public static final String NUMBER_MIN = "numberMinimum";
    public static final String SYMBOL_MIN = "symbolMinimum";
    public static final String NON_NUMERIC_MIN = "nonNumericMinimum";
    public static final String CHARACTER_DIFF_MIN = "charDiffMinimum";
    public static final String REPEAT_FREQUENCY = "repeatFrequency";
    public static final String PASSWORD_EXPIRY = "passwordExpiry";
    public static final String ALLOWABLE_CHANGES = "allowableChangesPerDay";

    public IdentityProviderPasswordPolicy() {
    }

    public IdentityProviderPasswordPolicy(Goid goid) {
        setGoid(goid);
    }

    public IdentityProviderPasswordPolicy( final boolean forcePasswordChange,
                                           final boolean noRepeatingCharacters,
                                           final int minPasswordLength,
                                           final int maxPasswordLength,
                                           final int minUpperCharacters,
                                           final int minLowerCharacters,
                                           final int minNumberCharacters,
                                           final int minSymbolCharacters,
                                           final int minNonNumberCharacters,
                                           final int minCharacterDifference,
                                           final int passwordRepeatFrequency,
                                           final int passwordExpiryDays,
                                           final boolean passwordChangeDaily ) {
        if (forcePasswordChange) setProperty( FORCE_PWD_CHANGE, forcePasswordChange );
        if (noRepeatingCharacters) setProperty( NO_REPEAT_CHARS, noRepeatingCharacters );
        if (minPasswordLength>0) setProperty( MIN_PASSWORD_LENGTH, minPasswordLength );
        if (maxPasswordLength>0) setProperty( MAX_PASSWORD_LENGTH, maxPasswordLength );
        if (minUpperCharacters>0) setProperty( UPPER_MIN, minUpperCharacters );
        if (minLowerCharacters>0) setProperty( LOWER_MIN, minLowerCharacters );
        if (minNumberCharacters>0) setProperty( NUMBER_MIN, minNumberCharacters );
        if (minSymbolCharacters>0) setProperty( SYMBOL_MIN, minSymbolCharacters );
        if (minNonNumberCharacters>0) setProperty( NON_NUMERIC_MIN, minNonNumberCharacters );
        if (minCharacterDifference>0) setProperty( CHARACTER_DIFF_MIN, minCharacterDifference );
        if (passwordRepeatFrequency>0) setProperty( REPEAT_FREQUENCY, passwordRepeatFrequency );
        if (passwordExpiryDays>0) setProperty( PASSWORD_EXPIRY, passwordExpiryDays );
        if (passwordChangeDaily) setProperty( ALLOWABLE_CHANGES, passwordChangeDaily );
    }

    @Column(name="internal_identity_provider_goid")
    @Type(type = "com.l7tech.server.util.GoidType")
    public Goid getInternalIdentityProviderGoid() {
        return internalIdentityProviderGoid;
    }

    public void setInternalIdentityProviderGoid(Goid oid) {
        this.internalIdentityProviderGoid = oid;
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
        desc.append("<html><body>The password must meet all of the following rules:<ul>");
        if (getIntegerProperty(MIN_PASSWORD_LENGTH)>0){desc.append("<li>"); desc.append(MessageFormat.format("at least {0} characters",getIntegerProperty(MIN_PASSWORD_LENGTH))); desc.append("</li>");}
        if (getIntegerProperty(MAX_PASSWORD_LENGTH)>0){desc.append("<li>"); desc.append(MessageFormat.format("less than {0} characters",getIntegerProperty(MAX_PASSWORD_LENGTH))); desc.append("</li>");}
        if (getIntegerProperty(UPPER_MIN)>0){desc.append("<li>"); desc.append(MessageFormat.format("at least {0} uppercase character(s)",getIntegerProperty(UPPER_MIN))); desc.append("</li>");}
        if (getIntegerProperty(LOWER_MIN)>0){desc.append("<li>"); desc.append(MessageFormat.format("at least {0} lowercase character(s)",getIntegerProperty(LOWER_MIN))); desc.append("</li>");}
        if (getIntegerProperty(NUMBER_MIN)>0){desc.append("<li>"); desc.append(MessageFormat.format("at least {0} number(s)",getIntegerProperty(NUMBER_MIN))); desc.append("</li>");}
        if (getIntegerProperty(SYMBOL_MIN)>0){desc.append("<li>"); desc.append(MessageFormat.format("at least {0} special character(s)",getIntegerProperty(SYMBOL_MIN))); desc.append("</li>");}
        if (getIntegerProperty(NON_NUMERIC_MIN)>0){desc.append("<li>"); desc.append(MessageFormat.format("at least {0} non-numeric character(s",getIntegerProperty(NON_NUMERIC_MIN))); desc.append("</li>");}
        if (getIntegerProperty(CHARACTER_DIFF_MIN)>0){desc.append("<li>"); desc.append(MessageFormat.format("if changing passwords, new password must differ from old password by {0} character(s)",getIntegerProperty(CHARACTER_DIFF_MIN))); desc.append("</li>");}
        if (getBooleanProperty(NO_REPEAT_CHARS)){desc.append("<li>"); desc.append("no consecutive repeating characters"); desc.append("</li>");}
        if (getIntegerProperty(REPEAT_FREQUENCY)>0){desc.append("<li>"); desc.append(MessageFormat.format("if changing passwords, an old password cannot be reused within {0} password change(s)",getIntegerProperty(REPEAT_FREQUENCY))); desc.append("</li>");}
        if (getBooleanProperty(ALLOWABLE_CHANGES)){desc.append("<li>"); desc.append("if changing passwords, only one password change is permitted per 24 hours"); desc.append("</li>");}

        desc.append("</ul> </body></html>");
        return desc.toString();
    }

    /**
     * Check that this policy is as "strong" as or stronger than the given policy
     *
     * <p>Note that this does not really test the strength of the policy, merely
     * that the values in this policy are equal than or greater to the values in
     * the given policy (or vice versa, depending on the property). It seems
     * likely that a policy with a requirement for many characters from the same
     * class would actually be weaker.</p>
     *
     * @param otherPolicy The policy to test against
     * @return True if the strength is equal or greater
     */
    public boolean hasStrengthOf( final IdentityProviderPasswordPolicy otherPolicy ) {
        return
            hasStrengthOf( ALLOWABLE_CHANGES, otherPolicy ) &&
            hasStrengthOf( CHARACTER_DIFF_MIN, otherPolicy ) &&
            hasStrengthOf( FORCE_PWD_CHANGE, otherPolicy ) &&
            hasStrengthOf( LOWER_MIN, otherPolicy ) &&
            hasStrengthOf( MAX_PASSWORD_LENGTH, otherPolicy ) &&
            hasStrengthOf( MIN_PASSWORD_LENGTH, otherPolicy ) &&
            hasStrengthOf( NO_REPEAT_CHARS, otherPolicy ) &&
            hasStrengthOf( NON_NUMERIC_MIN, otherPolicy ) &&
            hasStrengthOf( NUMBER_MIN, otherPolicy ) &&
            hasStrengthOf( PASSWORD_EXPIRY, otherPolicy, true ) &&
            hasStrengthOf( REPEAT_FREQUENCY, otherPolicy ) &&
            hasStrengthOf( SYMBOL_MIN, otherPolicy ) &&
            hasStrengthOf( UPPER_MIN, otherPolicy );
    }

    private boolean hasStrengthOf( final String propertyName,
                                   final IdentityProviderPasswordPolicy otherPolicy ) {
        return hasStrengthOf( propertyName, otherPolicy, false );
    }

    private boolean hasStrengthOf( final String propertyName,
                                   final IdentityProviderPasswordPolicy otherPolicy,
                                   final boolean invert ) {
        boolean stronger = false;

        final Object value = getPropertyValue( propertyName );
        final Object otherValue = otherPolicy.getPropertyValue( propertyName );

        if ( value instanceof Boolean && otherValue instanceof Boolean ) {
            stronger = invert ?
                 (Boolean)otherValue || !(Boolean)value:
                !(Boolean)otherValue || (Boolean)value;
        } else if ( value instanceof Integer && otherValue instanceof Integer ) {
            stronger = invert ?
                    (Integer)value <= (Integer)otherValue:
                    (Integer)value >= (Integer)otherValue;
        }

        return otherValue == null || stronger;
    }

}
