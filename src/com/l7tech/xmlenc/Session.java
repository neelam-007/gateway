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
     * get a session from the SessionManager
     */
    Session() {
        creationTimestamp = System.currentTimeMillis();
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
