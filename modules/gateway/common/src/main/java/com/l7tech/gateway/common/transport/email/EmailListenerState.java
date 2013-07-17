package com.l7tech.gateway.common.transport.email;

import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.GoidEntity;
import com.l7tech.objectmodel.PersistentEntity;

import javax.persistence.*;
import javax.persistence.Entity;
import javax.persistence.Table;

import com.l7tech.objectmodel.imp.GoidEntityImp;
import org.hibernate.annotations.*;

import java.io.Serializable;

/**
 * The state bean of the email listener.
 */
@Entity
@Proxy(lazy=false)
@Table(name="email_listener_state")
public class EmailListenerState  extends GoidEntityImp implements  Serializable{
    private String ownerNodeId;
    private long lastPollTime;
    private Long lastMessageId;
    private EmailListener emailListener;

    public EmailListenerState() {
    }

    public EmailListenerState(EmailListener emailListener) {
        this.emailListener = emailListener;
    }

    public EmailListenerState(EmailListener emailListener, String ownerNodeId, long lastPollTime, Long lastMessageId) {
        this(emailListener);
        this.ownerNodeId = ownerNodeId;
        this.lastPollTime = lastPollTime;
        this.lastMessageId = lastMessageId;
    }

    @OneToOne(optional=false)
    @JoinColumn(name="email_listener_goid", nullable=false)
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
        return super.getVersion();
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
