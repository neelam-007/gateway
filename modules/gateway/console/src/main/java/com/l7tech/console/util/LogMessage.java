package com.l7tech.console.util;

import java.io.IOException;

/**
 * LogMessages are displayed in the audit viewer.
 *
 * TODO [steve] rename after development in logging branch completed
 */
public abstract class LogMessage implements Comparable {

    //- PUBLIC

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public abstract long getMsgNumber();

    public abstract long getTimestamp();

    public abstract String getSeverity();

    public abstract String getMsgDetails();

    public abstract String getReqId();

    public abstract String getNodeId();

    public int getThreadID() {
        return 0;        
    }

    public String getServiceName() {
        return "";
    }

    public String getSignature(){
        return null;
    }

    public byte[] getSignatureDigest() throws IOException {
        return null;
    }

    @SuppressWarnings({"RedundantIfStatement"})
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LogMessage that = (LogMessage) o;

        if (getMsgNumber() != that.getMsgNumber()) return false;
        if (nodeName != null ? !nodeName.equals(that.nodeName) : that.nodeName != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result;

        result = Long.valueOf(getMsgNumber()).hashCode();
        result = 31 * result + (nodeName != null ? nodeName.hashCode() : 0);
        return result;
    }

    @Override
    public int compareTo(Object o) {
        int compareValue;

        if (!(o instanceof LogMessage)) {
            throw new IllegalStateException("Can only compare to other LogMessages ("+o.getClass()+")");
        }

        LogMessage other = (LogMessage) o;

        if ( this.equals(other) ) {
            compareValue = 0;
        } else {
            if ( other.getTimestamp() < getTimestamp() ) {
                compareValue = -1;
            } else if ( other.getTimestamp() > getTimestamp()) {
                compareValue = 1;
            } else {
                if (other.getNodeId().compareTo(getNodeId()) == -1) {
                    compareValue = -1;
                } else if (other.getNodeId().compareTo(getNodeId()) == 1) {
                    compareValue = 1;
                } else {
                    // this may not be meaningful for audit records, but is at least definitive
                    compareValue = Long.valueOf(other.getMsgNumber()).compareTo(getMsgNumber());
                }
            }
        }

        return compareValue;
    }

    //- PROTECTED

    protected LogMessage() {
    }

    //- PRIVATE

    private String nodeName = "";  // gets filled in afterward

}
