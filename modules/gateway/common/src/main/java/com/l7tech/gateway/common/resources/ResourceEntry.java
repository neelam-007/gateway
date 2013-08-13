package com.l7tech.gateway.common.resources;

import com.l7tech.objectmodel.imp.ZoneableGoidEntityImp;
import com.l7tech.util.Charsets;
import com.l7tech.util.HexUtils;
import org.hibernate.annotations.Proxy;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * Persistent entity representing a global resource.
 */
@Entity
@Proxy(lazy = false)
@Table(name = "resource_entry")
public class ResourceEntry extends ZoneableGoidEntityImp {

    //- PUBLIC

    @NotNull
    @Size(min = 0, max = 104857600)
    @Column(name = "content", length = Integer.MAX_VALUE)
    @Lob
    public String getContent() {
        return content;
    }

    public void setContent( final String content ) {
        this.content = content;
    }

    @NotNull
    @Size(min = 3, max = 255)
    @Column(name = "content_type", length = 255)
    public String getContentType() {
        return contentType;
    }

    public void setContentType( final String contentType ) {
        this.contentType = contentType;
    }

    @Size(min = 0, max = 255)
    @Column(name = "description", length = 255)
    public String getDescription() {
        return description;
    }

    public void setDescription( final String description ) {
        this.description = description;
    }

    @Size(min = 0, max = 4096)
    @Column(name = "resource_key1", length = 4096)
    public String getResourceKey1() {
        return resourceKey1;
    }

    public void setResourceKey1( final String resourceKey1 ) {
        this.resourceKey1 = resourceKey1;
    }

    @Size(min = 0, max = 4096)
    @Column(name = "resource_key2", length = 4096)
    public String getResourceKey2() {
        return resourceKey2;
    }

    public void setResourceKey2( final String resourceKey2 ) {
        this.resourceKey2 = resourceKey2;
    }

    @Size(min = 0, max = 4096)
    @Column(name = "resource_key3", length = 4096)
    public String getResourceKey3() {
        return resourceKey3;
    }

    public void setResourceKey3( final String resourceKey3 ) {
        this.resourceKey3 = resourceKey3;
    }

    @NotNull
    @Column(name = "type", length = 32)
    @Enumerated(EnumType.STRING)
    public ResourceType getType() {
        return type;
    }

    public void setType( final ResourceType type ) {
        this.type = type;
    }

    @NotNull
    @Size(min = 1, max = 4096)
    @Column(name = "uri", length = 4096)
    public String getUri() {
        return uri;
    }

    public void setUri( final String uri ) {
        this.uri = uri;
        this.uriHash = null;
    }

    @NotNull
    @Size(min = 0, max = 128)
    @Column(name = "uri_hash", length = 128)
    public String getUriHash() {
        if ( uriHash == null ) updateUriHash();
        return uriHash;
    }

    public void setUriHash( final String uriHash ) {
        this.uriHash = uriHash;
    }

    @Override
    @Version
    @Column(name="version")
    public int getVersion() {
        return super.getVersion();
    }

    //- PRIVATE

    private String description;
    private String uri;
    private String uriHash;
    private ResourceType type;
    private String contentType;
    private String content;
    private String resourceKey1;
    private String resourceKey2;
    private String resourceKey3;

    private void updateUriHash() {
        if ( uri != null ) {
            uriHash = HexUtils.encodeBase64(HexUtils.getSha512Digest(uri.getBytes( Charsets.UTF8)), true);
        }
    }

}
