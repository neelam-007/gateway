package com.l7tech.objectmodel;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import java.util.LinkedHashSet;
import java.util.Arrays;

/**
 * JAXB support for EntityHeaderSet.
 */
public class EntityHeaderSetType {

    //- PUBLIC

    public EntityHeaderSetType() {
    }

    @SuppressWarnings({"unchecked"})
    public EntityHeaderSetType( final EntityHeaderSet entityHeaderSet ) {
        this.headers = entityHeaderSet == null ? new EntityHeader[0] : (EntityHeader[])entityHeaderSet.toArray( new EntityHeader[entityHeaderSet.size()] );
        this.maxSize = entityHeaderSet == null ? null : entityHeaderSet.getExceededMax();
    }

    @XmlElementWrapper(name="EntityHeaders")
    @XmlElement(name="EntityHeader")
    public EntityHeader[] getHeaders() {
        return headers;
    }

    public void setHeaders(final EntityHeader[] headers) {
        this.headers = headers;
    }

    @XmlAttribute(name="maxSize")
    public Long getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(Long maxSize) {
        this.maxSize = maxSize;
    }

    public EntityHeaderSet toEntityHeaderSet() {
        EntityHeaderSet set;
        
        if ( headers == null ) {
            set = new EntityHeaderSet();
        } else {
           set = new EntityHeaderSet<EntityHeader>( new LinkedHashSet<EntityHeader>( Arrays.asList( headers ) ) );
        }

        if ( maxSize != null ) {
            set.setMaxExceeded( maxSize );            
        }

        return set;
    }

    public static final class EntityHeaderSetTypeAdapter extends XmlAdapter<EntityHeaderSetType,EntityHeaderSet> {
        @Override
        public EntityHeaderSet unmarshal( final EntityHeaderSetType entityHeaderSetType ) throws Exception {
            return entityHeaderSetType.toEntityHeaderSet();
        }

        @Override
        public EntityHeaderSetType marshal( final EntityHeaderSet entityHeaderSet ) throws Exception {
            return new EntityHeaderSetType( entityHeaderSet );
        }
    }

    //- PRIVATE

    private EntityHeader[] headers;
    private Long maxSize;
}
