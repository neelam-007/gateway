package com.l7tech.server.licensing;

import com.l7tech.gateway.common.InvalidLicenseException;
import com.l7tech.gateway.common.LicenseException;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.gateway.common.audit.AuditFactory;
import com.l7tech.gateway.common.audit.SystemMessages;
import com.l7tech.gateway.common.licensing.*;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.server.event.EntityInvalidationEvent;
import com.l7tech.util.BuildInfo;
import com.l7tech.util.DateUtils;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.SmartLifecycle;

import java.io.ByteArrayInputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Jamie Williams - wilja33 - jamie.williams2@ca.com
 */
public abstract class AbstractCompositeLicenseManager implements SmartLifecycle,
        ApplicationListener<EntityInvalidationEvent>, UpdatableCompositeLicenseManager {

    /* ---- Issuer Certificate(s) ---- */
    private static final X509Certificate[] TRUSTED_ISSUERS_X509_CERTIFICATES = generateTrustedIssuerCertificates();

    public static final int PHASE = -10000;
    private static final long DB_RETRY_INTERVAL = (5L * 60L * 1000L);
    private static final long DB_FAILURE_GRACE_PERIOD = 72L * 60L * 60L * 1000L; // ignore a DB failure for up to 72 hours before canceling any current license
    private static final String BUILD_PRODUCT_NAME = BuildInfo.getProductName();
    private static final String BUILD_VERSION_MAJOR = BuildInfo.getProductVersionMajor();
    private static final String BUILD_VERSION_MINOR = BuildInfo.getProductVersionMinor();

    private final Logger logger;
    private final Audit auditor;
    private final Lock updateLock = new ReentrantLock(); // Not synchronized because lost writes/stale reads are not important
    private final AtomicLong lastUpdate = new AtomicLong(-1L);
    private final RebuildScheduler rebuildScheduler = new RebuildScheduler();
    private final AtomicReference<CompositeLicense> compositeLicense = new AtomicReference<>(null);

    private volatile boolean running = false;

    @Autowired
    private AuditFactory auditFactory;

    @Autowired
    private FeatureLicenseFactory licenseFactory;

    @Autowired
    private LicenseDocumentManager licenseDocumentManager;

    protected AbstractCompositeLicenseManager(final Logger logger) {
        this.logger = logger;
        this.auditor = auditFactory.newInstance(this, logger);
    }

    @Override
    public CompositeLicense getCurrentCompositeLicense() {
        updateLock.lock();

        try {
            rebuildCompositeLicense();
            return compositeLicense.get();
        } finally {
            updateLock.unlock();
        }
    }

    @Override
    public FeatureLicense createFeatureLicense(LicenseDocument licenseDocument) throws InvalidLicenseException {
        return licenseFactory.newInstance(licenseDocument, TRUSTED_ISSUERS_X509_CERTIFICATES);
    }

    @Override
    public void validateLicense(FeatureLicense license) throws InvalidLicenseException {
        validateIssuer(license);
        validateLicenseStart(license);
        validateLicenseExpiry(license);
        validateProduct(license);
    }

    @Override
    public void installLicense(FeatureLicense license) throws LicenseInstallationException {
        updateLock.lock();

        try {
            // save to DB, other cluster nodes will rebuild their CompositeLicenses on EntityInvalidationEvent
            licenseDocumentManager.save(license.getLicenseDocument());

            // rebuild our CompositeLicense immediately to ensure calling code will have access to up-to-date info
            rebuildCompositeLicense();

            // audit successful installation
            getAudit().logAndAudit(SystemMessages.LICENSE_INSTALLED, Long.toString(license.getId()));
        } catch (SaveException e) {
            throw new LicenseInstallationException(ExceptionUtils.getMessage(e), e);
        } finally {
            updateLock.unlock();
        }
    }

    @Override
    public void uninstallLicense(FeatureLicense license) throws LicenseRemovalException {
        updateLock.lock();

        try {
            // save to DB, other cluster nodes will rebuild their CompositeLicenses on EntityInvalidationEvent
            licenseDocumentManager.delete(license.getLicenseDocument());

            // rebuild our CompositeLicense immediately to ensure calling code will have access to up-to-date info
            rebuildCompositeLicense();

            // audit successful removal
            getAudit().logAndAudit(SystemMessages.LICENSE_REMOVED, Long.toString(license.getId()));
        } catch (DeleteException e) {
            throw new LicenseRemovalException(ExceptionUtils.getMessage(e), e);
        } finally {
            updateLock.unlock();
        }
    }

    private void validateIssuer(FeatureLicense license) throws InvalidLicenseException {
        if (!license.hasTrustedIssuer()) {
            throw new InvalidLicenseException("License " + license.getId() +
                    " was not signed by a trusted license issuer.");
        }
    }

    private void validateLicenseStart(FeatureLicense license) throws InvalidLicenseException {
        if (!license.isLicensePeriodStartBefore(System.currentTimeMillis())) {
            throw new InvalidLicenseException("License " + license.getId() +
                    " is not yet valid: becomes valid on " + license.getStartDate() +
                    " (" + DateUtils.makeRelativeDateMessage(license.getStartDate(), false) + ").");
        }
    }

    private void validateLicenseExpiry(FeatureLicense license) throws InvalidLicenseException {
       if (!license.isLicensePeriodExpiryAfter(System.currentTimeMillis()))
            throw new InvalidLicenseException("License " + license.getId() +
                    " has expired: expired on " + license.getExpiryDate() +
                    " (" + DateUtils.makeRelativeDateMessage(license.getExpiryDate(), false) + ").");
    }

    private void validateProduct(FeatureLicense license) throws InvalidLicenseException {
        if (!license.isProductEnabled(BUILD_PRODUCT_NAME) || !license.isVersionEnabled(BUILD_VERSION_MAJOR, BUILD_VERSION_MINOR)) {
            throw new InvalidLicenseException("License " + license.getId() +
                    " does not grant access to this version of this product " +
                    "(" + BUILD_PRODUCT_NAME + " " + BUILD_VERSION_MAJOR + "." + BUILD_VERSION_MINOR + ").");
        }
    }

    @Override
    public boolean isAssertionEnabled(Assertion assertion) {
        // Get extra features factory for this assertion, if there is one
        Functions.Unary<Set<String>,Assertion> extraFeaturesFactory =
                assertion.meta().get(AssertionMetadata.FEATURE_SET_FACTORY);

        String assertionFeatureSetName = assertion.getFeatureSetName();
        boolean enabled = isFeatureEnabled(assertionFeatureSetName);

        if (enabled && extraFeaturesFactory != null) {
            // If there is an extra features checker it will return the required features for the given assertion
            Set<String> features = extraFeaturesFactory.call(assertion);

            if (features != null) {
                CompositeLicense license = compositeLicense.get();

                for (String feature : features) {
                    if (license == null || !license.isFeatureEnabled(feature)) {
                        enabled = false;

                        if (logger.isLoggable(Level.INFO)) {
                            logger.log(Level.INFO,
                                    "Assertion ''{0}'', is disabled due to unlicensed feature ''{1}''.",
                                    new String[] {assertionFeatureSetName, feature});
                        }

                        break;
                    }
                }
            }
        }

        return enabled;
    }

    @Override
    public void requireFeature(String featureName) throws LicenseException {
        if (!isFeatureEnabled(featureName)) {
            throw new LicenseException("The specified feature is not supported on this Gateway: " + featureName + ".");
        }
    }

    @Override
    public boolean isFeatureEnabled(String featureName) {
        if(null == featureName) {
            return false;
        }

        CompositeLicense l = compositeLicense.get();

        return l != null && l.isFeatureEnabled(featureName);
    }

    // --- LicenseDocument change event handling: reload LicenseDocuments, regenerate CompositeLicense --- ///

    @Override
    public void onApplicationEvent(EntityInvalidationEvent event) {
        if (LicenseDocument.class.equals(event.getEntityClass())) {
            rebuildCompositeLicense();
        }
    }

    /**
     * Reads all LicenseDocuments from the database, creates a new CompositeLicense from them, and sets
     * it as the LicenseManager's current license.
     *
     * If there is an error reading the LicenseDocuments they will be audited and a task to retry the
     * rebuild process will be scheduled. If the error occurred within the DB failure grace period the
     * CompositeLicense will be preserved (if there is one), otherwise it will be removed (set to null).
     *
     * Any errors encountered in creating FeatureLicenses from LicenseDocuments will be audited, but the
     * CompositeLicense will still be created.
     */
    private void rebuildCompositeLicense() {
        updateLock.lock();

        try {
            Collection<LicenseDocument> licenseDocuments;

            try {
                licenseDocuments = licenseDocumentManager.findAll();
            } catch (FindException e) {
                if (null != compositeLicense.get() &&
                        (System.currentTimeMillis() - lastUpdate.get() < DB_FAILURE_GRACE_PERIOD)) {
                    // audit the error, but leave the CompositeLicense as is
                    getAudit().logAndAudit(SystemMessages.LICENSE_DB_ERROR_RETRY, null, e);
                } else {
                    getAudit().logAndAudit(SystemMessages.LICENSE_DB_ERROR_GAVEUP, null, e);

                    // grace period has expired, treat as if no license exists
                    compositeLicense.set(null);
                }

                // schedule a task to retry the rebuild
                rebuildScheduler.schedule(DB_RETRY_INTERVAL);

                return;
            }

            if(licenseDocuments.isEmpty()) {
                getAudit().logAndAudit(SystemMessages.LICENSE_NO_LICENSE);
                compositeLicense.set(null);
            } else {
                ArrayList<FeatureLicense> featureLicenses = new ArrayList<>();
                ArrayList<LicenseDocument> invalidDocuments = new ArrayList<>();
                HashMap<Long, FeatureLicense> validLicenses = new HashMap<>();
                HashMap<Long, FeatureLicense> invalidLicenses = new HashMap<>();
                HashMap<Long, FeatureLicense> expiredLicenses = new HashMap<>();

                for(LicenseDocument document : licenseDocuments) {
                    try {
                        FeatureLicense license = createFeatureLicense(document);
                        featureLicenses.add(license);
                    } catch (InvalidLicenseException e) {
                        getAudit().logAndAudit(SystemMessages.LICENSE_INVALID, null, e);
                        // an invalid LicenseDocument has been found - include it as such in the CompositeLicense
                        invalidDocuments.add(document);
                    }
                }

                // this validation prevents the poking of invalid licenses by just adding them to the database
                for (FeatureLicense license : featureLicenses) {
                    try {
                        validateLicenseExpiry(license);
                    } catch (InvalidLicenseException e) {
                        getAudit().logAndAudit(SystemMessages.LICENSE_EXPIRED,
                                new String[] {Long.toString(license.getId())}, e);
                        expiredLicenses.put(license.getId(), license);
                        continue;
                    }

                    try {
                        validateLicenseStart(license);
                    } catch (InvalidLicenseException e) {
                        getAudit().logAndAudit(SystemMessages.LICENSE_NOT_YET_VALID,
                                new String[] {Long.toString(license.getId())}, e);
                        invalidLicenses.put(license.getId(), license);
                        continue;
                    }

                    try {
                        validateIssuer(license);
                    } catch (InvalidLicenseException e) {
                        getAudit().logAndAudit(SystemMessages.LICENSE_INVALID_ISSUER,
                                new String[] {Long.toString(license.getId())}, e);
                        invalidLicenses.put(license.getId(), license);
                        continue;
                    }

                    try {
                        validateProduct(license);
                    } catch (InvalidLicenseException e) {
                        getAudit().logAndAudit(SystemMessages.LICENSE_INVALID_PRODUCT,
                                new String[] {Long.toString(license.getId())}, e);
                        invalidLicenses.put(license.getId(), license);
                        continue;
                    }

                    validLicenses.put(license.getId(), license);
                }

                if (!validLicenses.isEmpty()) {
                    getAudit().logAndAudit(SystemMessages.LICENSE_NO_LICENSE);
                } else {
                    getAudit().logAndAudit(SystemMessages.LICENSE_FOUND);
                }

                final CompositeLicense newCompositeLicense = new CompositeLicense(validLicenses,
                        invalidLicenses, expiredLicenses, invalidDocuments, getFeatureSetExpander());

                compositeLicense.set(newCompositeLicense);
            }

            lastUpdate.set(System.currentTimeMillis());
        } finally {
            updateLock.unlock();
        }
    }

    protected Audit getAudit() {
        return auditor;
    }

    // --- Abstract methods --- //

    protected abstract FeatureSetExpander getFeatureSetExpander();

    // --- SmartLifecycle - ensures the bean starts early & automatically, performs cleanup tasks --- //

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public void stop(Runnable callback) {
        rebuildScheduler.close();
        running = false;
        callback.run();
    }

    @Override
    public void start() {
        running = true;
    }

    @Override
    public void stop() {
        rebuildScheduler.close();
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return PHASE;
    }
    
    /**
     * Generates our trusted issuer certificates, hardcoded here in encrypted form.
     *
     * @return all trusted issuer certificates, or null if they could not be generated
     */
    private static X509Certificate[] generateTrustedIssuerCertificates() {
        try {
            CertificateFactory certFactory = CertificateFactory.getInstance("X.509");

            // The obfuscator will encrypt this
            byte[] buf = ("-----BEGIN CERTIFICATE-----\n" +
                    "MIIC6DCCAdACBEM41hwwDQYJKoZIhvcNAQEEBQAwOTEdMBsGA1UEChMUTGF5ZXIg\n" +
                    "NyBUZWNobm9sb2dpZXMxGDAWBgNVBAMTD0xpY2Vuc2UgT2ZmaWNlcjAeFw0wNTA5\n" +
                    "MjcwNTE4MjBaFw0yNTA5MjYwNTE4MjBaMDkxHTAbBgNVBAoTFExheWVyIDcgVGVj\n" +
                    "aG5vbG9naWVzMRgwFgYDVQQDEw9MaWNlbnNlIE9mZmljZXIwggEhMA0GCSqGSIb3\n" +
                    "DQEBAQUAA4IBDgAwggEJAoIBAH6Zu7CyJnp/UqHlZ3WNEy4OKXzms7movyd4Bpqb\n" +
                    "6DRRzOq/qZfMMnoKCZ5tEpAODw9DPPJHoE3bXV67dDWTnDNwCU67r1fHBFqTqJaB\n" +
                    "WgU1Gzgy+Ve7N6BaoeAXVJgEXR5b9MVFabfG1FYsqEbvKwUvOVqow1XGLoPWqAKP\n" +
                    "3fdBDUPOJgGUnrzY1pBvBSLlQoKzGR+fHVrMn1zQRS9MFalwzIgrgvEUxeTA72DF\n" +
                    "G3ZJJ47ek+OmYP7q5Nzz1rCSBilv7CTW8TCZMKLJSBHfB0pPDaIMLdPdZqOes3ng\n" +
                    "9jXuWpVCHI/lljxjBBWNTne/fUmN8gayTKTztA4UbO/heJECAwEAATANBgkqhkiG\n" +
                    "9w0BAQQFAAOCAQEAEUDRup8nlBrK6z2114ReO2gt+k+ZwtqbSIGBMM6kCKvnUV7f\n" +
                    "Bmi9XnvglM/ekmKBNIqMXuCjbOcRqgU5eiuKpvctHRzUKTHT9CKUQfR7ow2+Kkq8\n" +
                    "0vD7JCcsbIqDyWD7tsf/RGNLNZIcOGuBFDrJx1+lNo8R/FlXnestXGVIRCLyH+Y2\n" +
                    "w8GvvmUdKMymq0Adpr14v4B6/+xikxWJoUVTwnBLCNWoAqizCjla9lm4wOtKqsS1\n" +
                    "8TyDvB+rL9Gz+K5SRUxpWt0ADRWUJRdmF29H8GcDUcaAK7Ka6BjyrOhE9t6emB7e\n" +
                    "cX/Yl+RgwYa4F314O0xBGP6baqtVy/5BObtucA==\n" +
                    "-----END CERTIFICATE-----").getBytes();

            Certificate certificate = certFactory.generateCertificate(new ByteArrayInputStream(buf));

            return new X509Certificate[] {(X509Certificate) certificate};
        } catch (Exception e) {
            Logger.getLogger(AbstractCompositeLicenseManager.class.getName())
                    .log(Level.SEVERE, "Unable to configure trusted issuer certificate(s).", e);
        }

        return null;
    }

    /**
     * Background service that periodically rebuilds the composite license from
     * the LicenseDocuments in the database.
     */
    private class RebuildScheduler {
        private final ScheduledExecutorService service = Executors.newScheduledThreadPool(1);

        private final Runnable rebuildTask = new Runnable() {
            @Override
            public void run() {
                rebuildCompositeLicense();
            }
        };

        /**
         * Execute the rebuild task after the specified delay time.
         *
         * @param delay the delay time for the scheduled task, in milliseconds
         */
        public void schedule(final long delay) {
            if (service.isShutdown()) {
                throw new IllegalStateException("Scheduler has already been shut down!");
            }

            service.schedule(rebuildTask, delay, TimeUnit.MILLISECONDS);
        }

        public void close() {
            service.shutdown();
        }
    }
}
