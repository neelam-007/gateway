/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Aug 15, 2005<br/>
 */
package com.l7tech.cluster;

import com.l7tech.objectmodel.imp.NamedEntityImp;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * A row in the cluster_properties table. On the server-side, this is managed through
 * the ClusterPropertyManager, and on the client side, through the ClusterStatusAdmin interface.
 *
 * @author flascelles@layer7-tech.com
 */
@XmlRootElement
public class ClusterProperty extends NamedEntityImp {
    private static final long serialVersionUID = 1L;

    private String value;
    private String description;
    public static final Pattern PATTERN_MID_DOTS = Pattern.compile("\\.([a-zA-Z0-9_])");

    public ClusterProperty() { }

    public ClusterProperty(String name, String value) {
        this._name = name;
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /** @return true if this property should be hidden in the cluster property GUI. */
    public boolean isHiddenInGui() {
        // Currently, there's only 1 hidden property, so for now we'll just hardcode it rather than
        // add a whole new DB column and support code
        return "license".equals(_name)
            || "audit.acknowledge.highestTime".equals(_name);
    }

    public boolean equals(Object other) {
        ClusterProperty cp = (ClusterProperty)other;
        if (cp == null) return false;
        if (_oid != cp._oid) return false;
        if (!(_name.equals(cp._name))) return false;
        if (!(value.equals(cp.value))) return false;
        return true;
    }

    public int hashCode() {
        int result;
        result = (value != null ? value.hashCode() : 0);
        result = 29 * result + (_name != null ? _name.hashCode() : 0);
        result = 29 * result + (int)(_oid ^ (_oid >>> 32));
        return result;
    }

    /**
     * Converts a cluster property name like "foo.bar.blatzBloof.bargleFoomp" into a ServerConfig property
     * root like "fooBarBlatzBlofBargleFoomp".
     *
     * @param clusterPropertyName the cluster property name to convert, ie "trfl.logs.enableShared"
     * @return the corresponding serverConfig property name, ie "trflLogsEnableShared".  Never null.
     */
    public static String asServerConfigPropertyName(String clusterPropertyName) {
        Matcher matcher = PATTERN_MID_DOTS.matcher(clusterPropertyName);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, matcher.group(1).toUpperCase());
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}