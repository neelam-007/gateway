package com.l7tech.server.licensing;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.common.licensing.LicenseDocument;
import com.l7tech.server.event.AdminInfo;
import com.l7tech.server.event.system.Started;
import com.l7tech.server.util.PostStartupApplicationListener;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.IOUtils;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
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
        if (event instanceof Started) {
            loadLicenseFromFile();
        }
    }

    private void loadLicenseFromFile() {
        try {
            AdminInfo.find(false).wrapCallable(new Callable<LicenseDocument>() {
                @Override
                public LicenseDocument call() throws Exception {
                if (licenseDocumentManager.findAll().isEmpty() && BOOTSTRAP_LICENSE_FOLDER != null) {
                    File licenseFolder = new File(BOOTSTRAP_LICENSE_FOLDER);
                    if (!licenseFolder.exists()) return null;
                    if (!licenseFolder.isDirectory()) return null;
                    File[] licenseFiles = licenseFolder.listFiles();
                    if (licenseFiles == null || licenseFiles.length == 0) return null;
                    if (licenseFiles.length > 1) {
                        logger.warning("More than one license found in " + licenseFolder.getCanonicalPath());
                        return null;
                    }
                    // attempt to load license from file
                    logger.info("Installing license from: " + licenseFiles[0].getCanonicalPath());
                    InputStream is = new FileInputStream(licenseFiles[0]);
                    try {
                        byte[] docBytes = IOUtils.slurpStream(is);
                        final LicenseDocument licenseDocument = new LicenseDocument(new String(docBytes));
                        licenseDocumentManager.saveWithImmediateFlush(licenseDocument);
                        return licenseDocument;
                    } finally {
                        is.close();
                    }
                }
                return null;
                }
            }).call();
        } catch (Exception e) {
            logger.warning(ExceptionUtils.getMessageWithCause(e));
        }
    }
}
