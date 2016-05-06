package com.l7tech.server.licensing;

import com.l7tech.gateway.common.licensing.LicenseDocument;
import com.l7tech.server.event.AdminInfo;
import com.l7tech.server.event.system.Initialized;
import com.l7tech.server.util.PostStartupApplicationListener;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.IOUtils;
import org.springframework.context.ApplicationEvent;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

/**
 * At gateway startup, if no license is installed, then try loading license file found in "bootstrap.folder.license"
 */
public class BootstrapLicenseService implements PostStartupApplicationListener {
    private final String BOOTSTRAP_LICENSE_FOLDER;

    private LicenseDocumentManager licenseDocumentManager;
    private final Logger logger = Logger.getLogger(getClass().getName());

    public BootstrapLicenseService(LicenseDocumentManager licenseDocumentManager) {
        this.licenseDocumentManager = licenseDocumentManager;
        BOOTSTRAP_LICENSE_FOLDER = ConfigFactory.getProperty("bootstrap.folder.license");
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof Initialized ) {
            loadLicenseFromFile();
        }
    }

    private void loadLicenseFromFile() {
        try {
            if (licenseDocumentManager.findAll().isEmpty() && BOOTSTRAP_LICENSE_FOLDER != null) {
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
                            InputStream is = new FileInputStream(licenseFile);
                            try {
                                byte[] docBytes = IOUtils.slurpStream(is);
                                final LicenseDocument licenseDocument = new LicenseDocument(new String(docBytes));
                                licenseDocumentManager.saveWithImmediateFlush(licenseDocument);
                                return licenseDocument;
                            } finally {
                                is.close();
                            }

                        }
                    }).call();
                }
            }
        } catch (Exception e) {
            logger.warning(ExceptionUtils.getMessageWithCause(e));
        }
    }
}
