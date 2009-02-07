package com.l7tech.gateway.common.log;

import com.l7tech.util.BufferPoolByteArrayOutputStream;
import com.l7tech.common.io.NonCloseableOutputStream;
import com.l7tech.objectmodel.imp.NamedEntityImp;

import javax.persistence.*;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.*;

import org.hibernate.annotations.Proxy;

/**
 * Describes the configuration of a logging sink.
 */
@Entity
@Proxy(lazy=false)
@Table(name="sink_config")
@AttributeOverride(name="name", column=@Column(name="name", nullable=false, length=32))
public class SinkConfiguration extends NamedEntityImp {

    /** The character encoding to use for encoding the properties to XML */
    public static final String PROPERTIES_ENCODING = "UTF-8";

    public static enum SinkType {
        /** Indicates that this is a log file sink. */
        FILE(
                new String[] {
                        PROP_FILE_MAX_SIZE,
                        PROP_FILE_LOG_COUNT
                }
        ),
        /** Indicates that this is a syslog sink. */
        SYSLOG(
                new String[] {
                        PROP_SYSLOG_PROTOCOL,
                        PROP_SYSLOG_HOSTLIST,
                        PROP_SYSLOG_FACILITY,
                        PROP_SYSLOG_LOG_HOSTNAME,
                        PROP_SYSLOG_CHAR_SET,
                        PROP_SYSLOG_TIMEZONE,
                        PROP_SYSLOG_SSL_CLIENTAUTH,
                        PROP_SYSLOG_SSL_KEYSTORE_ID,
                        PROP_SYSLOG_SSL_KEY_ALIAS
                }
        );

        private final String[] propertyNames;

        private SinkType(String[] propertyNames) {
            this.propertyNames = propertyNames;
        }

        public String[] getPropertyNames() {
            return propertyNames;
        }
    }

    public static enum SeverityThreshold {
        ALL,
        CONFIG,
        INFO,
        WARNING,
        SEVERE
    }

    public static final String CATEGORY_GATEWAY_LOGS = "LOG";
    public static final String CATEGORY_TRAFFIC_LOGS = "TRAFFIC";
    public static final String CATEGORY_AUDITS = "AUDIT";
    public static final Set<String> CATEGORIES_SET;
    static {
        Set<String> categories = new LinkedHashSet<String>();
        categories.add(CATEGORY_GATEWAY_LOGS);
        categories.add(CATEGORY_TRAFFIC_LOGS);
        categories.add(CATEGORY_AUDITS);
        CATEGORIES_SET = Collections.unmodifiableSet(categories);
    }

    // Log file sink property names
    /** The property name for the max file size */
    public static final String PROP_FILE_MAX_SIZE = "file.maxSize";
    /** The property name for the maximum number of rotated log files to keep */
    public static final String PROP_FILE_LOG_COUNT = "file.logCount";
    /** the property name for the format */
    public static final String PROP_FILE_FORMAT = "file.format";

    public static final String FILE_FORMAT_RAW = "RAW";
    public static final String FILE_FORMAT_STANDARD = "STANDARD";
    public static final String FILE_FORMAT_VERBOSE = "VERBOSE";
    public static final Set<String> FILE_FORMAT_SET;
    static {
        Set<String> formats = new LinkedHashSet<String>();
        formats.add(FILE_FORMAT_RAW);
        formats.add(FILE_FORMAT_STANDARD);
        formats.add(FILE_FORMAT_VERBOSE);
        FILE_FORMAT_SET = Collections.unmodifiableSet(formats);
    }

    // Syslog sink property names
    /** The property name for the syslog protocol to use */
    public static final String PROP_SYSLOG_PROTOCOL = "syslog.protocol";
    /** The property name for the list of syslog host to log to */
    public static final String PROP_SYSLOG_HOSTLIST = "syslog.hostList";
    /** The property name for the facility name to use */
    public static final String PROP_SYSLOG_FACILITY = "syslog.facility";
    /** The property name for the hostname to use in log messages */
    public static final String PROP_SYSLOG_LOG_HOSTNAME = "syslog.logHostname";
    /** The property name for the character set to use for logging */
    public static final String PROP_SYSLOG_CHAR_SET = "syslog.charSet";
    /** The property name for the timezone to use for log messages */
    public static final String PROP_SYSLOG_TIMEZONE = "syslog.timezone";
    /** The property name for enabling SSL with client authentication */
    public static final String PROP_SYSLOG_SSL_CLIENTAUTH = "syslog.ssl.clientAuth";
    /** The property name for the SSL keystore id */
    public static final String PROP_SYSLOG_SSL_KEYSTORE_ID = "syslog.ssl.keystore.id";
    /** The property name for the SSL key alias */
    public static final String PROP_SYSLOG_SSL_KEY_ALIAS = "syslog.ssl.key.alias";

    public static final String SYSLOG_PROTOCOL_TCP = "TCP";
    public static final String SYSLOG_PROTOCOL_UDP = "UDP";
    public static final String SYSLOG_PROTOCOL_SSL = "SSL";
    public static final Set<String> SYSLOG_PROTOCOL_SET;
    static {
        Set<String> protocols = new LinkedHashSet<String>();
        protocols.add(SYSLOG_PROTOCOL_TCP);
        protocols.add(SYSLOG_PROTOCOL_UDP);
        protocols.add(SYSLOG_PROTOCOL_SSL);
        SYSLOG_PROTOCOL_SET = Collections.unmodifiableSet(protocols);
    }

    private String description;
    private SinkType type;
    private boolean enabled;
    private SeverityThreshold severity;
    private String categories;
    private transient String xmlProperties;
    private Map<String, String> properties;
    private List<String> syslogHosts;

    /**
     * Drops the existing set of properties and reads the new properties to use from the
     * input XML string.
     * <p/>
     * This should not be called directly. Use the setProperty method instead.
     *
     * @param xml The XML string to parse containing the properties.
     */
    public void setXmlProperties(String xml) {
        if (xml != null && xml.equals(xmlProperties)) return;
        this.xmlProperties = xml;
        if ( xml != null && xml.length() > 0 ) {
            try {
                XMLDecoder xd = new XMLDecoder(new ByteArrayInputStream(xml.getBytes(PROPERTIES_ENCODING)));
                //noinspection unchecked
                Object parsedObject = xd.readObject();
                if (parsedObject instanceof Object[]) {
                    Object[] readProperties = (Object[]) parsedObject;
                    this.properties = (Map<String, String>) readProperties[0];
                    this.syslogHosts = (List<String>) readProperties[1];
                } else {
                    this.properties = (Map<String, String>) parsedObject;
                }
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e); // Can't happen
            }
        }
    }

    /**
     * Encodes the current list of properties as an XML string.
     *
     * @return An XML string that contains the current list of properties.
     */
    @Column(name="properties", length=Integer.MAX_VALUE)
    @Lob
    public String getXmlProperties() {
        if ( xmlProperties == null ) {
            Map<String, String> properties = this.properties;
            List<String> hosts = this.syslogHosts;
            if ( properties == null ) return null;
            BufferPoolByteArrayOutputStream baos = new BufferPoolByteArrayOutputStream();
            try {
                //noinspection IOResourceOpenedButNotSafelyClosed
                XMLEncoder xe = new XMLEncoder(new NonCloseableOutputStream(baos));

                Object writeObject;
                if (hosts != null) {
                    writeObject = new Object[] { properties, hosts };
                } else {
                    writeObject = properties;
                }

                xe.writeObject(writeObject);
                xe.close();
                xmlProperties = baos.toString(PROPERTIES_ENCODING);
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e); // Can't happen
            } finally {
                baos.close();
            }
        }
        return xmlProperties;
    }

    /**
     * Returns the description for this log sink.
     *
     * @return the description
     */
    @Column(name="description", length=Integer.MAX_VALUE)
    @Lob
    public String getDescription() {
        return description;
    }

    /**
     * Updates the description of this log sink.
     *
     * @param description the new description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Returns what type of log sink this is configuration is for.
     *
     * @return An instance of SinkType
     */
    @Enumerated(EnumType.STRING)
    @Column(name="type", length=32)
    public SinkType getType() {
        return type;
    }

    /**
     * Updates the type of log sink that this configuration is for.
     *
     * @param type An instance of SinkType
     */
    public void setType(SinkType type) {
        this.type = type;
        xmlProperties = null;

        if(properties != null) {
            for (SinkType value : SinkType.values()) {
                if (value != type) {
                    for (String propertyName : value.getPropertyNames()) {
                        properties.remove(propertyName);
                    }
                }
            }
        }
    }

    /**
     * Returns the enabled flag.
     *
     * @return <code>true</code> if this log sink is enabled.
     */
    @Column(name="enabled")
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Updates the enabled flag.
     * 
     * @param enabled the new value
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Returns the severity threshold.
     *
     * @return the severity threshold
     */
    @Enumerated(EnumType.STRING)
    @Column(name="severity", length=32)
    public SeverityThreshold getSeverity() {
        return severity;
    }

    /**
     * Updates the severity threshold.
     *
     * @param severity The new value
     */
    public void setSeverity(SeverityThreshold severity) {
        this.severity = severity;
    }

    /**
     * Returns the list of categories as a string.
     *
     * @return A comma deliminated list of categories
     */
    @Column(name="categories", length=Integer.MAX_VALUE)
    @Lob
    public String getCategories() {
        return categories;
    }

    /**
     * Updates the list of categories.
     *
     * @param categories A comma deliminated list of categories to use
     */
    public void setCategories(String categories) {
        this.categories = categories;
    }

    /**
     * Returns the value of the specified property.
     *
     * @param propertyName the name of the property to lookup
     * @return the value
     */
    public String getProperty(String propertyName) {
        String propertyValue = null;

        Map<String,String> properties = this.properties;
        if (properties != null) {
            propertyValue = properties.get(propertyName);
        }

        return propertyValue;
    }

    /**
     * Updates the value of the specified property.
     *
     * @param propertyName the name of the property to update
     * @param propertyValue the new value
     */
    public void setProperty(String propertyName, String propertyValue) {
        Map<String,String> properties = this.properties;
        if (properties == null) {
            properties = new HashMap<String, String>();
            this.properties = properties;
        }

        properties.put(propertyName, propertyValue);

        // invalidate cached properties
        xmlProperties = null;
    }

    public void addSyslogHostEntry(String value) {
        syslogHostList().add( new SyslogHostEntry(value).getValue() );

        // invalidate cached properties
        xmlProperties = null;
    }

    public void removeSyslogHostEntry(int index) {
        List<String> hostList = syslogHostList();

        if (index >= 0 && index < hostList.size())
            hostList.remove(index);

        // otherwise, just ignore because it shouldn't happen

        // invalidate cached properties
        xmlProperties = null;
    }

    public List<String> syslogHostList() {
        if (syslogHosts == null) {
            syslogHosts = new ArrayList<String>();
        }
        return syslogHosts;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        SinkConfiguration that = (SinkConfiguration)o;

        if(description != null ? !description.equals(that.description) : that.description != null) return false;
        if(type != null ? type != that.type : that.type != null) return false;
        if(enabled != that.enabled) return false;
        if(severity != null ? severity != that.severity : that.severity != null) return false;
        if(categories != null ? !categories.equals(that.categories) : that.categories != null) return false;

        Map<String, String> thatProperties = that.properties;
        if(properties != null && thatProperties == null || properties == null && thatProperties != null) {
            return false;
        }

        if(type != null && properties != null) {
            for(String propertyName : type.getPropertyNames()) {
                String thisValue = properties.get(propertyName);
                String thatValue = properties.get(propertyName);

                if(thisValue == null) {
                    if(thatValue != null) {
                        return false;
                    }
                } else {
                    if(!thisValue.equals(thatValue)) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    public int hashCode() {
        int result = super.hashCode();

        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (enabled ? 1 : 0);
        result = 31 * result + (severity != null ? severity.hashCode() : 0);
        result = 31 * result + (categories != null ? categories.hashCode() : 0);

        if(type != null && properties != null) {
            for(String propertyName : type.getPropertyNames()) {
                String value = properties.get(propertyName);
                result = 31 * result + (value != null ? value.hashCode() : 0);
            }
        }

        return result;
    }

    /**
     * Represents one host:port entry in the Syslog host list.
     */
    public class SyslogHostEntry implements Serializable {

        static final String HOST_ENTRY_DELIM = ":";

        String hostName;
        String port;

        /**
         * Constructor that parses the hostName and port number from a single host entry.
         * @param hostString single host entry to parse formatted as "hostName:portNumber"
         */
        SyslogHostEntry(String hostString) {
            StringTokenizer stok = new StringTokenizer(hostString, HOST_ENTRY_DELIM);
            if (stok.hasMoreTokens())
                hostName = stok.nextToken();
            if (stok.hasMoreTokens())
                port = stok.nextToken();

            // if either are null, get angry
            if (hostName == null || hostName.length() == 0) {
                throw new IllegalArgumentException("Syslog hostName cannot be null or empty");
            }
            if (port == null || port.length() == 0) {
                throw new IllegalArgumentException("Syslog port cannot be null or empty");
            }
        }

        public String getHostName() {
            return hostName;
        }

        public String getPort() {
            return port;
        }

        public String getValue() {
            return new StringBuffer(hostName).append(HOST_ENTRY_DELIM).append(port).toString();
        }
    }
}
