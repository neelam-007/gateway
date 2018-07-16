package com.l7tech.example.solutionkit.simple.v01_03;

import com.l7tech.policy.solutionkit.SolutionKitManagerCallback;
import com.l7tech.policy.solutionkit.SolutionKitManagerContext;
import com.l7tech.util.CollectionUtils;
import com.l7tech.xml.xpath.XpathUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Hold shared callback logic for both {@link SimpleOtherSolutionKitManagerCallback} and {@link SimpleServiceSolutionKitManagerCallback}.
 */
public abstract class BaseSolutionKitManagerCallback extends SolutionKitManagerCallback {

    private static final Map<String, String> nsMap = CollectionUtils.MapBuilder.<String, String>builder()
            .put("l7", "http://ns.l7tech.com/2010/04/gateway-management")
            .unmodifiableMap();
    public static Map<String, String> getNamespaceMap() {
        return nsMap;
    }

    protected final Logger logger;
    private String solutionKitName;
    private String solutionKitVersion;
    private String installedSolutionKitVersion;
    private boolean isUpgrade;
    
    public BaseSolutionKitManagerCallback() {
        this.logger = Logger.getLogger(getClass().getName());
    }

    @Override
    public void preMigrationBundleImport(final SolutionKitManagerContext context) throws CallbackException {
        // read metadata
        final Document solutionKitMetadata = context.getSolutionKitMetadata();
        final List<Element> nameElements = XpathUtil.findElements(solutionKitMetadata.getDocumentElement(), "//l7:SolutionKit/l7:Name", getNamespaceMap());
        solutionKitName = nameElements.size() > 0 ? nameElements.get(0).getTextContent() : "";
        final List<Element> versionElements = XpathUtil.findElements(solutionKitMetadata.getDocumentElement(), "//l7:SolutionKit/l7:Version", getNamespaceMap());
        solutionKitVersion = versionElements.size() > 0 ? versionElements.get(0).getTextContent() : "";

        // read *installed* metadata
        final Document installedSolutionKitMetadata = context.getInstalledSolutionKitMetadata();
        isUpgrade = false;
        installedSolutionKitVersion = null;
        if (installedSolutionKitMetadata != null ) {
            // upgrade when installed metadata is not null
            isUpgrade = true;

            final List<Element> installedVersionElements = XpathUtil.findElements(installedSolutionKitMetadata.getDocumentElement(), "//l7:SolutionKit/l7:Version", getNamespaceMap());
            installedSolutionKitVersion = installedVersionElements.size() > 0 ? installedVersionElements.get(0).getTextContent() : "";
        }

        OnPreMigrationBundleImport(context);
    }

    /**
     * Prints the context info for the specified {@code solutionKitGuids}.
     * The context is extracted from the shared context map {@link #getContextMap()}.
     *
     * @param solutionKitGuids    array of solution kit {@code GUIDS} to print.  Required
     * @throws CallbackException if an error happens or expected entries are missing.
     */
    protected void printOutContextMap(final String ... solutionKitGuids) throws CallbackException {
        final Map<String, SolutionKitManagerContext> contextMap = getContextMap();
        if (contextMap == null) {
            throw new CallbackException("Internal Error: Solution kit '" + getSolutionKitName() + "' custom callback in is missing the context map");
        }

        for (final String skGuid : solutionKitGuids) {
            final SolutionKitManagerContext serviceContext = contextMap.get(skGuid);
            if (serviceContext == null) {
                throw new CallbackException("Failed to retrieve context for other solution kit: " + skGuid);
            }

            // read metadata
            final Document solutionKitMetadata = serviceContext.getSolutionKitMetadata();
            if (solutionKitMetadata == null) {
                throw new CallbackException("Failed to retrieve metadata for other solution kit: " + skGuid);
            }
            final List<Element> nameElements = XpathUtil.findElements(solutionKitMetadata.getDocumentElement(), "//l7:SolutionKit/l7:Name", getNamespaceMap());
            final String solutionKitName = nameElements.size() > 0 ? nameElements.get(0).getTextContent() : "";

            final Map<String, String> serviceKeyValue = serviceContext.getKeyValues();
            if (serviceKeyValue == null) {
                throw new CallbackException("Other solution kit '" + solutionKitName + "' is missing the keyValue map");
            }
            // print out other kit keyValue map
            String message = "*** PRINTING KEY VALUES FOR " + solutionKitName + " ***:" + System.lineSeparator();
            for (final Map.Entry<String, String> entry : serviceKeyValue.entrySet()) {
                message += entry.getKey() + ":" + entry.getValue() + System.lineSeparator();
            }
            logger.info(message);
            System.out.println(message);
            System.out.println();
        }
    }

    protected abstract void OnPreMigrationBundleImport(final SolutionKitManagerContext context) throws CallbackException;

    public String getSolutionKitName() {
        return solutionKitName;
    }

    public String getSolutionKitVersion() {
        return solutionKitVersion;
    }

    public String getInstalledSolutionKitVersion() {
        return installedSolutionKitVersion;
    }

    public boolean isUpgrade() {
        return isUpgrade;
    }
}
