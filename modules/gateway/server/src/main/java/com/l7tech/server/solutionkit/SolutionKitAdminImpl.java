package com.l7tech.server.solutionkit;

import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.gateway.common.solutionkit.SolutionKit;
import com.l7tech.gateway.common.solutionkit.SolutionKitAdmin;
import com.l7tech.gateway.common.solutionkit.SolutionKitException;
import com.l7tech.gateway.common.solutionkit.SolutionKitHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.admin.AsyncAdminMethodsImpl;
import com.l7tech.server.policy.bundle.ssgman.restman.RestmanMessage;
import com.l7tech.util.Background;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import static com.l7tech.server.event.AdminInfo.find;

public class SolutionKitAdminImpl extends AsyncAdminMethodsImpl implements SolutionKitAdmin {
    @Inject
    private SolutionKitManager solutionKitManager;
    @Inject
    private LicenseManager licenseManager;


    @SuppressWarnings("unused")   // used for spring configuration
    public SolutionKitAdminImpl() {}

    public SolutionKitAdminImpl(LicenseManager licenseManager, SolutionKitManager solutionKitManager) {
        this.licenseManager = licenseManager;
        this.solutionKitManager = solutionKitManager;
    }

    @NotNull
    @Override
    public Collection<SolutionKitHeader> findSolutionKits() throws FindException {
        return solutionKitManager.findAllHeaders();
    }

    @Override
    public SolutionKit get(@NotNull Goid goid) throws FindException {
        return solutionKitManager.findByPrimaryKey(goid);
    }

    @NotNull
    @Override
    public JobId<String> testInstall(@NotNull final SolutionKit solutionKit, @NotNull final String bundle) {
        final FutureTask<String> task =
            new FutureTask<>(find(false).wrapCallable(new Callable<String>() {
                @Override
                public String call() throws Exception {
                    checkFeatureEnabled(solutionKit);
                    final boolean isTest = true;
                    return solutionKitManager.importBundle(bundle, solutionKit.getProperty(SolutionKit.SK_PROP_INSTANCE_MODIFIER_KEY), isTest);
                }
            }));

        Background.scheduleOneShot(new TimerTask() {
            @Override
            public void run() {
                task.run();
            }
        }, 0L);

        return registerJob(task, String.class);
    }

    @NotNull
    @Override
    public JobId<Goid> install(@NotNull final SolutionKit solutionKit, @NotNull final String bundle, final boolean isUpgrade) {
        final FutureTask<Goid> task =
            new FutureTask<>(find(false).wrapCallable(new Callable<Goid>() {
                @Override
                public Goid call() throws Exception {
                    checkFeatureEnabled(solutionKit);

                    // Install bundle.
                    final boolean isTest = false;
                    String mappings = solutionKitManager.importBundle(bundle, solutionKit.getProperty(SolutionKit.SK_PROP_INSTANCE_MODIFIER_KEY), isTest);

                    // Save solution kit entity.
                    solutionKit.setMappings(mappings);

                    // Update the delete mapping probably due to new entities created or an instance modifier specified.
                    solutionKit.setUninstallBundle(updateUninstallBundle(solutionKit, mappings));

                    if (isUpgrade) {
                        solutionKitManager.update(solutionKit);
                        return solutionKit.getGoid();
                    } else {
                        return solutionKitManager.save(solutionKit);
                    }
                }
            }));

        Background.scheduleOneShot(new TimerTask() {
            @Override
            public void run() {
                task.run();
            }
        }, 0L);

        return registerJob(task, Goid.class);
    }

    @NotNull
    @Override
    public JobId<String> uninstall(@NotNull final Goid goid) {
        final FutureTask<String> task =
            new FutureTask<>(find(false).wrapCallable(new Callable<String>() {
                @Override
                public String call() throws Exception {
                    final boolean isTest = false;
                    final SolutionKit solutionKit = get(goid);
                    String resultMappings = "";
                    if (solutionKit.getUninstallBundle() != null) {
                        resultMappings = solutionKitManager.importBundle(solutionKit.getUninstallBundle(), solutionKit.getProperty(SolutionKit.SK_PROP_INSTANCE_MODIFIER_KEY), isTest);
                    }
                    solutionKitManager.delete(goid);
                    return resultMappings;
                }
            }));

        Background.scheduleOneShot(new TimerTask() {
            @Override
            public void run() {
                task.run();
            }
        }, 0L);

        return registerJob(task, String.class);
    }

    private void checkFeatureEnabled(@NotNull final SolutionKit solutionKit) throws SolutionKitException {
        final String featureSet = solutionKit.getProperty(SolutionKit.SK_PROP_FEATURE_SET_KEY);
        if (!StringUtils.isEmpty(featureSet) && !licenseManager.isFeatureEnabled(featureSet)) {
            throw new SolutionKitException(solutionKit.getName() + " is unlicensed.  Required feature set " + featureSet);
        }
    }

    /**
     * After a new instance of a solution kit is installed and an instance modifier might be specified, the entity mappings
     * will contains targetId to replace srcId. In this case, the original uninstall mappings should be updated based on
     * the given entity mappings.
     */
    private String updateUninstallBundle(@NotNull final SolutionKit solutionKit, @NotNull final String mappings) throws SAXException, IOException {
        final Map<String, String> idsMap = new HashMap<>();
        String srcId, targetId;

        // Find all matches of srdId and targetId in "mappings" and save them in a map.
        final RestmanMessage mappingsMsg = new RestmanMessage(mappings);
        for (Element element: mappingsMsg.getMappings()) {
            srcId = element.getAttribute(RestmanMessage.MAPPING_SRC_ID_ATTRIBUTE);
            targetId = element.getAttribute(RestmanMessage.MAPPING_TARGET_ID_ATTRIBUTE);
            if (!StringUtils.isEmpty(srcId) && !StringUtils.isEmpty(targetId))
                idsMap.put(srcId, targetId);
        }

        // Add targetId in the uninstall mappings
        final RestmanMessage uninstallMappingsMsg = new RestmanMessage(solutionKit.getUninstallBundle());
        for (Element element: uninstallMappingsMsg.getMappings()) {
            srcId = element.getAttribute(RestmanMessage.MAPPING_SRC_ID_ATTRIBUTE);
            if (idsMap.containsKey(srcId)) {
                element.setAttribute(RestmanMessage.MAPPING_TARGET_ID_ATTRIBUTE, idsMap.get(srcId));
            }
        }

        return uninstallMappingsMsg.getAsString();
    }
}