/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Apr 1, 2005<br/>
 */
package com.l7tech.policy.assertion.sla;

import com.l7tech.policy.assertion.*;
import static com.l7tech.policy.assertion.AssertionMetadata.*;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.util.ValidationUtils;

import static com.l7tech.objectmodel.ExternalEntityHeader.ValueType.TEXT_ARRAY;

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
    public static final int MAX_THROUGHPUT_QUOTA = 100000;
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
    private boolean global = false;
    private int timeUnit = PER_MONTH;
    private String counterName = "";
    public static final int ALWAYS_INCREMENT = 1;
    public static final int INCREMENT_ON_SUCCESS = 2;
    public static final int DECREMENT = 3;
    private int counterStrategy = INCREMENT_ON_SUCCESS;

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

    @Override
    @Migration(mapName = MigrationMappingSelection.NONE, mapValue = MigrationMappingSelection.REQUIRED, export = false, valueType = TEXT_ARRAY, resolver = PropertyResolver.Type.SERVER_VARIABLE)
    public String[] getVariablesUsed() {
        return Syntax.getReferencedNames(counterName, getQuota());
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
            if (assertion.getCounterStrategy() == ThroughputQuota.DECREMENT) {
                buffer.append(": Decrement counter ").append(assertion.getCounterName());
            } else {
                buffer.append(": ").append(assertion.getCounterName()).append(": ").append(assertion.getQuota()).append(" per ").append(timeUnitStr(assertion.getTimeUnit()));
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
        if (varsUsed.length == 0 && !ValidationUtils.isValidInteger(quota, false, 1, MAX_THROUGHPUT_QUOTA)) {
            error = "Throughput quota must be an integer between 1 and " + MAX_THROUGHPUT_QUOTA;
        }
        return error;
    }
}