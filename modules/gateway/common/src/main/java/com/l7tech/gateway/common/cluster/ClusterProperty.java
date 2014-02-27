/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Aug 15, 2005<br/>
 */
package com.l7tech.gateway.common.cluster;

import com.l7tech.security.rbac.RbacAttribute;
import com.l7tech.objectmodel.imp.NamedEntityWithPropertiesImp;
import org.hibernate.annotations.Proxy;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A row in the cluster_properties table. On the server-side, this is managed through
 * the ClusterPropertyManager, and on the client side, through the ClusterStatusAdmin interface.
 *
 * @author flascelles@layer7-tech.com
 */
@XmlRootElement
@Entity
@Proxy(lazy=false)
@Table(name="cluster_properties")
@AttributeOverride(name="name", column=@Column(name="propKey", nullable=false, unique=true))
public class ClusterProperty extends NamedEntityWithPropertiesImp implements Comparable {
    private static final long serialVersionUID = 1L;
    public static final Pattern PATTERN_MID_DOTS = Pattern.compile("\\.([a-zA-Z0-9_])");
    public static final String DESCRIPTION_PROPERTY_KEY = "description";

    private String value;

    // Cluster properties that are hidden in the cluster properties GUI _even if_ they are customized with a non-default value.
    // Currently, there are only a few hidden properties
    // just hardcoded for now, rather than having a whole new DB column and support code
    private static HashSet<String> hiddenInGui = new HashSet<String>();
    static {
        hiddenInGui.add("license");
        hiddenInGui.add("audit.acknowledge.highestTime");
        hiddenInGui.add("audit.archiverInProgress");
        hiddenInGui.add("audit.archiver.ftp.config");
        hiddenInGui.add("audit.sink.policy.guid");
        hiddenInGui.add("audit.lookup.policy.guid");
        hiddenInGui.add("trace.policy.guid");
        hiddenInGui.add("audit.sink.alwaysSaveInternal");
        hiddenInGui.add("krb5.keytab");
        hiddenInGui.add("krb5.keytab.validate");
        hiddenInGui.add("keyStore.defaultSsl.alias");
        hiddenInGui.add("keyStore.defaultCa.alias");
        hiddenInGui.add("keyStore.auditViewer.alias");
        hiddenInGui.add("keyStore.auditSigning.alias");
        hiddenInGui.add("keyStore.luna.pinFinder"); // Currently no such cluster property, but just in case
        hiddenInGui.add("keyStore.luna.encryptedLunaPin");
        hiddenInGui.add("keyStore.luna.lunaSlotNum");
        hiddenInGui.add("keyStore.luna.installAsLeastPreference");
        hiddenInGui.add("security.jceProviderEngineName");
        hiddenInGui.add("interfaceTags");
        hiddenInGui.add("ioHttpProxy");
    }

    public ClusterProperty() { }

    public ClusterProperty(String name, String value) {
        this._name = name;
        this.value = value;
    }

    @RbacAttribute
    @Size(min=1,max=128)
    @Transient
    @Override
    public String getName() {
        return super.getName();
    }

    @NotNull
    @Size(min=0,max=131072) // limit to 128k
    @Column(name="propValue", nullable=false, length=Integer.MAX_VALUE)
    @Lob
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
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

    @SuppressWarnings({ "RedundantIfStatement" })
    @Override
    public boolean equals( final Object o ) {
        if ( this == o ) return true;
        if ( o == null || getClass() != o.getClass() ) return false;
        if ( !super.equals( o ) ) return false;

        final ClusterProperty that = (ClusterProperty) o;

        if ( value != null ? !value.equals( that.value ) : that.value != null ) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = (value != null ? value.hashCode() : 0);
        result = 29 * result + (_name != null ? _name.hashCode() : 0);
        result = 29 * result + getGoid().hashCode();
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

    @Override
    public int compareTo(Object o) {
        if (o == null || ! (o instanceof ClusterProperty)) throw new IllegalArgumentException("The compared object must be a ClusterProperty.");
        String originalPropName = getName();
        String comparedPropName = ((ClusterProperty)o).getName();
        if (originalPropName == null || comparedPropName == null) throw new NullPointerException("Cluster Property Name must not be null.");

        return originalPropName.toLowerCase().compareTo(comparedPropName.toLowerCase());
    }
}