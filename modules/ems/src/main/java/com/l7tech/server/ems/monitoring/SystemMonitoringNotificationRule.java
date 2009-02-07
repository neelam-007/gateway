package com.l7tech.server.ems.monitoring;

import com.l7tech.objectmodel.imp.NamedEntityImp;
import com.l7tech.util.BufferPoolByteArrayOutputStream;
import com.l7tech.util.ResourceUtils;
import com.l7tech.util.HexUtils;
import com.l7tech.common.io.NonCloseableOutputStream;
import com.l7tech.server.ems.enterprise.JSONConstants;

import javax.xml.bind.annotation.XmlRootElement;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Column;
import javax.persistence.Lob;

import org.mortbay.util.ajax.JSON;
import org.hibernate.annotations.Proxy;

import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import java.io.ByteArrayInputStream;

/**
 * @Copyright: Layer 7 Tech. Inc.
 * @Author: ghuang
 * @Date: Jan 28, 2009
 * @since Enterprise Manager 1.0
 */
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
