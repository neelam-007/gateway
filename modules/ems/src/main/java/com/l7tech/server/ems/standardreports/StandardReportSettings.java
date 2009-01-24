package com.l7tech.server.ems.standardreports;

import com.l7tech.objectmodel.imp.NamedEntityImp;
import com.l7tech.common.io.BufferPoolByteArrayOutputStream;
import com.l7tech.common.io.NonCloseableOutputStream;
import com.l7tech.util.ResourceUtils;
import com.l7tech.util.HexUtils;
import com.l7tech.server.ems.enterprise.JSONConstants;

import javax.persistence.*;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Map;
import java.util.HashMap;
import java.io.ByteArrayInputStream;

import org.mortbay.util.ajax.JSON;

/**
 * Entity encapsulating all settings of a standard report
 *
 * @Copyright: Layer 7 Tech. Inc.
 * @Author: ghuang
 * @Date: Jan 20, 2009
 */
@XmlRootElement
@Entity
@Table(name="standard_report_settings")
public class StandardReportSettings extends NamedEntityImp implements JSON.Convertible {

    private Map<String, Object> settingsProps = new HashMap<String, Object>();
    private String settingsPropsXml;

    @Deprecated // For serialization and persistence only
    public StandardReportSettings() {}

    public StandardReportSettings(String name, Map settingsProps) {
        _name = name;
        this.settingsProps = settingsProps;
    }

    /**
     * for serialization by axis and hibernate only.
     * to get the properties, call getProperty
     */
    @Column(name="settings_properties", length=Integer.MAX_VALUE)
    @Lob
    public String getSerializedSettingsProps() throws java.io.IOException {
        if (settingsPropsXml == null) {
            // if no props, return empty string
            if (settingsProps.size() < 1) {
                settingsPropsXml = "";
            } else {
                BufferPoolByteArrayOutputStream output = null;
                java.beans.XMLEncoder encoder = null;
                try {
                    output = new BufferPoolByteArrayOutputStream();
                    encoder = new java.beans.XMLEncoder(new NonCloseableOutputStream(output));
                    encoder.writeObject(settingsProps);
                    encoder.close(); // writes closing XML tag
                    encoder = null;
                    settingsPropsXml = output.toString("UTF-8");
                }
                finally {
                    if(encoder!=null) encoder.close();
                    ResourceUtils.closeQuietly(output);
                }
            }
        }

        return settingsPropsXml;
    }

    /*For JAXB processing. Needed a way to get this identity provider to recreate it's internal
    * settingsPropsXml after setting a property after it has been unmarshalled*/
    public void recreateSerializedSettingsProps() throws java.io.IOException{
        settingsPropsXml = null;
        this.getSerializedSettingsProps();
    }

    public Map<String, Object> obtainSettingsProps() {
        return settingsProps;
    }

    /**
     * for serialization by axis and hibernate only.
     * to set the properties, call setProperty
     */
    public void setSerializedSettingsProps(String serializedProps) {
        settingsPropsXml = serializedProps;
        if (serializedProps == null || serializedProps.length() < 2) {
            settingsProps.clear();
        } else {
            ByteArrayInputStream in = new ByteArrayInputStream(HexUtils.encodeUtf8(serializedProps));
            java.beans.XMLDecoder decoder = new java.beans.XMLDecoder(in);
            //noinspection unchecked
            settingsProps = (Map<String, Object>) decoder.readObject();
        }
    }

    public Object getSettingProperty(String name) {
        return settingsProps.get(name);
    }

    protected void setSettingProperty(String name, Object value) {
        settingsProps.put(name, value);
        settingsPropsXml = null;
    }

    public void toJSON(JSON.Output output) {
        output.add(JSONConstants.ID, getId());
        output.add(JSONConstants.NAME, getName());
    }

    public void fromJSON(Map map) {
        throw new UnsupportedOperationException("Mapping from JSON not supported.");
    }
}
