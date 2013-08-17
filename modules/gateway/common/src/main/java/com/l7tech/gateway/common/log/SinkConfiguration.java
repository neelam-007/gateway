package com.l7tech.gateway.common.log;

import com.l7tech.common.io.NonCloseableOutputStream;
import com.l7tech.objectmodel.imp.ZoneableNamedGoidEntityImp;
import com.l7tech.util.*;
import com.l7tech.util.Functions.Unary;
import org.hibernate.annotations.Proxy;
import org.jetbrains.annotations.NotNull;

import javax.persistence.*;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.*;
import java.util.logging.Level;

import static com.l7tech.util.CollectionUtils.*;
import static com.l7tech.util.Functions.map;
import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableMap;

/**
 * Describes the configuration of a logging sink.
 */
@Entity
@Proxy(lazy=false)
@Table(name="sink_config")
@AttributeOverride(name="name", column=@Column(name="name", nullable=false, length=32))
public class SinkConfiguration extends ZoneableNamedGoidEntityImp {

    //- PUBLIC

    /** The character encoding to use for encoding the properties to XML */
    public static final Charset PROPERTIES_ENCODING = Charsets.UTF8;

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
        ALL(Level.ALL),
        FINEST(Level.FINEST),
        FINER(Level.FINER),
        FINE(Level.FINE),
        CONFIG(Level.CONFIG),
        INFO(Level.INFO),
        WARNING(Level.WARNING),
        SEVERE(Level.SEVERE);

        private final Level logLevel;

        private SeverityThreshold( @NotNull final Level logLevel ) {
            this.logLevel = logLevel;
        }

        @NotNull
        public Level toLoggingLevel() {
            return logLevel;
        }
    }

    public static enum RollingInterval {
        HOURLY("yyyy-MM-dd-HH"),
        DAILY("yyyy-MM-dd");

        private final String pattern;

        private RollingInterval(@NotNull final String pattern){
            this.pattern = pattern;
        }

        public String getPattern() {
            return pattern;
        }
    }

    public static final String CATEGORY_GATEWAY_LOGS = "LOG";
    public static final String CATEGORY_TRAFFIC_LOGS = "TRAFFIC";
    public static final String CATEGORY_AUDITS = "AUDIT";
    public static final String CATEGORY_SSPC_LOGS= "SSPC";
    public static final Set<String> CATEGORIES_SET = set(
            CATEGORY_AUDITS,
            CATEGORY_GATEWAY_LOGS,
            CATEGORY_TRAFFIC_LOGS,
            CATEGORY_SSPC_LOGS
    );

    // Log file sink property names
    /** The property name for the max file size */
    public static final String PROP_FILE_MAX_SIZE = "file.maxSize";
    /** The property name for the maximum number of rotated log files to keep */
    public static final String PROP_FILE_LOG_COUNT = "file.logCount";
    /** the property name for the format */
    public static final String PROP_FILE_FORMAT = "file.format";

    public static final String PROP_ENABLE_ROLLING_FILE = "file.rolling";
    public static final String PROP_ROLLING_INTERVAL = "file.interval";

    public static final String FILE_FORMAT_RAW = "RAW";
    public static final String FILE_FORMAT_STANDARD = "STANDARD";
    public static final String FILE_FORMAT_VERBOSE = "VERBOSE";
    public static final Set<String> FILE_FORMAT_SET = set(
            FILE_FORMAT_RAW,
            FILE_FORMAT_STANDARD,
            FILE_FORMAT_VERBOSE
    );

    // Syslog sink property names
    /** The property name for the syslog protocol to use */
    public static final String PROP_SYSLOG_PROTOCOL = "syslog.protocol";
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
    /** the property name for the format */
    public static final String PROP_SYSLOG_FORMAT = "syslog.format";

    public static final String SYSLOG_PROTOCOL_TCP = "TCP";
    public static final String SYSLOG_PROTOCOL_UDP = "UDP";
    public static final String SYSLOG_PROTOCOL_SSL = "SSL";
    public static final Set<String> SYSLOG_PROTOCOL_SET = set(
            SYSLOG_PROTOCOL_TCP,
            SYSLOG_PROTOCOL_UDP,
            SYSLOG_PROTOCOL_SSL
    );

    public static final String SYSLOG_FORMAT_RAW = "RAW";
    public static final String SYSLOG_FORMAT_STANDARD = "STANDARD";
    public static final String SYSLOG_FORMAT_VERBOSE = "VERBOSE";
    public static final Set<String> SYSLOG_FORMAT_SET = set(
            SYSLOG_FORMAT_RAW,
            SYSLOG_FORMAT_STANDARD,
            SYSLOG_FORMAT_VERBOSE
    );

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

    @Transient
    public Map<String, List<String>> getFilters() {
        return filters;
    }

    public void setFilters( final Map<String, List<String>> filters ) {
        this.filters = immutable( filters );

        // invalidate cached properties
        xmlProperties = null;
    }

    @Transient
    public boolean isRollingEnabled(){
        final String v = getProperty(PROP_ENABLE_ROLLING_FILE);
        return v == null ? false : Boolean.valueOf(getProperty(PROP_ENABLE_ROLLING_FILE)).booleanValue();
    }

    @Transient
    public RollingInterval getRollingInterval(){
        final String v = getProperty(PROP_ROLLING_INTERVAL);
        return v == null ? RollingInterval.DAILY : RollingInterval.valueOf(v);
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
        if(filters != null ? !filters.equals(that.filters) : that.filters != null) return false;
        if(securityZone != null ? !securityZone.equals(that.securityZone) : that.securityZone != null) return false;

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
        result = 31 * result + (filters != null ? filters.hashCode() : 0);
        result = 31 * result + (securityZone != null ? securityZone.hashCode() : 0);

        if(type != null && properties != null) {
            for(String propertyName : type.getPropertyNames()) {
                String value = properties.get(propertyName);
                result = 31 * result + (value != null ? value.hashCode() : 0);
            }
        }

        return result;
    }

    public void copyFrom( SinkConfiguration objToCopy ) {
        this.setGoid(objToCopy.getGoid());
        this.setName(objToCopy.getName());
        this.setDescription(objToCopy.getDescription());
        this.setType(objToCopy.getType());
        this.setEnabled(objToCopy.isEnabled());
        this.setSeverity(objToCopy.getSeverity());
        this.setCategories(objToCopy.getCategories());
        this.setXmlProperties(objToCopy.getXmlProperties());
        this.setSecurityZone(objToCopy.getSecurityZone());
    }

    /**
     * Represents one host:port entry in the Syslog host list.
     */
    public class SyslogHostEntry implements Serializable {

        static final String HOST_ENTRY_DELIM = ":";

        final String hostName;
        final String port;

        /**
         * Constructor that parses the hostName and port number from a single host entry.
         * @param hostString single host entry to parse formatted as "hostName:portNumber"
         */
        SyslogHostEntry(String hostString) {

            Pair<String,String> hostAndPort = InetAddressUtil.getHostAndPort(hostString, null);

            // if either are null, get angry
            if (hostAndPort.left == null || hostAndPort.left.trim().isEmpty()) {
                throw new IllegalArgumentException("Syslog hostName cannot be null or empty");
            }
            if (hostAndPort.right == null ) {
                throw new IllegalArgumentException("Syslog port cannot be null or empty");
            }

            hostName = hostAndPort.left;
            port = hostAndPort.right;
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

        @SuppressWarnings({ "RedundantIfStatement" })
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            SyslogHostEntry that = (SyslogHostEntry) o;

            if (!hostName.equals(that.hostName)) return false;
            if (!port.equals(that.port)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = hostName.hashCode();
            result = 31 * result + port.hashCode();
            return result;
        }
    }

    //- PROTECTED

   /**
     * Encodes the current list of properties as an XML string.
     *
     * @return An XML string that contains the current list of properties.
     */
    @Column(name="properties", length=Integer.MAX_VALUE)
    @Lob
    protected String getXmlProperties() {
        if ( xmlProperties == null ) {
            final Map<String, String> properties = this.properties;
            final List<String> hosts = this.syslogHosts;
            final Map<String,List<String>> filters = mutable( this.filters );
            if ( properties == null && hosts == null && filters == null) return null;
            PoolByteArrayOutputStream baos = new PoolByteArrayOutputStream();
            try {
                //noinspection IOResourceOpenedButNotSafelyClosed
                XMLEncoder xe = new XMLEncoder(new NonCloseableOutputStream(baos));
                xe.writeObject( new Object[] { properties, hosts, filters } );
                xe.close();
                xmlProperties = baos.toString(PROPERTIES_ENCODING);
            } finally {
                baos.close();
            }
        }
        return xmlProperties;
    }

    /**
     * Drops the existing set of properties and reads the new properties to use from the
     * input XML string.
     * <p/>
     * This should not be called directly. Use the setProperty method instead.
     *
     * @param xml The XML string to parse containing the properties.
     */
    protected void setXmlProperties(String xml) {
        if (xml != null && xml.equals(xmlProperties)) return;
        this.xmlProperties = xml;
        if ( xml != null && xml.length() > 0 ) {
            XMLDecoder xd = new XMLDecoder(new ByteArrayInputStream(xml.getBytes(PROPERTIES_ENCODING)));
            final Object parsedObject = xd.readObject();
            if ( parsedObject instanceof Object[] ) {
                Object[] readProperties = (Object[]) parsedObject;
                this.properties = cast( readProperties[0], Map.class, String.class, String.class, new HashMap<String, String>());
                this.syslogHosts = cast( readProperties[1], List.class, String.class, new ArrayList<String>() );
                if ( readProperties.length > 2 ) {
                    this.filters = immutable( cast( readProperties[2], Map.class, String.class, List.class, new HashMap<String, List<String>>() ) );
                }
            } else {
                this.properties = cast( parsedObject, Map.class, String.class, String.class, new HashMap<String, String>());
            }
        }
    }

    //- PRIVATE

    private String description;
    private SinkType type;
    private boolean enabled;
    private SeverityThreshold severity;
    private String categories;
    private transient String xmlProperties;
    private Map<String, String> properties;
    private List<String> syslogHosts;
    private Map<String, List<String>> filters = emptyMap();


    private Map<String, List<String>> immutable( final Map<String, List<String>> map ) {
        return unmodifiableMap( map( map, null, Functions.<String>identity(), new Unary<List<String>, List<String>>() {
            @Override
            public List<String> call( final List<String> strings ) {
                return toList( strings );
            }
        } ) );
    }

    private Map<String, List<String>> mutable( final Map<String, List<String>> map ) {
        return map( map, null, Functions.<String>identity(), new Unary<List<String>, List<String>>() {
            @Override
            public List<String> call( final List<String> strings ) {
                return new ArrayList<String>( strings );
            }
        } );
    }
}
