package com.l7tech.external.assertions.icapantivirusscanner;

import com.l7tech.external.assertions.icapantivirusscanner.server.IcapAntivirusScannerAdminImpl;
import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.validator.AssertionValidator;
import com.l7tech.policy.validator.PolicyValidationContext;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.policy.wsp.CollectionTypeMapping;
import com.l7tech.policy.wsp.SimpleTypeMappingFinder;
import com.l7tech.policy.wsp.TypeMapping;
import com.l7tech.util.Functions;
import org.springframework.context.ApplicationContext;

import java.util.*;
import java.util.regex.Pattern;

/**
 * <p>The ICAP Antivirus modular assertion.</p>
 *
 * @author Ken Diep
 */
public class IcapAntivirusScannerAssertion extends MessageTargetableAssertion implements UsesVariables, SetsVariables {
    private static final String VARIABLE_PREFIX = "icap.response.";

    /**
     * The variable name to retrieve the infected file(s)/part(s) name.
     */
    public static final String INFECTED_PARTS = VARIABLE_PREFIX + "infected";

    /**
     * The variable prefix to retrieve all the header names.
     */
    public static final String VARIABLE_NAMES = VARIABLE_PREFIX + "header.names";

    /**
     * The variable prefix to retrieve all the header values.
     */
    public static final String VARIABLE_VALUES = VARIABLE_PREFIX + "header.values";

    /**
     * The variable prefix to retrieve a single header value.
     */
    public static final String VARIABLE_NAME = VARIABLE_PREFIX + "header.value";

    /**
     * The pattern to match and extract various parts of the ICAP uri.
     */
    public static final Pattern ICAP_URI = Pattern.compile("(?i)icap://(.*):(.*)/(.*)");

    private static final String META_INITIALIZED = IcapAntivirusScannerAssertion.class.getName() + ".metadataInitialized";

    private String failoverStrategy = null;

    private List<String> icapServers = new ArrayList<String>();

    private Map<String, String> serviceParameters = new HashMap<String, String>();

    private boolean continueOnVirusFound = false;

    private int maxMimeDepth = 1;

    private String readTimeout = "30";

    private String connectionTimeout = "30";

    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        meta.put(AssertionMetadata.SHORT_NAME, "Scan Using ICAP-Enabled Antivirus");
        meta.put(AssertionMetadata.LONG_NAME, "Scan for viruses using any antivirus server that supports the ICAP protocol.");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME, "ICAP Antivirus Scanner Properties");
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{"threatProtection"});
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/Properties16.gif");
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");
        meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/console/resources/Properties16.gif");
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");

        //add custom type mapping
        Collection<TypeMapping> othermappings = new ArrayList<TypeMapping>();
        othermappings.add(new CollectionTypeMapping(List.class, String.class, ArrayList.class, "icapConnections"));

        meta.put(AssertionMetadata.WSP_SUBTYPE_FINDER, new SimpleTypeMappingFinder(othermappings));

        meta.put(AssertionMetadata.POLICY_VALIDATOR_CLASSNAME, IcapAntivirusScannerAssertion.Validator.class.getName());

        meta.put(AssertionMetadata.EXTENSION_INTERFACES_FACTORY, new Functions.Unary<Collection<ExtensionInterfaceBinding>, ApplicationContext>() {
            @Override
            public Collection<ExtensionInterfaceBinding> call(ApplicationContext appContext) {
                final ExtensionInterfaceBinding<IcapAntivirusScannerAdmin> binding = new ExtensionInterfaceBinding<IcapAntivirusScannerAdmin>(
                        IcapAntivirusScannerAdmin.class,
                        null,
                        new IcapAntivirusScannerAdminImpl());
                return Collections.<ExtensionInterfaceBinding>singletonList(binding);
            }
        });

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

    /**
     * @return the failover strategy name.
     */
    public String getFailoverStrategy() {
        return failoverStrategy;
    }

    /**
     * @param failoverStrategy the failover strategy name to use.
     */
    public void setFailoverStrategy(final String failoverStrategy) {
        this.failoverStrategy = failoverStrategy;
    }

    /**
     * @return true if the assertion should continue processing if a virus is found, false otherwise.
     */
    public boolean isContinueOnVirusFound() {
        return continueOnVirusFound;
    }

    /**
     * @param continueOnVirusFound when true, the assertion will continue processing if a virus is found.  When false,
     *                             the assertion will fail and stop processing.
     */
    public void setContinueOnVirusFound(final boolean continueOnVirusFound) {
        this.continueOnVirusFound = continueOnVirusFound;
    }

    /**
     * @return all the configured ICAP servers.
     */
    public List<String> getIcapServers() {
        return icapServers;
    }

    /**
     * @param icapServers the list of ICAP servers.
     */
    public void setIcapServers(final List<String> icapServers) {
        this.icapServers = icapServers;
    }

    /**
     * @return a map containing all the configured service parameter name and values.   The key is the parameter name and the value is the parameter value.
     */
    public Map<String, String> getServiceParameters() {
        return serviceParameters;
    }

    /**
     * @param serviceParameters the server parameters to use.
     */
    public void setServiceParameters(final Map<String, String> serviceParameters) {
        this.serviceParameters = serviceParameters;
    }

    /**
     * @return the read timeout in term of seconds.
     */
    public String getReadTimeout() {
        return readTimeout;
    }

    /**
     * @param readTimeout the read timeout value in term of seconds.
     */
    public void setReadTimeout(final String readTimeout) {
        this.readTimeout = readTimeout;
    }

    /**
     * @return the connection timeout in term of seconds.
     */
    public String getConnectionTimeout() {
        return connectionTimeout;
    }

    /**
     * @param connectionTimeout the connection timeout in term of seconds.
     */
    public void setConnectionTimeout(final String connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    @Override
    public IcapAntivirusScannerAssertion clone() {
        IcapAntivirusScannerAssertion copy = (IcapAntivirusScannerAssertion) super.clone();
        copy.setFailoverStrategy(failoverStrategy);
        copy.setContinueOnVirusFound(continueOnVirusFound);
        copy.setIcapServers(new ArrayList<String>(icapServers));
        copy.setServiceParameters(new HashMap<String, String>(serviceParameters));
        copy.setMaxMimeDepth(maxMimeDepth);
        copy.setReadTimeout(readTimeout);
        copy.setConnectionTimeout(connectionTimeout);
        return copy;
    }

    /**
     *
     * @return the max number of MIME parts to scan.
     */
    public int getMaxMimeDepth() {
        return maxMimeDepth;
    }

    /**
     *
     * @param maxMimeDepth the max number of MIME parts to scan.
     */
    public void setMaxMimeDepth(final int maxMimeDepth) {
        this.maxMimeDepth = maxMimeDepth;
    }

    /**
     * <p>An implementation of the {@link AssertionValidator} to validate the ICAP Antivirus assertion.  This simply
     * check to see if there are one or more servers configured.
     * </p>
     *
     * @author Ken Diep
     */
    public static class Validator implements AssertionValidator {
        private final IcapAntivirusScannerAssertion assertion;

        public Validator(final IcapAntivirusScannerAssertion assertion) {
            if (assertion == null) {
                throw new IllegalArgumentException("assertion is required");
            }
            this.assertion = assertion;
        }

        @Override
        public void validate(final AssertionPath path, final PolicyValidationContext pvc, final PolicyValidatorResult result) {
            if (assertion.getIcapServers().isEmpty()) {
                result.addError(new PolicyValidatorResult.Error(assertion,
                        "Require at least one valid connection to an ICAP anti-virus server.", null));
            }
        }
    }

    @Override
    protected VariablesSet doGetVariablesSet() {
        return super.doGetVariablesSet().withVariables(
                new VariableMetadata(INFECTED_PARTS, false, true, null, false),
                new VariableMetadata(VARIABLE_NAMES, true, true, null, false),
                new VariableMetadata(VARIABLE_VALUES, true, true, null, false),
                new VariableMetadata(VARIABLE_NAME, true, false, null, false)
        );
    }
}
