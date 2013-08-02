package com.l7tech.gateway.common.service;

import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.GoidEntity;
import com.l7tech.objectmodel.imp.ZoneableNamedGoidEntityImp;

/**
 * Holds a sample message for a {@link PublishedService}.
 */
public class SampleMessage extends ZoneableNamedGoidEntityImp {
    public static final String ATTR_SERVICE_GOID = "serviceGoid";

    public SampleMessage(Goid serviceGoid, String name, String operationName, String xml) {
        // If the serviceGoid is the default, it means that sample messages are created for a policy.
        if (!GoidEntity.DEFAULT_GOID.equals(serviceGoid)) {
            this.serviceGoid = serviceGoid;
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
     * @return the GOID of the {@link PublishedService} to which this message belongs
     */
    public Goid getServiceGoid() {
        return serviceGoid;
    }

    /**
     * @return the name of the operation under which this message was categorized
     */
    public String getOperationName() {
        return operationName;
    }

    public SampleMessage() {
    }

    public void setServiceGoid(Goid serviceGoid) {
        this.serviceGoid = serviceGoid;
    }

    public void setXml(String xml) {
        this.xml = xml;
    }

    public void setOperationName(String operationName) {
        this.operationName = operationName;
    }

    private Goid serviceGoid;
    private String xml;
    private String operationName;

    public void copyFrom(SampleMessage sm) {
        setGoid(sm.getGoid());
        this._name = sm._name;
        this.serviceGoid = sm.serviceGoid;
        this.operationName = sm.operationName;
        this.xml = sm.xml;
        this.securityZone = sm.getSecurityZone();
    }
}
