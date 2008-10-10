/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Aug 15, 2005<br/>
 */
package com.l7tech.gateway.common.cluster;

import com.l7tech.objectmodel.imp.NamedEntityImp;

import javax.persistence.*;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.HashSet;

/**
 * A row in the cluster_properties table. On the server-side, this is managed through
 * the ClusterPropertyManager, and on the client side, through the ClusterStatusAdmin interface.
 *
 * @author flascelles@layer7-tech.com
 */
@XmlRootElement
@Entity
@Table(name="cluster_properties")
@AttributeOverride(name="name", column=@Column(name="propKey", nullable=false, unique=true))
public class ClusterProperty extends NamedEntityImp {
    private static final long serialVersionUID = 1L;
    public static final Pattern PATTERN_MID_DOTS = Pattern.compile("\\.([a-zA-Z0-9_])");

    private String value;
    private String description;

    // Currently, there are only a few hidden properties
    // just hardcoded for now, rather than having a whole new DB column and support code
    private static HashSet<String> hiddenInGui = new HashSet<String>();
    static {
        hiddenInGui.add("license");
        hiddenInGui.add("audit.acknowledge.highestTime");
        hiddenInGui.add("audit.archiverInProgress");
        hiddenInGui.add("audit.archiver.ftp.config");
        hiddenInGui.add("krb5.keytab");
        hiddenInGui.add("keyStore.defaultSsl.alias");
        hiddenInGui.add("keyStore.defaultCa.alias");
    }

    public ClusterProperty() { }

    public ClusterProperty(String name, String value) {
        this._name = name;
        this.value = value;
    }

    @Column(name="propValue", nullable=false, length=Integer.MAX_VALUE)
    @Lob
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Transient
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Check if this property is allowed to be used by the SSM or from within Policies.
     * Within a policy if this returns true, it will seem as this property doesn't exist
     *  @return true if this property should be hidden from users */
    @Transient
    public boolean isHiddenProperty() {
        // Currently, there's only a few hidden properties, so for now we'll just hardcode it rather than
        // add new metadata and support code
        return hiddenInGui.contains(_name);
    }

    public boolean equals(Object other) {
        if (! (other instanceof ClusterProperty) )
            return false;
        ClusterProperty cp = (ClusterProperty)other;
        return _oid == cp._oid && _name.equals(cp._name) && value.equals(cp.value);
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