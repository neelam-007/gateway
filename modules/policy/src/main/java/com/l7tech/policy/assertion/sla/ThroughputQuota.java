/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Apr 1, 2005<br/>
 */
package com.l7tech.policy.assertion.sla;

import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.util.TextUtils;
import com.l7tech.util.ValidationUtils;

import java.util.LinkedHashMap;
import java.util.Map;

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
public class ThroughputQuota extends Assertion implements UsesVariables, SetsVariables {
    public static final String PRESET_DEFAULT = "Authenticated user";
    public static final String PRESET_GLOBAL = "Gateway cluster";
    public static final String PRESET_CUSTOM = "Custom";
    public static final Map<String, String> COUNTER_NAME_TYPES = new LinkedHashMap<String, String>() {{
        put(PRESET_DEFAULT, "${request.authenticateduser.id}-${request.authenticateduser.providerid}");
        put("Client IP", "${request.tcp.remoteAddress}");
        put("SOAP operation", "${request.soap.operation}");
        put("SOAP namespace", "${request.soap.namespace}");
        put(PRESET_GLOBAL, "");
        put(PRESET_CUSTOM, CounterPresetInfo.makeUuid());
    }};
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
    private boolean synchronous = true; // true by default to preserve old behavior for pre-Goatfish policy XML

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

    public ThroughputQuota() {}

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
        if (idVariable == null) doVarNames();
        return idVariable;
    }

    public String valueVariable() {
        if (valueVariable == null) doVarNames();
        return valueVariable;
    }

    public String periodVariable() {
        if (periodVariable == null) doVarNames();
        return periodVariable;
    }

    public String userVariable() {
        if (userVariable == null) doVarNames();
        return userVariable;
    }

    public String maxVariable() {
        if (maxVariable == null) doVarNames();
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
        if (errorMsg != null) throw new IllegalArgumentException(errorMsg);

        this.quota = quota;
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
     */
    public boolean isGlobal() {
        return global;
    }

    /**
     * Set whether or not this quota is enforce on a per requestor basis or for all requestors together.
     * @param global true means quota applies to all requestors together, false means it's applied
     * to each requestor individually
     */
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
        if (!isGlobal() && !ThroughputQuota.DEFAULT_COUNTER_NAME.equals(counterName)) {
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

    @Override
    @Migration(mapName = MigrationMappingSelection.NONE, mapValue = MigrationMappingSelection.REQUIRED, export = false, valueType = TEXT_ARRAY, resolver = PropertyResolver.Type.SERVER_VARIABLE)
    public String[] getVariablesUsed() {
        // If the assertion is a previous version, then update the counter name by calling the method getCounterName().
        return Syntax.getReferencedNames(getCounterName(), getQuota());
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
            case ThroughputQuota.PER_SECOND: return "second";
            case ThroughputQuota.PER_MINUTE: return "minute";
            case ThroughputQuota.PER_HOUR: return "hour";
            case ThroughputQuota.PER_DAY: return "day";
            case ThroughputQuota.PER_MONTH: return "month";
            default: return "something";
        }
    }
    
    private final static String baseName = "Apply Throughput Quota";

    final static AssertionNodeNameFactory policyNameFactory = new AssertionNodeNameFactory<ThroughputQuota>(){
        @Override
        public String getAssertionName( final ThroughputQuota assertion, final boolean decorate) {
            if(!decorate) return baseName;

            final StringBuilder buffer = new StringBuilder( baseName );
            final String readableCounterName = getReadableCounterName(assertion.getCounterName());
            if (assertion.getCounterStrategy() == ThroughputQuota.DECREMENT) {
                buffer.append(": Decrement counter ").append(readableCounterName);
            } if (assertion.getCounterStrategy() == ThroughputQuota.RESET) {
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

        meta.put(SHORT_NAME, baseName);
        meta.put(DESCRIPTION, "Limit the number of service requests permitted within a predetermined time period.");

        meta.put(PALETTE_NODE_ICON, "com/l7tech/console/resources/policy16.gif");

        meta.put(POLICY_NODE_NAME_FACTORY, policyNameFactory);

        meta.put(PROPERTIES_ACTION_CLASSNAME, "com.l7tech.console.action.ThroughputQuotaPropertiesAction");
        meta.put(PROPERTIES_ACTION_NAME, "Throughput Quota Properties");
        
        return meta;
    }

    public static String validateQuota(String quota) {
        String error = null;
        final String[] varsUsed = Syntax.getReferencedNames(quota);
        if (varsUsed.length == 0 && !ValidationUtils.isValidLong(quota, false, 1, MAX_THROUGHPUT_QUOTA)) {
            error = "Throughput quota must be a value between 1 and " + MAX_THROUGHPUT_QUOTA;
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

        if (readableCounterName == null) readableCounterName = rawCounterName;
        if (readableCounterName.length() > 128) readableCounterName = TextUtils.truncateStringAtEnd(readableCounterName, 128);

        return readableCounterName;
    }
}