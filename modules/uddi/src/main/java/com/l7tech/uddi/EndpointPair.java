package com.l7tech.uddi;

import com.l7tech.util.XmlSafe;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * <p/>
 * EndpointPair is a Cluster / Gateway endpoint URL for a service and a WSDL URL for the same service.
 * A service may have more than one endpoint e.g. http + https, and the same for the WSDL
 * URL. The WSDL URL will be used by each tModel that each published bindingTemplate will reference, which is why
 * these URLs are kept together.
 *
 *
 * @author darmstrong
 */
@XmlSafe(allowAllConstructors = true, allowAllSetters = true)
public class EndpointPair implements Serializable {

    // - PUBLIC

    enum scheme{HTTP, HTTPS, UNKNOWN}
    
    public EndpointPair() {
    }

    public EndpointPair(String endPointUrl, String wsdlUrl) {
        validateUrl(endPointUrl, "endPointUrl");
        validateUrl(wsdlUrl, "wsdlUrl");

        this.endPointUrl = endPointUrl;
        this.wsdlUrl = wsdlUrl;
    }

    public scheme getEndpointScheme(){
        if(endPointUrl.startsWith("https")) return scheme.HTTPS;//must be first listed!
        if(endPointUrl.startsWith("http")) return scheme.HTTP;
        return scheme.UNKNOWN;
    }

    public scheme getWsdlUrlScheme(){
        if(wsdlUrl.startsWith("https")) return scheme.HTTPS;//must be first listed!
        if(wsdlUrl.startsWith("http")) return scheme.HTTP;
        return scheme.UNKNOWN;
    }

    public static scheme getScheme(String url){
        EndpointPair pair = new EndpointPair();
        pair.setEndPointUrl(url);
        return pair.getEndpointScheme();
    }

    public String getEndPointUrl() {
        return endPointUrl;
    }

    public void setEndPointUrl(String endPointUrl) {
        validateUrl(endPointUrl, "endPointUrl");
        this.endPointUrl = endPointUrl;
    }

    public String getWsdlUrl() {
        return wsdlUrl;
    }

    public void setWsdlUrl(String wsdlUrl) {
        validateUrl(wsdlUrl, "wsdlUrl");
        this.wsdlUrl = wsdlUrl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EndpointPair that = (EndpointPair) o;

        if (endPointUrl != null ? !endPointUrl.equals(that.endPointUrl) : that.endPointUrl != null) return false;
        if (wsdlUrl != null ? !wsdlUrl.equals(that.wsdlUrl) : that.wsdlUrl != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = endPointUrl != null ? endPointUrl.hashCode() : 0;
        result = 31 * result + (wsdlUrl != null ? wsdlUrl.hashCode() : 0);
        return result;
    }


    @Override
    public String toString() {
        return "EndpointPair{" +
                "endPointUrl='" + endPointUrl + '\'' +
                ", wsdlUrl='" + wsdlUrl + '\'' +
                '}';
    }

    // - PRIVATE

    private void validateUrl(String url, String paramName){
        if(url == null || url.trim().isEmpty())
            throw new IllegalArgumentException(paramName + " cannot be null or empty");
        try {
            new URL(url);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(paramName + " is not a valid URL: " + e.getMessage());
        }
    }

    private String endPointUrl;

    private String wsdlUrl;
}
