/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Apr 1, 2005<br/>
 */
package com.l7tech.policy.assertion.sla;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.UsesVariables;
import com.l7tech.policy.assertion.SetsVariables;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.Syntax;

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
    public static final int TIME_UNIT_UNDEFINED = 0;
    public static final int PER_SECOND = 1;
    public static final int PER_HOUR = 2;
    public static final int PER_DAY = 3;
    public static final int PER_MONTH = 4;
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
    private long quota = 200;
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
    public long getQuota() {
        return quota;
    }

    /**
     * The quota property represents the maximum number of requests that are allowed at run time per timeUnit.
     * @param quota the quota
     */
    public void setQuota(long quota) {
        this.quota = quota;
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
     * @return TIME_UNIT_UNDEFINED, PER_SECOND, PER_HOUR, PER_DAY or PER_MONTH
     */
    public int getTimeUnit() {
        return timeUnit;
    }

    /**
     * The time unit applicable to the quota.
     * @param timeUnit TIME_UNIT_UNDEFINED, PER_SECOND, PER_HOUR, PER_DAY or PER_MONTH
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

    public String[] getVariablesUsed() {
        if (counterName == null) return new String[0];
        return Syntax.getReferencedNames(counterName);
    }

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
}
