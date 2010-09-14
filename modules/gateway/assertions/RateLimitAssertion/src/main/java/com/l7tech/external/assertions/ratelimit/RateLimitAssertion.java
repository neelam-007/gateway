package com.l7tech.external.assertions.ratelimit;

import com.l7tech.util.Functions;
import com.l7tech.util.HexUtils;
import com.l7tech.policy.assertion.*;
import static com.l7tech.policy.assertion.AssertionMetadata.*;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import static com.l7tech.objectmodel.ExternalEntityHeader.ValueType.TEXT_ARRAY;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Adds rate limiting to a policy.
 * See http://sarek.l7tech_unsigned.com/mediawiki/index.php?title=POLM_1410_Rate_Limiting
 */
public class RateLimitAssertion extends Assertion implements UsesVariables {
    protected static final Logger logger = Logger.getLogger(RateLimitAssertion.class.getName());

    private String counterName = "RateLimit-${request.clientid}";
    private String maxRequestsPerSecond = "100";
    private boolean shapeRequests = false;
    private String maxConcurrency = "0";
    private boolean hardLimit = false;

    /**
     * Maximum number of requests per second that we can enforce.
     * Warning - if the value is changed to be higher than 93824, then the server assertion must be updated to use
     * BigIntegers. See the server assertion for more details. 
     */
    public static final int MAX_REQUESTS_PER_SECOND = 90000;

    @Override
    @Migration(mapName = MigrationMappingSelection.NONE, mapValue = MigrationMappingSelection.REQUIRED, export = false, valueType = TEXT_ARRAY, resolver = PropertyResolver.Type.SERVER_VARIABLE)
    public String[] getVariablesUsed() {
        return Syntax.getReferencedNames(getCounterName() + " " + getMaxRequestsPerSecond() + " " + getMaxConcurrency());
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
            if (maxRequests > MAX_REQUESTS_PER_SECOND) return "Max requests cannot be greater than " + MAX_REQUESTS_PER_SECOND + ".";
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

    private final static String baseName = "Apply Rate Limit";

    final static AssertionNodeNameFactory policyNameFactory = new AssertionNodeNameFactory<RateLimitAssertion>(){
        @Override
        public String getAssertionName( final RateLimitAssertion assertion, final boolean decorate) {
            if(!decorate) return baseName;

            String concurrency = assertion.getMaxConcurrency();
            StringBuffer sb = new StringBuffer(baseName + ": ");
            sb.append(assertion.isHardLimit() ? "up to " : "average ");
            sb.append(assertion.getMaxRequestsPerSecond()).
                    append(" msg/sec");
            if (assertion.isShapeRequests()) sb.append(", shaped,");

            final String counterName = assertion.getCounterName();
            String prettyName = PresetInfo.findCounterNameKey(counterName, null);

            sb.append(" per ").append(prettyName != null ? prettyName : ("\"" + counterName + "\""));
            if(Syntax.getReferencedNames(concurrency).length > 0){
                sb.append(" (concurrency ").append(concurrency).append(")");
            }else{
                if (Integer.parseInt(concurrency) > 0) sb.append(" (concurrency ").append(concurrency).append(")");
            }

            return sb.toString();
        }
    };


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
                rla.setCounterName(PresetInfo.makeDefaultCounterName());
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
        meta.put(CLUSTER_PROPERTIES, props);

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

    /** Contains information about preset counters and counter IDs. */
    public static class PresetInfo {
        public static final int DEFAULT_CONCURRENCY_LIMIT = 10;
        public static final String PRESET_DEFAULT = "User or client IP";
        public static final String PRESET_GLOBAL = "Gateway node";
        public static final String PRESET_CUSTOM = "Custom:";
        public static final Map<String, String> counterNameTypes = new LinkedHashMap<String, String>() {{
            put(PRESET_DEFAULT, "${request.clientid}");
            put("Authenticated user", "${request.authenticateduser}");
            put("Client IP", "${request.tcp.remoteAddress}");
            put("SOAP operation", "${request.soap.operation}");
            put("SOAP namespace", "${request.soap.namespace}");
            put(PRESET_GLOBAL, "");
            put(PRESET_CUSTOM, makeUuid());
        }};
        public static final Pattern presetFinder = Pattern.compile("^PRESET\\(([a-fA-F0-9]{16})\\)(.*)$");
        public static final Pattern defaultCustomExprFinder = Pattern.compile("^([a-fA-F0-9]{8})-?(.*)$");

        public static String makeDefaultCustomExpr(String uuid, String expr) {
            return (uuid != null ? uuid : makeUuid()).substring(8) + (expr == null || expr.length() < 1 ? "" : "-" + expr);
        }

        public static boolean isDefaultCustomExpr(String rawExpr) {
            Matcher matcher = defaultCustomExprFinder.matcher(rawExpr);
            if (!matcher.matches())
                return false;
            String expr = matcher.group(2);
            return counterNameTypes.containsValue(expr);
        }

        /**
         * Generate a new counter name corresponding to the default preset (clientid).
         * @return a counter name similar to "PRESET(deadbeefcafebabe)${request.clientid}".  Never null or empty.
         */
        public static String makeDefaultCounterName() {
            return findRawCounterName(PRESET_DEFAULT, makeUuid(), null);
        }

        /**
         * See if the specified raw counter name happens to match any of the user friendly presets we provide.
         * If a counter key is matched, this updates uuidOut[0] as a side effect.
         *
         * @param rawCounterName  raw counter name ot look up, ie "foo bar blatz ${mumble}"
         * @param uuidOut  An optional single-element String array to receive the UUID.
         *                 Any UUID found will be copied into the first element of this array, if present.
         * @return the key in counterNameTypes corresponding to the given raw counter name, or null for {@link #PRESET_CUSTOM}.
         * for example given "PRESET(deadbeefcafebabe)${request.clientid}" this will return "User or client IP";
         * when given        "PRESET(abcdefabcdefabcd)" this will return "Gateway node (global)"; and
         * when given        "RateLimit-${request.clientid}" this will return null.
         */
        public static String findCounterNameKey(String rawCounterName, String[] uuidOut) {
            String foundKey = null;
            String foundUuid = null;

            Matcher matcher = presetFinder.matcher(rawCounterName);
            if (matcher.matches()) {
                String expr = matcher.group(2);
                if ((foundKey = findKeyForValue(counterNameTypes, expr)) != null)
                    foundUuid = matcher.group(1);
            } else {
                matcher = defaultCustomExprFinder.matcher(rawCounterName);
                if (matcher.matches()) {
                    String expr = matcher.group(2);
                    if ((foundKey = findKeyForValue(counterNameTypes, expr)) != null)
                        foundUuid = makeUuid().substring(8) + matcher.group(1);
                }
            }

            if (foundUuid != null && uuidOut != null && uuidOut.length > 0) uuidOut[0] = foundUuid;
            return PRESET_CUSTOM.equals(foundKey) ? null : foundKey;
        }

        public static <K, V> K findKeyForValue(Map<K, V> map, V value) {
            for (Map.Entry<K, V> entry : map.entrySet())
                if (entry.getValue().equals(value))
                    return entry.getKey();
            return null;
        }

        /**
         * Create the raw counter name to save in the assertion instance given the specified counterNameKey.
         *
         * @param counterNameKey a counter name key from {@link #counterNameTypes}, or null to use {@link #PRESET_CUSTOM}.
         * @param uuid  the UUID to use to create unique counters.  Should be an 8-digit hex string.  Mustn't be null.
         * @param customExpr the custom string to use if counterNameKey is CUSTOM or null or can't be found in the map.
         *                    May be empty.  May only be null if counterNameKey is in the map and isn't CUSTOM.
         * @return the raw counter name to store into the assertion.  Never null or otherwise invalid.
         */
        public static String findRawCounterName(String counterNameKey, String uuid, String customExpr) {
            // If it claims to be custom, but looks just like a generated example, use whatever preset it was generated from
            if (counterNameKey == null || PRESET_CUSTOM.equals(counterNameKey))
                if (isDefaultCustomExpr(customExpr))
                    counterNameKey = findCounterNameKey(customExpr, null);

            String presetExpr = counterNameKey == null ? null : counterNameTypes.get(counterNameKey);
            if (presetExpr == null || PRESET_CUSTOM.equals(counterNameKey))
                return customExpr;
            return "PRESET(" + uuid + ")" + presetExpr;
        }

        public static String makeUuid() {
            Random random = new Random();
            byte[] bytes = new byte[8];
            random.nextBytes(bytes);
            return HexUtils.hexDump(bytes);
        }
    }
}
