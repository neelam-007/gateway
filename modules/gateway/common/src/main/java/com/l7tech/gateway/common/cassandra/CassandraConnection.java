package com.l7tech.gateway.common.cassandra;

import com.l7tech.common.io.NonCloseableOutputStream;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.imp.ZoneableNamedEntityImp;
import com.l7tech.search.Dependency;
import com.l7tech.security.rbac.RbacAttribute;
import com.l7tech.util.*;
import org.hibernate.annotations.Proxy;
import org.hibernate.annotations.Type;
import org.jetbrains.annotations.Nullable;

import javax.persistence.*;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;


/**
 * Cassandra connection entity class that stores connection configurations and properties.
 */

@XmlAccessorType(XmlAccessType.PROPERTY)
@XmlRootElement
@Entity
@Proxy(lazy=false)
@Table(name="cassandra_connection")
public class CassandraConnection extends ZoneableNamedEntityImp implements Comparable {

    private String connectionName;
    private String keyspaceName;
    private String contactPoints;
    private String port;
    private String username;
    private Goid passwordGoid;
    private String compression = "";
    private boolean ssl;

    private String propertiesXml;
    private Map<String, String> properties = new HashMap<>();


    @RbacAttribute
    @Size(min = 1, max = 255)
    @Column(name = "connection_name", nullable = false)
    public String getConnectionName() {
        return connectionName;
    }

    public void setConnectionName(String connectionName) {
        this.connectionName = connectionName;
    }

    @RbacAttribute
    @Size(min = 1, max = 255)
    @Column(name = "keyspace_name", nullable = false)
    public String getKeyspaceName() {
        return keyspaceName;
    }

    public void setKeyspaceName(String keyspaceName) {
        this.keyspaceName = keyspaceName;
    }

    @RbacAttribute
    @Size(min = 1, max = 4096)
    @Column(name = "contact_points", nullable = false)
    public String getContactPoints() {
        return contactPoints;
    }

    public void setContactPoints(String contactPoints) {
        this.contactPoints = contactPoints;
    }

    @RbacAttribute
    @Size(min = 1, max = 255)
    @Column(name = "port", nullable = false)
    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    @RbacAttribute
    @Size(min = 1, max = 255)
    @Column(name = "username", nullable = false)
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Column(name="password_goid")
    @Type(type = "com.l7tech.server.util.GoidType")
    @Dependency(type = Dependency.DependencyType.SECURE_PASSWORD, methodReturnType = Dependency.MethodReturnType.GOID)
    public Goid getPasswordGoid() {
        return passwordGoid;
    }

    public void setPasswordGoid(Goid passwordGoid) {
        this.passwordGoid = passwordGoid;
    }

    @RbacAttribute
    @Size(min = 1, max = 255)
    @Column(name = "compression", nullable = false)
    public String getCompression() {
        return compression;
    }

    public void setCompression(String compression) {
        this.compression = compression;
    }

    @RbacAttribute
    @Column(name = "ssl", nullable = false)
    public boolean isSsl() {
        return ssl;
    }

    public void setSsl(boolean ssl) {
        this.ssl = ssl;
    }

    @RbacAttribute
    @Column(name = "properties", nullable = true)
    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    @Column(name = "properties", length = Integer.MAX_VALUE)
    @Lob
    public String getSerializedProps() throws java.io.IOException {
        if (propertiesXml == null) {
            if (properties.size() < 1) {
                propertiesXml = "";
            } else {
                PoolByteArrayOutputStream output = null;
                java.beans.XMLEncoder encoder = null;
                try {
                    output = new PoolByteArrayOutputStream();
                    encoder = new java.beans.XMLEncoder(new NonCloseableOutputStream(output));
                    encoder.writeObject(properties);
                    encoder.close();
                    encoder = null;
                    propertiesXml = output.toString(Charsets.UTF8);
                } finally {
                    if (encoder != null) encoder.close();
                    ResourceUtils.closeQuietly(output);
                }
            }
        }

        return propertiesXml;
    }

    public void setSerializedProps(String serializedProps) {
        propertiesXml = serializedProps;
        if (serializedProps == null || serializedProps.length() < 2) {
            properties.clear();
        } else {
            ByteArrayInputStream in = new ByteArrayInputStream(HexUtils.encodeUtf8(serializedProps));
            SafeXMLDecoder decoder = new SafeXMLDecoderBuilder(in).build();
            //noinspection unchecked
            properties = (Map<String, String>) decoder.readObject();
        }
    }

    public void copyFrom(CassandraConnection other) {
        this.setGoid(other.getGoid());
        this.setName(other.getName());
        this.setConnectionName(other.getConnectionName());
        this.setKeyspaceName(other.getKeyspaceName());
        this.setContactPoints(other.getContactPoints());
        this.setPort(other.getPort());
        this.setUsername(other.getUsername());
        this.setCompression(other.getCompression());
        this.setSsl((other.isSsl()));
        this.setPasswordGoid(other.getPasswordGoid());
        this.setSecurityZone(other.getSecurityZone());
        this.setProperties(other.getProperties());
    }

    @Override
    public int compareTo(@Nullable Object o) {
        if (o == null || !(o instanceof CassandraConnection))
            throw new IllegalArgumentException("The compared object must be a CassandraConnection.");

        String originalConfigurationName = getName();
        String comparedConfigurationName = ((CassandraConnection) o).getName();

        if (originalConfigurationName == null || comparedConfigurationName == null)
            throw new NullPointerException("CassandraConnection name must not be null.");
        return originalConfigurationName.compareToIgnoreCase(comparedConfigurationName);
    }
}
