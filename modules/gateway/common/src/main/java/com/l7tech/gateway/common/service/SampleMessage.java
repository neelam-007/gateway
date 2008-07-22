package com.l7tech.gateway.common.service;

import com.l7tech.objectmodel.imp.NamedEntityImp;

/**
 * Holds a sample message for a {@link PublishedService}.
 */
public class SampleMessage extends NamedEntityImp {
    public static final String ATTR_SERVICE_OID = "serviceOid";

    public SampleMessage(long serviceOid, String name, String operationName, String xml) {
        // If the serviceOid is -1, it means that sample messages are created for a policy.
        if (serviceOid != -1) {
            this.serviceOid = serviceOid;
        }
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
    public Long getServiceOid() {
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

    public void setServiceOid(Long serviceOid) {
        this.serviceOid = serviceOid;
    }

    public void setXml(String xml) {
        this.xml = xml;
    }

    public void setOperationName(String operationName) {
        this.operationName = operationName;
    }

    private Long serviceOid;
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
