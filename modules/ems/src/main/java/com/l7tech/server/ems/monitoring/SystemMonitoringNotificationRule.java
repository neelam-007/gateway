/*
 * Copyright (C) 2009 Layer 7 Technologies Inc.
 */
package com.l7tech.server.ems.monitoring;

import com.l7tech.common.io.NonCloseableOutputStream;
import com.l7tech.common.http.HttpMethod;
import com.l7tech.objectmodel.imp.NamedEntityImp;
import com.l7tech.server.ems.enterprise.JSONConstants;
import com.l7tech.server.management.config.monitoring.*;
import com.l7tech.util.BufferPoolByteArrayOutputStream;
import com.l7tech.util.HexUtils;
import com.l7tech.util.ResourceUtils;
import org.hibernate.annotations.Proxy;
import org.mortbay.util.ajax.JSON;

import javax.persistence.*;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.ByteArrayInputStream;
import java.util.*;

@XmlRootElement
@Entity
@Proxy(lazy=false)
@Table(name="system_monitoring_notification_rule")
public class SystemMonitoringNotificationRule extends NamedEntityImp implements JSON.Convertible {

    private String guid;
    private String type;
    private Map<String, Object> paramsProps = new HashMap<String, Object>();
    private String paramsPropsXml;

    @Deprecated // For serialization and persistence only
    public SystemMonitoringNotificationRule() {
    }

    public SystemMonitoringNotificationRule(String name, String type, Map<String, Object> paramsProps) {
        setGuid(UUID.randomUUID().toString());
        _name = name;
        this.type = type;
        this.paramsProps = paramsProps;
    }

    public void copyFrom(String name, String type, Map<String, Object> paramsProps) {
        _name = name;
        this.type = type;
        this.paramsProps = paramsProps;
        paramsPropsXml = null;
    }

    @Column(name="guid", length=36, unique=true, nullable=false)
    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    @Column(name="type", length=20, nullable=false)
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    /**
     * Obtain a view of this rule as an HttpNotificationRule, if applicable.
     *
     * @param config the MonitoringConfiguration to use when creating the rule.
     * @return an appropriately-configured HttpNotificationRule instance, or null if this
     *         rule does not represent an HTTP request notification. 
     */
    public HttpNotificationRule asHttpNotificationRule(MonitoringConfiguration config) {
        if (!(JSONConstants.NotificationType.HTTP_REQUEST.equals(getType())))
            return null;

        HttpNotificationRule ret = new HttpNotificationRule(config);
        ret.setName(getName());
        ret.setOid(getOid());
        ret.setAuthInfo(null); // todo: where to get username and password to create authInfo?
        ret.setContentType((String)getParamProp(JSONConstants.NotificationHttpRequestParams.CONTENT_TYPE));
        ret.setMethod(obtainHttpMethod((String)getParamProp(JSONConstants.NotificationHttpRequestParams.HTTP_METHOD)));
        ret.setRequestBody((String)getParamProp(JSONConstants.NotificationHttpRequestParams.BODAY));
        ret.setUrl((String)getParamProp(JSONConstants.NotificationHttpRequestParams.URL));
        return ret;
    }

    /**
     * Obtain a view of this rule as an SnmpTrapNotificationRule, if applicable.
     *
     * @param config the MonitoringConfiguration to use when creating the rule.
     * @return an appropriately-configured SnmpTrapNotificationRule instance, or null if this
     *         rule does not represent an SNMP trap notification.
     */
    public SnmpTrapNotificationRule asSnmpTrapNotificationRule(MonitoringConfiguration config) {
        if (!(JSONConstants.NotificationType.SNMP_TRAP.equals(getType())))
            return null;

        SnmpTrapNotificationRule ret = new SnmpTrapNotificationRule(config);
        ret.setName(getName());
        ret.setOid(getOid());
        ret.setCommunity((String)getParamProp(JSONConstants.NotificationSnmpTrapParams.COMMUNITY));
        ret.setOidSuffix(Integer.valueOf(getParamProp(JSONConstants.NotificationSnmpTrapParams.OIDFUFFIX).toString()));
        ret.setPort(Integer.valueOf(getParamProp(JSONConstants.NotificationSnmpTrapParams.PORT).toString()));
        ret.setSnmpHost((String)getParamProp(JSONConstants.NotificationSnmpTrapParams.HOST));
        ret.setText((String)getParamProp(JSONConstants.NotificationSnmpTrapParams.TEXTDATA));
        return ret;
    }

    /**
     * Obtain a view of this rule as an EmailNotificationRule, if applicable.
     *
     * @param config the MonitoringConfiguration to use when creating the rule.
     * @return an appropriately-configured EmailNotificationRule instance, or null if this
     *         rule does not represent an email notification.
     */
    public EmailNotificationRule asEmailNotificationRule(MonitoringConfiguration config) {
        if (!(JSONConstants.NotificationType.E_MAIL.equals(getType())))
            return null;

        EmailNotificationRule ret = new EmailNotificationRule(config);
        ret.setName(getName());
        ret.setOid(getOid());
        ret.setAuthInfo(obtainAuthInfo());
        ret.setBcc(toStringList((String)getParamProp(JSONConstants.NotificationEmailParams.BCC)));
        ret.setCc(toStringList((String)getParamProp(JSONConstants.NotificationEmailParams.CC)));
        ret.setCryptoType(obtainCryptoType((String)getParamProp(JSONConstants.NotificationEmailParams.PROTOCOL)));
        ret.setFrom("postmaster"); // todo: get the real "from" after UI adds "From" field.
        ret.setPort(Integer.valueOf(getParamProp(JSONConstants.NotificationEmailParams.PORT).toString()));
        ret.setSmtpHost((String)getParamProp(JSONConstants.NotificationEmailParams.HOST));
        ret.setSubject((String)getParamProp(JSONConstants.NotificationEmailParams.SUBJECT));
        ret.setText((String)getParamProp(JSONConstants.NotificationEmailParams.BODY));
        ret.setTo(toStringList((String)getParamProp(JSONConstants.NotificationEmailParams.TO)));
        return ret;
    }

    /**
     * for serialization by axis and hibernate only.
     * to get the properties, call getProperty
     */
    @Column(name="parameters_properties", length=Integer.MAX_VALUE)
    @Lob
    public String getSerializedSettingsProps() throws java.io.IOException {
        if (paramsPropsXml == null) {
            // if no props, return empty string
            if (paramsProps.size() < 1) {
                paramsPropsXml = "";
            } else {
                BufferPoolByteArrayOutputStream output = null;
                java.beans.XMLEncoder encoder = null;
                try {
                    output = new BufferPoolByteArrayOutputStream();
                    encoder = new java.beans.XMLEncoder(new NonCloseableOutputStream(output));
                    encoder.writeObject(paramsProps);
                    encoder.close(); // writes closing XML tag
                    encoder = null;
                    paramsPropsXml = output.toString("UTF-8");
                }
                finally {
                    if(encoder!=null) encoder.close();
                    ResourceUtils.closeQuietly(output);
                }
            }
        }

        return paramsPropsXml;
    }

    /*For JAXB processing. Needed a way to get this identity provider to recreate it's internal
    * paramsPropsXml after setting a property after it has been unmarshalled*/
    public void recreateSerializedSettingsProps() throws java.io.IOException{
        paramsPropsXml = null;
        this.getSerializedSettingsProps();
    }

    public Map<String, Object> obtainParamsProps() {
        return paramsProps;
    }

    /**
     * for serialization by axis and hibernate only.
     * to set the properties, call setProperty
     */
    public void setSerializedSettingsProps(String serializedProps) {
        paramsPropsXml = serializedProps;
        if (serializedProps == null || serializedProps.length() < 2) {
            paramsProps.clear();
        } else {
            ByteArrayInputStream in = new ByteArrayInputStream(HexUtils.encodeUtf8(serializedProps));
            java.beans.XMLDecoder decoder = new java.beans.XMLDecoder(in);
            paramsProps = (Map<String, Object>) decoder.readObject();
        }
    }

    public Object getParamProp(String name) {
        return paramsProps.get(name);
    }

    protected void setParamProp(String name, Object value) {
        paramsProps.put(name, value);
        paramsPropsXml = null;
    }

    public void toJSON(JSON.Output output) {
        output.add(JSONConstants.ID, guid);
        output.add(JSONConstants.NAME, getName());
        output.add(JSONConstants.TYPE, type);
        output.add(JSONConstants.NotifiationRule.PARAMS, obtainParamsProps());
    }

    public void fromJSON(Map map) {
        throw new UnsupportedOperationException("Mapping from JSON not supported.");
    }

    private AuthInfo obtainAuthInfo() {
        AuthInfo authInfo = null;
        boolean requireAuth = (Boolean)getParamProp(JSONConstants.NotificationEmailParams.REQUIRES_AUTH);
        if (requireAuth) {
            String username = (String)getParamProp(JSONConstants.NotificationEmailParams.USERNAME);
            char[] password = ((String)getParamProp(JSONConstants.NotificationEmailParams.PASSWORD)).toCharArray();
            authInfo = new AuthInfo(username, password);
        }

        return authInfo;
    }

    private List<String> toStringList(String str) {
        List<String> list = new ArrayList<String>();
        list.addAll(Arrays.asList(str.split(",")));
        return list;
    }

    private EmailNotificationRule.CryptoType obtainCryptoType(String protocol) {
        if (protocol == null) {
            return null;
        } else if (protocol.equals(JSONConstants.NotificationEmailProtocol.PLAIN_SMTP)) {
            return EmailNotificationRule.CryptoType.PLAIN;
        } else if (protocol.equals(JSONConstants.NotificationEmailProtocol.SMTP_OVER_SSL)) {
            return EmailNotificationRule.CryptoType.SSL;
        } else if (protocol.equals(JSONConstants.NotificationEmailProtocol.SMTP_WITH_STARTTLS)) {
            return EmailNotificationRule.CryptoType.STARTTLS;
        } else {
            return null;
        }
    }

    private HttpMethod obtainHttpMethod(String type) {
        if (type == null) {
            return HttpMethod.OTHER;
        } else if (type.equals(JSONConstants.NotificationHttpMethodType.GET)) {
            return HttpMethod.GET;
        } else if (type.equals(JSONConstants.NotificationHttpMethodType.POST)) {
            return HttpMethod.POST;
        } else if (type.equals(JSONConstants.NotificationHttpMethodType.PUT)) {
            return HttpMethod.PUT;
        } else if (type.equals(JSONConstants.NotificationHttpMethodType.DELETE)) {
            return HttpMethod.DELETE;
        } else if (type.equals(JSONConstants.NotificationHttpMethodType.HEAD)) {
            return HttpMethod.HEAD;
        } else if (type.equals(JSONConstants.NotificationHttpMethodType.OTHER)) {
            return HttpMethod.OTHER;
        } else {
            return HttpMethod.OTHER;
        }
    }
}
