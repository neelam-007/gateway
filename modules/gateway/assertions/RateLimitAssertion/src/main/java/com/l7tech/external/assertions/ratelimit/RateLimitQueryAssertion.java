package com.l7tech.external.assertions.ratelimit;

import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.sla.CounterPresetInfo;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableMetadata;

import java.util.ArrayList;
import java.util.Collection;

import static com.l7tech.objectmodel.ExternalEntityHeader.ValueType.TEXT_ARRAY;
import static com.l7tech.policy.assertion.AssertionMetadata.*;

/**
 * Assertion for querying the current state of a rate limit counter on this node.
 */
public class RateLimitQueryAssertion extends Assertion implements UsesVariables, SetsVariables {

    public static final String COUNTER_NAME = "counter.name";
    public static final String COUNTER_POINTS = "counter.points";
    public static final String COUNTER_POINTSPERREQUEST = "counter.pointsperrequest";
    public static final String COUNTER_REQUESTSREMAINING = "counter.requestsremaining";
    public static final String COUNTER_CONCURRENCY = "counter.concurrency";
    public static final String COUNTER_MAXCONCURRENCY = "counter.maxconcurrency";
    public static final String COUNTER_IDLEMILLIS = "counter.idlemillis";
    public static final String COUNTER_BLACKOUTMILLISREMAINING = "counter.blackoutmillisremaining";
    public static final String COUNTER_RESOLUTION = "counter.resolution";
    
    private String counterName;
    private String variablePrefix;

    /**
     * @return the counter name, or null if not yet configured.
     */
    public String getCounterName() {
        return counterName;
    }

    /**
     * @param counterName the new counter name.
     */
    public void setCounterName(String counterName) {
        this.counterName = counterName;
    }

    /**
     * @return variable prefix for querying counter state, or null to avoid setting any variables.
     */
    public String getVariablePrefix() {
        return variablePrefix;
    }

    /**
     * @param variablePrefix variable prefxi for querying counter state, or null to avoid setting any variables.
     */
    public void setVariablePrefix(String variablePrefix) {
        this.variablePrefix = variablePrefix;
    }

    @Migration(mapName = MigrationMappingSelection.NONE, mapValue = MigrationMappingSelection.REQUIRED, export = false, valueType = TEXT_ARRAY, resolver = PropertyResolver.Type.SERVER_VARIABLE)
    @Override
    public String[] getVariablesUsed() {
        return Syntax.getReferencedNames(counterName);
    }

    @Override
    public VariableMetadata[] getVariablesSet() {
        Collection<VariableMetadata> ret = new ArrayList<VariableMetadata>();
        ret.add(prefix(COUNTER_NAME));
        //ret.add(prefix(COUNTER_POINTS));
        //ret.add(prefix(COUNTER_POINTSPERREQUEST));
        ret.add(prefix(COUNTER_REQUESTSREMAINING));
        ret.add(prefix(COUNTER_CONCURRENCY));
        //ret.add(prefix(COUNTER_MAXCONCURRENCY));
        //ret.add(prefix(COUNTER_IDLEMILLIS)); // -1 if nonexistent or never used (or was deleted for being stale)
        ret.add(prefix(COUNTER_BLACKOUTMILLISREMAINING)); // 0 if not blacked out
        //ret.add(prefix(COUNTER_RESOLUTION)); // "nanos" or "millis"
        return ret.toArray(new VariableMetadata[ret.size()]);
    }


    public static String[] getVariableSuffixes() {
        return new String[] {
            COUNTER_NAME,
            COUNTER_REQUESTSREMAINING,
            COUNTER_CONCURRENCY,
            COUNTER_BLACKOUTMILLISREMAINING
        };
    }

    private VariableMetadata prefix(String name) {
        String fullname = variablePrefix == null ? name : variablePrefix + "." + name;
        return new VariableMetadata(fullname);
    }

    //
    // Metadata
    //
    private static final String META_INITIALIZED = RateLimitQueryAssertion.class.getName() + ".metadataInitialized";

    private final static String baseName = "Query Rate Limit";

    final static AssertionNodeNameFactory policyNameFactory = new AssertionNodeNameFactory<RateLimitQueryAssertion>(){
        @Override
        public String getAssertionName( final RateLimitQueryAssertion assertion, final boolean decorate) {
            if(!decorate || assertion.getCounterName() == null) return baseName;

            StringBuilder sb = new StringBuilder(baseName);
            final String counterName = assertion.getCounterName();
            String prettyName = CounterPresetInfo.findCounterNameKey(counterName, null, RateLimitAssertion.PRESET_CUSTOM, RateLimitAssertion.COUNTER_NAME_TYPES);
            sb.append(" for limit per ").append(prettyName != null ? prettyName : ("\"" + counterName + "\""));

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
        meta.put(LONG_NAME, baseName);
        meta.put(DESCRIPTION, "Query a rate limit counter instance, by its exact counter name.  Counter instances are per-node.");

        meta.put(PALETTE_NODE_ICON, "com/l7tech/console/resources/disconnect-question.gif");

        meta.put(PROPERTIES_ACTION_NAME, "Rate Limit Query Properties");
        meta.put(POLICY_ADVICE_CLASSNAME, "auto");

        meta.put(POLICY_NODE_NAME_FACTORY, policyNameFactory);

        meta.put(FEATURE_SET_NAME, "(fromClass)");

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

}
