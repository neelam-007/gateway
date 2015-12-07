package com.l7tech.external.assertions.gatewaymetrics;

import com.l7tech.gateway.common.service.MetricsBin;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.policy.wsp.Java5EnumTypeMapping;
import com.l7tech.policy.wsp.SimpleTypeMappingFinder;
import com.l7tech.policy.wsp.TypeMapping;
import com.l7tech.search.Dependency;
import com.l7tech.util.GoidUpgradeMapper;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class GatewayMetricsAssertion extends Assertion implements UsesVariables, SetsVariables {

    //
    // Metadata
    //
    private static final String META_INITIALIZED = GatewayMetricsAssertion.class.getName() + ".metadataInitialized";

    private static final String DEFAULT_VARIABLE_PREFIX = "metrics";
    private static final String VARIABLE_DOCUMENT = "document";

    private String clusterNodeId = null;
    private long publishedServiceOid;
    private Goid publishedServiceGoid = Goid.DEFAULT_GOID;
    private int resolution = MetricsBin.RES_FINE;

    private boolean useVariables = false;
    private String clusterNodeName = null;
    private String publishedServiceName = null;
    private String resolutionName = null;

    private IntervalType intervalType = IntervalType.MOST_RECENT;
    private String numberOfRecentIntervals = null;
    private String numberOfRecentIntervalsWithinTimePeriod = null;
    private IntervalTimeUnit intervalTimeUnit = IntervalTimeUnit.SECONDS;

    private String variablePrefix = DEFAULT_VARIABLE_PREFIX;

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        // Cluster properties used by this assertion
        Map<String, String[]> props = new HashMap<String, String[]>();
        meta.put(AssertionMetadata.CLUSTER_PROPERTIES, props);

        // Set description for GUI
        meta.put(AssertionMetadata.SHORT_NAME, "Get Gateway Metrics");
        meta.put(AssertionMetadata.LONG_NAME, "Get Gateway Metrics.");

        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.gatewaymetrics.console.GatewayMetricsConfigDialog");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME, "Get Gateway Metrics Properties");

        // Add to palette folder(s)
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "audit" });
        meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/console/resources/Edit16.gif");

        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");

        // Set up smart Getter for nice, informative policy node name, for GUI
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/Edit16.gif");

        // request default feature set name for our class name, since we are a known optional module
        // that is, we want our required feature set to be "assertion:GatewayMetrics" rather than "set:modularAssertions"
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "set:modularAssertions");

        meta.put(AssertionMetadata.WSP_SUBTYPE_FINDER, new SimpleTypeMappingFinder(Arrays.<TypeMapping>asList(
            new Java5EnumTypeMapping(IntervalType.class, "intervalType"),
            new Java5EnumTypeMapping(IntervalTimeUnit.class, "intervalTimeUnit"))));

        meta.put(AssertionMetadata.MODULE_LOAD_LISTENER_CLASSNAME, "com.l7tech.external.assertions.gatewaymetrics.server.GatewayMetricsAssertionLoadListener");

        meta.put(META_INITIALIZED, Boolean.TRUE);

        return meta;
    }

    @Override
    public String[] getVariablesUsed() {
        return Syntax.getReferencedNames(
            clusterNodeName,
            publishedServiceName,
            resolutionName,
            numberOfRecentIntervals,
            numberOfRecentIntervalsWithinTimePeriod);
    }

    @Override
    public VariableMetadata[] getVariablesSet() {
        return new VariableMetadata[]{
            new VariableMetadata(getVariablePrefix() + "." + VARIABLE_DOCUMENT, false, false, null, false, DataType.STRING)
        };
    }

    public String[] getVariableSuffix(){
        return new String[] {
            VARIABLE_DOCUMENT
        };
    }

    public String getDocumentVariable () {
        return getVariablePrefix() + "." + VARIABLE_DOCUMENT;
    }

    public void setClusterNodeId(String clusterNodeId) {
        this.clusterNodeId = clusterNodeId;
    }

    public String getClusterNodeId() {
        return clusterNodeId;
    }

    public void setPublishedServiceGoid(Goid publishedServiceGoid) {
        this.publishedServiceGoid = publishedServiceGoid;
    }

    public void setPublishedServiceOid(long publishedServiceOid) {
        this.publishedServiceGoid = GoidUpgradeMapper.mapOid(EntityType.SERVICE, publishedServiceOid);
    }

    @Migration(export = false)
    @Dependency(type = Dependency.DependencyType.SERVICE, methodReturnType = Dependency.MethodReturnType.GOID)
    public Goid getPublishedServiceGoid() {
        return publishedServiceGoid;
    }

    public void setResolution(int resolution) {
        this.resolution = resolution;
    }

    public int getResolution() {
        return resolution;
    }

    public boolean getUseVariables() {
        return useVariables;
    }

    public void setUseVariables(boolean useVariables) {
        this.useVariables = useVariables;
    }

    public void setClusterNodeVariable(String clusterNodeName) {
        this.clusterNodeName = clusterNodeName;
    }

    public String getClusterNodeVariable() {
        return clusterNodeName;
    }

    public void setPublishedServiceVariable(String publishedServiceName) {
        this.publishedServiceName = publishedServiceName;
    }

    public String getPublishedServiceVariable() {
        return publishedServiceName;
    }

    public void setResolutionVariable(String resolutionName) {
        this.resolutionName = resolutionName;
    }

    public String getResolutionVariable() {
        return resolutionName;
    }

    public void setIntervalType(IntervalType intervalType) {
        this.intervalType = intervalType;
    }

    public IntervalType getIntervalType() {
        return intervalType;
    }

    public void setNumberOfRecentIntervals(String numberOfRecentIntervals) {
        this.numberOfRecentIntervals = numberOfRecentIntervals;
    }

    public String getNumberOfRecentIntervals() {
        return numberOfRecentIntervals;
    }

    public void setNumberOfRecentIntervalsWithinTimePeriod(String numberOfRecentIntervalsWithinTimePeriod) {
        this.numberOfRecentIntervalsWithinTimePeriod = numberOfRecentIntervalsWithinTimePeriod;
    }

    public String getNumberOfRecentIntervalsWithinTimePeriod() {
        return numberOfRecentIntervalsWithinTimePeriod;
    }

    public void setIntervalTimeUnit(IntervalTimeUnit intervalTimeUnit) {
        this.intervalTimeUnit = intervalTimeUnit;
    }

    public IntervalTimeUnit getIntervalTimeUnit() {
        return intervalTimeUnit;
    }

    public void setVariablePrefix (final String variablePrefix) {
        if (variablePrefix != null && !variablePrefix.trim().isEmpty()) {
            this.variablePrefix = variablePrefix;
        } else {
            this.variablePrefix = DEFAULT_VARIABLE_PREFIX;
        }
    }

    public String getVariablePrefix () {
        if (variablePrefix == null || variablePrefix.trim().isEmpty()) {
            variablePrefix = DEFAULT_VARIABLE_PREFIX;
        }
        return variablePrefix;
    }
}