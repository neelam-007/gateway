package com.l7tech.gateway.common.custom;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.policy.assertion.ext.message.CustomContentType;

import org.jetbrains.annotations.NotNull;

import java.nio.charset.Charset;

/**
 * Convert ContentTypeHeader to CustomContentHeader.
 */
public final class ContentTypeHeaderToCustomConverter implements CustomContentType {

    private final ContentTypeHeader contentTypeHeader;
    final public ContentTypeHeader getContentTypeHeader() {
        return contentTypeHeader;
    }

    public ContentTypeHeaderToCustomConverter(@NotNull final ContentTypeHeader contentTypeHeader) {
        this.contentTypeHeader = contentTypeHeader;
    }

    @Override
    public Charset getEncoding() {
        return getContentTypeHeader().getEncoding();
    }

    @Override
    public String getType() {
        return getContentTypeHeader().getType();
    }

    @Override
    public String getSubtype() {
        return getContentTypeHeader().getSubtype();
    }

    @Override
    public String getFullValue() {
        return getContentTypeHeader().getFullValue();
    }

    @Override
    public String getMultipartBoundary() throws IllegalStateException {
        return getContentTypeHeader().getMultipartBoundary();
    }

    @Override
    public boolean isText() {
        return getContentTypeHeader().isText();
    }

    @Override
    public boolean isApplication() {
        return getContentTypeHeader().isApplication();
    }

    @Override
    public boolean isApplicationFormUrlEncoded() {
        return getContentTypeHeader().isApplicationFormUrlEncoded();
    }

    @Override
    public boolean isJson() {
        return getContentTypeHeader().isJson();
    }

    @Override
    public boolean isMultipart() {
        return getContentTypeHeader().isMultipart();
    }

    @Override
    public boolean isXml() {
        return getContentTypeHeader().isXml();
    }

    @Override
    public boolean isHtml() {
        return getContentTypeHeader().isHtml();
    }

    @Override
    public boolean isSoap12() {
        return getContentTypeHeader().isSoap12();
    }

    @Override
    public boolean matches(final CustomContentType contentType) {
        // just in case it's not do a manual match
        return matches(contentType.getType(), contentType.getSubtype());
    }

    @Override
    public boolean matches(final String type, final String subtype) {
        return getContentTypeHeader().matches(type, subtype);
    }
}
