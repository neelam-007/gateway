package com.l7tech.gateway.common.custom;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.policy.assertion.ext.message.CustomContentHeader;

import org.jetbrains.annotations.NotNull;

import java.nio.charset.Charset;

/**
 * Convert ContentTypeHeader to CustomContentHeader.
 * For now only XML, JSON, TEXT, and BINARY content-types are supported.
 *
 * Add new content-types here, in order to extend CustomAssertions content-type support.
 *
 * @author tveninov
 */
public final class CustomToContentTypeHeaderConverter implements CustomContentHeader {

    public static ContentTypeHeader toContentTypeHeader(final Type type) {
        switch (type){
            case OCTET_STREAM:
                return ContentTypeHeader.OCTET_STREAM_DEFAULT;
            case JSON:
                return ContentTypeHeader.APPLICATION_JSON;
            case TEXT:
                return ContentTypeHeader.TEXT_DEFAULT;
            case XML:
                return ContentTypeHeader.XML_DEFAULT;
        }
        return null;
    }

    private final ContentTypeHeader contentTypeHeader;
    final public ContentTypeHeader getInternalType() {
        return contentTypeHeader;
    }

    public CustomToContentTypeHeaderConverter(@NotNull final ContentTypeHeader contentTypeHeader) {
        this.contentTypeHeader = contentTypeHeader;
    }

    @Override
    public Charset getEncoding() {
        return contentTypeHeader.getEncoding();
    }

    @Override
    public String getType() {
        return contentTypeHeader.getType();
    }

    @Override
    public String getSubtype() {
        return contentTypeHeader.getSubtype();
    }

    @Override
    public String getFullValue() {
        return contentTypeHeader.getFullValue();
    }

    @Override
    public String getMultipartBoundary() throws IllegalStateException {
        return contentTypeHeader.getMultipartBoundary();
    }

    @Override
    public boolean isText() {
        return contentTypeHeader.isText();
    }

    @Override
    public boolean isApplication() {
        return contentTypeHeader.isApplication();
    }

    @Override
    public boolean isApplicationFormUrlEncoded() {
        return contentTypeHeader.isApplicationFormUrlEncoded();
    }

    @Override
    public boolean isJson() {
        return contentTypeHeader.isJson();
    }

    @Override
    public boolean isMultipart() {
        return contentTypeHeader.isMultipart();
    }

    @Override
    public boolean isXml() {
        return contentTypeHeader.isXml();
    }

    @Override
    public boolean isHtml() {
        return contentTypeHeader.isHtml();
    }

    @Override
    public boolean isSoap12() {
        return contentTypeHeader.isSoap12();
    }

    @Override
    public boolean matches(final CustomContentHeader contentHeader) {
        // contentHeader should always be instanceof CustomToContentTypeHeaderConverter
        if (contentHeader instanceof CustomToContentTypeHeaderConverter) {
            return contentTypeHeader.matches(((CustomToContentTypeHeaderConverter)contentHeader).contentTypeHeader);
        }
        // just in case it's not do a manual match
        return matches(contentHeader.getType(), contentHeader.getSubtype());
    }

    @Override
    public boolean matches(final String type, final String subtype) {
        return contentTypeHeader.matches(type, subtype);
    }
}
