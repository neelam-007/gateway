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
    private String businessServiceName;
    private String businessServiceKey;
    private String businessEntityKey;
    private String wsdlServiceName;
    private long uddiRegistryOid;
    private boolean gatewayWsdl;
    private boolean likelyGatewayWsdl;
    private String accessPointURL;

    public WsdlPortInfoImpl() {
    }

    public WsdlPortInfoImpl(String name, String wsdlUrl) {
        this.wsdlPortName = name;
        this.wsdlUrl = wsdlUrl;
    }

    /**
     * Validate that all require information was found
     * @return null if no errors, otherwise a String explaining the problem
     */
    public String validate(){

        if(!checkStringValue(wsdlUrl)) return "WSDL URL was not set";
//        if(!checkStringValue(wsdlPortName)) return "wsdl:port name from bindingTemplate instance parameter not set";
//        if(!checkStringValue(wsdlPortBinding)) return "wsdl:binding referenced from wsdl:port not set";
        if(!checkStringValue(businessServiceKey)) return "serviceKey not set";
//        if(!checkStringValue(businessEntityKey)) return "businessKey not set";
//        if(!checkStringValue(wsdlServiceName)) return "wsdl:service name not set";
        if(!checkStringValue(accessPointURL)) return "accessPointURL name not set";
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
    public long getUddiRegistryOid() {
        return uddiRegistryOid;
    }

    @Override
    public void setUddiRegistryOid(long uddiRegistryOid) {
        this.uddiRegistryOid = uddiRegistryOid;
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

    @Override
    public String getWsdlPortBinding() {
        return wsdlPortBinding;
    }

    public void setWsdlPortName(String wsdlPortName) {
        this.wsdlPortName = wsdlPortName;
    }

    public void setWsdlPortBinding(String wsdlPortBinding) {
        this.wsdlPortBinding = wsdlPortBinding;
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
    public String getAccessPointURL() {
        return accessPointURL;
    }

    public void setAccessPointURL(String accessPointURL) {
        this.accessPointURL = accessPointURL;
    }
}
