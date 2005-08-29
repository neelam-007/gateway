package com.l7tech.service;

import com.l7tech.objectmodel.imp.NamedEntityImp;

/**
 * Holds a sample message for a {@link PublishedService}.
 */
public class SampleMessage extends NamedEntityImp {
    public SampleMessage(long serviceOid, String name, String operationName, String xml) {
        this.serviceOid = serviceOid;
        this._name = name;
        this.operationName = operationName;
        this.xml = xml;
    }

    /**
     * @return the XML contents of the message
     */
    public String getXml() {
        return xml;
    }

    /**
     * @return the OID of the {@link PublishedService} to which this message belongs
     */
    public long getServiceOid() {
        return serviceOid;
    }

    /**
     * @return the name of the operation under which this message was categorized
     */
    public String getOperationName() {
        return operationName;
    }

    public SampleMessage() {
    }

    public void setServiceOid(long serviceOid) {
        this.serviceOid = serviceOid;
    }

    public void setXml(String xml) {
        this.xml = xml;
    }

    public void setOperationName(String operationName) {
        this.operationName = operationName;
    }

    private long serviceOid;
    private String xml;
    private String operationName;

    public void copyFrom(SampleMessage sm) {
        this._oid = sm._oid;
        this._name = sm._name;
        this.serviceOid = sm.serviceOid;
        this.operationName = sm.operationName;
        this.xml = sm.xml;
    }
}
