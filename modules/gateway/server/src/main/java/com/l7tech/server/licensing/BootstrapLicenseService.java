package com.l7tech.server.licensing;

import com.l7tech.gateway.common.licensing.LicenseDocument;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.server.event.AdminInfo;
import com.l7tech.server.event.system.Initialized;
import com.l7tech.server.util.PostStartupApplicationListener;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.HexUtils;
import com.l7tech.util.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.context.ApplicationEvent;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
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
        if (event instanceof Initialized) {
            loadLicenses();
        }
    }

    private void loadLicenses() {
        try {
            if (!licenseDocumentManager.findAll().isEmpty()) {
                //there are already existing licenses. in this case do not load any from environment variables of the bootstrap folder
                logger.fine("License already installed. Skipping.");
                return;
            }
        } catch (ObjectModelException e) {
            throw new IllegalStateException("Unable to load licenses: " + ExceptionUtils.getMessageWithCause(e), e);
        }

        final List<LicenseDocument> loadedLicenses = new ArrayList<>();
        try {
            if (ConfigFactory.getBooleanProperty("bootstrap.env.license.enable", false) &&
                    StringUtils.isNotBlank(licenseFromEnv)) {
                // Check for single license in environment variable
                byte[] bytes = HexUtils.decodeBase64(licenseFromEnv, true);
                logger.info("Installing license from " + ENV_LICENSE_VAR + " environment variable");
                loadedLicenses.add(loadLicense(new GZIPInputStream(new ByteArrayInputStream(bytes))));
            }

            if (BOOTSTRAP_LICENSE_FOLDER != null) {
                // Might be license files on the filesystem
                File licenseFolder = new File(BOOTSTRAP_LICENSE_FOLDER);
                if (licenseFolder.exists() && licenseFolder.isDirectory()) {
                    File[] licenseFiles = licenseFolder.listFiles();
                    if (licenseFiles != null && licenseFiles.length > 0) {
                        for (final File licenseFile : licenseFiles) {
                            if(licenseFile.isFile()) {
                                logger.info("Installing license from: " + licenseFile.getCanonicalPath());
                                loadedLicenses.add(loadLicense(new FileInputStream(licenseFile)));
                            }else {
                                logger.info("Skipped loading from: " + licenseFile.getPath());
                            }
                        }
                    }
                }else {
                    logger.info("License directory not found: "+ licenseFolder.getPath());
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Fail to install license: " + ExceptionUtils.getMessageWithCause(e), e);
        }
        if (loadedLicenses.isEmpty() && ConfigFactory.getBooleanProperty("bootstrap.license.require", false)) {
            throw new IllegalStateException("No license loaded. Exiting.");
        }
    }

    private LicenseDocument loadLicense(InputStream licenseStream) throws Exception {
        try {
            return AdminInfo.find(false).wrapCallable(new Callable<LicenseDocument>() {
                @Override
                public LicenseDocument call() throws Exception {
                    // attempt to load license from file
                    byte[] docBytes = IOUtils.slurpStream(licenseStream);
                    final LicenseDocument licenseDocument = new LicenseDocument(new String(docBytes));
                    licenseDocumentManager.saveWithImmediateFlush(licenseDocument);
                    return licenseDocument;
                }
            }).call();
        } finally {
            licenseStream.close();
        }
    }
}
