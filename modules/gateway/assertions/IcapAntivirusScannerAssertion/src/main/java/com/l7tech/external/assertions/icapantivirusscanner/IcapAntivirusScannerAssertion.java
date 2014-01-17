package com.l7tech.external.assertions.icapantivirusscanner;

import com.l7tech.external.assertions.icapantivirusscanner.server.IcapAntivirusScannerAdminImpl;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.validator.AssertionValidator;
import com.l7tech.policy.validator.PolicyValidationContext;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.policy.wsp.*;
import com.l7tech.util.Functions;
import com.l7tech.util.InetAddressUtil;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationContext;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>The ICAP Antivirus modular assertion.</p>
 *
 * @author Ken Diep
 */
public class IcapAntivirusScannerAssertion extends MessageTargetableAssertion implements UsesVariables, SetsVariables {

    /**
     * The default prefix value
     */
    public static final String VARIABLE_PREFIX = "icap.response";

    /**
     * The variable name to retrieve the infected file(s)/part(s) name.
     */
    public static final String INFECTED_PARTS = "infected";

    /**
     * The variable prefix to retrieve all the header names.
     */
    public static final String VARIABLE_NAMES = "header.names";

    /**
     * The variable prefix to retrieve all the header values.
     */
    public static final String VARIABLE_VALUES = "header.values";

    /**
     * The variable prefix to retrieve a single header value.
     */
    public static final String VARIABLE_NAME = "header.value";

    /**
     * Pattern to match the icap scheme - URL is used to validate the first group from a match
     */
    public static final Pattern ICAP_URI = Pattern.compile("(?i)icap(.*)");

    /**
     * According to RFC 3507 ICAP default port is 1344.
     */
    public static final int ICAP_DEFAULT_PORT = 1344;
    /**
     * The channel idle timeout cluster entry.
     */
    public static final String CLUSTER_PROPERTY_CHANNEL_TIMEOUT = "icap.channelIdleTimeout";

    /**
     * The channel idle timeout property name.
     */
    public static final String CHANNEL_TIMEOUT_PROPERTY_NAME = ClusterProperty.asServerConfigPropertyName(CLUSTER_PROPERTY_CHANNEL_TIMEOUT);

    /**
     * The default channel idle timeout (1 minute).
     */
    public static final String DEFAULT_CHANNEL_IDLE_TIMEOUT = "1m";

    /**
     * The maximum channel idle timeout (1 hour).
     */
    public static final long MAX_CHANNEL_IDLE_TIMEOUT = TimeUnit.HOURS.toMillis(1);

    /**
     * The minimum channel idle timeout (1 minute).
     */
    public static final long MIN_CHANNEL_IDLE_TIMEOUT = TimeUnit.MINUTES.toMillis(1);

    private static final String META_INITIALIZED = IcapAntivirusScannerAssertion.class.getName() + ".metadataInitialized";

    private String failoverStrategy = null;

    private List<String> icapServers = new ArrayList<String>();

    //deprecated
    private Map<String, String> serviceParameters = new HashMap<String, String>();

    private List<IcapServiceParameter> parameters = new ArrayList<IcapServiceParameter>();

    private boolean continueOnVirusFound = false;

    private int maxMimeDepth = 1;

    private String readTimeout = "30";

    private String connectionTimeout = "30";

    private String responseReadTimeout = "30";

    private String variablePrefix = VARIABLE_PREFIX;

    /**
     * Get a path value for serviceName removing any leading '/' character.
     *
     * @param value serverName value to remove leading slash from
     * @return serviceName, same as value if no leading slash.
     */
    public static String getServiceName(@NotNull String value) {
        String path = value.trim();
        if (path.startsWith("/")) {
            path = path.substring(1, path.length());
        }

        return path;
    }

    /**
     * Get a displayable IPV6 string with any opening [ and closing ] removed when they contain an IPV6 literal address.
     *
     * @param hostname String hostname to check
     * @return hostname if not surrounded with [ and ], otherwise the hostname minus these characters.
     */
    public static String getDisplayableHostname(@NotNull String hostname) {
        if (hostname.isEmpty()) {
            return hostname;
        }

        if (hostname.charAt(0) == '[' && hostname.charAt(hostname.length() - 1) == ']') {
            final String maybeIpV6Literal = hostname.substring(1, hostname.length() - 1);
            if (InetAddressUtil.isValidIpv6Address(maybeIpV6Literal)) {
                return maybeIpV6Literal;
            }
        }

        return hostname;
    }

    @Override
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
        othermappings.add(new CollectionTypeMapping(List.class, IcapServiceParameter.class, ArrayList.class, "serviceParameters"));
        othermappings.add(new BeanTypeMapping(IcapServiceParameter.class, "parameter"));

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

        //cluster properties
        Map<String, String[]> props = new HashMap<String, String[]>();
        props.put(CLUSTER_PROPERTY_CHANNEL_TIMEOUT, new String[] {
                "The maximum idle time for a connected channel in the connection pool.  Any channels exceeding this timeout value will be disconnected and removed from " +
                        "the pool.  The value is expressed as a TimeUnit and its accepted range is between one second and one hour.  The default channel idle time is one minute.",
                DEFAULT_CHANNEL_IDLE_TIMEOUT
        });
        meta.put(AssertionMetadata.CLUSTER_PROPERTIES, props);
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
        //for backward compatibility
        for(Map.Entry<String, String> ent : serviceParameters.entrySet()){
            //default to query as it was being added in the query string previously
            IcapServiceParameter p = new IcapServiceParameter(ent.getKey(), ent.getValue(), "Query");
            parameters.add(p);
        }
    }

    public List<IcapServiceParameter> getParameters() {
        return parameters;
    }

    public void setParameters(final List<IcapServiceParameter> parameters) {
        this.parameters = parameters;
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

    public String getResponseReadTimeout() {
        return responseReadTimeout;
    }

    public void setResponseReadTimeout(String responseReadTimeout) {
        this.responseReadTimeout = responseReadTimeout;
    }

    /**
     * @return the context variable prefix
     */
    public String getVariablePrefix() {
        return variablePrefix;
    }
    /**
     * @param variablePrefix the context variable prefix
     */
    public void setVariablePrefix( final String variablePrefix ) {
        this.variablePrefix = variablePrefix;
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
        copy.setResponseReadTimeout(responseReadTimeout);
        copy.setVariablePrefix(variablePrefix);
        copy.setParameters(parameters);
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
                new VariableMetadata(getVariablePrefix() + "." + INFECTED_PARTS, false, true, null, false),
                new VariableMetadata(getVariablePrefix() + "." + VARIABLE_NAMES, true, true, null, false),
                new VariableMetadata(getVariablePrefix() + "." + VARIABLE_VALUES, true, true, null, false),
                new VariableMetadata(getVariablePrefix() + "." + VARIABLE_NAME, true, false, null, false)
        );
    }

    public static String[] getVariableSuffixes() {
        return new String[] {
            INFECTED_PARTS,
            VARIABLE_NAMES,
            VARIABLE_VALUES,
            VARIABLE_NAME,
        };
    }

    @Override
    protected MessageTargetableAssertion.VariablesUsed doGetVariablesUsed() {
        VariablesUsed vars = super.doGetVariablesUsed().withExpressions(connectionTimeout, readTimeout, responseReadTimeout);
        for(String server : icapServers){
            Matcher matcher = ICAP_URI.matcher(server);
            if(matcher.matches()){
                vars.withExpressions(matcher.group(1));
            }
        }
        for(IcapServiceParameter p : parameters){
            vars.withExpressions(p.getName(), p.getValue());
        }
        return vars;
    }
}
