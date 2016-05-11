package com.l7tech.server.licensing;

import com.l7tech.gateway.common.licensing.LicenseDocument;
import com.l7tech.server.event.AdminInfo;
import com.l7tech.server.event.system.Initialized;
import com.l7tech.server.util.PostStartupApplicationListener;
import com.l7tech.util.*;
import org.springframework.context.ApplicationEvent;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

/**
 * At gateway startup, if no license is installed, then try loading license file found in "bootstrap.folder.license"
 */
public class BootstrapLicenseService implements PostStartupApplicationListener {
    private final String BOOTSTRAP_LICENSE_FOLDER;
    private final String ENV_LICENSE_VAR;
    String licenseFromEnv;

    private LicenseDocumentManager licenseDocumentManager;
    private final Logger logger = Logger.getLogger(getClass().getName());


    public BootstrapLicenseService(LicenseDocumentManager licenseDocumentManager) {
        this.licenseDocumentManager = licenseDocumentManager;
        BOOTSTRAP_LICENSE_FOLDER = ConfigFactory.getProperty("bootstrap.folder.license");
        ENV_LICENSE_VAR = ConfigFactory.getProperty( "bootstrap.env.license.variable" );
        licenseFromEnv = ENV_LICENSE_VAR != null && ENV_LICENSE_VAR.trim().length() > 0
                ? System.getenv( ENV_LICENSE_VAR )
                : null;
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof Initialized ) {
            loadLicenseFromFile();
        }
    }

    private void loadLicenseFromFile() {
        try {
            if ( licenseDocumentManager.findAll().isEmpty() ) {
                if ( ConfigFactory.getBooleanProperty( "bootstrap.env.license.enable", false ) &&
                        licenseFromEnv != null && licenseFromEnv.trim().length() > 0 )
                {
                    // Check for single license in environment variable
                    byte[] bytes = HexUtils.decodeBase64( licenseFromEnv, true );
                    try ( InputStream is = new GZIPInputStream( new ByteArrayInputStream( bytes ) ) ) {
                        final byte[] docBytes = IOUtils.slurpStream( is );
                        AdminInfo.find(false).wrapCallable(new Callable<LicenseDocument>() {
                            @Override
                            public LicenseDocument call() throws Exception {
                                // attempt to load license from file
                                logger.info( "Installing license from " + ENV_LICENSE_VAR + " environment variable" );
                                final LicenseDocument licenseDocument = new LicenseDocument(new String(docBytes));
                                licenseDocumentManager.saveWithImmediateFlush(licenseDocument);
                                return licenseDocument;
                            }
                        }).call();
                    }
                }

                if ( BOOTSTRAP_LICENSE_FOLDER != null ) {
                    // Might be license files on the filesystem
                    File licenseFolder = new File(BOOTSTRAP_LICENSE_FOLDER);
                    if (!licenseFolder.exists()) return;
                    if (!licenseFolder.isDirectory()) return;
                    File[] licenseFiles = licenseFolder.listFiles();
                    if (licenseFiles == null || licenseFiles.length == 0) return;
                    for (final File licenseFile : licenseFiles) {
                        AdminInfo.find(false).wrapCallable(new Callable<LicenseDocument>() {
                            @Override
                            public LicenseDocument call() throws Exception {
                                // attempt to load license from file
                                logger.info("Installing license from: " + licenseFile.getCanonicalPath());
                                try ( InputStream is = new FileInputStream( licenseFile ) ) {
                                    byte[] docBytes = IOUtils.slurpStream( is );
                                    final LicenseDocument licenseDocument = new LicenseDocument( new String( docBytes ) );
                                    licenseDocumentManager.saveWithImmediateFlush( licenseDocument );
                                    return licenseDocument;
                                }
                            }
                        }).call();
                    }
                }
            }
        } catch (Exception e) {
            logger.log( Level.WARNING, "Unable to install license: " + ExceptionUtils.getMessageWithCause(e), ExceptionUtils.getDebugException( e ) );
        }
    }
}
