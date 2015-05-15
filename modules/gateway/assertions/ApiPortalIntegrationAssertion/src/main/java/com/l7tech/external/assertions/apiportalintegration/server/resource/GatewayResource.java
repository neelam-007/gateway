package com.l7tech.external.assertions.apiportalintegration.server.resource;

import javax.xml.bind.annotation.*;

/**
 * A portal Gateway Status which has an xml representation.
 */
@XmlRootElement(name = "Gateway", namespace = JAXBResourceMarshaller.NAMESPACE)
@XmlAccessorType(XmlAccessType.NONE)
public class GatewayResource extends Resource {
    @XmlElement(name = "Api", namespace = JAXBResourceMarshaller.NAMESPACE)
    private GatewayStatResource api = new GatewayStatResource();
    @XmlElement(name = "ApiPlan", namespace = JAXBResourceMarshaller.NAMESPACE)
    private GatewayStatResource apiPlan = new GatewayStatResource();
    @XmlElement(name = "ApiKey", namespace = JAXBResourceMarshaller.NAMESPACE)
    private GatewayStatResource apiKey = new GatewayStatResource();
    @XmlElement(name = "ApiLegacyKey", namespace = JAXBResourceMarshaller.NAMESPACE)
    private GatewayStatResource apiLegacyKey = new GatewayStatResource();
    @XmlElement(name = "AccountPlans", namespace = JAXBResourceMarshaller.NAMESPACE)
    private GatewayStatResource accountPlan = new GatewayStatResource();
    @XmlElement(name = "ApiFragments", namespace = JAXBResourceMarshaller.NAMESPACE)
    private GatewayStatResource apiFragment = new GatewayStatResource();

    public GatewayStatResource getApi() {
        return api;
    }

    public void setApi(final GatewayStatResource api) {
        if (api == null) {
            this.api = new GatewayStatResource();
        } else {
            this.api = api;
        }
    }

    public GatewayStatResource getApiPlan() {
        return apiPlan;
    }

    public void setApiPlan(final GatewayStatResource apiPlan) {
        if (api == null) {
            this.apiPlan = new GatewayStatResource();
        } else {
            this.apiPlan = apiPlan;
        }
    }

    public GatewayStatResource getApiKey() {
        return apiKey;
    }

    public void setApiKey(final GatewayStatResource apiKey) {
        if (apiKey == null) {
            this.apiKey = new GatewayStatResource();
        } else {
            this.apiKey = apiKey;
        }
    }

    public GatewayStatResource getApiLegacyKey() {
        return apiLegacyKey;
    }

    public void setApiLegacyKey(final GatewayStatResource apiLegacyKey) {
        if (apiLegacyKey == null) {
            this.apiLegacyKey = new GatewayStatResource();
        } else {
            this.apiLegacyKey = apiLegacyKey;
        }
    }

    public GatewayStatResource getAccountPlan() {
        return accountPlan;
    }

    public void setAccountPlan(final GatewayStatResource accountPlans) {
        if (accountPlans == null) {
            this.accountPlan = new GatewayStatResource();
        } else {
            this.accountPlan = accountPlans;
        }
    }

    public GatewayStatResource getApiFragment() {
        return apiFragment;
    }

    public void setApiFragment(final GatewayStatResource apiFragment) {
        if (apiFragment == null) {
            this.apiFragment = new GatewayStatResource();
        } else {
            this.apiFragment = apiFragment;
        }
    }
}
