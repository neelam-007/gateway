package com.l7tech.gateway.common.transport.email;

import com.l7tech.objectmodel.imp.PersistentEntityImp;
import com.l7tech.objectmodel.PersistentEntity;

import javax.persistence.*;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Proxy;

import java.io.Serializable;

/**
 * The state bean of the email listener.
 */
@Entity
@Proxy(lazy=false)
@Table(name="email_listener_state")
public class EmailListenerState implements PersistentEntity, Serializable {
    private Long Id;
    private int version;
    private String ownerNodeId;
    private long lastPollTime;
    private Long lastMessageId;
    private EmailListener emailListener;

    public EmailListenerState() {
        super();
    }

    public EmailListenerState(EmailListener emailListener) {
        super();
        this.emailListener = emailListener;
    }

    public EmailListenerState(EmailListener emailListener, String ownerNodeId, long lastPollTime, Long lastMessageId) {
        this(emailListener);
        this.ownerNodeId = ownerNodeId;
        this.lastPollTime = lastPollTime;
        this.lastMessageId = lastMessageId;
    }

    @OneToOne(optional=false)
    @JoinColumn(name="email_listener_id", nullable=false)
    public EmailListener getEmailListener() {
        return emailListener;
    }

    public void setEmailListener(EmailListener emailListener) {
        this.emailListener = emailListener;
    }

    @Override
    @Version
    @Column(name="version")
    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    @Override
    @Transient
    public long getOid() {
        return (Id == null) ? DEFAULT_OID : Id;
    }

    @Override
    public void setOid(long oid) {
        setObjectId(oid == DEFAULT_OID ? null : oid);
    }

    @Id
    @Column(name="objectid", nullable=false, updatable=false)
    @GenericGenerator( name="generator", strategy = "hilo" )
    @GeneratedValue(strategy=GenerationType.SEQUENCE, generator = "generator")
    public Long getObjectId() {
        return Id;
    }

    public void setObjectId(Long id) {
        Id = id;
    }

    @Transient
    public Long getOidAsLong() {
        return getOid();
    }

    @Transient
    public String getId() {
        return Long.toString(getOid());
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

    public void copyTo(EmailListenerState state) {
        this.lastMessageId = state.getLastMessageId();
        this.lastPollTime = state.getLastPollTime();
        this.ownerNodeId = state.getOwnerNodeId();
    }
}
