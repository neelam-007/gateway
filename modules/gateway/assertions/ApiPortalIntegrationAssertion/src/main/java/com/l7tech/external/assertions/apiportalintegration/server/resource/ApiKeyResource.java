package com.l7tech.external.assertions.apiportalintegration.server.resource;

import org.apache.commons.lang.StringUtils;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * A portal API key which has an xml representation.
 */
@XmlRootElement(name = "ApiKey", namespace = JAXBResourceMarshaller.NAMESPACE)
@XmlAccessorType(XmlAccessType.NONE)
public class ApiKeyResource extends Resource {
    @XmlElement(name = "Key", namespace = JAXBResourceMarshaller.NAMESPACE)
    private String key = StringUtils.EMPTY;
    @XmlElement(name = "Status", namespace = JAXBResourceMarshaller.NAMESPACE)
    private String status = StringUtils.EMPTY;
    @XmlElement(name = "AccountPlanMappingId", namespace = JAXBResourceMarshaller.NAMESPACE)
    private String accountPlanMappingId = StringUtils.EMPTY;
    @XmlElement(name = "LastUpdate", namespace = JAXBResourceMarshaller.NAMESPACE)
    @XmlSchemaType(name = "timestamp")
    private Date lastUpdate;
    @XmlElement(name = "CustomMetaData", namespace = JAXBResourceMarshaller.NAMESPACE)
    private String customMetaData = StringUtils.EMPTY;
    @XmlElement(name = "ApplicationId", namespace = JAXBResourceMarshaller.NAMESPACE)
    private String applicationId = StringUtils.EMPTY;


    /**
     * Key = api id, Value = plan id.
     */
    @XmlElement(name = "Apis", namespace = JAXBResourceMarshaller.NAMESPACE)
    @XmlJavaTypeAdapter(ApiIdPlanIdMapAdapter.class)
    private Map<String, String> apis = new HashMap<String, String>();

    @XmlElement(name = "Secret", namespace = JAXBResourceMarshaller.NAMESPACE)
    private String secret = StringUtils.EMPTY;

    @XmlElement(name = "Label", namespace = JAXBResourceMarshaller.NAMESPACE)
    private String label = StringUtils.EMPTY;

    @XmlElement(name = "Platform", namespace = JAXBResourceMarshaller.NAMESPACE)
    private String platform = StringUtils.EMPTY;

    @XmlElement(name = "Security", namespace = JAXBResourceMarshaller.NAMESPACE)
    private SecurityDetails security = new SecurityDetails();

    public String getKey() {
        return key;
    }

    public void setKey(final String key) {
        if (key != null) {
            this.key = key;
        } else {
            this.key = StringUtils.EMPTY;
        }
    }

    public Map<String, String> getApis() {
        return apis;
    }

    public void setApis(final Map<String, String> apis) {
        if (apis == null) {
            this.apis.clear();
        } else {
            this.apis = apis;
        }
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(final String secret) {
        if (secret != null) {
            this.secret = secret;
        } else {
            this.secret = StringUtils.EMPTY;
        }
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        if (status != null) {
            this.status = status;
        } else {
            this.status = StringUtils.EMPTY;
        }
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(final String label) {
        if (label != null) {
            this.label = label;
        } else {
            this.label = StringUtils.EMPTY;
        }
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(final String platform) {
        if (platform != null) {
            this.platform = platform;
        } else {
            this.platform = StringUtils.EMPTY;
        }
    }

    public SecurityDetails getSecurity() {
        return security;
    }

    public void setSecurity(final SecurityDetails security) {
        if (security == null) {
            this.security = new SecurityDetails();
        } else {
            this.security = security;
        }
    }

    public Date getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(final Date lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public String getAccountPlanMappingId() {
        return accountPlanMappingId;
    }

    public void setAccountPlanMappingId(final String accountPlanMappingId) {
        if (accountPlanMappingId != null) {
            this.accountPlanMappingId = accountPlanMappingId;
        } else {
            this.accountPlanMappingId = StringUtils.EMPTY;
        }
    }

    public String getCustomMetaData() {
        return customMetaData;
    }

    public void setCustomMetaData(final String customMetaData) {
        if (customMetaData != null) {
            this.customMetaData = customMetaData;
        } else {
            this.customMetaData = StringUtils.EMPTY;
        }
    }

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(final String applicationId) {
        if (applicationId != null) {
            this.applicationId = applicationId;
        } else {
            this.applicationId = StringUtils.EMPTY;
        }
    }

}
