package com.l7tech.gateway.api;

import com.l7tech.gateway.api.impl.AccessorSupport;
import static com.l7tech.gateway.api.impl.AttributeExtensibleType.*;
import static com.l7tech.gateway.api.impl.AttributeExtensibleType.set;

import com.l7tech.gateway.api.impl.ElementExtendableAccessibleObject;
import com.l7tech.gateway.api.impl.Extension;
import com.l7tech.gateway.api.impl.ExtensionSupport;
import com.l7tech.gateway.api.impl.ManagedObjectReference;
import com.l7tech.gateway.api.impl.PropertiesMapType;

import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * TODO [steve] javadoc and add to client docs / wsdl
 */
@XmlRootElement(name="ListenPort")
@XmlType(name="ListenPortType", propOrder={"nameValue","enabledValue","protocolValue","interfaceValue","portValue","enabledFeatureValues","targetServiceReference","tlsSettings","properties","extension","extensions"})
@AccessorSupport.AccessibleResource(name ="listenPorts")
public class ListenPortMO extends ElementExtendableAccessibleObject {

    //- PUBLIC

    /**
     * Get the name for the listen port (case insensitive, required)
     *
     * @return The name (may be null)
     */
    public String getName() {
        return get(name);
    }

    /**
     * Set the name for the listen port
     *
     * @param name The name to use
     */
    public void setName( final String name ) {
        this.name = set(this.name,name);
    }

    public boolean isEnabled() {
        return get(enabled,false);
    }

    public void setEnabled( final boolean enabled ) {
        this.enabled = set(this.enabled, enabled);
    }

    public String getProtocol() {
        return get(protocol);
    }

    public void setProtocol( final String protocol ) {
        this.protocol = set(this.protocol,protocol);
    }

    /**
     * Get the IP address or interface tag (optional)
     *
     * @return The IP address or interface tag or null.
     */
    public String getInterface() {
        return get(interfaceTag);
    }

    /**
     * Set the IP address or interface tag.
     *
     * @param interfaceTag The IP address or interface tag to use (null for any)
     */
    public void setInterface( final String interfaceTag ) {
        this.interfaceTag = set(this.interfaceTag,interfaceTag);
    }

    /**
     * Get the port to use (required)
     *
     * @return The port number.
     */
    public int getPort() {
        return get(port, 0);
    }

    /**
     * Set the port to use
     *
     * @param port The port value to use
     */
    public void setPort( final int port ) {
        this.port = set(this.port,port);
    }

    /**
     * Get the enabled features for this listen port.
     *
     * TODO [steve] document the possible features (XmlEnumValue annotate Endpoint enum with names)
     *
     * @return The list of enabled features (never null)
     */
    public List<String> getEnabledFeatures() {
        return unwrap(get( enabledFeatures, new ArrayList<AttributeExtensibleString>() ));
    }

    /**
     * Set the enabled features for this listen port.
     *
     * @param enabledFeatures The features to enable
     */
    public void setEnabledFeatures( final List<String> enabledFeatures ) {
        this.enabledFeatures = set( this.enabledFeatures, wrap(enabledFeatures,AttributeExtensibleStringBuilder) );
    }

    /**
     * Get the identifier of the target service (optional)
     *
     * @return The identifier for the service or null
     */
    public String getTargetServiceId() {
        return targetServiceReference==null ? null : targetServiceReference.getId();
    }

    /**
     * Set the identifier for the target service or null for none
     *
     * @param id The service identifier.
     */
    public void setTargetServiceId( final String id ) {
        targetServiceReference = id==null ?
                null :
                new ManagedObjectReference( ServiceMO.class, id );
    }

    @XmlElement(name="TlsSettings")
    public TlsSettings getTlsSettings() {
        return tlsSettings;
    }

    public void setTlsSettings( final TlsSettings tlsSettings ) {
        this.tlsSettings = tlsSettings;
    }

    /**
     * Get the properties for this listen port.
     *
     * @return The properties (may be null)
     */
    @XmlElement(name="Properties")
    @XmlJavaTypeAdapter(PropertiesMapType.PropertiesMapTypeAdapter.class)
    public Map<String, Object> getProperties() {
        return properties;
    }

    /**
     * Set the properties for this listen port.
     *
     * @param properties The properties to use
     */
    public void setProperties( final Map<String, Object> properties ) {
        this.properties = properties;
    }

    @SuppressWarnings({ "ProtectedMemberInFinalClass" })
    @XmlType(name="TlsSettingsType", propOrder={"clientAuthenticationValue","privateKeyReference","enabledVersionValues","enabledCipherSuiteValues","properties","extension","extensions"})
    public static final class TlsSettings extends ExtensionSupport {
        @XmlEnum(String.class)
        @XmlType(name="ClientAuthenticationType")
        public enum ClientAuthentication {
            @XmlEnumValue("None") NONE,
            @XmlEnumValue("Optional") OPTIONAL,
            @XmlEnumValue("Required") REQUIRED
        }

        public ClientAuthentication getClientAuthentication() {
            return get(clientAuthentication, ClientAuthentication.OPTIONAL);
        }

        public void setClientAuthentication( final ClientAuthentication clientAuthentication ) {
            this.clientAuthentication = setNonNull( this.clientAuthentication == null ? new AttributeExtensibleClientAuthentication() : this.clientAuthentication, clientAuthentication );
        }

        public String getPrivateKeyId() {
            return privateKeyReference==null ? null : privateKeyReference.getId();
        }

        public void setPrivateKeyId( final String id ) {
            privateKeyReference = id==null ?
                    null :
                    new ManagedObjectReference( PrivateKeyMO.class, id );
        }

        public List<String> getEnabledVersions() {
            return unwrap(get( enabledVersions, new ArrayList<AttributeExtensibleString>() ));
        }

        public void setEnabledVersions( final List<String> enabledVersions ) {
            this.enabledVersions = set( this.enabledVersions, wrap(enabledVersions,AttributeExtensibleStringBuilder) );
        }

        public List<String> getEnabledCipherSuites() {
            return unwrap(get( enabledCipherSuites, new ArrayList<AttributeExtensibleString>() ));
        }

        public void setEnabledCipherSuites( final List<String> enabledVersions ) {
            this.enabledCipherSuites = set( this.enabledCipherSuites, wrap(enabledVersions,AttributeExtensibleStringBuilder) );
        }

        @XmlElement(name="Properties")
        @XmlJavaTypeAdapter(PropertiesMapType.PropertiesMapTypeAdapter.class)
        public Map<String, Object> getProperties() {
            return properties;
        }

        public void setProperties( final Map<String, Object> properties ) {
            this.properties = properties;
        }

        @XmlElement(name="ClientAuthentication")
        protected AttributeExtensibleClientAuthentication getClientAuthenticationValue() {
            return clientAuthentication;
        }

        protected  void setClientAuthenticationValue( final AttributeExtensibleClientAuthentication clientAuthentication ) {
            this.clientAuthentication = clientAuthentication;
        }

        @XmlElement(name="PrivateKeyReference")
        protected ManagedObjectReference getPrivateKeyReference() {
            return privateKeyReference;
        }

        protected void setPrivateKeyReference( final ManagedObjectReference privateKeyReference ) {
            this.privateKeyReference = privateKeyReference;
        }

        @XmlElement(name="EnabledVersions", required=true)
        protected AttributeExtensibleStringList getEnabledVersionValues() {
            return enabledVersions;
        }

        protected void setEnabledVersionValues( final AttributeExtensibleStringList enabledVersions ) {
            this.enabledVersions = enabledVersions;
        }

        @XmlElement(name="EnabledCipherSuites", required=true)
        protected AttributeExtensibleStringList getEnabledCipherSuiteValues() {
            return enabledCipherSuites;
        }

        protected void setEnabledCipherSuiteValues( final AttributeExtensibleStringList enabledCipherSuites ) {
            this.enabledCipherSuites = enabledCipherSuites;
        }

        @XmlElement(name="Extension")
        @Override
        protected Extension getExtension() {
            return super.getExtension();
        }

        @XmlAnyElement(lax=true)
        @Override
        protected List<Object> getExtensions() {
            return super.getExtensions();
        }

        @XmlType(name="ClientAuthenticationPropertyType")
        protected static class AttributeExtensibleClientAuthentication extends AttributeExtensible<ClientAuthentication> {
            private ClientAuthentication value;

            @XmlValue
            @Override
            public ClientAuthentication getValue() {
                return value;
            }

            @Override
            public void setValue( final ClientAuthentication value ) {
                this.value = value;
            }
        }

        TlsSettings() {
        }

        private ManagedObjectReference privateKeyReference;
        private AttributeExtensibleClientAuthentication clientAuthentication;
        private AttributeExtensibleStringList enabledVersions;
        private AttributeExtensibleStringList enabledCipherSuites;
        private Map<String,Object> properties;
    }

    //- PROTECTED

    @XmlElement(name="Name", required=true)
    protected AttributeExtensibleString getNameValue() {
        return name;
    }

    protected void setNameValue( final AttributeExtensibleString name ) {
        this.name = name;
    }

    @XmlElement(name="Enabled", required=true)
    protected AttributeExtensibleBoolean getEnabledValue() {
        return enabled;
    }

    protected void setEnabledValue( final AttributeExtensibleBoolean enabled ) {
        this.enabled = enabled;
    }

    @XmlElement(name="Protocol", required=true)
    protected AttributeExtensibleString getProtocolValue() {
        return protocol;
    }

    protected void setProtocolValue( final AttributeExtensibleString protocol ) {
        this.protocol = protocol;
    }

    @XmlElement(name="Interface", required=false)
    protected AttributeExtensibleString getInterfaceValue() {
        return interfaceTag;
    }

    protected void setInterfaceValue( final AttributeExtensibleString interfaceTag ) {
        this.interfaceTag = interfaceTag;
    }

    @XmlElement(name="Port", required=true)
    protected AttributeExtensibleInteger getPortValue() {
        return port;
    }

    protected void setPortValue( final AttributeExtensibleInteger port ) {
        this.port = port;
    }

    @XmlElement(name="EnabledFeatures", required=true)
    protected AttributeExtensibleStringList getEnabledFeatureValues() {
        return enabledFeatures;
    }

    protected void setEnabledFeatureValues( final AttributeExtensibleStringList enabledFeatures ) {
        this.enabledFeatures = enabledFeatures;
    }

    @XmlElement(name="TargetServiceReference")
    protected ManagedObjectReference getTargetServiceReference() {
        return targetServiceReference;
    }

    protected void setTargetServiceReference( final ManagedObjectReference targetServiceReference ) {
        this.targetServiceReference = targetServiceReference;
    }

    //- PACKAGE

    ListenPortMO() {
    }

    //- PRIVATE

    private AttributeExtensibleString name;
    private AttributeExtensibleBoolean enabled;
    private AttributeExtensibleString protocol;
    private AttributeExtensibleString interfaceTag; // or interface
    private AttributeExtensibleInteger port;
    private AttributeExtensibleStringList enabledFeatures;
    private ManagedObjectReference targetServiceReference;
    private TlsSettings tlsSettings;
    private Map<String,Object> properties;
}
