package com.l7tech.portal.metrics;

public class PublishedService {
    private long id;
    private int version;
    private String name;
    private String policyXml;
    private Long policyId;
    private String wsdlUrl;
    private String wsdlXml;
    private int disabled;
    private int soap;
    private int internal;
    private String routingUri;
    private String defaultRoutingUrl;
    private String httpMethods;
    private int laxResolution;
    private int wssProcessing;
    private int tracing;
    private Long folderId;
    private String soapVersion;
    private String uuid;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPolicyXml() {
        return policyXml;
    }

    public void setPolicyXml(String policyXml) {
        this.policyXml = policyXml;
    }

    public Long getPolicyId() {
        return policyId;
    }

    public void setPolicyId(Long policyId) {
        this.policyId = policyId;
    }

    public String getWsdlUrl() {
        return wsdlUrl;
    }

    public void setWsdlUrl(String wsdlUrl) {
        this.wsdlUrl = wsdlUrl;
    }

    public String getWsdlXml() {
        return wsdlXml;
    }

    public void setWsdlXml(String wsdlXml) {
        this.wsdlXml = wsdlXml;
    }

    public int getDisabled() {
        return disabled;
    }

    public void setDisabled(int disabled) {
        this.disabled = disabled;
    }

    public int getSoap() {
        return soap;
    }

    public void setSoap(int soap) {
        this.soap = soap;
    }

    public int getInternal() {
        return internal;
    }

    public void setInternal(int internal) {
        this.internal = internal;
    }

    public String getRoutingUri() {
        return routingUri;
    }

    public void setRoutingUri(String routingUri) {
        this.routingUri = routingUri;
    }

    public String getDefaultRoutingUrl() {
        return defaultRoutingUrl;
    }

    public void setDefaultRoutingUrl(String defaultRoutingUrl) {
        this.defaultRoutingUrl = defaultRoutingUrl;
    }

    public String getHttpMethods() {
        return httpMethods;
    }

    public void setHttpMethods(String httpMethods) {
        this.httpMethods = httpMethods;
    }

    public int getLaxResolution() {
        return laxResolution;
    }

    public void setLaxResolution(int laxResolution) {
        this.laxResolution = laxResolution;
    }

    public int getWssProcessing() {
        return wssProcessing;
    }

    public void setWssProcessing(int wssProcessing) {
        this.wssProcessing = wssProcessing;
    }

    public int getTracing() {
        return tracing;
    }

    public void setTracing(int tracing) {
        this.tracing = tracing;
    }

    public Long getFolderId() {
        return folderId;
    }

    public void setFolderId(Long folderId) {
        this.folderId = folderId;
    }

    public String getSoapVersion() {
        return soapVersion;
    }

    public void setSoapVersion(String soapVersion) {
        this.soapVersion = soapVersion;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}
