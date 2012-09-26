package com.l7tech.external.assertions.oauthinstaller;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.policy.bundle.BundleInfo;
import com.l7tech.policy.bundle.BundleMapping;
import com.l7tech.server.admin.AsyncAdminMethodsImpl;
import com.l7tech.server.event.InstallPolicyBundleEvent;
import com.l7tech.server.policy.bundle.*;
import com.l7tech.util.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.server.event.AdminInfo.find;

public class OAuthInstallerAdminImpl extends AsyncAdminMethodsImpl implements OAuthInstallerAdmin {

    public static final String NS_INSTALLER_VERSION = "http://ns.l7tech.com/2012/09/oauth-toolkit";

    public OAuthInstallerAdminImpl(final String bundleBaseName) throws OAuthToolkitInstallationException {
        final String oauthBundleInfo = bundleBaseName + "OAuthToolkitBundleInfo.xml";
        final URL oauthBundleInfoUrl = getClass().getResource(oauthBundleInfo);
        if (oauthBundleInfoUrl == null) {
            throw new OAuthToolkitInstallationException("Could not find OAuthToolkitBundleInfo.xml");
        }

        final byte[] bundleBytes;
        try {
            bundleBytes = IOUtils.slurpUrl(oauthBundleInfoUrl);
        } catch (IOException e) {
            throw new OAuthToolkitInstallationException(e);
        }

        final Document oauthInfoDoc;
        try {
            oauthInfoDoc = XmlUtil.parse(new ByteArrayInputStream(bundleBytes));
        } catch (IOException e) {
            throw new OAuthToolkitInstallationException("Unable to parse resource: " + oauthBundleInfo, e);
        } catch (SAXException e) {
            throw new OAuthToolkitInstallationException("Unable to parse resource: " + oauthBundleInfo, e);
        }

        try {
            final Element versionElm = XmlUtil.findExactlyOneChildElementByName(oauthInfoDoc.getDocumentElement(), NS_INSTALLER_VERSION, "Version");
            oAuthInstallerVersion = DomUtils.getTextValue(versionElm, true);
            if (oAuthInstallerVersion.isEmpty()) {
                throw new OAuthToolkitInstallationException("Could not get version information for OAuth Toolkit");
            }
        } catch (Exception e) {
            throw new OAuthToolkitInstallationException("Could not find version information in: " + oauthBundleInfo, e);
        }

        try {
            bundleResolver = new OAuthToolkitBundleResolver(bundleBaseName);
            final List<BundleInfo> resultList1 = bundleResolver.getResultList();
            for (BundleInfo bundleInfo : resultList1) {
                BundleUtils.findReferences(bundleInfo, bundleResolver);
            }
        } catch (BundleResolver.BundleResolverException e) {
            throw new OAuthToolkitInstallationException(e);
        } catch (BundleResolver.UnknownBundleException e) {
            throw new OAuthToolkitInstallationException(e);
        } catch (BundleResolver.InvalidBundleException e) {
            throw new OAuthToolkitInstallationException(e);
        }
        this.spring = spring;
    }

    @NotNull
    @Override
    public String getOAuthToolkitVersion() throws OAuthToolkitInstallationException {
        return oAuthInstallerVersion;
    }

    /**
     * All bundles in bundleNames MUST USE the same GUIDS for all policies which have the same name. The name of a policy
     * is unique on a Gateway. If the bundles contain the same policy with different guids the bundles will not install.
     *
     * @param otkComponentId names of all bundles to install. Bundles may depend on each others items, but there is no
     *                       install dependency order.
     * @param folderOid      oid of the folder to install into.
     * @param installFolder  if not null or empty, this folder will be the install into folder. It may already exist.
     *                       If it does not exist it will be created.
     * @return Job ID, which will report on which bundles were installed.
     * @throws IOException for any problem installing. Installation is cancelled on the first error.
     */
    @NotNull
    @Override
    public JobId<ArrayList> installOAuthToolkit(@NotNull final Collection<String> otkComponentId,
                                                final long folderOid,
                                                @Nullable final String installFolder,
                                                @NotNull final Map<String, BundleMapping> bundleMappings) throws OAuthToolkitInstallationException {

        final FutureTask<ArrayList> future = new FutureTask<ArrayList>(find(false).wrapCallable(new Callable<ArrayList>() {
            @Override
            public ArrayList call() throws Exception {
                return new ArrayList<String>(doInstallOAuthToolkit(otkComponentId, folderOid, installFolder, bundleMappings));
            }
        }));

        // Lets schedule this to run in a second, not straight away...want the UI to wait just a little
        Background.scheduleOneShot(new TimerTask() {
            @Override
            public void run() {
                future.run();
            }
        }, 1000L);

        return registerJob(future, ArrayList.class);
    }

    protected List<String> doInstallOAuthToolkit(@NotNull final Collection<String> otkComponentId,
                                                 final long folderOid,
                                                 @Nullable final String installFolder,
                                                 @NotNull Map<String, BundleMapping> bundleMappings) throws OAuthToolkitInstallationException {

        //todo check version of bundle to ensure it's supported.

        // When installing more than one bundle, allow for optimization of not trying to recreate items already created.
        final Map<String, Object> contextMap = new HashMap<String, Object>();

        final List<String> installedBundles = new ArrayList<String>();
        if (isInstallInProgress.compareAndSet(false, true)) {
            try {
                //iterate through all the bundle names to install
                for (String bundleId : otkComponentId) {
                    //todo search for and updated jdbc references as needed
                    try {
                        if (Thread.currentThread().isInterrupted()) {
                            logger.info("Installation cancelled");
                            break;
                        }

                        final List<BundleInfo> resultList1 = bundleResolver.getResultList();
                        for (BundleInfo bundleInfo : resultList1) {
                            if (bundleInfo.getId().equals(bundleId)) {
                                final InstallPolicyBundleEvent bundleEvent =
                                        new InstallPolicyBundleEvent(this, bundleResolver,
                                                new PolicyBundleInstallerContext(
                                                        bundleInfo, folderOid, installFolder, contextMap, bundleMappings.get(bundleId)));

                                spring.publishEvent(bundleEvent);
                                if (!bundleEvent.isProcessed()) {
                                    throw new OAuthToolkitInstallationException("Policy Bundle Installer module is not installed.");
                                }

                                final Exception processingException = bundleEvent.getProcessingException();
                                if (processingException != null) {
                                    throw processingException;
                                }
                            }
                        }

                    } catch (Exception e) {
                        //todo log and audit with stack trace
                        if (!(e instanceof BundleResolver.UnknownBundleException)) {
                            // catch everything including any runtime errors representing programming errors e.g. NPEs
                            logger.warning("Unexpected error during installation: " + ExceptionUtils.getMessage(e));
                            throw new OAuthToolkitInstallationException(e);
                        } else {
                            throw new OAuthToolkitInstallationException(e);
                        }

                    }
                    installedBundles.add(bundleId);
                }

            } finally {
                isInstallInProgress.set(false);
            }
        } else {
            throw new OAuthToolkitInstallationException("Install is already in progress");
        }

        return installedBundles;

    }

    @NotNull
    @Override
    public List<BundleInfo> getAllOtkComponents() throws OAuthToolkitInstallationException {
        return bundleResolver.getResultList();
    }

    public void setSpring(ApplicationContext spring) {
        this.spring = spring;
    }

    public static class InstanceHolder{
        final String bundleBaseName = "/com/l7tech/external/assertions/oauthinstaller/bundles/";
        final public OAuthInstallerAdminImpl instance;

        public InstanceHolder() {
            try {
                instance = new OAuthInstallerAdminImpl(bundleBaseName);
            } catch (OAuthToolkitInstallationException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public final static InstanceHolder INSTANCE_HOLDER = new InstanceHolder();

    /**
     * Wired via OAuthInstallerAssertion meta data.
     *
     * @param context spring application context
     */
    public static synchronized void onModuleLoaded(final ApplicationContext context) {

        if (adminImpl != null) {
            logger.log(Level.WARNING, "OAuthInstaller module is already initialized");
        } else {
            final OAuthInstallerAdminImpl instance = INSTANCE_HOLDER.instance;
            instance.setSpring(context);
            adminImpl = instance;
        }
    }

    // - PRIVATE

    private BundleResolver bundleResolver;
    private final AtomicBoolean isInstallInProgress = new AtomicBoolean(false);
    private static final Logger logger = Logger.getLogger(OAuthInstallerAdminImpl.class.getName());
    private final String oAuthInstallerVersion;
    private ApplicationContext spring;
    private static OAuthInstallerAdmin adminImpl = null;

}
