package com.l7tech.identity;

import com.l7tech.common.io.NonCloseableOutputStream;
import com.l7tech.objectmodel.imp.ZoneableNamedEntityImp;
import com.l7tech.security.types.CertificateValidationType;
import com.l7tech.util.*;
import org.hibernate.annotations.Proxy;
import org.jetbrains.annotations.Nullable;

import javax.persistence.*;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import java.beans.ExceptionListener;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

//TODO change to use NamedEntityWithPropertiesImp
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
@Entity
@Proxy(lazy=false)
@Table(name="identity_provider")
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(
    name="type",
    discriminatorType=DiscriminatorType.INTEGER
)
@DiscriminatorValue("1")
public class IdentityProviderConfig extends ZoneableNamedEntityImp {

    public IdentityProviderConfig(IdentityProviderType type) {
        this.type = type;
    }

    public IdentityProviderConfig() {
        // only default value which makes sense for now.
        type = IdentityProviderType.LDAP;
    }

    public IdentityProviderConfig(IdentityProviderConfig other) {
        super(other);
        this.type = other.type;
    }

    @Size(min=1,max=128)
    @Override
    @Transient
    public String getName() {
        return super.getName();
    }

    /**
     * the type is set in constructor
     */
    public IdentityProviderType type() {return type;}

    /**
     * @return true if the Gateway can update this Identity Provider e.g. change passwords, reset certs etc.
     */
    @Transient
    public boolean isWritable() {
        return true; // Internal is writable and there's no InternalIdentityProviderConfig
    }

    @Column(name="description", length=Integer.MAX_VALUE)
    @Lob
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * for serialization by axis and hibernate only.
     * to get the properties, call getProperty
     */
    @Column(name="properties", length=Integer.MAX_VALUE)
    @Lob
    public String getSerializedProps() throws java.io.IOException {
        if (propsXml == null) {
            // if no props, return empty string
            if (props.size() < 1) {
                propsXml = "";
            } else {
                PoolByteArrayOutputStream output = null;
                java.beans.XMLEncoder encoder = null;
                try {
                    output = new PoolByteArrayOutputStream();
                    encoder = new java.beans.XMLEncoder(new NonCloseableOutputStream(output));
                    encoder.setExceptionListener( new ExceptionListener() {
                        @Override
                        public void exceptionThrown( final Exception e ) {
                            logger.log( Level.WARNING, "Error storing configuration '"+ExceptionUtils.getMessage(e)+"'.", ExceptionUtils.getDebugException(e));
                        }
                    });
                    encoder.writeObject(props);
                    encoder.close(); // writes closing XML tag
                    encoder = null;
                    propsXml = output.toString(Charsets.UTF8);
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
            java.beans.XMLDecoder decoder = new java.beans.XMLDecoder(in, null, new ExceptionListener() {
                @Override
                public void exceptionThrown( final Exception e ) {
                    logger.log( Level.WARNING, "Error loading configuration '"+ExceptionUtils.getMessage(e)+"'.", ExceptionUtils.getDebugException(e));
                }
            });
            //noinspection unchecked
            props = (Map<String, Object>) decoder.readObject();
        }
    }

    /**
     * for serialization during policy export
     * to get the properties, call getProperty
     */
    @Transient
    public String getExportableSerializedProps() throws java.io.IOException {
        // if no props, return empty string
        if (props.size() < 1) {
            return "";
        } else {
            Map<String, Object> filteredProps = new HashMap<String, Object>(props);
            for(String unexportablePropKey : getUnexportablePropKeys()) {
                filteredProps.remove(unexportablePropKey);
            }

            PoolByteArrayOutputStream output = null;
            java.beans.XMLEncoder encoder = null;
            try {
                output = new PoolByteArrayOutputStream();
                encoder = new java.beans.XMLEncoder(new NonCloseableOutputStream(output));
                encoder.setExceptionListener( new ExceptionListener() {
                    @Override
                    public void exceptionThrown( final Exception e ) {
                        logger.log( Level.WARNING, "Error exporting configuration '"+ExceptionUtils.getMessage(e)+"'.", ExceptionUtils.getDebugException(e));
                    }
                });
                encoder.writeObject(filteredProps);
                encoder.close(); // writes closing XML tag
                encoder = null;
                return output.toString(Charsets.UTF8);
            }
            finally {
                if(encoder!=null) encoder.close();
                ResourceUtils.closeQuietly(output);
            }
        }
    }

    /**
     * for serialization by axis and hibernate only.
     */
    @Column(name="type", insertable=false, updatable=false)
    public int getTypeVal() {
        return type.toVal();
    }

    /**
     * for serialization by axis and hibernate only.
     */
    public void setTypeVal(int val) {
        type = IdentityProviderType.fromVal(val);
    }

    @Transient
    public boolean isAdminEnabled() {
        Boolean b = (Boolean) getProperty(ADMIN_ENABLED);
        return b != null && b;
    }

    public void setAdminEnabled(boolean adminEnabled) {
        setProperty(ADMIN_ENABLED, adminEnabled);
    }

    /**
     * @return true if the Gateway can issue Certificates on behalf of this identity provider.
     */
    @Transient
    public boolean canIssueCertificates() {
        return true;
    }

    /**
     * @return true if a valid example username and password must be provided in order to test this identity provider.
     */
    @Transient
    public boolean isCredentialsRequiredForTest() {
        return false;
    }

    @Transient
    public CertificateValidationType getCertificateValidationType() {
        CertificateValidationType validationType = null;

        Object value = getProperty(PROP_CERTIFICATE_VALIDATION_TYPE);
        if ( value instanceof CertificateValidationType ) { // old serialization format
            validationType = (CertificateValidationType)  value;
        } else if ( value instanceof String ) {
            validationType = getEnumProperty(PROP_CERTIFICATE_VALIDATION_TYPE, null, CertificateValidationType.class);
        }

        return validationType;
    }

    public void setCertificateValidationType( CertificateValidationType validationType) {
        if ( validationType == null ) {
            setProperty(PROP_CERTIFICATE_VALIDATION_TYPE, null);
        } else {
            setProperty(PROP_CERTIFICATE_VALIDATION_TYPE, validationType.toString());
        }
    }

    /**
     * allows to set all properties from another object
     */
    public final void copyFrom(IdentityProviderConfig objToCopy) {
        setDescription(objToCopy.getDescription());
        setName(objToCopy.getName());
        setSecurityZone(objToCopy.getSecurityZone());
        type = objToCopy.type();
        props = objToCopy.props;
        propsXml = null;
    }

    //- PROTECTED

    protected String description;
    protected IdentityProviderType type;

    @SuppressWarnings({ "unchecked" })
    protected <T> T getProperty(String name) {
        return (T)props.get(name);
    }

    protected void setProperty(String name, @Nullable Object value) {
        if ( value == null ) {
            props.remove( name );
        } else {
            props.put(name, value);
        }
        propsXml = null;
    }

    protected boolean getBooleanProperty(String prop, boolean dflt) {
        Boolean val = (Boolean)props.get(prop);
        return val == null ? dflt : val;
    }

    protected <E extends Enum<E>> E getEnumProperty(String prop, E dflt, Class<E> template) {
        E value = dflt;

        String val = (String) props.get(prop);
        if ( val != null ) {
            try {
                value = Enum.valueOf( template, val );
            } catch ( IllegalArgumentException iae ) {
                // use default
            }
        }

        return value;
    }

    @Transient
    protected String[] getUnexportablePropKeys() {
        return new String[0];
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(IdentityProviderConfig.class.getName());

    private static final String ADMIN_ENABLED = "adminEnabled";
    private static final String PROP_CERTIFICATE_VALIDATION_TYPE = "certificateValidationType";

    private String propsXml;
    private Map<String, Object> props = new HashMap<String, Object>();
}
