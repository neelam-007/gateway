/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Apr 1, 2005<br/>
 */
package com.l7tech.external.assertions.throughputquota;

import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.sla.CounterPresetInfo;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.policy.variable.VariableNameSyntaxException;
import com.l7tech.policy.wsp.*;
import com.l7tech.util.TextUtils;
import com.l7tech.util.ValidationUtils;
import org.w3c.dom.Element;

import java.util.*;

import static com.l7tech.objectmodel.ExternalEntityHeader.ValueType.TEXT_ARRAY;
import static com.l7tech.policy.assertion.AssertionMetadata.*;

/**
 * An assertion that enforce the number of requests that can be made to a particular resource per time unit.
 * A quota can be defined as global or per requestor.
 *
 * For each runtime request if two quota assertions are invoked and use the same counter name, only the first will
 * increment the counter but both quota assertions will assert their quota based on the current value of the counter.
 *
 * @author flascelles@layer7-tech.com
 */
public class ThroughputQuotaAssertion extends Assertion implements UsesVariables, SetsVariables {

    public static final String PRESET_DEFAULT = "Authenticated user";
    public static final String PRESET_GLOBAL = "Gateway cluster";
    public static final String PRESET_CUSTOM = "Custom";
    private static final Map<String, String> COUNTER_NAME_TYPES = new LinkedHashMap<>();
    static {
        COUNTER_NAME_TYPES.put(PRESET_DEFAULT, "${request.authenticateduser.id}-${request.authenticateduser.providerid}");
        COUNTER_NAME_TYPES.put("Client IP", "${request.tcp.remoteAddress}");
        COUNTER_NAME_TYPES.put("SOAP operation", "${request.soap.operation}");
        COUNTER_NAME_TYPES.put("SOAP namespace", "${request.soap.namespace}");
        COUNTER_NAME_TYPES.put(PRESET_GLOBAL, "");
        COUNTER_NAME_TYPES.put(PRESET_CUSTOM, CounterPresetInfo.makeUuid());
    }

    public static final String DEFAULT_COUNTER_NAME = "ThroughputQuota-${request.authenticateduser.id}-${request.authenticateduser.providerid}";

    public static final long MAX_THROUGHPUT_QUOTA = Integer.MAX_VALUE;
    public static final int PER_SECOND = 1;
    public static final int PER_MINUTE = 2;
    public static final int PER_HOUR = 3;
    public static final int PER_DAY = 4;
    public static final int PER_MONTH = 5;
    public static final String DEFAULT_VAR_PREFIX = "counter";
    public static final String VAR_SUFFIX_ID = "id";
    public static final String VAR_SUFFIX_VALUE = "value";
    public static final String VAR_SUFFIX_PERIOD = "period";
    public static final String VAR_SUFFIX_USER = "user";
    public static final String VAR_SUFFIX_MAX = "max";

    private String idVariable; // actually id is the same as name, since the GUI shows counter id for counter name.
    private String valueVariable;
    private String periodVariable;
    private String userVariable;
    private String maxVariable;
    private String variablePrefix = "";
    private String quota = "200";
    private String byValue;

    private boolean synchronous = true; // true by default to preserve old behavior for pre-Goatfish policy XML
    private boolean readSynchronous = true;

    @SuppressWarnings({"UnusedDeclaration"})
    @Deprecated
    // Keeping this variable is for backwards compatibility with previous versions of Throughput Quota Assertion.
    // When a new assertion is created, global is always set to true.  There is only one case, in which "global" has a false value.
    // This case is a pre-6.2 Throughput Quota assertion with a Requestor quota option chosen.
    private boolean global = false;

    private int timeUnit = PER_MONTH;
    private String counterName = DEFAULT_COUNTER_NAME;
    public static final int ALWAYS_INCREMENT = 1;
    public static final int INCREMENT_ON_SUCCESS = 2;
    public static final int DECREMENT = 3;
    public static final int RESET = 4;

    private int counterStrategy = INCREMENT_ON_SUCCESS;
    private boolean logOnly = false;

    public ThroughputQuotaAssertion() {
        // Empty constructor for reflection purposes
    }

    public String getVariablePrefix() {
        return variablePrefix;
    }

    public void setVariablePrefix(String variablePrefix) {
        this.variablePrefix = variablePrefix;
        doVarNames();
    }

    private void doVarNames() {
        String prefix = variablePrefix;
        if (prefix == null || prefix.length() == 0) {
            prefix = DEFAULT_VAR_PREFIX;
        }
        idVariable = prefix + "." + VAR_SUFFIX_ID;
        valueVariable = prefix + "." + VAR_SUFFIX_VALUE;
        periodVariable = prefix + "." + VAR_SUFFIX_PERIOD;
        userVariable = prefix + "." + VAR_SUFFIX_USER;
        maxVariable = prefix + "." + VAR_SUFFIX_MAX;
    }

    public String[] getVariableSuffixes() {
        return new String[] {
                VAR_SUFFIX_ID,
                VAR_SUFFIX_VALUE,
                VAR_SUFFIX_PERIOD,
                VAR_SUFFIX_USER,
                VAR_SUFFIX_MAX
        };
    }

    public String idVariable() {
        if (idVariable == null) {
            doVarNames();
        }
        return idVariable;
    }

    public String valueVariable() {
        if (valueVariable == null) {
            doVarNames();
        }
        return valueVariable;
    }

    public String periodVariable() {
        if (periodVariable == null) {
            doVarNames();
        }
        return periodVariable;
    }

    public String userVariable() {
        if (userVariable == null) {
            doVarNames();
        }
        return userVariable;
    }

    public String maxVariable() {
        if (maxVariable == null) {
            doVarNames();
        }
        return maxVariable;
    }

    /**
     * The quota property represents the maximum number of requests that are allowed at run time per timeUnit.
     * @return the quota
     */
    public String getQuota() {
        return quota;
    }

    /**
     * The quota property represents the maximum number of requests that are allowed at run time per timeUnit.
     * @param quota the quota
     */
    public void setQuota(String quota) {
        String errorMsg = validateQuota(quota);
        if (errorMsg != null) {
            throw new IllegalArgumentException(errorMsg);
        }

        this.quota = quota;
    }

    /**
     * The value property represents the amount to increment/decrement the counter by.
     * @return the increment/decrement value
     */
    public String getByValue() {
        return byValue;
    }

    /**
     * Retrieves the increment value property, which represents the amount to increment/decrement the counter by.
     * @param byValue the increment/decrement value
     */
    public void setByValue(String byValue) {
        this.byValue = byValue;
    }

    /**
     * Provided for backwards compatability for policies before bug5043 was fixed
     * @param quota int
     */
    @SuppressWarnings({"UnusedDeclaration"})
    public void setQuota(long quota) {
        setQuota(String.valueOf(quota));
    }

    /**
     * Whether or not this quota is enforce on a per requestor basis or for all requestors together.
     * @return true means quota applies to all requestors together, false means it's applied to each
     * requestor individually
     * @deprecated since 6.2
     */
    @Deprecated
    public boolean isGlobal() {
        return global;
    }

    /**
     * Set whether or not this quota is enforce on a per requestor basis or for all requestors together.
     * @param global true means quota applies to all requestors together, false means it's applied
     * to each requestor individually
     * @deprecated since 6.2
     */
    @Deprecated
    public void setGlobal(boolean global) {
        this.global = global;
    }

    /**
     * The time unit applicable to the quota.
     * @return TIME_UNIT_UNDEFINED, PER_SECOND, PER_MINUTE, PER_HOUR, PER_DAY or PER_MONTH
     */
    public int getTimeUnit() {
        return timeUnit;
    }

    /**
     * The time unit applicable to the quota.
     * @param timeUnit TIME_UNIT_UNDEFINED, PER_SECOND, PER_MINUTE, PER_HOUR, PER_DAY or PER_MONTH
     */
    public void setTimeUnit(int timeUnit) {
        this.timeUnit = timeUnit;
    }

    /**
     * For each runtime request if two quota assertions are invoked and use the same counter name, only the first
     * will increment the counter but both quota assertions will assert their quota based on the current value
     * of the counter.
     *
     * @return the name for this counter
     */
    public String getCounterName() {
        // First check if the assertion is pre-6.2 version.  If so, update the counter name.
        if (!isGlobal() && !ThroughputQuotaAssertion.DEFAULT_COUNTER_NAME.equals(counterName)) {
            setCounterName(counterName + "-" + COUNTER_NAME_TYPES.get(PRESET_DEFAULT));
            setGlobal(true);
        }

        return counterName;
    }

    /**
     * For each runtime request if two quota assertions are invoked and use the same counter name, only the first
     * will increment the counter but both quota assertions will assert their quota based on the current value
     * of the counter.
     *
     * @param counterName the name for this counter
     */
    public void setCounterName(String counterName) {
        this.counterName = counterName;
    }

    public int getCounterStrategy() {
        return counterStrategy;
    }

    public void setCounterStrategy(int counterStrategy) {
        this.counterStrategy = counterStrategy;
    }

    /**
     * @return true if this assertion will only log if quota or false if the assertion will fail instead.
     */
    public boolean isLogOnly() {
        return logOnly;
    }

    /**
     * @param logOnly set to true if the assertion should only log if quota is exceeded or false if the assertion should fail instead.
     */
    public void setLogOnly(final boolean logOnly) {
        this.logOnly = logOnly;
    }

    /**
     * @return true if the counter should be updated in synchronous mode, with a new transaction for each update.
     *         false to use half-async mode where combining updates into a batched-up transaction is acceptable.
     */
    public boolean isSynchronous() {
        return synchronous;
    }

    /**
     * @param synchronous true if the counter should be updated in synchronous mode, with a new transaction for each update.
     *                    false to use half-async mode where combining updates into a batched-up transaction is acceptable.
     */
    public void setSynchronous(boolean synchronous) {
        this.synchronous = synchronous;
    }

    /**
     * @return  true if the counter should be read in synchronous mode, meaning that the database should be queries for eachread.
     *          false to use asynchronous mode where the cache will be checked for the counter first, before hitting the database.
     */
    public boolean isReadSynchronous() {
        return readSynchronous;
    }

    /**
     * @param readAsynchronous  true if the counter should be read in synchronous mode, meaning that the database should be queries for eachread.
     *                          false to use asynchronous mode where the cache will be checked for the counter first, before hitting the database.
     */
    public void setReadSynchronous(boolean readAsynchronous) {
        this.readSynchronous = readAsynchronous;
    }

    @Override
    @Migration(mapName = MigrationMappingSelection.NONE, mapValue = MigrationMappingSelection.REQUIRED, export = false, valueType = TEXT_ARRAY, resolver = PropertyResolver.Type.SERVER_VARIABLE)
    public String[] getVariablesUsed() {
        // If the assertion is a previous version, then update the counter name by calling the method getCounterName().
        return Syntax.getReferencedNames(getCounterName(), getQuota(), getByValue());
    }

    @Override
    public VariableMetadata[] getVariablesSet() {
        return new VariableMetadata[] {
                // Note default prefixes are used here for property lookup purposes
                new VariableMetadata(idVariable(), false, false, DEFAULT_VAR_PREFIX + "." + VAR_SUFFIX_ID, false, DataType.STRING),
                new VariableMetadata(valueVariable(), false, false, DEFAULT_VAR_PREFIX + "." + VAR_SUFFIX_VALUE, false, DataType.INTEGER),
                new VariableMetadata(periodVariable(), false, false, DEFAULT_VAR_PREFIX + "." + VAR_SUFFIX_PERIOD, false, DataType.STRING),
                new VariableMetadata(userVariable(), false, false, DEFAULT_VAR_PREFIX + "." + VAR_SUFFIX_USER, false, DataType.STRING),
                new VariableMetadata(maxVariable(), false, false, DEFAULT_VAR_PREFIX + "." + VAR_SUFFIX_MAX, false, DataType.INTEGER),
        };
    }

    private static String timeUnitStr(int timeUnit) {
        switch (timeUnit) {
            case ThroughputQuotaAssertion.PER_SECOND: return "second";
            case ThroughputQuotaAssertion.PER_MINUTE: return "minute";
            case ThroughputQuotaAssertion.PER_HOUR: return "hour";
            case ThroughputQuotaAssertion.PER_DAY: return "day";
            case ThroughputQuotaAssertion.PER_MONTH: return "month";
            default: return "something";
        }
    }

    private final static String BASE_NAME = "Apply Throughput Quota";

    final static AssertionNodeNameFactory policyNameFactory = new AssertionNodeNameFactory<ThroughputQuotaAssertion>(){
        @Override
        public String getAssertionName(final ThroughputQuotaAssertion assertion, final boolean decorate) {
            if(!decorate) {
                return BASE_NAME;
            }

            final StringBuilder buffer = new StringBuilder(BASE_NAME);
            final String readableCounterName = getReadableCounterName(assertion.getCounterName());
            if (assertion.getCounterStrategy() == ThroughputQuotaAssertion.DECREMENT) {
                buffer.append(": Decrement counter ").append(readableCounterName);
            }
            if (assertion.getCounterStrategy() == ThroughputQuotaAssertion.RESET) {
                buffer.append(": Reset counter ").append(readableCounterName);
            } else {
                buffer.append(": ").append(readableCounterName).append(": ").append(assertion.getQuota()).append(" per ").append(timeUnitStr(assertion.getTimeUnit()));
            }
            return buffer.toString();
        }
    };

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();

        meta.put(PALETTE_FOLDERS, new String[]{"misc"});

        meta.put(SHORT_NAME, BASE_NAME);
        meta.put(DESCRIPTION, "Limit the number of service requests permitted within a predetermined time period.");

        meta.put(PALETTE_NODE_ICON, "com/l7tech/external/assertions/throughputquota/console/resources/policy16.gif");

        meta.put(POLICY_NODE_NAME_FACTORY, policyNameFactory);

        meta.put(PROPERTIES_ACTION_CLASSNAME, "com.l7tech.external.assertions.throughputquota.console.action.ThroughputQuotaPropertiesAction");
        meta.put(PROPERTIES_ACTION_NAME, "Throughput Quota Properties");

        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME,
                "com.l7tech.external.assertions.throughputquota.console.advice.AddThroughputQuotaAssertionAdvice");

        final AssertionMapping tm = (AssertionMapping)meta.get(WSP_TYPE_MAPPING_INSTANCE);

        Map<String, TypeMapping> wspMappings = new HashMap<>();
        wspMappings.put("HalfAsyncThroughputQuota", halfAsyncCompatibilityMapping(tm));
        meta.put(AssertionMetadata.WSP_COMPATIBILITY_MAPPINGS, wspMappings);

        return meta;
    }

    public static String validateQuota(String quota) {
        String error = null;

        try {

            final String[] varsUsed = Syntax.getReferencedNames(quota);
            if (varsUsed.length == 0 && !ValidationUtils.isValidLong(quota, false, 1, MAX_THROUGHPUT_QUOTA)) {
                error = "Throughput quota must be a value between 1 and " + MAX_THROUGHPUT_QUOTA;
            } else if(varsUsed.length > 0 && !Syntax.isOnlyASingleVariableReferenced(quota)) {
                error = "Only a single context variable is allowed for quota";
            }

        } catch (VariableNameSyntaxException e) {
            error = "The context variable syntax for maximum quota is invalid";
        }

        return error;
    }

    /**
     * Helper method which will validation the increment/decrement value to determine if it is a valid integer value,
     * otherwise it will return an error message.
     *
     * @param byValue the increment/decrement value
     * @param counterStrategy
     * @return null if valid, error message if otherwise
     */
    public static String validateByValue(String byValue, int counterStrategy) {

        String error = null;
        String strategy = null;

        if(INCREMENT_ON_SUCCESS == counterStrategy || ALWAYS_INCREMENT == counterStrategy) {
            strategy = "increment";
        } else {
            strategy = "decrement";
        }

        try {

            final String[] varsUsed = Syntax.getReferencedNames(byValue);

            if (varsUsed.length == 0 && !ValidationUtils.isValidInteger(byValue, false, 0, Integer.MAX_VALUE)) {
                error = "Please enter an integer for " + strategy + " value between 0 and " + Integer.MAX_VALUE;
            } else if(varsUsed.length > 0 && !Syntax.isOnlyASingleVariableReferenced(byValue)) {
                error = "Only a single context variable is allowed for " + strategy + " value";
            }

            return error;

        } catch (VariableNameSyntaxException e) {
            error = "The context variable syntax for " + strategy + " is invalid";
        }

        return error;
    }

    /**
     * Generate a user-experienced readable counter name to be displayed in UI.
     * @param rawCounterName: the raw counter name with a format PRESET(<uuid>)-${<context variable>}, if the raw counter name is well-formatted.
     * @return a readable counter name in a format, "<8-digit of uuid>-${<context variable>}.
     */
    static String getReadableCounterName(final String rawCounterName) {
        final String[] uuidOut = new String[]{null};
        final String quotaOption = CounterPresetInfo.findCounterNameKey(rawCounterName, uuidOut, PRESET_CUSTOM, COUNTER_NAME_TYPES);

        String readableCounterName = quotaOption == null?
                rawCounterName : CounterPresetInfo.makeDefaultCustomExpr(uuidOut[0], COUNTER_NAME_TYPES.get(quotaOption));

        if (readableCounterName == null) {
            readableCounterName = rawCounterName;
        } else if (readableCounterName.length() > 128) {
            readableCounterName = TextUtils.truncateStringAtEnd(readableCounterName, 128);
        }

        return readableCounterName;
    }

    /**
     * Build a compatibility mapping that will read serialized XML produced by a HalfAsyncThroughputQuota.aar file
     * (that we used as a stopgap pre-Goatfish) and instantiate a modern ThroughputQuotaAssertion in half-async mode.
     *
     * @param defaultMapping the default mapping for ThroughputQuotaAssertion, to which almost all the work will be delegated.
     * @return a TypeMapping that will parse HalfAsyncThroughputQuota policy XML into a ThroughputQuotaAssertion in half-async mode.
     */
    static TypeMapping halfAsyncCompatibilityMapping(final AssertionMapping defaultMapping) {
        return new CompatibilityAssertionMapping(new ThroughputQuotaAssertion(), "HalfAsyncThroughputQuota") {
            protected void configureAssertion(Assertion ass, Element source, WspVisitor visitor) throws InvalidPolicyStreamException {
                defaultMapping.populateObject(new TypedReference(ThroughputQuotaAssertion.class, ass), source, visitor);
                ((ThroughputQuotaAssertion)ass).setSynchronous(false);
            }
        };
    }

    public static Map<String, String> getCounterNameTypes() {
        return Collections.unmodifiableMap(COUNTER_NAME_TYPES);
    }
}