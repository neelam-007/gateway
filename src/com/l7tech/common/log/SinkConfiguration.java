package com.l7tech.common.log;

import com.l7tech.common.io.BufferPoolByteArrayOutputStream;
import com.l7tech.common.io.NonCloseableOutputStream;
import com.l7tech.objectmodel.imp.NamedEntityImp;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * Describes the configuration of a logging sink.
 */
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
                        PROP_SYSLOG_HOST,
                        PROP_SYSLOG_PORT,
                        PROP_SYSLOG_FACILITY,
                        PROP_SYSLOG_LOG_HOSTNAME,
                        PROP_SYSLOG_CHAR_SET,
                        PROP_SYSLOG_TIMEZONE
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
        CONFIG,
        INFO,
        WARNING,
        SEVERE
    }

    public static final HashSet<String> CATEGORIES_SET = new HashSet<String>();
    static {
        CATEGORIES_SET.add("Gateway Log");
        CATEGORIES_SET.add("Traffic Log");
        CATEGORIES_SET.add("Audits");
    }

    // Log file sink property names
    /** The property name for the max file size */
    public static final String PROP_FILE_MAX_SIZE = "file.maxSize";
    /** The property name for the maximum number of rotated log files to keep */
    public static final String PROP_FILE_LOG_COUNT = "file.logCount";

    // Syslog sink property names
    /** The property name for the syslog protocol to use */
    public static final String PROP_SYSLOG_PROTOCOL = "syslog.protocol";
    /** The property name for the syslog host to log to */
    public static final String PROP_SYSLOG_HOST = "syslog.host";
    /** The property name for the port to connect to */
    public static final String PROP_SYSLOG_PORT = "syslog.port";
    /** The property name for the facility name to use */
    public static final String PROP_SYSLOG_FACILITY = "syslog.facility";
    /** The property name for the hostname to use in log messages */
    public static final String PROP_SYSLOG_LOG_HOSTNAME = "syslog.logHostname";
    /** The property name for the character set to use for logging */
    public static final String PROP_SYSLOG_CHAR_SET = "syslog.charSet";
    /** The property name for the timezone to use for log messages */
    public static final String PROP_SYSLOG_TIMEZONE = "syslog.timezone";

    public static final String PROP_SYSLOG_PROTOCOL_TCP = "TCP (plain)";
    public static final String PROP_SYSLOG_PROTOCOL_UDP = "UDP";

    private String description;
    private SinkType type;
    private boolean enabled;
    private SeverityThreshold severity;
    private String categories;
    private transient String xmlProperties;
    private Map<String, String> properties;

    /**
     * Drops the existing set of properties and reads the new properties to use from the
     * input XML string.
     * <p/>
     * This should not be called directly. Use the setProperty method instead.
     *
     * @param xml The XML string to parse containing the properties.
     */
    public synchronized void setXmlProperties(String xml) {
        if (xml != null && xml.equals(xmlProperties)) return;
        this.xmlProperties = xml;
        if ( xml != null && xml.length() > 0 ) {
            try {
                XMLDecoder xd = new XMLDecoder(new ByteArrayInputStream(xml.getBytes(PROPERTIES_ENCODING)));
                //noinspection unchecked
                this.properties = (Map<String, String>)xd.readObject();
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
    public synchronized String getXmlProperties() {
        if ( xmlProperties == null ) {
            Map<String, String> properties = this.properties;
            if ( properties == null ) return null;
            BufferPoolByteArrayOutputStream baos = new BufferPoolByteArrayOutputStream();
            try {
                XMLEncoder xe = new XMLEncoder(new NonCloseableOutputStream(baos));
                xe.writeObject(properties);
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
    public SinkType getType() {
        return type;
    }

    /**
     * Updates the type of log sink that this configuration is for.
     *
     * @param type An instance of SinkType
     */
    public synchronized void setType(SinkType type) {
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
    public synchronized String getProperty(String propertyName) {
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
    public synchronized void setProperty(String propertyName, String propertyValue) {
        Map<String,String> properties = this.properties;
        if (properties == null) {
            properties = new HashMap<String, String>();
            this.properties = properties;
        }

        properties.put(propertyName, propertyValue);

        // invalidate cached properties
        xmlProperties = null;
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
}
