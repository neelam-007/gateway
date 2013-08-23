package com.l7tech.server.ems.migration;

import com.l7tech.common.io.IOExceptionThrowingInputStream;
import com.l7tech.gateway.common.security.rbac.AttemptedReadSpecific;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.ems.ui.SecureResource;
import com.l7tech.server.ems.util.PgpUtil;
import com.l7tech.server.util.JaasUtils;
import com.l7tech.util.*;
import org.apache.wicket.util.resource.AbstractResourceStream;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.ResourceStreamNotFoundException;
import org.apache.wicket.util.resource.StringResourceStream;
import org.apache.wicket.util.time.Time;
import org.apache.wicket.util.value.ValueMap;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Web resource for migration artifact downloads.
 *
 * TODO remove ownership based access check and restore rbac checks.
 */
public class MigrationArtifactResource extends SecureResource {

    //- PUBLIC

    /**
     *
     */
    public MigrationArtifactResource() {
        super( null );//new AttemptedReadAny(EntityType.ESM_MIGRATION_RECORD) );
        setCacheable( false );
    }

    public static final class MigrationArtifactParameters extends SecureResourceParameters {
        private final String migrationId;
        private final String password;

        public MigrationArtifactParameters( final String disposition,
                                            final String migrationId,
                                            final String password ) {
            super( disposition );
            this.migrationId = migrationId;
            this.password = password;
        }

        public String getMigrationId() {
            return migrationId;
        }

        public String getPassword() {
            return password;
        }
    }

    //- PROTECTED

    @Override
    protected IResourceStream getSecureResourceStream() {
        IResourceStream resource = null;

        final MigrationArtifactParameters parameters = getResourceParameters();
        if ( parameters.getMigrationId() != null ) {
            final String id = parameters.getMigrationId();

            if ( !hasPermission( new AttemptedReadSpecific(EntityType.ESM_MIGRATION_RECORD, id) ) &&
                 !isOwner(id) ) {
                resource = getAccessDeniedStream();
            } else {
                MigrationRecordManager manager = getMigrationRecordManager();
                if ( manager != null ) {
                    try {
                        final MigrationRecord record = manager.findByPrimaryKey( Goid.parseGoid( id ) );
                        if ( record != null ) {
                            if ( parameters.getPassword() == null ) {
                                final String contentType = "application/zip";
                                final Time lastModified = Time.milliseconds( record.getTimeCreated() );
                                resource = buildResourceStream( new Functions.NullaryThrows<InputStream,IOException>(){
                                    @Override
                                    public InputStream call() throws IOException {
                                        return new ByteArrayInputStream( zip(record.serializeXml()) );
                                    }
                                }, contentType, lastModified );
                            } else {
                                String filename = getFilename();
                                if ( filename.endsWith( ".pgp" ) ) filename = filename.substring( 0, filename.length()-4 );
                                final String resourceFilename = filename;
                                final String contentType = "application/octet-stream";
                                final Time lastModified = Time.milliseconds( System.currentTimeMillis() );
                                resource = buildResourceStream( new Functions.NullaryThrows<InputStream,IOException>(){
                                    @Override
                                    public InputStream call() throws IOException {
                                        return new ByteArrayInputStream( encrypt( record.serializeXml(), resourceFilename, record.getTimeCreated(), parameters.getPassword() ) );
                                    }
                                }, contentType, lastModified );
                            }
                        } else {
                            logger.warning("Migration artifact not found when accessing migration resource '"+id+"'.");
                            resource = new StringResourceStream( "" );
                        }
                    } catch ( NumberFormatException nfe ) {
                        logger.warning("Invalid migration id when accessing migration resource '"+id+"'.");
                        resource = new StringResourceStream( "" );
                    } catch (FindException e) {
                        logger.log( Level.WARNING, "Error finding migration.", e );
                    }
                }
            }
        } 

        if ( resource == null ){
            logger.warning("Not processing resource request for migration artifact, download details not found.");
            resource = new StringResourceStream( "" );
        }

        return resource;
    }

    @Override
    protected String getFilename() {
        String name = null;

        final MigrationArtifactParameters parameters = getResourceParameters();
        if ( parameters.getMigrationId() != null ) {
            final String id = parameters.getMigrationId();

            String label = null;
            long time = 0L;
            final MigrationRecordManager manager = getMigrationRecordManager();
            if ( manager != null ) {
                try {
                    final MigrationRecord record = manager.findByPrimaryKey(Goid.parseGoid(id));
                    if ( record != null ) {
                        label = record.getName();
                        if ( label == null || label.length()==0 ) {
                            label = "migration";
                        }
                        time = record.getTimeCreated();
                    }
                } catch ( NumberFormatException nfe ) {
                    logger.warning("Invalid migration id when accessing migration resource '"+id+"'.");
                } catch (FindException e) {
                    logger.log( Level.WARNING, "Error finding migration record for filename.", e );
                }
            }

            final String extension = parameters.getPassword() == null ? "zip" : "xml.pgp";
            if ( label == null ) {
                name = "migration_" + id + "." + extension;
            } else {
                SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd HHmmss");
                name = format.format( new Date(time) ) + "_" + label + "." + extension;
            }
        }

        return name;
    }

    /**
     * Utility method to convert an XML migration record to a ZIP.
     *
     * @param data The migration record XML
     * @return The data
     * @throws IOException If an error occurs.
     */
    public static byte[] zip( final String data ) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream( 10000 );

        ZipOutputStream zipOut = new ZipOutputStream( out );
        zipOut.putNextEntry( new ZipEntry( ZIP_ENTRY_NAME ) );
        zipOut.write( HexUtils.encodeUtf8(data) );
        zipOut.closeEntry();
        zipOut.close();

        return out.toByteArray();
    }

    /**
     * Utility method to extract an XML migration record from a downloaded ZIP.
     *
     * @param inputStream The ZIP file input stream
     * @return The data or null if not found
     * @throws IOException If an error occurs processing the stream
     */
    public static byte[] unzip( final InputStream inputStream ) throws IOException {
        byte[] data = null;
        
        ZipInputStream zipIn = null;
        try {
            zipIn = new ZipInputStream( inputStream );
            ZipEntry entry;
            while ( (entry = zipIn.getNextEntry()) != null ) {
                if ( entry.getName().equals( ZIP_ENTRY_NAME ) ) {
                    data = IOUtils.slurpStream( zipIn );
                }
            }
        } finally {
            ResourceUtils.closeQuietly(zipIn);
        }

        return data;
    }

    public static byte[] encrypt( final String data,
                                  final String filename,
                                  final long modified,
                                  final String password ) throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream( (int)((double)data.length() * 1.2) );
        try {
            PgpUtil.encrypt(
                    new ByteArrayInputStream(data.getBytes( Charsets.UTF8 )),
                    out,
                    filename,
                    modified,
                    password.toCharArray(),
                    false,
                    true );
        } catch ( PgpUtil.PgpException e ) {
            throw new IOException( "Error encrypting archive", e );
        }
        return out.toByteArray();
    }

    public static byte[] decrypt( final InputStream inputStream,
                                  final String password ) throws IOException {
        final PushbackInputStream pushbackInputStream = new PushbackInputStream( inputStream, 16 );
        final ByteArrayOutputStream out = new ByteArrayOutputStream( 10000 );
        try {
            byte[] header = new byte[2];
            int read = pushbackInputStream.read(header);
            if ( read != 2 ) {
                throw new IOException( "Error decrypting archive" );
            }
            pushbackInputStream.unread( header );
            if ( header[0] == (int)'P' && header[1] == (int)'K' ) {
                // This is a PK-Zip file not an encrypted archive
                throw new IOException( "Error decrypting archive (ZIP file detected)" );
            }

            final PgpUtil.DecryptionMetadata metadata = PgpUtil.decrypt(
                    pushbackInputStream,
                    out,
                    password.toCharArray() );

            if ( !metadata.isIntegrityChecked() ) {
                throw new IOException( "Error decrypting archive" );
            }
        } catch ( PgpUtil.PgpException e ) {
            throw new IOException( "Error decrypting archive", e );
        }
        return out.toByteArray();
    }

    //- PACKAGE

    static MigrationRecordManager getMigrationRecordManager() {
        return migrationRecordManagerRef.get();
    }

    static void setMigrationRecordManager( final MigrationRecordManager migrationRecordManager ) {
        migrationRecordManagerRef.set( migrationRecordManager );
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(MigrationArtifactResource.class.getName());

    private static final String ZIP_ENTRY_NAME = "migration.xml";

    private static AtomicReference<MigrationRecordManager> migrationRecordManagerRef = new AtomicReference<MigrationRecordManager>();

    private boolean isOwner( final String migrationId ) {
        boolean owner = false;

        MigrationRecordManager manager = getMigrationRecordManager();
        User user = JaasUtils.getCurrentUser();
        if ( manager != null && user != null ) {
            try {
                MigrationRecord migration = manager.findByPrimaryKey( Goid.parseGoid( migrationId ) );
                if ( migration != null ) {
                    if ( user.getId().equals( migration.getUserId() ) &&
                         user.getProviderId().equals(migration.getProvider())) {
                        owner = true;
                    }
                }
            } catch ( NumberFormatException nfe ) {
                // not owner
            } catch ( FindException e ) {
                logger.log( Level.WARNING, "Error finding migration to check resource ownership", e );
            }
        }

        return owner;
    }

    private MigrationArtifactParameters getResourceParameters() {
        final ValueMap parameters = getParameters();

        MigrationArtifactParameters resourceParameters = getResourceParameters( parameters.getString( "id" ), MigrationArtifactParameters.class );
        if ( resourceParameters==null ) {
            resourceParameters = new MigrationArtifactParameters(
                    parameters.getString("disposition"),
                    parameters.getString("migrationId"),
                    null );
        }

        return resourceParameters;
    }

    private AbstractResourceStream buildResourceStream( final Functions.NullaryThrows<InputStream,IOException> streamCallback,
                                                        final String contentType,
                                                        final Time lastModified ) {
        return new AbstractResourceStream(){
            private InputStream in = null;

            @Override
            public String getContentType() {
                return contentType;
            }

            @Override
            public InputStream getInputStream() throws ResourceStreamNotFoundException {
                if ( in == null ) {
                    try {
                        in = streamCallback.call();
                    } catch ( IOException e ) {
                        in = new IOExceptionThrowingInputStream( e );
                    }
                }
                return in;
            }

            @Override
            public void close() throws IOException {
                ResourceUtils.closeQuietly( in );
            }

            @Override
            public Time lastModifiedTime() {
                return lastModified;
            }
        };
    }
}
