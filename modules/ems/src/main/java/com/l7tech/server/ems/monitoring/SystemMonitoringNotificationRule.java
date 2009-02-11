/*
 * Copyright (C) 2009 Layer 7 Technologies Inc.
 */
package com.l7tech.server.ems.monitoring;

import com.l7tech.common.io.NonCloseableOutputStream;
import com.l7tech.common.http.HttpMethod;
import com.l7tech.objectmodel.imp.NamedEntityImp;
import com.l7tech.server.ems.enterprise.JSONConstants;
import com.l7tech.server.management.config.monitoring.HttpNotificationRule;
import com.l7tech.server.management.config.monitoring.MonitoringConfiguration;
import com.l7tech.server.management.config.monitoring.SnmpTrapNotificationRule;
import com.l7tech.server.management.config.monitoring.EmailNotificationRule;
import com.l7tech.util.BufferPoolByteArrayOutputStream;
import com.l7tech.util.HexUtils;
import com.l7tech.util.ResourceUtils;
import org.hibernate.annotations.Proxy;
import org.mortbay.util.ajax.JSON;

import javax.persistence.*;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
        // TODO convert into HttpNotificationRule by copying properties
        ret.setAuthInfo(null);
        ret.setContentType(null);
        ret.setMethod(HttpMethod.POST);
        ret.setRequestBody(null);
        ret.setUrl(null);
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
        // TODO convert into SnmpTrapNotificationRule by copying properties
        ret.setCommunity(null);
        ret.setOidSuffix(1);
        ret.setPort(0);
        ret.setSnmpHost(null);
        ret.setText(null);
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
        // TODO convert into a EmailNotificationRule by copying properties
        ret.setAuthInfo(null);   getParamProp(JSONConstants.NotificationEmailParams.REQUIRES_AUTHENTICATION); // TODO ??
        ret.setBcc(null);
        ret.setCc(null);
        ret.setCryptoType(null);
        ret.setFrom(null);
        ret.setPort(0);
        ret.setSmtpHost(null);
        ret.setSubject(null);
        ret.setText(null);
        ret.setTo(null);
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
}
