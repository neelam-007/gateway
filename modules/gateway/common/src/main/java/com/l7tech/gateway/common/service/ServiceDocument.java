package com.l7tech.gateway.common.service;

import com.l7tech.objectmodel.imp.PersistentEntityImp;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.PropertyResolver;
import static com.l7tech.objectmodel.migration.MigrationMappingSelection.*;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * ServiceDocument represents document that is related to a service.
 *
 * <p>This may be a WSDL, Policy, XML Schema, etc.</p>
 *
 * @author Steve Jones
 */
@XmlRootElement
public class ServiceDocument extends PersistentEntityImp {

    //- PUBLIC

    public ServiceDocument() {
    }

    /**
     * Copy constructor
     *
     * @param serviceDocument The service document to copy
     * @param copyIdentity True to copy the identity and version
     */
    public ServiceDocument( final ServiceDocument serviceDocument,
                            final boolean copyIdentity ) {
        if ( copyIdentity ) {
            setOid( serviceDocument.getOid() );
            setVersion( serviceDocument.getVersion() );
        }
        setServiceId( serviceDocument.getServiceId() );
        setUri( serviceDocument.getUri() );
        setType( serviceDocument.getType() );
        setContentType( serviceDocument.getContentType() );
        setContents( serviceDocument.getContents() );
    }

    /**
     * Get the identifier for the service that is the owner of this document.
     *
     * @return The service identifier
     */
    @Migration(mapName = NONE, mapValue = NONE, resolver = PropertyResolver.Type.SERVICE)
    public long getServiceId() {
        return serviceId;
    }

    /**
     * Set the identifier for the service that is the owner of this document.
     *
     * @param serviceId The service identifier
     */
    public void setServiceId(final long serviceId) {
        this.serviceId = serviceId;
    }

    /**
     * Get the type of this document.
     *
     * <p>This should describe the usage of the document and it's logical
     * contents (e.g. "wsdl-imports", "xml-schema-for-responses").</p>
     *
     * @return The type identifier
     * @see #getContentType
     */
    @NotNull
    @Size(min=1,max=32)
    public String getType() {
        return type;
    }

    /**
     * Set the type of this document.
     *
     * @param type the type for the document
     */
    public void setType(final String type) {
        this.type = type;
    }

    /**
     * The uniform resource identifier for the document.
     *
     * <p>This may be related to the original document location (URL)</p>
     *
     * @return The URI for the document
     */
    @NotNull
    @Size(min=1,max=4096)
    public String getUri() {
        return uri;
    }

    /**
     * Set the uniform resource identifier for the document.
     *
     * @param uri The URI to use
     */
    public void setUri(final String uri) {
        this.uri = uri;
    }

    /**
     * Get the content type for the document contents.
     *
     * <p>The content type, but not the encoding (e.g "text/xml")</p>
     *
     * @return The content type
     */
    @NotNull
    @Size(min=1,max=32)
    public String getContentType() {
        return contentType;
    }

    /**
     * Set the content type for the document contents.
     *
     * @param contentType The content type to use
     */
    public void setContentType(final String contentType) {
        this.contentType = contentType;
    }

    /**
     * Get the contents for the document.
     *
     * @return The document text
     */
    @NotNull
    @Size(min=1,max=5242880)
    public String getContents() {
        return contents;
    }

    /**
     * Set the content for the document.
     *
     * @param contents The document text
     */
    public void setContents(final String contents) {
        this.contents = contents;
    }

    //- PRIVATE

    private long serviceId;
    private String uri;
    private String type;
    private String contentType;
    private String contents;
}
