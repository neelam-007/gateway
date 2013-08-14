package com.l7tech.gateway.common.transport.email;

import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.imp.ZoneableNamedGoidEntityImp;
import com.l7tech.policy.wsp.WspSensitive;
import com.l7tech.search.Dependency;
import com.l7tech.util.Charsets;
import com.l7tech.util.PoolByteArrayOutputStream;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.Proxy;

import javax.persistence.*;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An email listener configuration.
 */
@XmlRootElement
@Entity
@Proxy(lazy=false)
@Table(name="email_listener")
public class EmailListener extends ZoneableNamedGoidEntityImp {
    private static final Logger logger = Logger.getLogger(EmailListener.class.getName());
    private static final Charset ENCODING = Charsets.UTF8;

    private String host;
    private int port;
    private EmailServerType serverType;
    private boolean useSsl;
    private boolean deleteOnReceive;
    private String username;
    private String password;
    private String folder;
    private int pollInterval = 60;
    private boolean active;
    private transient EmailListenerState emailListenerState;    //transient because don't need state information when editing email listener in GUI

    private String properties;

    public static final String PREFIX = "com.l7tech.server.email.prop";

    // Due to a bug, these properties were incorrect. Since this has been shipped, we
    // now use "JMS" properties for email listeners. Any new properties should use the
    // above prefix for email props.
    public static final String PROP_IS_HARDWIRED_SERVICE = "com.l7tech.server.jms.prop.hardwired.service.bool";
    public static final String PROP_HARDWIRED_SERVICE_ID = "com.l7tech.server.jms.prop.hardwired.service.id";

    /**If specified, the request xml size limit is overridden */
    public static final String PROP_REQUEST_SIZE_LIMIT = PREFIX + ".request.sizeLimit";

    public EmailListener() {
        super();
        emailListenerState = new EmailListenerState(this);
    }

    public EmailListener(final EmailServerType serverType) {
        super();
        this.serverType = serverType;
        this.port = serverType.getDefaultClearPort();
        this.emailListenerState = new EmailListenerState(this);
    }

    public EmailListener(final EmailListener emailListener) {
        super(emailListener);
        host = emailListener.getHost();
        port = emailListener.getPort();
        serverType = emailListener.getServerType();
        useSsl = emailListener.isUseSsl();
        deleteOnReceive = emailListener.isDeleteOnReceive();
        username = emailListener.getUsername();
        password = emailListener.getPassword();
        folder = emailListener.getFolder();
        pollInterval = emailListener.getPollInterval();
        active = emailListener.isActive();
        properties = emailListener.getProperties();
        createEmailListenerState(emailListener.getEmailListenerState());

    }

    public EmailListener(Goid goid, String name, String host, int port, EmailServerType serverType, boolean useSsl,
                         boolean deleteOnReceive, String username, String password, String folder, int pollInterval,
                         boolean active, String ownerNodeId, long lastPollTime, Long lastMessageId, String properties) {
        super();
        setGoid(goid);
        setName(name);
        this.host = host;
        this.port = port;
        this.serverType = serverType;
        this.useSsl = useSsl;
        this.deleteOnReceive = deleteOnReceive;
        this.username = username;
        this.password = password;
        this.folder = folder;
        this.pollInterval = pollInterval;
        this.active = active;
        this.properties = properties;
        createEmailListenerState(ownerNodeId, lastPollTime, lastMessageId);
    }

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "emailListener")
    @Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
    @Dependency(isDependency = false)
    public EmailListenerState getEmailListenerState() {
        return emailListenerState;
    }

    public void setEmailListenerState(EmailListenerState emailListenerState) {
        this.emailListenerState = emailListenerState;
    }

    /**
     * Creates a new email listener state
     *
     * @param emailListenerState    The email listener state values should be copied over.  It will not copy the email listener instance
     *                              because it may not be the actual email listener it wants to refer to.
     */
    public void createEmailListenerState(EmailListenerState emailListenerState) {
        if (this.emailListenerState != null) {
            this.emailListenerState = new EmailListenerState(this);
        }

        if (emailListenerState == null) return;
        this.emailListenerState.setOwnerNodeId(emailListenerState.getOwnerNodeId());
        this.emailListenerState.setLastPollTime(emailListenerState.getLastPollTime());
        this.emailListenerState.setLastMessageId(emailListenerState.getLastMessageId());
    }

    /**
     * Creates a new email listener state.
     *
     * @param ownerNodeId   owner node id
     * @param lastPollTime  last poll time
     * @param lastMessageId last message id
     */
    public void createEmailListenerState(String ownerNodeId, long lastPollTime, long lastMessageId) {
        if (emailListenerState == null) {
            emailListenerState = new EmailListenerState(this, ownerNodeId, lastPollTime, lastMessageId);
        } else {
            updateEmailListenerState(ownerNodeId, lastPollTime, lastMessageId);
        }
    }

    /**
     * Updates the email listener state.
     *
     * @param ownerNodeId   owner node id
     * @param lastPollTime  last poll time
     * @param lastMessageId last message id
     */
    public void updateEmailListenerState(String ownerNodeId, long lastPollTime, long lastMessageId) {
        emailListenerState.setOwnerNodeId(ownerNodeId);
        emailListenerState.setLastPollTime(lastPollTime);
        emailListenerState.setLastMessageId(lastMessageId);
    }

    @Column(name="host", length=128, nullable=false)
    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }


    @Column(name="properties", length=Integer.MAX_VALUE)
    @Lob
    public String getProperties() {
        return properties;
    }

    public void setProperties(String properties) {
        this.properties = properties;
    }

    @Dependency(key = PROP_HARDWIRED_SERVICE_ID, methodReturnType = Dependency.MethodReturnType.GOID, type = Dependency.DependencyType.SERVICE)
    public Properties properties() {
        Properties properties = new Properties();
        try {
            String propertiesStr = getProperties();
            if (propertiesStr != null && propertiesStr.trim().length()>0) {
                properties.loadFromXML(new ByteArrayInputStream(propertiesStr.getBytes(ENCODING)));
            }
        }
        catch(Exception e) {
            logger.log(Level.WARNING, "Error loading properties", e);
        }
        return properties;
    }

    public void properties(Properties properties) {
        if (properties == null) {
            setProperties("");
        } else {
            PoolByteArrayOutputStream baos = new PoolByteArrayOutputStream();
            try {
                properties.storeToXML(baos, null, ENCODING.name());
                setProperties(baos.toString(ENCODING));
            }
            catch (Exception e) {
                logger.log(Level.WARNING, "Error saving properties", e);
            } finally {
                baos.close();
            }
        }
    }

    @Column(name="port", nullable=false)
    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Enumerated(EnumType.STRING)
    @Column(name="server_type", length=4, nullable=false)
    public EmailServerType getServerType() {
        return serverType;
    }

    public void setServerType(EmailServerType serverType) {
        this.serverType = serverType;
    }

    @Column(name="use_ssl", nullable=false)
    public boolean isUseSsl() {
        return useSsl;
    }

    public void setUseSsl(boolean useSsl) {
        this.useSsl = useSsl;
    }

    @Column(name="delete_on_receive", nullable=false)
    public boolean isDeleteOnReceive() {
        return deleteOnReceive;
    }

    public void setDeleteOnReceive(boolean deleteOnReceive) {
        this.deleteOnReceive = deleteOnReceive;
    }

    @Column(name="username", length=255, nullable=false)
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Column(name="password", length=32, nullable=false)
    @WspSensitive
    @Dependency(type = Dependency.DependencyType.SECURE_PASSWORD, methodReturnType = Dependency.MethodReturnType.VARIABLE)
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Column(name="folder", length=255, nullable=false)
    public String getFolder() {
        return folder;
    }

    public void setFolder(String folder) {
        this.folder = folder;
    }

    @Column(name="poll_interval", nullable=false)
    public int getPollInterval() {
        return pollInterval;
    }

    public void setPollInterval(int pollInterval) {
        this.pollInterval = pollInterval;
    }

    @Column(name="active", nullable=false)
    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public void lock() {
        super.lock();
    }
}
