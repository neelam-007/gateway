/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.gateway.common.service;

import com.l7tech.objectmodel.Entity;

import java.util.List;
import java.util.Map;
import java.io.Serializable;

/** @author alex */
public class ServiceTemplate implements Entity, Serializable, Comparable<ServiceTemplate> {

    final private String name;

    final private String serviceDescriptorXml;

    /**
     * The desired default URL prefix for services published from this template, usually appended after "/ssg/".
     * e.g. "token" results in the URI "/ssg/token"
     */
    final private String defaultUriPrefix;

    /**
     * The default policy for services published from this template
     */
    final private String defaultPolicyXml;

    /**
     * The type of service for which this is a template
     */
    final private ServiceType type;

    final private List<ServiceDocument> serviceDocuments;

    /**
     * The subtype of service for which this is a template, if {@link #type} == {@link com.l7tech.gateway.common.service.ServiceType#OTHER_INTERNAL_SERVICE}.
     */
    final private Map<String,String> policyTags;
    final private String serviceDescriptorUrl;

    final private boolean isSoap;

    public ServiceTemplate(String name, String defaultUriPrefix, String serviceDescriptorXml, String serviceDescriptorUrl, String defaultPolicyXml, List<ServiceDocument> serviceDocuments, ServiceType type, Map<String, String> tags, boolean soap) {
        this.name = name;
        this.defaultUriPrefix = defaultUriPrefix;
        this.serviceDescriptorXml = serviceDescriptorXml;
        this.serviceDescriptorUrl = serviceDescriptorUrl;
        this.defaultPolicyXml = defaultPolicyXml;
        this.serviceDocuments = serviceDocuments;
        this.type = type;
        this.policyTags = tags;
        this.isSoap = soap;
    }

    public String getName() {
        return name;
    }

    public String getDefaultUriPrefix() {
        return defaultUriPrefix;
    }

    public String getDefaultPolicyXml() {
        return defaultPolicyXml;
    }

    public List<ServiceDocument> getServiceDocuments() {
        return serviceDocuments;
    }

    public ServiceType getType() {
        return type;
    }

    public Map<String, String> getPolicyTags() {
        return policyTags;
    }

    public int compareTo(ServiceTemplate o) {
        return this.getName().compareTo(o.getName());
    }

    public String toString() {
        return this.getName();
    }

    public String getServiceDescriptorXml() {
        return serviceDescriptorXml;
    }

    public String getServiceDescriptorUrl() {
        return serviceDescriptorUrl;
    }

    public boolean isSoap() {
        return isSoap;
    }

    @SuppressWarnings({ "RedundantIfStatement" })
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ServiceTemplate that = (ServiceTemplate)o;

        if (isSoap != that.isSoap) return false;
        if (defaultPolicyXml != null ? !defaultPolicyXml.equals(that.defaultPolicyXml) : that.defaultPolicyXml != null)
            return false;
        if (defaultUriPrefix != null ? !defaultUriPrefix.equals(that.defaultUriPrefix) : that.defaultUriPrefix != null)
            return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (policyTags != null ? !policyTags.equals(that.policyTags) : that.policyTags != null) return false;
        if (serviceDocuments != null ? !serviceDocuments.equals(that.serviceDocuments) : that.serviceDocuments != null)
            return false;
        if (type != that.type) return false;
        if (serviceDescriptorUrl != null ? !serviceDescriptorUrl.equals(that.serviceDescriptorUrl) : that.serviceDescriptorUrl != null) return false;
        if (serviceDescriptorXml != null ? !serviceDescriptorXml.equals(that.serviceDescriptorXml) : that.serviceDescriptorXml != null) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = (name != null ? name.hashCode() : 0);
        result = 31 * result + (serviceDescriptorXml != null ? serviceDescriptorXml.hashCode() : 0);
        result = 31 * result + (defaultUriPrefix != null ? defaultUriPrefix.hashCode() : 0);
        result = 31 * result + (defaultPolicyXml != null ? defaultPolicyXml.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (serviceDocuments != null ? serviceDocuments.hashCode() : 0);
        result = 31 * result + (policyTags != null ? policyTags.hashCode() : 0);
        result = 31 * result + (serviceDescriptorUrl != null ? serviceDescriptorUrl.hashCode() : 0);
        result = 31 * result + (isSoap ? 1 : 0);
        return result;
    }

    public String getId() {
        return null;
    }
}