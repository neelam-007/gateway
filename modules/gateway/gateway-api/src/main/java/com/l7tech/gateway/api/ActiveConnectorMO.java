package com.l7tech.gateway.api;

import com.l7tech.gateway.api.impl.AccessorSupport;
import com.l7tech.gateway.api.impl.AttributeExtensibleType;
import com.l7tech.gateway.api.impl.ElementExtendableAccessibleObject;
import com.l7tech.gateway.api.impl.PropertiesMapType;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.List;
import java.util.Map;

import static com.l7tech.gateway.api.impl.AttributeExtensibleType.*;

/**
 * MO representation of active connectors (e.g. MQ native queues and SFTP Polling listeners).
 *
 * <p>Active connectors can be of type SFTP (SFTP Polling Listener) and MqNative (MQ Destination)
 * </p>
 *
 *
 * <p>The following properties are used for SFTP:
 * <ul>
 *   <li>SftpHost</li>
 *   <li>SftpPort</li>
 *   <li>SftpDirectory</li>
 *   <li>SftpFileNamePattern</li>
 *   <li>SftpUsername</li>
 *   <li>SftpSecurePasswordOid</li>
 *   <li>SftpSecurePasswordKeyOid</li>
 *   <li>SftpServerFingerPrint</li>
 *   <li>SftpDeleteOnReceive</li>
 * </ul>
 * </p>
 *
 *
 * <p>The following properties are used for MqNative:
 * <ul>
 *    <li> general connection properties:
 *    <ul>
 *      <li>MqNativeHostName</li>
 *      <li>MqNativePort</li>
 *      <li>MqNativeQueueManagerName</li>
 *      <li>MqNativeChannel</li>
 *      <li>MqNativeTargetQueueName</li>
 *    </ul>
 *    </li>
 *    <li> security connection properties:
 *    <ul>
 *      <li>MqNativeIsQueueCredentialRequired</li>
 *      <li>MqNativeUserId</li>
 *      <li>MqNativeSecurePasswordOid</li>
 *      <li>MqNativeIsSslEnabled</li>
 *      <li>MqNativeCipherSuite</li>
 *      <li>MqNativeIsSslKeystoreUsed</li>
 *      <li>MqNativeSslKeystoreAlias</li>
 *      <li>MqNativeSslKeystoreId</li>
 *    </ul>
 *    </li>
 *    <li> inbound and outbound common properties:
 *    <ul>
 *      <li>MqNativeReplyType</li>
 *      <li>MqNativeIsCopyCorrelationIdFromRequest</li>
 *      <li>MqNativeSpecifiedReplyQueueName</li>
 *    </ul>
 *    </li>
 *    <li> inbound properties:
 *    <ul>
 *      <li>MqNativeInboundAcknowledgementType</li>
 *      <li>MqNativeInboundIsSoapActionUsed</li>
 *      <li>MqNativeInboundSoapAction</li>
 *      <li>MqNativeInboundContentTypeFromProperty</li>
 *      <li>MqNativeInboundIsFailedQueueUsed</li>
 *      <li>MqNativeInboundFailedQueueName</li>
 *    </ul>
 *    </li>
 *    <li> outbound properties:
 *    <ul>
 *      <li>MqNativeOutboundIsTemplateQueue</li>
 *      <li>MqNativeOutboundTemporaryQueueNamePattern</li>
 *      <li>MqNativeOutboundMessageFormat</li>
 *    </ul>
 *    </li>
 * </ul>
 * </p>
 */
@XmlRootElement(name ="ActiveConnector")
@XmlType(name="ActiveConnector", propOrder={"nameValue","enabledValue","typeValue","hardwiredIdValue","properties","extension","extensions"})
@AccessorSupport.AccessibleResource(name ="activeConnectors")
public class ActiveConnectorMO extends ElementExtendableAccessibleObject {

    //- PUBLIC

    public String getName() {
        return get(name);
    }

    public void setName(final String name) {
        this.name = set(this.name, name);
    }

    public boolean isEnabled() {
        return get(enabled, false);
    }

    public void setEnabled(final boolean enabled) {
        this.enabled = set(this.enabled, enabled);
    }

    /**
     * Type for active connectors
     */
    public String getType() {
        return get(type);
    }

    public void setType(final String type) {
        this.type = set(this.type,type);
    }

    public String getHardwiredId() {
        return get(hardwiredId);
    }

    public void setHardwiredId(final String hardwiredId) {
        this.hardwiredId = set(this.hardwiredId, hardwiredId);
    }


    @XmlElement(name="Properties")
    @XmlJavaTypeAdapter( PropertiesMapType.PropertiesMapTypeAdapter.class)
    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties( final Map<String, String> properties ) {
        this.properties = properties;
    }

    @XmlAnyElement(lax=true)
    @Override
    protected List<Object> getExtensions() {
        return super.getExtensions();
    }

    @XmlElement(name="Type", required=true)
    protected AttributeExtensibleString getTypeValue() {
        return type;
    }

    protected void setTypeValue( final AttributeExtensibleString activeConnectorType ) {
        this.type = activeConnectorType;
    }

    @XmlElement(name="Name", required=true)
    protected AttributeExtensibleString getNameValue() {
        return name;
    }

    protected void setNameValue( final AttributeExtensibleString value ) {
        this.name = value;
    }

    @XmlElement(name="Enabled", required=true)
    protected AttributeExtensibleBoolean getEnabledValue() {
        return enabled;
    }

    protected void setEnabledValue( final AttributeExtensibleBoolean value ) {
        this.enabled = value;
    }

    @XmlElement(name="HardwiredId", required=false)
    protected AttributeExtensibleString getHardwiredIdValue() {
        return hardwiredId;
    }

    protected void setHardwiredIdValue(final AttributeExtensibleString value) {
        this.hardwiredId = value;
    }

    //- PACKAGE

    ActiveConnectorMO() {
    }

    //- PRIVATE

    private AttributeExtensibleString name;
    private AttributeExtensibleBoolean enabled = new AttributeExtensibleType.AttributeExtensibleBoolean(false);
    private Map<String,String> properties;
    private AttributeExtensibleString type;
    private AttributeExtensibleString hardwiredId;

}
