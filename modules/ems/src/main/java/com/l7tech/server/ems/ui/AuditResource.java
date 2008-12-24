package com.l7tech.server.ems.ui;

import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;
import org.apache.wicket.util.resource.ResourceStreamNotFoundException;
import org.apache.wicket.util.value.ValueMap;
import org.apache.wicket.util.time.Time;
import org.apache.wicket.protocol.http.WebResponse;
import org.apache.wicket.IClusterable;

import java.io.InputStream;
import java.io.IOException;
import java.util.logging.Logger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.Locale;

import com.l7tech.server.audit.AuditDownloadManager;
import com.l7tech.util.OpaqueId;
import com.l7tech.gateway.common.security.rbac.AttemptedReadAll;
import com.l7tech.objectmodel.EntityType;

/**
 * Web resource for audit downloads. 
 */
public class AuditResource extends SecureResource {

    //- PUBLIC

    /**
     * 
     */
    public AuditResource() {
        super( new AttemptedReadAll(EntityType.AUDIT_RECORD) );
    }

    //- PROTECTED

    @Override
    protected IResourceStream getSecureResourceStream() {
        IResourceStream resource = null;
        ValueMap parameters = getParameters();

        if ( parameters.containsKey("start") &&
             parameters.containsKey("end") ) {
            String start = parameters.getString("start");
            String end = parameters.getString("end");

            AuditDownloadManager manager = getAuditDownloadManager();
            if ( manager != null ) {
                try {
                    resource = new AuditResourceStream( manager, Long.parseLong(start), Long.parseLong(end) );
                } catch ( NumberFormatException nfe ) {
                    logger.fine("Invalid audit download request start '"+start+"', end '"+end+"'.");
                }
            }
        }

        if ( resource == null ){
            logger.warning("Not processing resource request for audit data, download details not found.");
            resource = new StringResourceStream( "" );
        }

        return resource;
    }

    @Override
    protected void setHeaders(WebResponse webResponse) {
        super.setHeaders(webResponse);
        webResponse.setAttachmentHeader( AUDIT_FILENAME );
    }

    //- PACKAGE

    static AuditDownloadManager getAuditDownloadManager() {
        return AuditDownloadManagerRef.get();
    }

    static void setAuditDownloadManager( final AuditDownloadManager auditDownloadManager ) {
        AuditDownloadManagerRef.set( auditDownloadManager );
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(AuditResource.class.getName());
    private static final String AUDIT_FILENAME = "audits.zip";

    private static AtomicReference<AuditDownloadManager> AuditDownloadManagerRef = new AtomicReference<AuditDownloadManager>();

    private final class AuditResourceStream implements IClusterable, IResourceStream {
        private Locale locale;
        private final long created;
        private final long start;
        private final long end;
        private final AuditDownloadManager manager;
        private OpaqueId downloadId;

        public AuditResourceStream( final AuditDownloadManager manager, final long start, final long end ) {
            this.created = System.currentTimeMillis();
            this.start = start;
            this.end = end;
            this.manager = manager;
        }

        public String getContentType() {
            return "application/zip";
        }

        public long length() {
            return -1;
        }

        public InputStream getInputStream() throws ResourceStreamNotFoundException {
            if ( downloadId == null ) {
                try {
                    this.downloadId = manager.createDownloadContext( start, end, null );
                } catch ( IOException ioe ) {
                    throw new ResourceStreamNotFoundException(ioe);
                }
            }

            return manager.getDownloadInputStream( downloadId );
        }

        public void close() throws IOException {
            if ( downloadId != null ) {
                manager.closeDownloadContext( downloadId );
            }
        }

        public Locale getLocale() {
            return locale;
        }

        public void setLocale(Locale locale) {
            this.locale = locale;
        }

        public Time lastModifiedTime() {
            return Time.valueOf( created );
        }
    }
}