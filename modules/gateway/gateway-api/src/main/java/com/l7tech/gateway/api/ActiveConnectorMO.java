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
 *   <li>enableResponseMessages</li>
 *   <li>overrideContentType</li>
 *   <li>pollingInterval</li>
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
 *      <li>overrideContentType</li>
 *      <li>numberOfSacToCreate</li>
 *      <li>requestSizeLimit</li>
 *      <li>inbound</li>
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

    /**
    /**
     * Get name of the active connector (case insensitive, required)
     *
     * @return The name (may be null)
     */
    public String getName() {
        return get(name);
    }

    /**
     * Set the name for the active connector.
     *
     * @param name The name to use.
     */
    public void setName(final String name) {
        this.name = set(this.name, name);
    }

    /**
     * The active connector enabled flag
     *
     * @return True if the active connector is enabled.
     */
    public boolean isEnabled() {
        return get(enabled, false);
    }

    /**
     * Set the active connector enabled flag.
     *
     * @param enabled True to enable the active connector
     */
    public void setEnabled(final boolean enabled) {
        this.enabled = set(this.enabled, enabled);
    }

    /**
     * Get the type of the active connector
     *
     * @return The type of the active connector (may be null)
     */
    public String getType() {
        return get(type);
    }

    /**
     * Set the type of the active connector (required)
     *
     * @param type  The type of this active connector
     */
    public void setType(final String type) {
        this.type = set(this.type,type);
    }

    /**
     * Get the hardwired service ID for an inbound connection ( bypass resoution).
     * Null if using service resolution
     *
     * @return The hardwired service ID (may be null)
     */
    public String getHardwiredId() {
        return get(hardwiredId);
    }

    /**
     * Set the hardwired service ID for an inbound connection
     *
     * @param hardwiredId  The service ID to use
     */
    public void setHardwiredId(final String hardwiredId) {
        this.hardwiredId = set(this.hardwiredId, hardwiredId);
    }

    /**
     * Get the properties for this active connector
     *
     * @return The properties (may be null)
     */
    @XmlElement(name="Properties")
    @XmlJavaTypeAdapter( PropertiesMapType.PropertiesMapTypeAdapter.class)
    public Map<String, String> getProperties() {
        return properties;
    }

    /**
     * Set the properties for this active connector.
     *
     * @param properties The properties to use
     */
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
