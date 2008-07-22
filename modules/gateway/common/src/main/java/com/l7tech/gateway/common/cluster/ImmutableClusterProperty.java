package com.l7tech.gateway.common.cluster;

/**
 * Immutable extension of ClusterProperty class.
 *
 * @author Steve Jones
 */
public final class ImmutableClusterProperty extends ClusterProperty {

    //- PUBLIC

    /**
     * Create an immutable version of the given cluster property.
     *
     * @param clusterProperty
     * @throws IllegalArgumentException if the name of the given property is null.
     */
    public ImmutableClusterProperty(ClusterProperty clusterProperty) {
        oid = clusterProperty.getOid();
        id = clusterProperty.getId();
        version = clusterProperty.getVersion();
        name = clusterProperty.getName();
        value = clusterProperty.getValue()==null ? "" : clusterProperty.getValue();
        description = clusterProperty.getDescription();

        if (name == null)
            throw new IllegalArgumentException("Null name for cluster property with oid '" + oid + "'.");
    }

    public long getOid() {
        return oid;
    }

    public String getId() {
        return id;
    }

    public int getVersion() {
        return version;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }

    public void setOid(long oid) {
        throw new UnsupportedOperationException();
    }

    public void setVersion(int version) {
        throw new UnsupportedOperationException();
    }

    public void setName(String name) {
        throw new UnsupportedOperationException();
    }

    public void setValue(String value) {
        throw new UnsupportedOperationException();
    }

    public void setDescription(String description) {
        throw new UnsupportedOperationException();
    }

    public boolean equals(Object other) {
        ClusterProperty cp = (ClusterProperty)other;
        if (cp == null) return false;
        if (_oid != cp.getOid()) return false;
        if (!_name.equals(cp.getName())) return false;
        if (!value.equals(cp.getValue())) return false;
        return true;
    }

    public int hashCode() {
        int result;
        result = value.hashCode();
        result = 29 * result + _name.hashCode();
        result = 29 * result + (int)(_oid ^ (_oid >>> 32));
        return result;
    }

    //- PRIVATE

    private final long oid;
    private final String id;
    private final int version;
    private final String name;
    private final String value;
    private final String description;
}
