package com.l7tech.external.assertions.ratelimit;

import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.util.CounterPresetInfoUtils;
import com.l7tech.util.Functions;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.l7tech.objectmodel.ExternalEntityHeader.ValueType.TEXT_ARRAY;
import static com.l7tech.policy.assertion.AssertionMetadata.*;

/**
 * Adds rate limiting to a policy.
 * See http://sarek.l7tech_unsigned.com/mediawiki/index.php?title=POLM_1410_Rate_Limiting
 */
public class RateLimitAssertion extends Assertion implements UsesVariables {
    public static final int DEFAULT_CONCURRENCY_LIMIT = 10;
    public static final String PRESET_DEFAULT = "User or client IP";
    public static final String PRESET_GLOBAL = "Gateway node";
    public static final String PRESET_CUSTOM = "Custom:";
    public static final Map<String, String> COUNTER_NAME_TYPES = new LinkedHashMap<String, String>() {{
        put(PRESET_DEFAULT, "${request.clientid}");
        put("Authenticated user", "${request.authenticateduser}");
        put("Client IP", "${request.tcp.remoteAddress}");
        put("SOAP operation", "${request.soap.operation}");
        put("SOAP namespace", "${request.soap.namespace}");
        put(PRESET_GLOBAL, "");
        put(PRESET_CUSTOM, CounterPresetInfoUtils.makeUuid());
    }};

    private String counterName = "RateLimit-${request.clientid}";
    private String maxRequestsPerSecond = "100";
    private boolean shapeRequests = false;
    private String maxConcurrency = "0";
    private boolean hardLimit = false;
    private String windowSizeInSeconds = "1"; // When hardLimit is false, this is how many seconds of points that are permitted to accumulate in the token bucket during idle periods.
    private String blackoutPeriodInSeconds = null; // When the assertion fails, for the next N seconds all further attempts to use the same counter will immediately fail.
    private boolean splitRateLimitAcrossNodes = true;
    private boolean splitConcurrencyLimitAcrossNodes = true;
    private boolean logOnly = false;

    @Override
    @Migration(mapName = MigrationMappingSelection.NONE, mapValue = MigrationMappingSelection.REQUIRED, export = false, valueType = TEXT_ARRAY, resolver = PropertyResolver.Type.SERVER_VARIABLE)
    public String[] getVariablesUsed() {
        return Syntax.getReferencedNames(getCounterName(), getMaxRequestsPerSecond(), getMaxConcurrency(), getWindowSizeInSeconds(), getBlackoutPeriodInSeconds());
    }

    /**
     * @return the counter name.  Never null.
     */
    public String getCounterName() {
        return counterName;
    }

    /**
     * @param counterName the new counter name.  Must not be null.
     */
    public void setCounterName(String counterName) {
        if (counterName == null) throw new IllegalArgumentException();
        this.counterName = counterName;
    }

    public String getMaxRequestsPerSecond() {
        return maxRequestsPerSecond;
    }

    /**
     * @param maxRequestsPerSecond String the rate limit to enforce.  Must be positive. The value may be a single context
     *                             variable. If it is not the value must be parseable as an int and it's value must be greater than 1.
     */
    public void setMaxRequestsPerSecond(String maxRequestsPerSecond) {
        final String errorMsg = validateMaxRequestsPerSecond(maxRequestsPerSecond);
        if (errorMsg != null) throw new IllegalArgumentException(errorMsg);

        this.maxRequestsPerSecond = maxRequestsPerSecond;
    }

    /**
     * Provided for backwards compatability for policies before bug5041 was fixed
     * @param maxRequestsPerSecond int
     */
    public void setMaxRequestsPerSecond(int maxRequestsPerSecond) {
        setMaxRequestsPerSecond(String.valueOf(maxRequestsPerSecond));
    }

    public boolean isShapeRequests() {
        return shapeRequests;
    }

    /** @param shapeRequests if true, assertion should attempt to delay requests to keep them from exceeding the limit. */
    public void setShapeRequests(boolean shapeRequests) {
        this.shapeRequests = shapeRequests;
    }

    public String getMaxConcurrency() {
        return maxConcurrency;
    }

    /**
     * @param maxConcurrency String maximum number of requests that may be in progress at once for the counter configured by
     *                       this instance of this assertion, or zero for no special limit. May either be an int value as a String
     *                       or a single context variable. If the value is not a context variable, then the value must be parseable
     *                       as an int and must be nonnegative.
     */
    public void setMaxConcurrency(String maxConcurrency) {
        final String errorMsg = validateMaxConcurrency(maxConcurrency);
        if (errorMsg != null) throw new IllegalArgumentException(errorMsg);

        this.maxConcurrency = maxConcurrency;
    }

    /**
     * Provided for backwards compatibility for policies before bug5041 was fixed
     * @param maxConcurrency int
     */
    public void setMaxConcurrency(int maxConcurrency) {
        setMaxConcurrency(String.valueOf(maxConcurrency));
    }


    public boolean isHardLimit() {
        return hardLimit;
    }

    /**
     * Set whether the limit is hard or soft.
     * <p/>
     * A hard limit will reject (or delay) a second message that arrives
     * too soon after a first message, even if the counter had been sitting idle for a long time before them.
     * Thus, a hard limit prevents burst rate from exceeding the limit.
     * <p/>
     * A soft limit will allow an idle counter to accumulate up to one seconds worth of "buffer", and will then
     * allow burst traffic at full speed until this buffer is exhausted.
     * Thus, a soft limit puts a lid of requests over time, but may allow burst traffic to exceed the limit temporarily
     * (as long as the counter was idle for a period of time before the burst).
     *
     * @param hardLimit if true, no burst traffic above the limit will be permitted.
     *                  if false, up to one second's worth of traffic may be bursted before rate limiting is imposed
     */
    public void setHardLimit(boolean hardLimit) {
        this.hardLimit = hardLimit;
    }

    /**
     * @return the maximum number of seconds worth of points that may accumulate in the token buffer while the counter is idle.
     */
    public String getWindowSizeInSeconds() {
        return windowSizeInSeconds;
    }

    /**
     * @param windowSizeInSeconds the maximum number of seconds worth of points that may accumulate in the token buffer while the counter is idle.  Minimum and default is 1.  Ignored unles {@link #hardLimit}.
     */
    public void setWindowSizeInSeconds(String windowSizeInSeconds) {
        this.windowSizeInSeconds = windowSizeInSeconds;
    }

    /**
     * @return number of seconds to black out a counter after failure, or null if no blackout should be done.
     */
    public String getBlackoutPeriodInSeconds() {
        return blackoutPeriodInSeconds;
    }

    /**
     * @param blackoutPeriodInSeconds number of second to black out a counter after a failure, or null if no blackout should be done.
     */
    public void setBlackoutPeriodInSeconds(String blackoutPeriodInSeconds) {
        this.blackoutPeriodInSeconds = blackoutPeriodInSeconds;
    }

    /**
     * @return true if the rate limit should be divided by the number of "up" nodes, when used on a multi-node Gateway cluster.
     *         false if the limit should be enforced per-node.  See {@link #setSplitRateLimitAcrossNodes(boolean)} for an example.
     */
    public boolean isSplitRateLimitAcrossNodes() {
        return splitRateLimitAcrossNodes;
    }

    /**
     * @param splitRateLimitAcrossNodes true if the rate limit should be divided by the number of "up" nodes, when used on a multi-node Gateway cluster.
     *         false if the limit should be enforced per-node.<p/>
     *         For example, on a cluster with 3 nodes, 2 of which are up, and a limit of 100 requests per second:
     *         <ul><li>If this is true, a limit of 50 requests per second will be enforced by the server assertion.  The server assertion will assume that the
     *         other 50 requests per second will be handled by the other "up" node.</li>
     *         <li>Otherwise, a limit of 100 requests per second will be enforced.  The server assertion will make no assumptions about what other nodes might be allowing.</li></ul>
     */
    public void setSplitRateLimitAcrossNodes(boolean splitRateLimitAcrossNodes) {
        this.splitRateLimitAcrossNodes = splitRateLimitAcrossNodes;
    }

    /**
     * @return true if the concurrency limit should be divided by the number of "up" nodes, when used on a multi-node Gateway cluster.
     *         false if the limit should be enforced per-node.  See {@link #setSplitConcurrencyLimitAcrossNodes(boolean)} for an example.
     */
    public boolean isSplitConcurrencyLimitAcrossNodes() {
        return splitConcurrencyLimitAcrossNodes;
    }

    /**
     * @param splitConcurrencyLimitAcrossNodes true if the concurrency limit should be divided by the number of "up" nodes, when used on a multi-node Gateway cluster.
     *         false if the limit should be enforced per-node.<p/>
     *         For example, on a cluster with 3 nodes, 2 of which are up, and a limit of 10 concurrent requests through a counter:
     *         <ul><li>If this is true, a limit of 5 concurrent requests will be enforced by the server assertion.  The server assertion will assume that the
     *         other 5 concurrent requests will be handled by the other "up" node.</li>
     *         <li>Otherwise, a limit of 10 concurrent requests will be enforced.  The server assertion will make no assumptions about what other nodes might be allowing.</li></ul>
     */
    public void setSplitConcurrencyLimitAcrossNodes(boolean splitConcurrencyLimitAcrossNodes) {
        this.splitConcurrencyLimitAcrossNodes = splitConcurrencyLimitAcrossNodes;
    }

    /**
     * @return true if exceeding the rate limit should only log or false if the assertion should fail.
     */
    public boolean isLogOnly() {
        return logOnly;
    }

    /**
     *
     * @param logOnly set to true if exceeding the rate limit should log only as opposed to failing the assertion.
     */
    public void setLogOnly(final boolean logOnly) {
        this.logOnly = logOnly;
    }

    public static String validateMaxRequestsPerSecond(String maxRequestsPerSecond) {
        final String[] referencedVars = Syntax.getReferencedNamesIndexedVarsNotOmitted(maxRequestsPerSecond);
        if (referencedVars.length > 0) {
            if (referencedVars.length != 1) {
                return "Only a single context variable can be supplied for max requests per second.";
            }
            if (!maxRequestsPerSecond.trim().equals("${" + referencedVars[0] + "}")) {
                return "If a context variable is supplied for maximum requests per second it must be a reference to exactly one context variable.";
            }
        } else {
            final int maxRequests;
            try {
                maxRequests = Integer.parseInt(maxRequestsPerSecond);
            } catch (NumberFormatException e) {
                return "Invalid value for maximum requests per second.";
            }
            if (maxRequests < 1) return "Max requests per second cannot be less than 1.";
        }

        return null;
    }

    public static String validateMaxConcurrency(String maxConcurrency) {
        final String[] referencedVars = Syntax.getReferencedNamesIndexedVarsNotOmitted(maxConcurrency);
        if (referencedVars.length > 0) {
            if (referencedVars.length != 1) {
                return "Only a single context variable can be supplied for max concurrency.";
            }
            if (!maxConcurrency.trim().equals("${" + referencedVars[0] + "}")) {
                return "If a context variable is supplied for maximum concurrency it must be a reference to exactly one context variable.";
            }
        } else {
            final int maxConnurencyInt;
            try {
                maxConnurencyInt = Integer.parseInt(maxConcurrency);
            } catch (NumberFormatException e) {
                return "Invalid value for maximum concurrency";
            }
            if (maxConnurencyInt < 0) return "Max concurrency cannot be less than 0";
        }

        return null;
    }

    //
    // Metadata
    //
    private static final String META_INITIALIZED = RateLimitAssertion.class.getName() + ".metadataInitialized";

    public static final String PARAM_MAX_QUEUED_THREADS = "ratelimitMaxQueuedThreads";
    public static final String PARAM_CLEANER_PERIOD = "ratelimitCleanerPeriod";
    public static final String PARAM_MAX_NAP_TIME = "ratelimitMaxNapTime";
    public static final String PARAM_MAX_TOTAL_SLEEP_TIME = "ratelimitMaxTotalSleepTime";
    public static final String PARAM_CLUSTER_POLL_INTERVAL = "ratelimitClusterPollInterval";
    public static final String PARAM_CLUSTER_STATUS_INTERVAL = "ratelimitClusterStatusInterval";

    private final static String baseName = "Apply Rate Limit";

    final static AssertionNodeNameFactory policyNameFactory = new AssertionNodeNameFactory<RateLimitAssertion>(){
        @Override
        public String getAssertionName( final RateLimitAssertion assertion, final boolean decorate) {
            if(!decorate) return baseName;

            String concurrency = assertion.getMaxConcurrency();
            StringBuilder sb = new StringBuilder(baseName + ": ");
            if (assertion.isHardLimit()) {
                sb.append("up to ");
            } else {
                sb.append("average ");
                String window = assertion.getWindowSizeInSeconds();
                if (window != null && window.trim().length() > 0)
                    sb.append("(over ").append(window).append(" seconds) ");
            }
            sb.append(assertion.getMaxRequestsPerSecond()).
                    append(" msg/sec");
            if (assertion.isShapeRequests()) sb.append(", shaped,");

            final String counterName = assertion.getCounterName();
            String prettyName = CounterPresetInfoUtils.findCounterNameKey(counterName, null, PRESET_CUSTOM, COUNTER_NAME_TYPES);

            sb.append(" per ").append(prettyName != null ? prettyName : ("\"" + counterName + "\""));
            if(Syntax.getReferencedNames(concurrency).length > 0){
                sb.append(" (concurrency ").append(concurrency).append(")");
            }else{
                if (Integer.parseInt(concurrency) > 0) sb.append(" (concurrency ").append(concurrency).append(")");
            }

            if (assertion.getBlackoutPeriodInSeconds() != null && assertion.getBlackoutPeriodInSeconds().trim().length() > 0) {
                sb.append(" with ").append(assertion.getBlackoutPeriodInSeconds()).append(" sec blackout");
            }

            return sb.toString();
        }
    };


    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        // Request to appear in "misc" ("Service Availability") palette folder
        meta.put(PALETTE_FOLDERS, new String[] { "misc" });

        meta.put(SHORT_NAME, baseName);
        meta.put(DESCRIPTION, "Enforce a maximum transactions per second that may pass through this assertion.");

        meta.put(PALETTE_NODE_ICON, "com/l7tech/console/resources/disconnect.gif");

        meta.put(PROPERTIES_ACTION_NAME, "Rate Limit Properties");
        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(POLICY_ADVICE_CLASSNAME, "auto");

        meta.put(POLICY_NODE_NAME_FACTORY, policyNameFactory);

        // Customize newly-created assertions so they have a nice counter name
        meta.put(ASSERTION_FACTORY, new Functions.Unary<RateLimitAssertion, RateLimitAssertion>() {
            @Override
            public RateLimitAssertion call(RateLimitAssertion rlaVariantPrototype) {
                RateLimitAssertion rla = new RateLimitAssertion();
                rla.setCounterName(CounterPresetInfoUtils.makeDefaultCounterName(PRESET_DEFAULT, PRESET_CUSTOM, COUNTER_NAME_TYPES));
                return rla;
            }
        });

        // request default feature set name for our class name, since we are a known optional module
        // that is, we want our required feature set to be "assertion:RateLimit" rather than "set:modularAssertions"
        meta.put(FEATURE_SET_NAME, "(fromClass)");

        // Cluster properties used by this assertion
        Map<String, String[]> props = new HashMap<String, String[]>();
        props.put("ratelimit.maxQueuedThreads", new String[] {
                "Maximum number of requests that can be delayed for traffic shaping purposes on a single node.  When this limit is reached, rate limiters will start failing requests that hit the limit.",
                "70"
        });
        props.put("ratelimit.cleanerPeriod", new String[] {
                "Time interval for removing rate limit counters that have not been used recently (Milliseconds)",
                "13613"
        });
        props.put("ratelimit.maxNapTime", new String[] {
                "Maximum time a request subject to traffic shaping will wait before awakening to check its status (Milliseconds)",
                "4703"
        });
        props.put("ratelimit.maxTotalSleepTime", new String[] {
                "Maximum total time a request subject to traffic shaping will wait before giving up and failing (Milliseconds)",
                "18371"
        });
        props.put("ratelimit.clusterPollInterval", new String[] {
                "Time in between checks of the cluster status table, to check how many cluster nodes are up (Milliseconds)",
                "43000"
        });
        props.put("ratelimit.clusterStatusInterval", new String[] {
                "Another cluster node will be considered down if it has not posted its status more recently than this number of milliseconds ago (Milliseconds)",
                "8000"
        });
        meta.put(CLUSTER_PROPERTIES, props);

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }
}