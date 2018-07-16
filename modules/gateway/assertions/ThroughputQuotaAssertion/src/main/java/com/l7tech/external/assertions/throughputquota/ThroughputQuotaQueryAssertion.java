package com.l7tech.external.assertions.throughputquota;

import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableMetadata;

import java.util.ArrayList;
import java.util.Collection;

import static com.l7tech.objectmodel.ExternalEntityHeader.ValueType.TEXT_ARRAY;
import static com.l7tech.policy.assertion.AssertionMetadata.*;

/**
 * An Assertion that can query the status of a throughput quota counter.
 */
public class ThroughputQuotaQueryAssertion extends Assertion implements SetsVariables, UsesVariables {

    //
    // Metadata
    //
    private static final String META_INITIALIZED = ThroughputQuotaQueryAssertion.class.getName() + ".metadataInitialized";

    private static final String BASE_NAME = "Query Throughput Quota";

    public static final String COUNTER_NAME = "counter.name";
    public static final String COUNTER_SEC = "counter.sec";
    public static final String COUNTER_MIN = "counter.min";
    public static final String COUNTER_HR = "counter.hr";
    public static final String COUNTER_DAY = "counter.day";
    public static final String COUNTER_MNT = "counter.mnt";
    public static final String COUNTER_LASTUPDATE = "counter.lastupdate";

    static final AssertionNodeNameFactory policyNameFactory = new AssertionNodeNameFactory<ThroughputQuotaQueryAssertion>(){
        @Override
        public String getAssertionName( final ThroughputQuotaQueryAssertion assertion, final boolean decorate) {
            if(!decorate) {
                return BASE_NAME;
            }

            StringBuilder sb = new StringBuilder(BASE_NAME);
            final String assertionCounterName = assertion.getCounterName();
            String prettyName = ThroughputQuotaAssertion.getReadableCounterName(assertionCounterName);
            sb.append(" for counter ").append(prettyName != null ? prettyName : ("\"" + assertionCounterName + "\""));

            return sb.toString();
        }
    };

    private String counterName;
    private String variablePrefix;

    public String getCounterName() {
        return counterName;
    }

    public void setCounterName(String counterName) {
        this.counterName = counterName;
    }

    public String getVariablePrefix() {
        return variablePrefix;
    }

    public void setVariablePrefix(String variablePrefix) {
        this.variablePrefix = variablePrefix;
    }

    @Override
    public VariableMetadata[] getVariablesSet() {
        Collection<VariableMetadata> ret = new ArrayList<>();
        ret.add(prefix(COUNTER_NAME));
        ret.add(prefix(COUNTER_SEC));
        ret.add(prefix(COUNTER_MIN));
        ret.add(prefix(COUNTER_HR));
        ret.add(prefix(COUNTER_DAY));
        ret.add(prefix(COUNTER_MNT));
        ret.add(prefix(COUNTER_LASTUPDATE));
        return ret.toArray(new VariableMetadata[ret.size()]);
    }

    public static String[] getVariableSuffixes() {
        return new String[] {
                COUNTER_NAME,
                COUNTER_SEC,
                COUNTER_MIN,
                COUNTER_HR,
                COUNTER_DAY,
                COUNTER_MNT,
                COUNTER_LASTUPDATE
        };
    }
    private VariableMetadata prefix(String name) {
        String fullname = variablePrefix == null ? name : variablePrefix + "." + name;
        return new VariableMetadata(fullname);
    }

    @Migration(mapName = MigrationMappingSelection.NONE, mapValue = MigrationMappingSelection.REQUIRED, export = false, valueType = TEXT_ARRAY, resolver = PropertyResolver.Type.SERVER_VARIABLE)
    @Override
    public String[] getVariablesUsed() {
        return Syntax.getReferencedNames(counterName);
    }

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        // Request to appear in "misc" ("Service Availability") palette folder
        meta.put(PALETTE_FOLDERS, new String[] { "misc" });

        meta.put(SHORT_NAME, BASE_NAME);
        meta.put(LONG_NAME, BASE_NAME);
        meta.put(DESCRIPTION, "Query a throughput quota counter instance, by its exact counter name.  Counters are stored in the database and shared cluster-wide.");

        meta.put(PALETTE_NODE_ICON, "com/l7tech/external/assertions/throughputquota/console/resources/disconnect-question.gif");

        meta.put(PROPERTIES_ACTION_NAME, "Throughput Quota Query Properties");
        meta.put(PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.throughputquota.console.ThroughputQuotaQueryPropertiesDialog");
        meta.put(POLICY_ADVICE_CLASSNAME, "auto");

        meta.put(POLICY_NODE_NAME_FACTORY, policyNameFactory);

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }


}