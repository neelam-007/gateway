package com.l7tech.message;

import com.l7tech.cluster.DistributedMessageIdManager;
import com.l7tech.common.RequestId;
import com.l7tech.common.security.xml.processor.ProcessorResult;
import com.l7tech.identity.User;
import com.l7tech.policy.assertion.RoutingStatus;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.server.MessageProcessor;
import com.l7tech.server.RequestIdGenerator;
import com.l7tech.server.util.MessageIdManager;

import java.util.logging.Level;

/**
 * Encapsulates a SOAP request. Not thread-safe. Don't forget to call close() when you're done!
 *
 * @version $Revision$
 */
public abstract class SoapRequest extends XmlMessageAdapter implements SoapMessage, XmlRequest {
    public SoapRequest( TransportMetadata metadata ) {
        super( metadata );
        _id = RequestIdGenerator.next();
        MessageProcessor.setCurrentRequest(this);
    }

    public MessageIdManager getMessageIdManager() {
        return DistributedMessageIdManager.getInstance();
    }

    public ProcessorResult getWssProcessorOutput() {
        return wssRes;
    }

    public void setWssProcessorOutput(ProcessorResult res) {
        wssRes = res;
    }

    public RequestId getId() {return _id;}

    public LoginCredentials getPrincipalCredentials() {
        return _principalCredentials;
    }

    public void setPrincipalCredentials( LoginCredentials pc ) {
        _principalCredentials = pc;
    }

    public boolean isAuthenticated() {
        return _authenticated;
    }

    public void setAuthenticated(boolean authenticated) {
        _authenticated = authenticated;
    }

    public User getUser() {
        return _user;
    }

    public void setUser( User user ) {
        _user = user;
    }

    public void setRoutingStatus( RoutingStatus status ) {
        _routingStatus = status;
    }

    public RoutingStatus getRoutingStatus() {
        return _routingStatus;
    }

    public Level getAuditLevel() {
        return auditLevel;
    }

    public void setAuditLevel( Level auditLevel ) {
        if (auditLevel == null || this.auditLevel == null) return;
        if (auditLevel.intValue() <= this.auditLevel.intValue()) return;
        this.auditLevel = auditLevel;
    }

    public boolean isAuditSaveResponse() {
        return auditSaveResponse;
    }

    public void setAuditSaveResponse(boolean auditSaveResponse) {
        this.auditSaveResponse = auditSaveResponse;
    }

    public boolean isAuditSaveRequest() {
        return auditSaveRequest;
    }

    public void setAuditSaveRequest(boolean auditSaveRequest) {
        this.auditSaveRequest = auditSaveRequest;
    }

    protected RequestId _id;
    protected boolean _authenticated;
    protected User _user;
    protected RoutingStatus _routingStatus = RoutingStatus.NONE;

    // Set to lowest by default so it can be overridden by MessageProcessor
    protected Level auditLevel = Level.ALL;
    protected boolean auditSaveRequest = false;
    protected boolean auditSaveResponse = false;

    /** The cached XML document. */
    protected LoginCredentials _principalCredentials;
    
    protected ProcessorResult wssRes = null;
}
