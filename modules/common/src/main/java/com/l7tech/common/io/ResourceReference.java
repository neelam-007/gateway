package com.l7tech.common.io;

/**
 * A reference to a resource.
 */
public class ResourceReference {

    //- PUBLIC

    /**
     * Create a new resource reference, typically for an XML Schema.
     *
     * @param baseUri The URI of the document containing the reference (may be null)
     * @param uri The URI of the reference as found in the document (may be null)
     * @param hasTargetNamespace Does this reference include a target namespace.
     * @param targetNamespace The target namespace of the reference (may be null)
     */
    public ResourceReference( final String baseUri,
                              final String uri,
                              final boolean hasTargetNamespace,
                              final String targetNamespace ) {
        this( baseUri, uri, hasTargetNamespace, targetNamespace, null );
    }

    /**
     * Create a new resource reference, typically for a DTD.
     *
     * @param baseUri The URI of the document containing the reference (may be null)
     * @param uri The URI of the reference as found in the document (may be null)
     * @param publicIdentifier The public identifier of the reference (may be null)
     */
    public ResourceReference( final String baseUri,
                              final String uri,
                              final String publicIdentifier ) {
        this( baseUri, uri, false, null, publicIdentifier );
    }

    /**
     * Create a new resource reference.
     *
     * @param baseUri The URI of the document containing the reference (may be null)
     * @param uri The URI of the reference as found in the document (may be null)
     * @param hasTargetNamespace Does this reference include a target namespace.
     * @param targetNamespace The target namespace of the reference (may be null)
     * @param publicIdentifier The public identifier of the reference (may be null)
     */
    public ResourceReference( final String baseUri,
                              final String uri,
                              final boolean hasTargetNamespace,
                              final String targetNamespace,
                              final String publicIdentifier ) {
        this.baseUri = baseUri;
        this.uri = uri;
        this.hasTargetNamespace = hasTargetNamespace;
        this.targetNamespace = targetNamespace;
        this.publicIdentifier = publicIdentifier;    
    }

    /**
     * Get the base URI for the reference.
     *
     * <p>This may be an absolute or relative URI.</p>
     *
     * @return The base URI (may be null)
     */
    public String getBaseUri() {
        return baseUri;
    }

    /**
     * Get the URI for the reference.
     *
     * <p>This may be an absolute or relative URI.</p>
     *
     * @return The uri (may be null)
     */
    public String getUri() {
        return uri;
    }

    /**
     * Does this reference include a target namespace
     *
     * @return True if there is a target namespace.
     */
    public boolean hasTargetNamespace() {
        return hasTargetNamespace;
    }

    /**
     * Get the target namespace for the reference.
     *
     * <p>Note that the target namespace may be null even when
     * <code>hasTargetNamespace</code> is true.</p>
     *
     * @return The target namespace (may be null)
     */
    public String getTargetNamespace() {
        return targetNamespace;
    }

    /**
     * Get the public identifier for the reference.
     *
     * @return The public identifier (may be null)
     */
    public String getPublicIdentifier() {
        return publicIdentifier;
    }

    //- PRIVATE

    private final String baseUri;
    private final String uri;
    private final boolean hasTargetNamespace;
    private final String targetNamespace;
    private final String publicIdentifier;

}
