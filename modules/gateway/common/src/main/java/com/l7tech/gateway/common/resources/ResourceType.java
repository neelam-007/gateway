package com.l7tech.gateway.common.resources;

/**
 *
 */
public enum ResourceType {

    //- PUBLIC

    /**
     * An XML Schema
     */
    XML_SCHEMA("xsd", "text/xml"),

    /**
     * A DTD or entity referenced from a DTD
     */
    DTD("dtd", "text/plain");

    /**
     * Get the filename suffix commonly used with the resource type.
     *
     * @return The filename suffix.
     */
    public String getFilenameSuffix() {
        return filenameSuffix;
    }

    /**
     * Get the mime type commonly used with the resource type.
     *
     * @return The mime type.
     */
    public String getMimeType() {
        return mimeType;
    }

    //- PRIVATE

    private final String filenameSuffix;
    private final String mimeType;

    private ResourceType( final String filenameSuffix,
                          final String mimeType ) {
        this.filenameSuffix = filenameSuffix;
        this.mimeType = mimeType;
    }
}
