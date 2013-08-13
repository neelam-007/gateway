package com.l7tech.server.licensing;

import com.l7tech.gateway.common.InvalidLicenseException;
import com.l7tech.gateway.common.LicenseException;
import com.l7tech.gateway.common.audit.AuditDetailMessage;
import com.l7tech.gateway.common.audit.SystemMessages;
import com.l7tech.gateway.common.custom.CustomAssertionsRegistrar;
import com.l7tech.gateway.common.licensing.*;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.CustomAssertionHolder;
import com.l7tech.server.event.GoidEntityInvalidationEvent;
import com.l7tech.server.event.system.LicenseEvent;
import com.l7tech.util.*;
import org.apache.commons.lang.ObjectUtils;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.*;
import org.springframework.context.support.ApplicationObjectSupport;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.*;
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
public abstract class AbstractCompositeLicenseManager extends ApplicationObjectSupport implements UpdatableCompositeLicenseManager, Lifecycle, Phased,
        ApplicationListener<GoidEntityInvalidationEvent>, ApplicationEventPublisherAware {

    /* ---- Issuer Certificate(s) ---- */
    private static final X509Certificate[] TRUSTED_ISSUERS_X509_CERTIFICATES = generateTrustedIssuerCertificates();

    public static final int PHASE = -10000;
    private static final long REFRESH_INTERVAL = 6L * 60L * 60L * 1000L; // rebuild the composite license every 6 hours, even if not notified of a change
    private static final long DB_RETRY_INTERVAL = 5L * 60L * 1000L; // in case of a db failure, retry rebuilding the composite license every 5 minutes
    private static final long DB_FAILURE_GRACE_PERIOD = 72L * 60L * 60L * 1000L; // ignore a DB failure for up to 72 hours before canceling any current license
    private static final String BUILD_PRODUCT_NAME = BuildInfo.getProductName();
    private static final String BUILD_VERSION_MAJOR = BuildInfo.getProductVersionMajor();
    private static final String BUILD_VERSION_MINOR = BuildInfo.getProductVersionMinor();

    private final Logger logger;
    private final Lock updateLock = new ReentrantLock(); // Not synchronized because lost writes/stale reads are not important
    private final AtomicLong lastCheck = new AtomicLong(-1L);
    private final AtomicLong lastUpdate = new AtomicLong(-1L);
    private final RebuildScheduler rebuildScheduler = new RebuildScheduler();
    private final AtomicReference<CompositeLicense> compositeLicense = new AtomicReference<>(null);
    private final List<LicenseEvent> events = new ArrayList<>();

    private ApplicationEventPublisher eventPublisher;

    private volatile boolean running = false;
    private volatile boolean licenseSet = false;

    @Autowired
    private FeatureLicenseFactory licenseFactory;

    @Autowired
    private LicenseDocumentManager licenseDocumentManager;

    private CustomAssertionsRegistrar customAssertionsRegistrar;

    protected AbstractCompositeLicenseManager(final Logger logger) {
        this.logger = logger;
    }

    @Override
    public CompositeLicense getCurrentCompositeLicense() {
        updateLock.lock();

        try {
            checkLicenseCurrent();
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
    @Transactional
    public void installLicense(FeatureLicense license) throws LicenseInstallationException {
        updateLock.lock();

        try {
            // save to DB, other cluster nodes will rebuild their CompositeLicenses on EntityInvalidationEvent
            licenseDocumentManager.saveWithImmediateFlush(license.getLicenseDocument());

            // rebuild our CompositeLicense immediately to ensure calling code will have access to up-to-date info
            rebuildCompositeLicense();

            // audit successful installation
//            audit.logAndAudit(SystemMessages.LICENSE_INSTALLED, Long.toString(license.getId()));
        } catch (SaveException e) {
            throw new LicenseInstallationException(ExceptionUtils.getMessage(e), e);
        } finally {
            updateLock.unlock();
        }
    }

    @Override
    public void uninstallLicense(LicenseDocument licenseDocument) throws LicenseRemovalException {
        updateLock.lock();

        try {
            // save to DB, other cluster nodes will rebuild their CompositeLicenses on EntityInvalidationEvent
            licenseDocumentManager.delete(licenseDocument);

            // rebuild our CompositeLicense immediately to ensure calling code will have access to up-to-date info
            rebuildCompositeLicense();

            // audit successful removal
//            audit.logAndAudit(SystemMessages.LICENSE_REMOVED, Long.toString(license.getId()));
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

        if (assertion instanceof CustomAssertionHolder) {
            // Override custom feature set name of the individual custom assertion serialized in policy, with the one from
            // the registrar prototype.  This enables feature control from the module (e.g. change feature set name in the jar).
            CustomAssertionHolder customAssertionHolder = (CustomAssertionHolder) assertion;
            try {
                CustomAssertionHolder registeredCustomAssertionHolder = getCustomAssertionsRegistrar().getAssertion(customAssertionHolder.getCustomAssertion().getClass().getName());
                customAssertionHolder.setRegisteredCustomFeatureSetName(registeredCustomAssertionHolder.getRegisteredCustomFeatureSetName());
            } catch (Exception e) {
                logger.log(Level.WARNING, "Unable to get custom feature set name from registrar: " + e.getMessage(), ExceptionUtils.getDebugException(e));
            }
        }

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

                        logger.log(Level.INFO,
                                "Assertion ''{0}'', is disabled due to unlicensed feature ''{1}''.",
                                new String[] {assertionFeatureSetName, feature});

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

        checkLicenseCurrent();

        CompositeLicense l = compositeLicense.get();

        return l != null && l.isFeatureEnabled(featureName);
    }

    /**
     * Check if the license information is due to be refreshed (per the {@link #REFRESH_INTERVAL}), and if so
     * rebuild the CompositeLicense.
     */
    private void checkLicenseCurrent() {
        if (System.currentTimeMillis() - lastCheck.get() < REFRESH_INTERVAL)
            return;

        boolean gotLock = false;

        try {
            if (compositeLicense.get() != null) {
                // if we already have a license then just try a lock so we don't wait if another thread is holding it
                gotLock = updateLock.tryLock();
            } else {
                gotLock = true;
                updateLock.lock();
            }

            if (!gotLock) // see if someone else got here first
                return;

            rebuildCompositeLicense();

            lastCheck.set(System.currentTimeMillis());
        } finally {
            if (gotLock) updateLock.unlock();
        }
    }

    // --- LicenseDocument change event handling: reload LicenseDocuments, regenerate CompositeLicense --- ///

    @Override
    public void onApplicationEvent(GoidEntityInvalidationEvent event) {
        if (LicenseDocument.class.equals(event.getEntityClass())) {
            requestReload();
        }
    }

    /**
     * Ensure that the next time a license query is performed, the license will be reloaded from the database.
     */
    private void requestReload() {
        updateLock.lock();

        try {
            lastCheck.set(-1L);
        }
        finally {
            updateLock.unlock();
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
//                    audit.logAndAudit(SystemMessages.LICENSE_DB_ERROR_RETRY, null, e);
                } else {
//                    audit.logAndAudit(SystemMessages.LICENSE_DB_ERROR_GAVEUP, null, e);

                    // grace period has expired, treat as if no licenses exist
                    setCompositeLicense(null);
                }

                // schedule a task to retry the rebuild
                rebuildScheduler.schedule(DB_RETRY_INTERVAL);

                return;
            }

            if(licenseDocuments.isEmpty()) {
//                audit.logAndAudit(SystemMessages.LICENSE_NO_LICENSE);
                setCompositeLicense(null);
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
//                        audit.logAndAudit(SystemMessages.LICENSE_INVALID, null, e);
                        // an invalid LicenseDocument has been found - include it as such in the CompositeLicense
                        invalidDocuments.add(document);
                    }
                }

                // this validation prevents the poking of invalid licenses by just adding them to the database
                for (FeatureLicense license : featureLicenses) {
                    try {
                        validateLicenseExpiry(license);
                    } catch (InvalidLicenseException e) {
//                        audit.logAndAudit(SystemMessages.LICENSE_EXPIRED,
//                                new String[] {Long.toString(license.getId())}, e);
                        expiredLicenses.put(license.getId(), license);
                        continue;
                    }

                    try {
                        validateLicenseStart(license);
                    } catch (InvalidLicenseException e) {
//                        audit.logAndAudit(SystemMessages.LICENSE_NOT_YET_VALID,
//                                new String[] {Long.toString(license.getId())}, e);
                        invalidLicenses.put(license.getId(), license);
                        continue;
                    }

                    try {
                        validateIssuer(license);
                    } catch (InvalidLicenseException e) {
//                        audit.logAndAudit(SystemMessages.LICENSE_INVALID_ISSUER,
//                                new String[] {Long.toString(license.getId())}, e);
                        invalidLicenses.put(license.getId(), license);
                        continue;
                    }

                    try {
                        validateProduct(license);
                    } catch (InvalidLicenseException e) {
//                        audit.logAndAudit(SystemMessages.LICENSE_INVALID_PRODUCT,
//                                new String[] {Long.toString(license.getId())}, e);
                        invalidLicenses.put(license.getId(), license);
                        continue;
                    }

                    validLicenses.put(license.getId(), license);
                }

                if (validLicenses.isEmpty()) {
//                    audit.logAndAudit(SystemMessages.LICENSE_NO_LICENSE);
                } else {
//                    audit.logAndAudit(SystemMessages.LICENSE_FOUND);
                }

                final CompositeLicense newCompositeLicense = new CompositeLicense(validLicenses,
                        invalidLicenses, expiredLicenses, invalidDocuments, getFeatureSetExpander());

                setCompositeLicense(newCompositeLicense);
            }

        } finally {
            updateLock.unlock();
        }
    }

    /**
     * Set the LicenseManager's CompositeLicense. Will only fire a LicenseEvent if the value has changed.
     *
     * @param newCompositeLicense the new CompositeLicense value
     */
    private void setCompositeLicense(@Nullable final CompositeLicense newCompositeLicense) {
        updateLock.lock();

        try {
            CompositeLicense current = compositeLicense.get();

            if (licenseSet && ObjectUtils.equals(current, newCompositeLicense))
                return;

            compositeLicense.set(newCompositeLicense);
            lastUpdate.set(System.currentTimeMillis());
            licenseSet = true;

            fireEvent("Updated", SystemMessages.LICENSE_UPDATED);
        } finally {
            updateLock.unlock();
        }
    }

    /**
     * Publish a LicenseEvent.
     *
     * @param action the event action
     * @param message the audit message
     */
    private void fireEvent(final String action, final AuditDetailMessage message) {
        final LicenseEvent event = new LicenseEvent(action, message.getLevel(), action, message.getMessage());

        if (!isRunning()) {
            // Save these and send them out later to avoid deadlocks from sending events too early in startup.
            // This is not strictly thread safe, but is sufficiently so for our current usage (should set/recheck
            // started flag when synchronized)
            synchronized(events) {
                events.add(event);
            }
        } else {
            // avoid firing while still holding the license update lock
            Background.scheduleOneShot(new TimerTask() {
                @Override
                public void run() {
                    eventPublisher.publishEvent(event);
                }
            }, 0);
        }
    }

    // --- Abstract methods --- //

    protected abstract FeatureSetExpander getFeatureSetExpander();

    // --- ApplicationEventPublisher --- //

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.eventPublisher = applicationEventPublisher;
    }

    // --- Lifecycle --- //

    @Override
    public void start() {
        running = true;

        synchronized(events) {
            for (final ApplicationEvent event : events) {
                eventPublisher.publishEvent(event);
            }

            events.clear();
        }
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

    public CustomAssertionsRegistrar getCustomAssertionsRegistrar() {
        if (customAssertionsRegistrar == null) {
            // license manager is instantiated very early, before custom assertions registrar
            // must create custom assertions registrar later (not @Autowired, nor in license manager constructor) to avoid unresolvable circular reference
            customAssertionsRegistrar = getApplicationContext().getBean("customAssertionRegistrar", CustomAssertionsRegistrar.class);
        }
        return customAssertionsRegistrar;
    }
}
