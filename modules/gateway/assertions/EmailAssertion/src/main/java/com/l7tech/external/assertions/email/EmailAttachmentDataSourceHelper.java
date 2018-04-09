package com.l7tech.external.assertions.email;

import com.l7tech.common.mime.MimeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.mime.PartInfo;
import com.l7tech.common.mime.PartIterator;
import com.l7tech.message.Message;
import org.apache.commons.lang3.StringUtils;

import javax.mail.internet.ContentDisposition;
import javax.mail.internet.ParseException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.external.assertions.email.EmailAttachmentException.INVALID_ATTACHMENT_NAME;
import static com.l7tech.external.assertions.email.EmailAttachmentException.INVALID_ATTACHMENT_SOURCE_VARIABLE;
import static com.l7tech.external.assertions.email.EmailAttachmentException.MISSING_ATTACHMENT_NAME_FROM_MIMEPART;
import static com.l7tech.external.assertions.email.EmailExceptionHelper.*;

/**
 * Helper class to get EmailAttachmentDataSource implementation from the source context variable.
 */
public final class EmailAttachmentDataSourceHelper {

    private static final Logger LOGGER = Logger.getLogger(EmailAttachmentDataSourceHelper.class.getName());

    private EmailAttachmentDataSourceHelper() {}

    /**
     * Gets the Email Attachment data source using the filename and the source object. Supported source objects are
     * com.l7tech.common.mime.PartInfo and com.l7tech.message.Message.
     * @param name
     * @param source
     * @return
     * @throws EmailAttachmentException
     * @throws IOException
     */
    public static EmailAttachmentDataSource getAttachmentDataSource(final EmailAttachment attachment, final Object source, final String name)
            throws EmailAttachmentException, IOException {
        if (source instanceof PartInfo) {
            return getAttachmentDataSource(name, (PartInfo) source);
        } else if (source instanceof Message) {
            return getAttachmentDataSource(name, ((Message) source).getMimeKnob().getFirstPart());
        } else {
            throw newAttachmentException(INVALID_ATTACHMENT_SOURCE_VARIABLE, attachment.getSourceVariable());
        }
    }

    /**
     * Returns the list of Email attachment sources.
     * @param attachment
     * @param source
     * @return list of attachment data sources
     * @throws EmailAttachmentException
     */
    public static List<EmailAttachmentDataSource> getAttachmentDataSources(final EmailAttachment attachment, final Object source) throws EmailAttachmentException {
        final List<EmailAttachmentDataSource> attachments = new ArrayList<>();

        if (source instanceof PartInfo) {
            attachments.add(getAttachmentDataSource((PartInfo) source));
        } else if (source instanceof PartInfo[]) {
            for (final PartInfo partInfo : (PartInfo[]) source) {
                attachments.add(getAttachmentDataSource(partInfo));
            }
        } else if (source instanceof Message) {
            final PartIterator it = ((Message) source).getMimeKnob().iterator();
            while (it.hasNext()) {
                attachments.add(getAttachmentDataSource(it.next()));
            }
        } else {
            throw newAttachmentException(INVALID_ATTACHMENT_SOURCE_VARIABLE, attachment.getSourceVariable());
        }

        return attachments;
    }

    /**
     * Returns attachment data source for the specified MIME part
     * @param partInfo
     * @return Attachment data source
     * @throws EmailAttachmentException
     */
    private static EmailAttachmentDataSource getAttachmentDataSource(final PartInfo partInfo) throws EmailAttachmentException {
        return getAttachmentDataSource(getAttachmentName(partInfo), partInfo);
    }

    /**
     * Returns the Email attachment data source implementation from PartInfo.
     * @param name
     * @param partInfo
     * @return Data source implementation
     */
    private static EmailAttachmentDataSource getAttachmentDataSource(final String name, final PartInfo partInfo) {
        return new EmailAttachmentDataSource() {
            @Override
            public long getContentLength() {
                long length = partInfo.getContentLength();
                if(length == -1) {
                    try {
                        length = partInfo.getActualContentLength();
                    } catch (IOException | NoSuchPartException e) {
                        LOGGER.log(Level.WARNING, "Error reading the actual length of the PartInfo.", e);
                    }
                }
                return length;
            }

            @Override
            public InputStream getInputStream() throws IOException {
                try {
                    return partInfo.getInputStream(false);
                } catch (NoSuchPartException e) {
                    throw new IOException(e);
                }
            }

            @Override
            public OutputStream getOutputStream() {
                throw new UnsupportedOperationException();
            }

            @Override
            public String getContentType() {
                return partInfo.getContentType().getMainValue();
            }

            @Override
            public String getName() {
                return name;
            }
        };
    }

    /**
     * Returns the filename from PartInfo Content-Disposition header.
     * @param partInfo
     * @return
     * @throws EmailAttachmentException
     */
    private static String getAttachmentName(final PartInfo partInfo) throws EmailAttachmentException {
        final MimeHeader dispositionMimeHeader = partInfo.getHeader("Content-Disposition");
        if(dispositionMimeHeader != null) {
            final String disposition = dispositionMimeHeader.getFullValue();
            try {
                final ContentDisposition contentDisposition = new ContentDisposition(disposition);
                final String name = contentDisposition.getParameter("filename");
                if (StringUtils.isBlank(name)) {
                    throw newAttachmentException(INVALID_ATTACHMENT_NAME, disposition);
                }
                return name;
            } catch (ParseException e) {
                throw newAttachmentException(MISSING_ATTACHMENT_NAME_FROM_MIMEPART, disposition, e);
            }
        }

        throw newAttachmentException(MISSING_ATTACHMENT_NAME_FROM_MIMEPART, "Invalid or empty Content-Disposition header");
    }
}
