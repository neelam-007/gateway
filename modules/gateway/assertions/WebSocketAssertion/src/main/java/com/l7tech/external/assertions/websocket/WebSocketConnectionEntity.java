package com.l7tech.external.assertions.websocket;

import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.PersistentEntity;
import com.l7tech.policy.GenericEntity;
import com.l7tech.util.GoidUpgradeMapper;
import com.l7tech.util.XmlSafe;


/**
 * User: cirving
 * Date: 6/4/12
 * Time: 11:43 AM
 */
@XmlSafe
public class WebSocketConnectionEntity extends GenericEntity {

    @XmlSafe
    public static enum ClientAuthType {
        NONE,
        OPTIONAL,
        REQUIRED
    }

    //Inbound fields
    private int inboundMaxIdleTime;
    private int inboundMaxConnections;
    // Updated in 8.0 for GOIDs
    private Goid inboundPolicyOID;
    private int inboundListenPort;
    private boolean inboundSsl;
    private Goid inboundPrivateKeyId;
    private String inboundPrivateKeyAlias;
    private ClientAuthType inboundClientAuth;
    private boolean removePortFlag;


    //Outbound Fields
    private String outboundUrl;
    private int outboundMaxIdleTime;
    // Updated in 8.0 for GOIDs
    private Goid outboundPolicyOID;
    private boolean outboundSsl;
    private Goid outboundPrivateKeyId;
    private String outboundPrivateKeyAlias;
    private boolean outboundClientAuthentication;
    private boolean loopback = true;


    public boolean isLoopback() {
        return loopback;
    }

    public int getInboundMaxConnections() {
        return inboundMaxConnections;
    }

    public void setInboundMaxConnections(int inboundMaxConnections) throws InvalidRangeException {
        if (inboundMaxConnections < 0 ) {
            throw new InvalidRangeException("Unable to set max connections below 0");
        }
        this.inboundMaxConnections = inboundMaxConnections;
    }

    public Goid getInboundPolicyOID() {
        return inboundPolicyOID;
    }

    public void setInboundPolicyOID(long inboundPolicyOID) {
        this.inboundPolicyOID = GoidUpgradeMapper.mapOid(EntityType.SERVICE, inboundPolicyOID);
    }
    public void setInboundPolicyOID(Goid inboundPolicyOID) {
        this.inboundPolicyOID = inboundPolicyOID;
    }

    public String getOutboundUrl() {
        return outboundUrl;
    }

    public void setOutboundUrl(String outboundUrl) {
        loopback = outboundUrl == null || "".equals(outboundUrl);
        this.outboundUrl = outboundUrl;
    }

    public int getInboundMaxIdleTime() {
        return inboundMaxIdleTime;
    }

    public void setInboundMaxIdleTime(int maxIdleTime) {
        this.inboundMaxIdleTime = maxIdleTime;
    }

    public int getOutboundMaxIdleTime() {
        return outboundMaxIdleTime;
    }

    public void setOutboundMaxIdleTime(int maxIdleTime) {
        this.outboundMaxIdleTime = maxIdleTime;
    }

    public Goid getOutboundPolicyOID() {
        return outboundPolicyOID;
    }

    public void setOutboundPolicyOID(long outboundPolicyOID) {
        this.outboundPolicyOID = GoidUpgradeMapper.mapOid(EntityType.SERVICE, outboundPolicyOID);
    }
    public void setOutboundPolicyOID(Goid outboundPolicyOID) {
        this.outboundPolicyOID = outboundPolicyOID;
    }

    public int getInboundListenPort() {
        return inboundListenPort;
    }

    public boolean isInboundSsl() {
        return inboundSsl;
    }

    public void setInboundSsl(boolean inboundSsl) {
        this.inboundSsl = inboundSsl;
        if (!inboundSsl) {
            clearInboundPrivateKey();
        }
    }

    public boolean isOutboundSsl() {
        return outboundSsl;
    }

    public void setOutboundSsl(boolean outboundSsl) {
        this.outboundSsl = outboundSsl;
        if (!outboundSsl) {
            clearOutboundPrivateKey();
        }
    }

    public boolean isOutboundClientAuthentication() {
        return outboundClientAuthentication;
    }

    public void setOutboundClientAuthentication(boolean outboundClientAuthentication) {
        this.outboundClientAuthentication = outboundClientAuthentication;
    }

    public Goid getInboundPrivateKeyId() {
        return inboundPrivateKeyId;
    }

    public void setInboundPrivateKeyId(Goid inboundPrivateKeyId) {
        this.inboundPrivateKeyId = inboundPrivateKeyId;
    }
    public void setInboundPrivateKeyId(long inboundPrivateKeyId) {
        this.inboundPrivateKeyId = GoidUpgradeMapper.mapOid(EntityType.SSG_KEY_ENTRY, inboundPrivateKeyId);
    }

    public String getInboundPrivateKeyAlias() {
        return inboundPrivateKeyAlias;
    }

    public void setInboundPrivateKeyAlias(String inboundPrivateKeyAlias) {
        this.inboundPrivateKeyAlias = inboundPrivateKeyAlias;
    }

    public Goid getOutboundPrivateKeyId() {
        return outboundPrivateKeyId;
    }

    public void setOutboundPrivateKeyId(Goid outboundPrivateKeyId) {
        this.outboundPrivateKeyId = outboundPrivateKeyId;
    }

    public void setOutboundPrivateKeyId(long outboundPrivateKeyId) {
        this.outboundPrivateKeyId = GoidUpgradeMapper.mapOid(EntityType.SSG_KEY_ENTRY, outboundPrivateKeyId);
    }

    public String getOutboundPrivateKeyAlias() {
        return outboundPrivateKeyAlias;
    }

    public void setOutboundPrivateKeyAlias(String outboundPrivateKeyAlias) {
        this.outboundPrivateKeyAlias = outboundPrivateKeyAlias;
    }

    public ClientAuthType getInboundClientAuth() {
        return inboundClientAuth;
    }

    public void setInboundClientAuth(ClientAuthType inboundClientAuth) {
        this.inboundClientAuth = inboundClientAuth;
    }

    public void setInboundListenPort(int inboundListenPort) throws InvalidRangeException {
        if (inboundListenPort < WebSocketConstants.MIN_LISTEN_PORT || inboundListenPort > WebSocketConstants.MAX_LISTEN_PORT) {
            throw new InvalidRangeException("Listening port must be in the range " + WebSocketConstants.MIN_LISTEN_PORT + " : " + WebSocketConstants.MAX_LISTEN_PORT);
        }
        this.inboundListenPort = inboundListenPort;
    }

    private void clearInboundPrivateKey() {
        inboundPrivateKeyId = PersistentEntity.DEFAULT_GOID;
        inboundPrivateKeyAlias = null;
    }

    private void clearOutboundPrivateKey() {
        outboundPrivateKeyId = PersistentEntity.DEFAULT_GOID;
        outboundPrivateKeyAlias = null;
    }

    public String toString() {
        return getName();
    }

    public boolean getRemovePortFlag() {
        return removePortFlag;
    }

    public void setRemovePortFlag(boolean removePortFlag){
        this.removePortFlag = removePortFlag;
    }
}
