package com.l7tech.common.security.xml;

import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.SequenceInputStream;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.l7tech.common.mime.PartIterator;
import com.l7tech.common.mime.PartInfo;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.util.CausedIOException;

/**
 * Entity resolver for SOAP attachments.
 *
 * @author Steve Jones
 */
public class AttachmentEntityResolver implements EntityResolver {

    //- PUBLIC

    /**
     * Create an attachment resolver for the given parts.
     *
     * @param partIterator the iterator for MIME parts (may be null)
     * @param partMap the map for recording resolved parts by their system identifier (must not be null)
     */
    public AttachmentEntityResolver(final PartIterator partIterator, final Map<String,PartInfo> partMap) {
        this(partIterator, partMap, null);
    }

    /**
     * Create an attachment resolver for the given parts.
     *
     * <p>Note that if delegation is used for resolution, the resolved entity
     * is NOT considered to be a MIME part (so is not put in the part map)</p>
     *
     * @param partIterator the iterator for MIME parts (may be null)
     * @param partMap the map for recording resolved parts by their system identifier (must not be null)
     * @param delegate The entity resolver to delegate to in the case that a part is not found
     */
    public AttachmentEntityResolver(final PartIterator partIterator,
                                    final Map<String,PartInfo> partMap,
                                    final EntityResolver delegate) {
        this.partIterator = partIterator;
        this.partMap = partMap;
        this.delegate = delegate;
    }

    /**
     * Resolve the given system identifier as a MIME part.
     *
     * @param publicId ignored
     * @param systemId the raw content id (such as "cid:asdf%65eef@somewhere.com")
     * @return the InputSource from which the part can be read.
     * @throws IOException if the part is not found
     */
    public InputSource resolveEntity(final String publicId,
                                     final String systemId) throws SAXException, IOException {
        InputSource is;

        if ( partIterator != null && systemId != null && systemId.startsWith(CONTENTID_PREFIX) ) {
            Collection<PartInfo> parts = getParts();

            String partId = getPartId(systemId);
            if (partId.length() == 0) {
                throw new CausedIOException("Invalid attachment identifier '"+systemId+"'.");    
            }

            PartInfo part = findPart(parts, partId);
            if (part == null) {
                throw new CausedIOException("Attachment not found for identifier '"+partId+"'.");    
            }

            if (partMap != null) {
                partMap.put(systemId, part);
            }

            is = toInputSource(part);
        } else {
            if (delegate == null) {
                throw new IOException("Cannot resolve entity with system identifier '"+toString(systemId)+"', public identifier '"+toString(publicId)+"'.");
            }

            is = delegate.resolveEntity(publicId, systemId);
        }

        return is;
    }

    //- PRIVATE

    private static final String CONTENTID_PREFIX = "cid:";

    private final PartIterator partIterator;
    private final Map<String,PartInfo> partMap;
    private final EntityResolver delegate;
    private Collection<PartInfo> parts;

    /**
     *
     */
    private String toString(Object value) {
        String text = "";

        if (value != null) {
            text = value.toString();
        }

        return text;
    }

    /**
     *
     */
    private String getPartId(final String id) {
        // TODO converting %hh hex-escaped characters to their ASCII equivalents 
        return id.substring(CONTENTID_PREFIX.length());
    }

    /**
     *
     */
    private PartInfo findPart(final Collection<PartInfo> parts, final String partId) {
        PartInfo part = null;

        for (PartInfo partInfo : parts) {
            String partIdentifier = partInfo.getContentId(true);

            if ( partIdentifier!=null && partIdentifier.equals(partId) ) {
                part = partInfo;
                break;
            }
        }

        return part;
    }

    /**
     *
     */
    private Collection<PartInfo> getParts() throws IOException {
        Collection<PartInfo> parts = this.parts;

        if ( parts == null && partIterator != null) {
            parts = toParts(partIterator);
            this.parts = parts;
        }

        return parts;
    }

    /**
     *
     */
    private Collection<PartInfo> toParts(final PartIterator partIterator) throws IOException {
        List<PartInfo> parts = new ArrayList();

        try {
            if ( partIterator != null ) {
                while ( partIterator.hasNext() ) {
                    parts.add( partIterator.next() );
                }
            }
        } catch ( NoSuchPartException nspe ) {
            throw new CausedIOException( nspe );
        }

        return Collections.unmodifiableCollection(parts);
    }

    /**
     *
     */
    private InputSource toInputSource(final PartInfo partInfo) throws IOException {
        try {
            InputStream bodyIn = partInfo.getInputStream(false);
            InputStream headersIn = new ByteArrayInputStream(partInfo.getHeaders().toByteArray());

            //noinspection IOResourceOpenedButNotSafelyClosed
            SequenceInputStream sis = new SequenceInputStream(headersIn, bodyIn);

            return new InputSource(sis);
        }  catch ( NoSuchPartException nspe ) {
            throw new CausedIOException( nspe );
        }
    }
}
