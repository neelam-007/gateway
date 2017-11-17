package com.l7tech.gateway.api;

import com.l7tech.gateway.api.impl.AccessorSupport;
import com.l7tech.gateway.api.impl.ElementExtendableAccessibleObject;
import com.l7tech.gateway.api.impl.PropertiesMapType;

import java.util.ArrayList;
import java.util.Map;
import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import static com.l7tech.gateway.api.impl.AttributeExtensibleType.*;

/**
 * The LogSinkMO managed object represents a LogSink entity.
 *
 * <p>LogSinks defines what is logged and where it goes.</p>
 *
 * <p>The Accessor for log sinks supports read and write. Log
 * Sinks can be accessed by name or identifier.</p>
 *
 * <p>The following properties are used for FILE type:
 * <ul>
 *   <li>file.maxSize</li>
 *   <li>file.logCount</li>
 *   <li>file.format</li>
 *   <li>file.rolling</li>
 *   <li>file.interval</li>
 * </ul>
 *
 * <p>The following properties are used for SYSLOG type:
 * <ul>
 *   <li>syslog.protocol</li>
 *   <li>syslog.facility</li>
 *   <li>syslog.logHostname</li>
 *   <li>syslog.charSet</li>
 *   <li>syslog.timezone</li>
 *   <li>syslog.ssl.clientAuth</li>
 *   <li>syslog.ssl.keystore.id</li>
 *   <li>syslog.ssl.key.alias</li>
 *   <li>syslog.forma</li>
 * </ul>
 * </p>
 *
 * @see ManagedObjectFactory#createLogSinkMO()
 */

@XmlRootElement(name="LogSink")
@XmlType(name="LogSinkType", propOrder={"name", "description", "type", "categories", "enabled", "severity", "syslogHostValues", "filters", "properties", "extension", "extensions"})
@AccessorSupport.AccessibleResource(name ="LogSinks")
public class LogSinkMO extends ElementExtendableAccessibleObject {

    //- PUBLIC

    /**
     * Get the name for the log sink (case insensitive, required)
     *
     * <p>LogSink names are unique within their containing LogSink.</p>
     *
     * @return The name of the log sink
     */
    @XmlElement(name="Name", required = true)
    public String getName() {
        return name;
    }

    /**
     * Sets the name for the log sink.
     *
     * @param name The name to use.
     */
    public void setName( final String name ) {
        this.name = name;
    }

    /**
     * Gets the description of the log sink. (required)
     *
     * @return description of the log sink
     */
    @XmlElement(name="Description", required = true)
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description for the log sink.
     *
     * @param description The description of the log sink
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the type of the log sink. (required)
     *
     * @return type of the log sink
     */
    @XmlElement(name="Type", required = true)
    public SinkType getType() {
        return type;
    }

    /**
     * Sets the type for the log sink.
     *
     * @param type The type to use.
     */
    public void setType(SinkType type) {
        this.type = type;
    }

    /**
     * Gets if the log sink is enabled. (required)
     *
     * @return True if log sink is enabled
     */
    @XmlElement(name="Enabled", required = true)
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Sets if the log sink is enabled
     *
     * @param enabled True to enable the log sink.
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Gets the severity threshold of the log sink. (required)
     *
     * @return severity threshold of the log sink
     */
    @XmlElement(name="Severity", required = true)
    public SeverityThreshold getSeverity() {
        return severity;
    }

    /**
     * Sets the severity threshold for the log sink.
     *
     * @param severity The severity threshold to use.
     */
    public void setSeverity(SeverityThreshold severity) {
        this.severity = severity;
    }

    /**
     * Gets the categories of the log sink. (required)
     *
     * @return categories of the log sink
     */
    @XmlElementWrapper(name="Categories", required=true)
    @XmlElement(name="Category", required = true)
    public List<Category> getCategories() {
        return categories;
    }

    /**
     * Sets the categories for the log sink.
     *
     * @param categories The categories to use.
     */
    public void setCategories(List<Category> categories) {
        this.categories = categories;
    }

    /**
     * Gets the syslog hosts of the log sink.
     *
     * @return syslog hosts of the log sink
     */
    public List<String> getSyslogHosts() {
        return unwrap(get(syslogHosts, new ArrayList<>()));
    }

    /**
     * Sets the syslog hosts for the log sink.
     *
     * @param syslogHosts The syslog hosts to use.
     */
    public void setSyslogHosts(List<String> syslogHosts) {
        this.syslogHosts = set(this.syslogHosts, wrap(syslogHosts, AttributeExtensibleStringBuilder));
    }

    @XmlElement(name="SyslogHosts")
    protected AttributeExtensibleStringList getSyslogHostValues() {
        return syslogHosts;
    }

    protected void setSyslogHostValues( final AttributeExtensibleStringList enabledFeatures ) {
        this.syslogHosts = enabledFeatures;
    }

    /**
     * Gets the filters of the log sink. (required)
     *
     * @return filters of the log sink
     */
    @XmlElementWrapper(name="Filters", required=true)
    @XmlElement(name="Filter")
    public List<LogSinkFilter> getFilters() {
        return filters;
    }

    /**
     * Sets the filters for the log sink.
     *
     * @param filters The filters to use.
     */
    public void setFilters(List<LogSinkFilter> filters) {
        this.filters = filters;
    }


    /**
     * Get the properties for the LogSink.
     *
     * @return The properties (may be null)
     */
    @XmlElement(name="Properties")
    @XmlJavaTypeAdapter(PropertiesMapType.PropertiesMapTypeAdapter.class)
    public Map<String, String> getProperties() {
        return properties;
    }

    /**
     * Set the properties for the LogSink.
     *
     * @param properties The properties to use.
     */
    public void setProperties( final Map<String, String> properties ) {
        this.properties = properties;
    }

    @XmlType(name="CategoryType")
    public enum Category{
        LOG,
        TRAFFIC,
        AUDIT,
        SSPC
    }

    @XmlType(name="SinkTypeType")
    public enum SinkType {
        /** Indicates that this is a log file sink. */
        FILE,
        /** Indicates that this is a syslog sink. */
        SYSLOG

    }

    /**
     * Severity Threshold
     */
    @XmlType(name="SeverityThresholdType")
    public enum SeverityThreshold {
        ALL,
        FINEST,
        FINER,
        FINE,
        CONFIG,
        INFO,
        WARNING,
        SEVERE
    }

    //- PACKAGE

    LogSinkMO() {
    }

    //- PRIVATE

    private String name;

    private String description;
    private SinkType type;
    private boolean enabled;
    private SeverityThreshold severity;
    private List<Category> categories;
    private AttributeExtensibleStringList syslogHosts;
    private List<LogSinkFilter> filters;

    private Map<String,String> properties;
}
