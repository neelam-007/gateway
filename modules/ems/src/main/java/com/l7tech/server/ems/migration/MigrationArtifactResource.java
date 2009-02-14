package com.l7tech.server.ems.migration;

import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;
import org.apache.wicket.util.resource.ResourceStreamNotFoundException;
import org.apache.wicket.util.resource.AbstractResourceStream;
import org.apache.wicket.util.value.ValueMap;
import org.apache.wicket.util.time.Time;

import java.io.InputStream;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.concurrent.atomic.AtomicReference;
import java.util.Date;
import java.util.zip.ZipOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.text.SimpleDateFormat;

import com.l7tech.server.ems.ui.SecureResource;
import com.l7tech.server.util.JaasUtils;
import com.l7tech.util.ResourceUtils;
import com.l7tech.util.HexUtils;
import com.l7tech.util.IOUtils;
import com.l7tech.gateway.common.security.rbac.AttemptedReadSpecific;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.identity.User;

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
    }

    //- PROTECTED

    @Override
    protected IResourceStream getSecureResourceStream() {
        IResourceStream resource = null;
        ValueMap parameters = getParameters();

        if ( parameters.containsKey("migrationId") ) {
            String id = parameters.getString("migrationId");

            if ( !hasPermission( new AttemptedReadSpecific(EntityType.ESM_MIGRATION_RECORD, id) ) &&
                 !isOwner(id) ) {
                resource = getAccessDeniedStream();
            } else {
                MigrationRecordManager manager = getMigrationRecordManager();
                if ( manager != null ) {
                    try {
                        final MigrationRecord record = manager.findByPrimaryKey( Long.parseLong( id ) );
                        if ( record != null ) {
                            final ByteArrayInputStream in = new ByteArrayInputStream( zip(record.serializeXml()) );
                            resource = new AbstractResourceStream(){
                                @Override
                                public String getContentType() {
                                    return "application/zip";
                                }

                                @Override
                                public InputStream getInputStream() throws ResourceStreamNotFoundException {
                                    return in;
                                }

                                @Override
                                public void close() throws IOException {
                                    ResourceUtils.closeQuietly( in );
                                }

                                @Override
                                public Time lastModifiedTime() {
                                    return Time.milliseconds( record.getTimeCreated() );
                                }
                            };
                        } else {
                            logger.warning("Migration artifact not found when accessing migration resource '"+id+"'.");
                            resource = new StringResourceStream( "" );
                        }
                    } catch ( NumberFormatException nfe ) {
                        logger.warning("Invalid migration id when accessing migration resource '"+id+"'.");
                        resource = new StringResourceStream( "" );
                    } catch ( IOException ioe ) {
                        logger.log( Level.WARNING, "Error processing migration resource.", ioe );
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

        ValueMap parameters = getParameters();
        if ( parameters.containsKey("migrationId") ) {
            String id = parameters.getString("migrationId");

            String label = null;
            long time = 0;
            MigrationRecordManager manager = getMigrationRecordManager();
            if ( manager != null ) {
                try {
                    final MigrationRecord record = manager.findByPrimaryKey( Long.parseLong( id ) );
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

            if ( label == null ) {
                name = "migration_" + id + ".zip";
            } else {
                SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd HHmmss");
                name = format.format( new Date(time) ) + "_" + label + ".zip";
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
                MigrationRecord migration = manager.findByPrimaryKey( Long.parseLong( migrationId ) );
                if ( migration != null ) {
                    if ( user.getId().equals( migration.getUserId() ) &&
                         user.getProviderId() == migration.getProvider() ) {
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
}
