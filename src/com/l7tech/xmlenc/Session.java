package com.l7tech.xmlenc;


/**
 * User: flascell
 * Date: Aug 25, 2003
 * Time: 3:43:45 PM
 *
 * XML-ENC session
 */
public class Session {

    /**
     * Create a new Session, if you are the SessionManager.  Otherwise, use the SessionManager if you are
     * running on the SSG, or the SsgSessionManager if you are running on the client proxy.
     */
    Session() {
        creationTimestamp = System.currentTimeMillis();
    }

    /**
     * Create a new Session with the given state.  Used by the SsgSessionManager on the client to fill in
     * session information obtained from the session servlet.
     *
     * @param id                 id assigned to this session by the server
     * @param creationTimestamp  timestamp of session creation
     * @param keyreq             key to be used for encrypting requests
     * @param keyres             key to be used for encrypting responses
     */
    public Session(long id, long creationTimestamp, byte[] keyreq, byte[] keyres, long seq) {
        this.id = id;
        this.creationTimestamp = creationTimestamp;
        this.keyreq = keyreq;
        this.keyres = keyres;
        this.highestSeq = seq;
    }

    public long getHighestSeq() {
        return highestSeq;
    }

    public void setHighestSeq(long highestSeq) {
        this.highestSeq = highestSeq;
    }

    public byte[] getKeyReq() {
        return keyreq;
    }

    public void setKeyReq(byte[] key) {
        this.keyreq = key;
    }

    public byte[] getKeyRes() {
        return keyres;
    }

    public void setKeyRes(byte[] key) {
        this.keyres = key;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getCreationTimestamp() {
        return creationTimestamp;
    }

    public long getNrRequestsUsed() {
        return nrRequestsUsed;
    }

    public long incrementRequestsUsed() {
        return ++nrRequestsUsed;
    }

    /**
     * Used on the client for sequence numbers for signed requests within this session.
     * @return the next sequence number
     */
    public synchronized long nextSequenceNumber() {
        // TODO: if sequence number gets too big, could invalidate on client side rather than wait for the SSG to do so
        return ++highestSeq;
    }

    // ************************************************
    // PRIVATES
    // ************************************************

    private long id = 0;
    private long highestSeq = 0;
    private byte[] keyreq = null;
    private byte[] keyres = null;
    private long creationTimestamp = 0;
    private long nrRequestsUsed = 0;
}
