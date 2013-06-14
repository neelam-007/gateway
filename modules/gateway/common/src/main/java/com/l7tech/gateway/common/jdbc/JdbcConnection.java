package com.l7tech.gateway.common.jdbc;

import com.l7tech.common.io.NonCloseableOutputStream;
import com.l7tech.objectmodel.imp.ZoneableNamedEntityImp;
import com.l7tech.policy.wsp.WspSensitive;
import com.l7tech.search.Dependency;
import com.l7tech.util.Charsets;
import com.l7tech.util.HexUtils;
import com.l7tech.util.PoolByteArrayOutputStream;
import com.l7tech.util.ResourceUtils;
import org.hibernate.annotations.Proxy;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.TreeMap;

//TODO change to use NamedEntityWithPropertiesImp
/**
 * This entity stores JDBC Connection Configuration.
 *
 * @author ghuang
 */

@XmlRootElement
@Entity
@Proxy(lazy=false)
@Table(name="jdbc_connection")
public class JdbcConnection extends ZoneableNamedEntityImp implements Comparable {
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
        minPoolSize = JdbcAdmin.ORIGINAL_C3P0_BASIC_POOL_CONFIG_MINPOOLSIZE;
        maxPoolSize = JdbcAdmin.ORIGINAL_C3P0_BASIC_POOL_CONFIG_MAXPOOLSIZE;
        enabled = true;
    }

    @Size(min=1,max=128)
    @Override
    @Transient
    public String getName() {
        return super.getName();
    }

    @NotNull
    @Size(min=1,max=256)
    @Column(name="driver_class",nullable=false)
    public String getDriverClass() {
        return driverClass;
    }

    public void setDriverClass(String driverClass) {
        this.driverClass = driverClass;
    }

    @NotNull
    @Size(min=1,max=4096)
    @Column(name="jdbc_url",nullable=false)
    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public void setJdbcUrl(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    @NotNull
    @Size(max=128)
    @Column(name="user_name",nullable=false)
    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    @NotNull
    @Size(max=64)
    @Column(name="password",nullable=false)
    @WspSensitive
    @Dependency(type = Dependency.DependencyType.SECURE_PASSWORD, methodReturnType = Dependency.MethodReturnType.VARIABLE)
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
                PoolByteArrayOutputStream output = null;
                java.beans.XMLEncoder encoder = null;
                try {
                    output = new PoolByteArrayOutputStream();
                    encoder = new java.beans.XMLEncoder(new NonCloseableOutputStream(output));
                    encoder.writeObject(additionalProps);
                    encoder.close(); // writes closing XML tag
                    encoder = null;
                    additionalPropsXml = output.toString(Charsets.UTF8);
                }
                finally {
                    if(encoder!=null) encoder.close();
                    ResourceUtils.closeQuietly(output);
                }
            }
        }

        return additionalPropsXml;
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

    /**
     * Get a copy of the additional properties.
     *
     * @return A mutable copy of the properties.
     */
    @Transient
    public Map<String, Object> getAdditionalProperties() {
        return new TreeMap<String, Object>( this.additionalProps );
    }

    /**
     * Set the additional properties.
     *
     * @param additionalProperties The properties to use.
     */
    public void setAdditionalProperties( final Map<String,Object> additionalProperties ) {
        this.additionalPropsXml = null;
        this.additionalProps = new TreeMap<String, Object>( additionalProperties );
    }

    @Override
    public int compareTo(Object o) {
        if (o == null || ! (o instanceof JdbcConnection)) throw new IllegalArgumentException("The compared object must be a JdbcConnection.");

        String originalConnectionName = getName();
        String comparedConnectionName = ((JdbcConnection)o).getName();

        if (originalConnectionName == null || comparedConnectionName == null) throw new NullPointerException("JDBC connection name must not be null.");
        return originalConnectionName.compareToIgnoreCase(comparedConnectionName);
    }

    public void copyFrom(JdbcConnection other) {
        this.setOid(other.getOid());
        this.setName(other.getName());
        this.setDriverClass(other.getDriverClass());
        this.setJdbcUrl(other.getJdbcUrl());
        this.setUserName(other.getUserName());
        this.setPassword(other.getPassword());
        this.setMinPoolSize(other.getMinPoolSize());
        this.setMaxPoolSize(other.getMaxPoolSize());
        this.setEnabled(other.isEnabled());
        this.setAdditionalProperties(other.getAdditionalProperties());
        this.setSecurityZone(other.getSecurityZone());
    }
}
