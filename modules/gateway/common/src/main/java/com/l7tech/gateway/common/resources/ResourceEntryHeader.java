package com.l7tech.gateway.common.resources;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;

/**
 * Entity header for resource entries.
 */
public class ResourceEntryHeader extends EntityHeader {

    //- PUBLIC

    public ResourceEntryHeader( final ResourceEntry resourceEntry ) {
        this( resourceEntry.getId(),
              resourceEntry.getUri(),
              resourceEntry.getDescription(),
              resourceEntry.getType(),
              resourceEntry.getResourceKey1(),
              resourceEntry.getResourceKey2(),
              resourceEntry.getResourceKey3(),
              resourceEntry.getVersion() );
    }

    public ResourceEntryHeader( final String id,
                                final String uri,
                                final String description,
                                final ResourceType resourceType,
                                final String resourceKey1,
                                final String resourceKey2,
                                final String resourceKey3,
                                final Integer version ) {
        super( id, EntityType.RESOURCE_ENTRY, null, description, version );

        this.uri = uri;
        this.resourceType = resourceType;
        this.resourceKey1 = resourceKey1;
        this.resourceKey2 = resourceKey2;
        this.resourceKey3 = resourceKey3;
    }

    public String getResourceKey1() {
        return resourceKey1;
    }

    public String getResourceKey2() {
        return resourceKey2;
    }

    public String getResourceKey3() {
        return resourceKey3;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public String getUri() {
        return uri;
    }

    //- PRIVATE

    private String uri;
    private ResourceType resourceType;
    private String resourceKey1;
    private String resourceKey2;
    private String resourceKey3;
}
