package com.l7tech.server.ems.standardreports;

import com.l7tech.common.io.NonCloseableOutputStream;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.imp.NamedEntityImp;
import com.l7tech.server.ems.enterprise.JSONConstants;
import com.l7tech.util.Charsets;
import com.l7tech.util.HexUtils;
import com.l7tech.util.PoolByteArrayOutputStream;
import com.l7tech.util.ResourceUtils;
import org.hibernate.annotations.Proxy;
import org.hibernate.annotations.Type;
import org.mortbay.util.ajax.JSON;

import javax.persistence.*;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Entity encapsulating all settings of a standard report
 *
 * @Copyright: Layer 7 Tech. Inc.
 * @Author: ghuang
 * @Date: Jan 20, 2009
 */
@XmlRootElement
@Entity
@Proxy(lazy=false)
@Table(name="standard_report_settings")
public class StandardReportSettings extends NamedEntityImp implements JSON.Convertible {

    //- PUBLIC

    @Deprecated // For serialization and persistence only
    public StandardReportSettings() {}

    public StandardReportSettings( final String name,
                                   final User user,
                                   final Map<String,Object> settingsProps ) {
        this._name = name;
        this.userId = user.getId();
        this.provider = user.getProviderId();
        this.settingsProps = settingsProps;
    }

    @Column(name="provider", nullable=false)
    @Type(type = "com.l7tech.server.util.GoidType")
    public Goid getProvider() {
        return provider;
    }

    public void setProvider( final Goid providerId ) {
        this.provider = providerId;
    }

    @Column(name="user_id", nullable=false, length=255)
    public String getUserId() {
        return userId;
    }

    public void setUserId( final String userId ) {
        this.userId = userId;
    }

    public Object getProperty( final String name ) {
        return settingsProps.get(name);
    }

    public void setProperty( final String name, final Object value ) {
        settingsProps.put(name, value);
        settingsPropsXml = null;
    }

    /**
     * Get a read only copy of the properties for this settings.
     *
     * @return The settings property map.
     */
    @Transient
    public Map<String,Object> getProperties() {
        return Collections.unmodifiableMap(settingsProps);
    }

    /**
     *
     */
    public void setProperties( final Map<String,Object> properties ) {
        settingsProps.clear();
        settingsProps.putAll( properties );
        settingsPropsXml = null;
    }

    /**
     * for serialization by axis and hibernate only.
     * to get the properties, call getProperty
     */
    @Deprecated
    @Column(name="settings_properties", length=Integer.MAX_VALUE)
    @Lob
    public String getSerializedSettingsProps() throws java.io.IOException {
        if (settingsPropsXml == null) {
            // if no props, return empty string
            if (settingsProps.size() < 1) {
                settingsPropsXml = "";
            } else {
                PoolByteArrayOutputStream output = null;
                java.beans.XMLEncoder encoder = null;
                try {
                    output = new PoolByteArrayOutputStream();
                    encoder = new java.beans.XMLEncoder(new NonCloseableOutputStream(output));
                    encoder.writeObject(settingsProps);
                    encoder.close(); // writes closing XML tag
                    encoder = null;
                    settingsPropsXml = output.toString(Charsets.UTF8);
                }
                finally {
                    if(encoder!=null) encoder.close();
                    ResourceUtils.closeQuietly(output);
                }
            }
        }

        return settingsPropsXml;
    }

    /**
     * for serialization by axis and hibernate only.
     * to set the properties, call setProperty
     */
    @Deprecated
    public void setSerializedSettingsProps( final String serializedProps ) {
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

    public void toJSON( final JSON.Output output ) {
        output.add(JSONConstants.ID, getId());
        output.add(JSONConstants.NAME, getName());
    }

    public void fromJSON( final Map map ) {
        throw new UnsupportedOperationException("Mapping from JSON not supported.");
    }

    //- PRIVATE

    private Goid provider;
    private String userId;
    private Map<String, Object> settingsProps = new HashMap<String, Object>();
    private String settingsPropsXml;
    
}
