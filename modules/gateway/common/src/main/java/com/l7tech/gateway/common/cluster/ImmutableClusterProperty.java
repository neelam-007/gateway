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
        _oid = clusterProperty.getOid();
        _version = clusterProperty.getVersion();
        _name = clusterProperty.getName();
        super.setValue(clusterProperty.getValue()==null ? "" : clusterProperty.getValue());
        super.setXmlProperties(clusterProperty.getXmlProperties());

        if (_name == null)
            throw new IllegalArgumentException("Null name for cluster property with oid '" + _oid + "'.");
    }

    @Override
    public void setOid(long oid) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setVersion(int version) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setName(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setValue(String value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setXmlProperties(String xml) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setProperty(String propertyName, String propertyValue) {
        throw new UnsupportedOperationException();
    }

    //- PRIVATE

}
