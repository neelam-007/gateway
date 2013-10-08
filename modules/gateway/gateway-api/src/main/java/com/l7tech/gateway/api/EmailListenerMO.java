package com.l7tech.gateway.api;

import com.l7tech.gateway.api.impl.*;

import static com.l7tech.gateway.api.impl.AttributeExtensibleType.*;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.Map;

/**
 * The EmailListenerMO managed object represents an email listener.
 *
 * <p>Email listeners can be accessed by name or identifier.</p>
 *
 * @see com.l7tech.gateway.api.ManagedObjectFactory#createEmailListener()
 */
@XmlRootElement(name="EmailListener")
@XmlType(name="EmailListenerType", propOrder={"nameValue", "activeValue","hostnameValue","portValue","serverTypeValue","useSslValue","deleteOnReceiveValue","usernameValue", "passwordValue","folderValue","pollIntervalValue","properties","extension", "extensions"})
@AccessorSupport.AccessibleResource(name ="emailListeners")
public class EmailListenerMO extends ElementExtendableAccessibleObject {

    //- PUBLIC

    /**
     * Get the name for the email listener (case insensitive, required)
     *
     * @return The name of the email listener (may be null)
     */
    public String getName() {
        return get(name);
    }

    /**
     * Set the name for the email listener.
     *
     * @param name The name to use.
     */
    public void setName( final String name ) {
        this.name = set(this.name,name);
    }

    /**
     * Is the email listener active? (required)
     *
     * @return True if active.
     */
    public boolean getActive() {
        return get(active);
    }

    /**
     * Set the active state of the email listener.
     *
     * @param active True for active.
     */
    public void setActive(boolean active) {
        this.active = set(this.active,active);
    }

    /**
     * The hostname of the email server (required)
     *
     * @return the hostname
     */
    public String getHostname() {
        return get(hostname);
    }

    /**
     * Sets the hostname of the email server
     *
     * @param hostname The hostname to set
     */
    public void setHostname(String hostname) {
        this.hostname = set(this.hostname,hostname);
    }

    /**
     * Gets the port number to monitor(required)
     *
     * @return The port number
     */
    public int getPort() {
        return get(port);
    }

    /**
     * Sets the port number to monitor.
     *
     * @param port The port number to set.
     */
    public void setPort(int port) {
        this.port = set(this.port,port);
    }

    /**
     * Gets the email server type (POP3 or IMAP , required)
     *
     * @return The email server type
     */
    public EmailServerType getServerType() {
        return get(serverType);
    }

    /**
     * Sets the email server type
     *
     * @param serverType The email server type to set
     */
    public void setServerType(EmailServerType serverType) {
        this.serverType = setNonNull(
                this.serverType==null ? new AttributeExtensibleEmailServerType() : this.serverType,
                serverType );
    }

    /**
     * Gets the folder name, only valid for IMAP servers
     *
     * @return the folder name
     */
    public String getFolder() {
        return get(folder);
    }

    /**
     * Sets the folder name
     *
     * @param folder The folder name
     */
    public void setFolder(String folder) {
        this.folder = set(this.folder,folder);
    }

    /**
     * Gets the email account name (required)
     *
     * @return The email account name
     */
    public String getUsername() {
        return get(username);
    }

    /**
     * Sets the email account name
     *
     * @param username The email account name to use
     */
    public void setUsername(String username) {
        this.username = set(this.username,username);
    }

    /**
     * Gets the email account password for accessing the email server. Maybe a secured password reference.  Required for on create.
     *
     * @return the password to use
     */
    public String getPassword() {
        return get(password);
    }

    /**
     * Sets the password to use.
     *
     * @param password The password to set
     */
    public void setPassword(String password) {
        this.password = set(this.password,password);
    }

    /**
     * Get if email messages are deleted on receive
     *
     * @return True if deletes on receive.
     */
    public boolean getDeleteOnReceive() {
        return get(deleteOnReceive);
    }

    /**
     * Get if using SSL to connect to email server
     *
     * @return true if using SSL
     */
    public boolean getUseSsl() {
        return get(useSsl);
    }

    /**
     * Sets if using SSL to connect to email server
     *
     * @param useSsl True to use SSL
     */
    public void setUseSsl(boolean useSsl) {
        this.useSsl = set(this.useSsl,useSsl);
    }

    /**
     * Sets the delete on receive flag of the email listener.
     *
     * @param deleteOnReceive True for deletes on receive.
     */
    public void setDeleteOnReceive(boolean deleteOnReceive) {
        this.deleteOnReceive = set(this.deleteOnReceive,deleteOnReceive);
    }


    /**
     * Gets the polling interval for the email listener (required, seconds)
     *
     * @return The polling interval
     */
    public int getPollInterval() {
        return get(pollInterval,60);
    }

    /**
     * Sets the polling interval (seconds)
     *
     * @param pollInterval The polling interval to set
     */
    public void setPollInterval(int pollInterval) {
        this.pollInterval = set(this.pollInterval,pollInterval);
    }

    /**
     * Get the properties for this email listener
     *
     * <p>The following properties are supported:
     * <ul>
     *   <li>com.l7tech.server.jms.prop.hardwired.service.bool = false </li>
     *   <li>com.l7tech.server.jms.prop.hardwired.service.id </li>
     *   <li>com.l7tech.server.email.prop.request.sizeLimit = 1024 (bytes)  </li>
     * </ul>
     * </p>
     * @return The properties (may be null)
     */
    @XmlElement(name="Properties")
    @XmlJavaTypeAdapter( PropertiesMapType.PropertiesMapTypeAdapter.class)
    public Map<String, String> getProperties() {
        return properties;
    }

    /**
     * Set the properties for this Email listener
     *
     * @return The properties to use
     */
    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    /**
     * Type for email server
     */
    @XmlEnum(String.class)
    @XmlType(name="EmailServerType")
    public enum EmailServerType {
        /**
         * POP3 Server type
         */
        @XmlEnumValue("POP3") POP3,

        /**
         * IMAP Server type
         */
        @XmlEnumValue("IMAP") IMAP,

    }

    //- PROTECTED

    @XmlElement(name="Name", required=true)
    protected AttributeExtensibleString getNameValue() {
        return name;
    }

    protected void setNameValue( final AttributeExtensibleString name ) {
        this.name = name;
    }

    @XmlElement(name="Active", required=true)
    public AttributeExtensibleBoolean getActiveValue() {
        return active;
    }

    public void setActiveValue(AttributeExtensibleBoolean active) {
        this.active = active;
    }

    @XmlElement(name="DeleteOnReceive", required=true)
    public AttributeExtensibleBoolean getDeleteOnReceiveValue() {
        return deleteOnReceive;
    }

    public void setDeleteOnReceiveValue(AttributeExtensibleBoolean deleteOnReceive) {
        this.deleteOnReceive = deleteOnReceive;
    }

    @XmlElement(name="Folder")
    public AttributeExtensibleString getFolderValue() {
        return folder;
    }

    public void setFolderValue(AttributeExtensibleString folder) {
        this.folder = folder;
    }

    @XmlElement(name="Hostname", required=true)
    public AttributeExtensibleString getHostnameValue() {
        return hostname;
    }

    public void setHostnameValue(AttributeExtensibleString hostname) {
        this.hostname = hostname;
    }

    @XmlElement(name="Username", required=true)
    public AttributeExtensibleString getUsernameValue() {
        return username;
    }

    public void setUsernameValue(AttributeExtensibleString username) {
        this.username = username;
    }

    @XmlElement(name="Password")
    public AttributeExtensibleString getPasswordValue() {
        return password;
    }

    public void setPasswordValue(AttributeExtensibleString password) {
        this.password = password;
    }

    @XmlElement(name="PollInterval", required=true)
    public AttributeExtensibleInteger getPollIntervalValue() {
        return pollInterval;
    }

    public void setPollIntervalValue(AttributeExtensibleInteger pollInterval) {
        this.pollInterval = pollInterval;
    }

    @XmlElement(name="Port", required=true)
    public AttributeExtensibleInteger getPortValue() {
        return port;
    }

    public void setPortValue(AttributeExtensibleInteger port) {
        this.port = port;
    }

    public Map<String, String> getPropertiesValue() {
        return properties;
    }

    @XmlElement(name="UseSsl")
    public AttributeExtensibleBoolean getUseSslValue() {
        return useSsl;
    }

    public void setUseSslValue(AttributeExtensibleBoolean useSsl) {
        this.useSsl = useSsl;
    }

    @XmlElement(name="ServerType", required=true)
    protected AttributeExtensibleEmailServerType getServerTypeValue() {
        return serverType;
    }

    protected void setServerTypeValue( final AttributeExtensibleEmailServerType serverType ) {
        this.serverType = serverType;
    }

    @XmlType(name="EmailServerTypePropertyType")
    protected static class AttributeExtensibleEmailServerType extends AttributeExtensible<EmailServerType> {
        private EmailServerType value;

        @XmlValue
        @Override
        public EmailServerType getValue() {
            return value;
        }

        @Override
        public void setValue( final EmailServerType value ) {
            this.value = value;
        }
    }

    //- PACKAGE

    EmailListenerMO() {
    }

    //- PRIVATE
    private AttributeExtensibleString name;
    private AttributeExtensibleString hostname;
    private AttributeExtensibleInteger port;
    private AttributeExtensibleEmailServerType serverType;
    private AttributeExtensibleBoolean useSsl;
    private AttributeExtensibleBoolean deleteOnReceive;
    private AttributeExtensibleString username;
    private AttributeExtensibleString password;
    private AttributeExtensibleString folder;
    private AttributeExtensibleInteger pollInterval;
    private AttributeExtensibleBoolean active;
    private Map<String, String> properties;

}
