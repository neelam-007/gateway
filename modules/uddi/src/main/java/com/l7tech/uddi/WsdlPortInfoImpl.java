package com.l7tech.uddi;

import java.io.Serializable;

/**
 * <p> Copyright (C) 2005 Layer 7 Technologies Inc.</p>
 *
 * @author darmstrong
 */
public class WsdlPortInfoImpl implements WsdlPortInfo, Serializable {

    public final static WsdlPortInfo MAXED_OUT_SEARCH_RESULT = new WsdlPortInfoImpl(MAXED_OUT_UDDI_RESULTS_NAME, MAXED_OUT_UDDI_RESULTS_URL);
    public final static UDDINamedEntity MAXED_OUT_SEARCH_RESULT_ENTITY =
            new UDDINamedEntityImpl(MAXED_OUT_UDDI_RESULTS_NAME, MAXED_OUT_UDDI_RESULTS_URL);


    private String wsdlUrl;
    private String wsdlPortName;
    private String wsdlPortBinding;
    private String wsdlPortBindingNamespace;
    private String businessServiceName;
    private String businessServiceKey;
    private String businessEntityKey;
    private String businessEntityName;
    private String wsdlServiceName;
    private String uddiRegistryId;
    private boolean gatewayWsdl;
    private boolean likelyGatewayWsdl;
    private String accessPointURL;
    private long lastUddiMonitoredTimeStamp;
    private boolean wasWsdlPortSelected;

    public WsdlPortInfoImpl() {
    }

    /**
     * Only used to flag to the SSM that the search results yielded too many results
     * @param name String name of the Business Service - simply used for sorting WsdlPortInfoImpl's
     * @param wsdlUrl String flag URI checked in the SSM to determine if search results were truncated or not
     */
    private WsdlPortInfoImpl(String name, String wsdlUrl) {
        this.businessServiceName = name;
        this.wsdlUrl = wsdlUrl;
    }

    /**
     * Validate that all require properties were set with value values
     * @return null if no errors, otherwise a String explaining the problem
     */
    public String validate(){
        if(!checkStringValue(businessEntityKey)) return "businessKey property was not set";
        if(!checkStringValue(businessEntityName)) return "businessEntityName property was not set";
        if(!checkStringValue(businessServiceKey)) return "businessServiceKey property was not set";
        if(!checkStringValue(businessServiceName)) return "businessServiceName property was not set";
        if(!checkStringValue(wsdlServiceName)) return "wsdlServiceName property was not set";
        if(!checkStringValue(wsdlPortName)) return "wsdlPortName property was not set";
        if(!checkStringValue(wsdlPortBinding)) return "wsdlPortBinding property was not set";
        //ok for binding namespace to be null
        if(wsdlPortBindingNamespace != null){
            if(!checkStringValue(wsdlPortBindingNamespace)) return "wsdlPortBindingNamespace property was not set, when it contained a non null value";            
        }
        if(!checkStringValue(accessPointURL)) return "accessPointURL property was not set";
        if(!checkStringValue(wsdlUrl)) return "wsdlUrl property was not set";
        return null;
    }

    @Override
    public boolean isGatewayWsdl() {
        return gatewayWsdl;
    }

    public void setGatewayWsdl(boolean gatewayWsdl) {
        this.gatewayWsdl = gatewayWsdl;
    }

    @Override
    public boolean isLikelyGatewayWsdl() {
        return likelyGatewayWsdl;
    }

    public void setLikelyGatewayWsdl(boolean likelyGatewayWsdl) {
        this.likelyGatewayWsdl = likelyGatewayWsdl;
    }

    private boolean checkStringValue(final String value){
        if(value == null || value.trim().isEmpty()) return false;
        return true;
    }

    @Override
    public String getUddiRegistryId() {
        return uddiRegistryId;
    }

    @Override
    public void setUddiRegistryId(String uddiRegistryId) {
        this.uddiRegistryId = uddiRegistryId;
    }

    @Override
    public String getWsdlUrl() {
        return wsdlUrl;
    }

    public void setWsdlUrl(String wsdlUrl) {
        this.wsdlUrl = wsdlUrl;
    }

    @Override
    public String getWsdlPortName() {
        return wsdlPortName;
    }

    public void setWsdlPortName(String wsdlPortName) {
        this.wsdlPortName = wsdlPortName;
    }

    @Override
    public String getWsdlPortBinding() {
        return wsdlPortBinding;
    }

    public void setWsdlPortBinding(String wsdlPortBinding) {
        this.wsdlPortBinding = wsdlPortBinding;
    }

    @Override
    public String getWsdlPortBindingNamespace() {
        return wsdlPortBindingNamespace;
    }

    public void setWsdlPortBindingNamespace(String wsdlPortBindingNamespace) {
        this.wsdlPortBindingNamespace = wsdlPortBindingNamespace;
    }

    @Override
    public String getBusinessServiceName() {
        return businessServiceName;
    }

    public void setBusinessServiceName(String businessServiceName) {
        this.businessServiceName = businessServiceName;
    }

    @Override
    public String getWsdlServiceName() {
        return wsdlServiceName;
    }

    public void setWsdlServiceName(String wsdlServiceName) {
        this.wsdlServiceName = wsdlServiceName;
    }

    @Override
    public String getBusinessServiceKey() {
        return businessServiceKey;
    }

    public void setBusinessServiceKey(String businessServiceKey) {
        this.businessServiceKey = businessServiceKey;
    }

    @Override
    public String getBusinessEntityKey() {
        return businessEntityKey;
    }

    public void setBusinessEntityKey(String businessEntityKey) {
        this.businessEntityKey = businessEntityKey;
    }

    @Override
    public String getBusinessEntityName() {
        return businessEntityName;
    }

    public void setBusinessEntityName(String businessEntityName) {
        this.businessEntityName = businessEntityName;
    }

    @Override
    public String getAccessPointURL() {
        return accessPointURL;
    }

    public void setAccessPointURL(String accessPointURL) {
        this.accessPointURL = accessPointURL;
    }

    @Override
    public long getLastUddiMonitoredTimeStamp() {
        return lastUddiMonitoredTimeStamp;
    }

    public void setLastUddiMonitoredTimeStamp(long lastUddiMonitoredTimeStamp) {
        this.lastUddiMonitoredTimeStamp = lastUddiMonitoredTimeStamp;
    }

    @Override
    public boolean isWasWsdlPortSelected() {
        return wasWsdlPortSelected;
    }

    @Override
    public void setWasWsdlPortSelected(boolean wasWsdlPortSelected) {
        this.wasWsdlPortSelected = wasWsdlPortSelected;
    }
}
