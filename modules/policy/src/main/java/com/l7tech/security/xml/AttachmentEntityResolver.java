package com.l7tech.security.xml;

import com.l7tech.common.io.ByteLimitInputStream;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.mime.PartInfo;
import com.l7tech.common.mime.PartIterator;
import com.l7tech.util.CausedIOException;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

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
     * @param delegate The entity resolver to delegate to in the case that a part is not found (may be null)
     * @throws IOException if there is a partIterator and positioning fails
     */
    public AttachmentEntityResolver(final PartIterator partIterator,
                                    final EntityResolver delegate) throws IOException {
        this(partIterator, delegate, new HashMap<String,PartInfo>(), 0);
    }

    /**
     * Create an attachment resolver for the given parts.
     *
     * <p>Note that if delegation is used for resolution, the resolved entity
     * is NOT considered to be a MIME part (so is not put in the part map)</p>
     *
     * @param partIterator the iterator for MIME parts (may be null)
     * @param delegate The entity resolver to delegate to in the case that a part is not found (may be null)
     * @param partMap the map for recording resolved parts by their system identifier (must not be null)
     * @param sizeLimit The maximum size for an attachment (0 for no limit)
     */
    public AttachmentEntityResolver(final PartIterator partIterator,
                                    final EntityResolver delegate,
                                    final Map<String,PartInfo> partMap,
                                    final long sizeLimit) throws IOException {
        this.partIterator = partIterator;
        this.partMap = partMap;
        this.delegate = delegate;
        this.sizeLimit = sizeLimit;

        positionAfterFirstPart();
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

    private static final Logger logger = Logger.getLogger(AttachmentEntityResolver.class.getName());

    private static final String CONTENTID_PREFIX = "cid:";

    private final PartIterator partIterator;
    private final Map<String,PartInfo> partMap;
    private final EntityResolver delegate;
    private final long sizeLimit;
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
            ensureNoDuplicates(parts);
            this.parts = parts;
        }

        return parts;
    }

    /**
     *
     */
    private void positionAfterFirstPart() throws IOException {
        if (partIterator != null && partIterator.hasNext()) {
            try {
                partIterator.next();
            } catch ( NoSuchElementException nspe ) {
                throw new CausedIOException( nspe );
            }
        }
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
        } catch ( NoSuchElementException nspe ) {
            throw new CausedIOException( nspe );
        }

        return Collections.unmodifiableCollection(parts);
    }

    /**
     * Ignore all attachments if content-ids look dubious 
     */
    private void ensureNoDuplicates(Collection<PartInfo> parts) {
        Set<String> identifiers = new HashSet();

        for ( PartInfo partInfo : parts ) {
            String partIdentifier = partInfo.getContentId(true);

            if (partIdentifier != null) {

                if (partIdentifier.startsWith("<")) {
                    logger.log(Level.WARNING,
                            "Ignoring attachments for message with invalid content-id ''{0}''.",
                            partIdentifier);
                    parts.clear();
                    break;
                }

                if (identifiers.contains(partIdentifier)) {
                    logger.log(Level.WARNING,
                            "Ignoring attachments for message with duplicated content-id ''{0}''.",
                            partIdentifier);
                    parts.clear();                              
                    break;
                }

                identifiers.add(partIdentifier);
            }
        }
    }

    /**
     *
     */
    private InputSource toInputSource(final PartInfo partInfo) throws IOException {
        try {
            InputStream bodyIn = partInfo.getInputStream(false);
            InputStream headersIn = new ByteArrayInputStream(partInfo.getHeaders().toByteArray());

            //noinspection IOResourceOpenedButNotSafelyClosed
            InputStream is = new SequenceInputStream(headersIn, bodyIn);

            if ( sizeLimit > 0 ) {
                is = new ByteLimitInputStream(is, 16, sizeLimit);
            }

            return new InputSource(is);
        }  catch ( NoSuchPartException nspe ) {
            throw new CausedIOException( nspe );
        }
    }
}
