package com.l7tech.console.util;

import com.l7tech.objectmodel.Goid;
import com.l7tech.util.Pair;

import java.io.IOException;

/**
 * AbstractAuditMessages are displayed in the audit viewer.
 */
public abstract class AbstractAuditMessage implements Comparable {

    //- PUBLIC

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public abstract Goid getMsgNumber();

    public abstract long getTimestamp();

    public abstract String getSeverity();

    public abstract String getMsgDetails();

    public abstract String getReqId();

    public abstract String getNodeId();

    public String getServiceName() {
        return "";
    }

    public abstract String getSignature();

    public abstract Pair<byte[],byte[]> getSignatureDigest() throws IOException;

    @SuppressWarnings({"RedundantIfStatement"})
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AbstractAuditMessage that = (AbstractAuditMessage) o;

        if (getMsgNumber() != that.getMsgNumber()) return false;
        if (nodeName != null ? !nodeName.equals(that.nodeName) : that.nodeName != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result;

        result = getMsgNumber().hashCode();
        result = 31 * result + (nodeName != null ? nodeName.hashCode() : 0);
        return result;
    }

    @Override
    public int compareTo(Object o) {
        int compareValue;

        if (!(o instanceof AbstractAuditMessage)) {
            throw new IllegalStateException("Can only compare to other LogMessages ("+o.getClass()+")");
        }

        AbstractAuditMessage other = (AbstractAuditMessage) o;

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
                    compareValue = other.getMsgNumber().compareTo(getMsgNumber());
                }
            }
        }

        return compareValue;
    }

    //- PROTECTED

    protected AbstractAuditMessage() {
    }

    //- PRIVATE

    private String nodeName = "";  // gets filled in afterward

}
