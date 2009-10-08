package com.l7tech.gateway.common.jdbcconnection;

import com.l7tech.objectmodel.imp.NamedEntityImp;
import com.l7tech.util.BufferPoolByteArrayOutputStream;
import com.l7tech.util.ResourceUtils;
import com.l7tech.util.HexUtils;
import com.l7tech.common.io.NonCloseableOutputStream;

import javax.persistence.*;
import javax.xml.bind.annotation.XmlRootElement;

import org.hibernate.annotations.Proxy;

import java.util.*;
import java.io.ByteArrayInputStream;

/**
 * This entity stores JDBC Connection Configuration.
 *
 * @author ghuang
 */

@XmlRootElement
@Entity
@Proxy(lazy=false)
@Table(name="jdbc_connection")
public class JdbcConnection extends NamedEntityImp implements Comparable {
    private String driverClass;
    private String jdbcUrl;
    private String userName;
    private String password;
    private int minPoolSize;
    private int maxPoolSize;
    private boolean enabled;

    private String additionalPropsXml;
    private Map<String, Object> additionalProps = new TreeMap<String, Object>();

    public JdbcConnection() {
        _name = "";
        driverClass = "";
        jdbcUrl = "";
        userName = "";
        password = "";
        minPoolSize = JdbcConnectionAdmin.ORIGINAL_C3P0_BASIC_POOL_CONFIG_MINPOOLSIZE;
        maxPoolSize = JdbcConnectionAdmin.ORIGINAL_C3P0_BASIC_POOL_CONFIG_MAXPOOLSIZE;
        enabled = true;
    }

    @Column(name="driver_class")
    public String getDriverClass() {
        return driverClass;
    }

    public void setDriverClass(String driverClass) {
        this.driverClass = driverClass;
    }

    @Column(name="jdbc_url")
    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public void setJdbcUrl(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    @Column(name="user_name")
    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    @Column(name="password")
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Column(name="enabled")
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Column(name="max_pool_size")
    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    public void setMaxPoolSize(int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

    @Column(name="min_pool_size")
    public int getMinPoolSize() {
        return minPoolSize;
    }

    public void setMinPoolSize(int minPoolSize) {
        this.minPoolSize = minPoolSize;
    }

    /**
     * for serialization by axis and hibernate only.
     * to get the properties, call getProperty
     */
    @Deprecated
    @Column(name="additional_properties", length=Integer.MAX_VALUE)
    @Lob
    public String getSerializedProps() throws java.io.IOException {
        if (additionalPropsXml == null) {
            // if no additionalProps, return empty string
            if (additionalProps.size() < 1) {
                additionalPropsXml = "";
            } else {
                BufferPoolByteArrayOutputStream output = null;
                java.beans.XMLEncoder encoder = null;
                try {
                    output = new BufferPoolByteArrayOutputStream();
                    encoder = new java.beans.XMLEncoder(new NonCloseableOutputStream(output));
                    encoder.writeObject(additionalProps);
                    encoder.close(); // writes closing XML tag
                    encoder = null;
                    additionalPropsXml = output.toString("UTF-8");
                }
                finally {
                    if(encoder!=null) encoder.close();
                    ResourceUtils.closeQuietly(output);
                }
            }
        }

        return additionalPropsXml;
    }

    /*For JAXB processing. Needed a way to get this identity provider to recreate it's internal
    * additionalPropsXml after setting a property after it has been unmarshalled*/
    public void recreateSerializedProps() throws java.io.IOException{
        additionalPropsXml = null;
        //noinspection deprecation
        getSerializedProps();
    }

    /**
     * for serialization by axis and hibernate only.
     * to set the properties, call setProperty
     */
    public void setSerializedProps(String serializedProps) {
        additionalPropsXml = serializedProps;
        if (serializedProps == null || serializedProps.length() < 2) {
            additionalProps.clear();
        } else {
            ByteArrayInputStream in = new ByteArrayInputStream(HexUtils.encodeUtf8(serializedProps));
            java.beans.XMLDecoder decoder = new java.beans.XMLDecoder(in);
            //noinspection unchecked
            additionalProps = (Map<String, Object>) decoder.readObject();
        }
    }

//    @Transient
//    public String getAdditionalPropertyName(int index) {
//        return (String) additionalProps.keySet().toArray()[index];
//    }

    @Transient
    public Object getAdditionalPropertyValue(String propName) {
        return additionalProps.get(propName);
    }

    public void setAdditionalProperty(String name, Object value) {
        if ( value == null ) {
            additionalProps.remove( name );
        } else {
            additionalProps.put(name, value);
        }
        additionalPropsXml = null;
    }

    @Transient
    public Map<String, Object> getAllAddtionalProperties() {
        return additionalProps;
    }

    @Transient
    public Properties getAllProperties() {
        Properties props = new Properties();

        // Basic Connection Configuration
        props.put("driverClass", driverClass);
        props.put("jdbcUrl", jdbcUrl);
        props.put("user", userName);
        props.put("password", password);

        // C3P0 Pool Configuration
        props.put("minPoolSize", minPoolSize);
        props.put("maxPoolSize", maxPoolSize);

        // Additional Properties
        for (String key: additionalProps.keySet()) {
            String value = (String) additionalProps.get(key);
            if (value != null && !value.isEmpty())
                props.put(key, value);
        }

        return props;
    }

    @Override
    public int compareTo(Object o) {
        if (o == null || ! (o instanceof JdbcConnection)) throw new IllegalArgumentException("The compared object must be a JdbcConnection.");

        String originalConnectionName = getName();
        String comparedConnectionName = ((JdbcConnection)o).getName();

        if (originalConnectionName == null || comparedConnectionName == null) throw new NullPointerException("JDBC connection name must not be null.");
        return originalConnectionName.compareToIgnoreCase(comparedConnectionName);
    }
}
