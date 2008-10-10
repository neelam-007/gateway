package com.l7tech.gateway.common.transport.email;

import com.l7tech.objectmodel.imp.NamedEntityImp;

import javax.xml.bind.annotation.XmlRootElement;
import javax.persistence.*;

/**
 * An email listener configuration.
 */
@XmlRootElement
@Entity
@Table(name="email_listener")
public class EmailListener extends NamedEntityImp {
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
    private String ownerNodeId;
    private long lastPollTime;
    private Long lastMessageId;

    public EmailListener() {
        super();
    }

    public EmailListener(final EmailServerType serverType) {
        super();
        this.serverType = serverType;
        this.port = serverType.getDefaultClearPort();
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
        ownerNodeId = emailListener.getOwnerNodeId();
        lastPollTime = emailListener.getLastPollTime();
        lastMessageId = emailListener.getLastMessageId();
    }

    public EmailListener(long oid, String name, String host, int port, EmailServerType serverType, boolean useSsl,
                         boolean deleteOnReceive, String username, String password, String folder, int pollInterval,
                         boolean active, String ownerNodeId, long lastPollTime, Long lastMessageId)
    {
        super();
        setOid(oid);
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
        this.ownerNodeId = ownerNodeId;
        this.lastPollTime = lastPollTime;
        this.lastMessageId = lastMessageId;
    }

    @Column(name="host", length=128, nullable=false)
    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
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

    @Column(name="owner_node_id", length=36)
    public String getOwnerNodeId() {
        return ownerNodeId;
    }

    public void setOwnerNodeId(String ownerNodeId) {
        this.ownerNodeId = ownerNodeId;
    }

    @Column(name="last_poll_time")
    public long getLastPollTime() {
        return lastPollTime;
    }

    public void setLastPollTime(long lastPollTime) {
        this.lastPollTime = lastPollTime;
    }

    @Column(name="last_message_id")
    public Long getLastMessageId() {
        return lastMessageId;
    }

    public void setLastMessageId(Long lastMessageId) {
        this.lastMessageId = lastMessageId;
    }
}
