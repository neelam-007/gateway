package com.l7tech.external.assertions.analytics;

import static com.l7tech.objectmodel.ExternalEntityHeader.ValueType.*;
import static com.l7tech.policy.assertion.AssertionMetadata.*;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.JdbcConnectionable;
import com.l7tech.policy.assertion.SetsVariables;
import com.l7tech.policy.assertion.UsesVariables;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.policy.wsp.MapTypeMapping;
import com.l7tech.policy.wsp.SimpleTypeMappingFinder;
import com.l7tech.policy.wsp.TypeMapping;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Capture an API Analytics record after executing all the child assertion.
 *
 * @author rraquepo, 7/4/2014
 */
public final class AnalyticsAssertion extends CompositeAssertion implements JdbcConnectionable, UsesVariables, SetsVariables, Cloneable, Serializable {

  public static final String ASSERTION_SHORT_NAME = "Capture API Portal Analytics ";
  private static final String META_INITIALIZED = AnalyticsAssertion.class.getName() + ".metadataInitialized";

  public static final String SSG_NODE_ID = "${ssgnode.id}";
  public static final String RESPONSE_CODE = "${response.http.status}";
  public static final String REQUEST_ID = "${requestId}";
  public static final String REQUEST_REMOTE_IP = "${request.tcp.remoteIP}";
  public static final String REQUEST_METHOD = "${request.http.method}";
  public static final String ROUTING_LATENCY = "${httpRouting.latency}";
  public static final String ROUTING_REASON_CODE = "${httpRouting.reasonCode}";
  public static final String REQUEST_URI = "${request.http.uri}";
  public static final String GATEWAY_TIME_MILLIS = "${gateway.time.millis}";
  public static final String PORTALMAN_API_ID = "${portal.managed.service.apiId}";
  public static final String PORTALMAN_API_KEY = "${apiKeyRecord.key}";
  public static final String PORTALMAN_ACCOUNT_PLAN_MAPPING_ID = "${accountPlanMappingId}";
  public static final String ANALYTICS_AUTH_TYPE = "${portal.analytics.authType}";
  public static final String ANALYTICS_RESPONSE_CODE = "${portal.analytics.response.code}";
  public static final String ANALYTICS_API_UUID = "${portal.analytics.api.uuid}";
  public static final String ANALYTICS_API_NAME = "${portal.analytics.api.name}";
  public static final String ANALYTICS_APP_UUID = "${portal.analytics.application.uuid}";
  public static final String ANALYTICS_APP_NAME = "${portal.analytics.application.name}";
  public static final String ANALYTICS_ORG_UUID = "${portal.analytics.organization.uuid}";
  public static final String ANALYTICS_ORG_NAME = "${portal.analytics.organization.name}";
  public static final String ANALYTICS_ACCOUNT_PLAN_UUID = "${portal.analytics.accountPlan.uuid}";
  public static final String ANALYTICS_ACCOUNT_PLAN_NAME = "${portal.analytics.accountPlan.name}";
  public static final String ANALYTICS_API_PLAN_UUID = "${portal.analytics.apiPlan.uuid}";
  public static final String ANALYTICS_API_PLAN_NAME = "${portal.analytics.apiPlan.name}";
  public static final String ANALYTICS_CUSTOM_TAG1 = "${portal.analytics.custom.tag1}";
  public static final String ANALYTICS_CUSTOM_TAG2 = "${portal.analytics.custom.tag2}";
  public static final String ANALYTICS_CUSTOM_TAG3 = "${portal.analytics.custom.tag3}";
  public static final String ANALYTICS_CUSTOM_TAG4 = "${portal.analytics.custom.tag4}";
  public static final String ANALYTICS_CUSTOM_TAG5 = "${portal.analytics.custom.tag5}";

  public static final String ANALYTICS_CAPTURE_BATCH_SIZE = "com.l7tech.analytics.capture.batchSize";
  public static final String PARAM_ANALYTICS_CAPTURE_BATCH_SIZE = ClusterProperty.asServerConfigPropertyName(ANALYTICS_CAPTURE_BATCH_SIZE);
  public static final int ANALYTICS_CAPTURE_BATCH_SIZE_DEFAULT = 200;

  private String connectionName;

  public AnalyticsAssertion() {
  }

  public AnalyticsAssertion(final List<? extends Assertion> children) {
    super(children);
  }

  public AnalyticsAssertion(final String connectionName, final List<? extends Assertion> children) {
    super(children);
    setConnectionName(connectionName);
  }

  @Override
  public AnalyticsAssertion clone() {
    AnalyticsAssertion copy = (AnalyticsAssertion) super.clone();

    copy.setConnectionName(connectionName);
    copy.setChildren(getChildren());

    return copy;
  }

  public void copyFrom(final AnalyticsAssertion source) {
    setConnectionName(source.getConnectionName());
    setChildren(source.getChildren());

  }

  @Override
  @Migration(mapName = MigrationMappingSelection.REQUIRED, export = false, resolver = PropertyResolver.Type.JDBC_CONNECTION)
  public String getConnectionName() {
    return connectionName;
  }

  @Override
  public void setConnectionName(String connectionName) {
    this.connectionName = connectionName;
  }

  @Override
  public VariableMetadata[] getVariablesSet() {
    List<VariableMetadata> varMeta = new ArrayList<VariableMetadata>();
    return varMeta.toArray(new VariableMetadata[varMeta.size()]);
  }

  @Override
  @Migration(mapName = MigrationMappingSelection.NONE, mapValue = MigrationMappingSelection.REQUIRED, export = false, valueType = TEXT_ARRAY, resolver = PropertyResolver.Type.SERVER_VARIABLE)
  public String[] getVariablesUsed() {
    return Syntax.getReferencedNames(connectionName);
  }

  @Override
  public boolean permitsEmpty() {
    return true;
  }

  @Override
  public AssertionMetadata meta() {
    DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
      return meta;

    meta.put(PALETTE_FOLDERS, new String[]{"policyLogic"});
    //meta.put(PALETTE_NODE_CLASSNAME, "com.l7tech.console.tree.AllNode");
    meta.put(PALETTE_NODE_ICON, "com/l7tech/console/resources/Dash16.gif");
    meta.put(POLICY_NODE_ICON_OPEN, "com/l7tech/console/resources/folderOpen.gif");

    meta.put(SHORT_NAME, ASSERTION_SHORT_NAME);
    meta.put(DESCRIPTION, "Capture an API Portal Analytic record after evaluating all child assertions. ");
    meta.put(PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.analytics.console.AnalyticsAssertionPropertiesDialog");
    meta.put(PROPERTIES_ACTION_NAME, "Analytics JDBC Properties");
    meta.put(POLICY_ADVICE_CLASSNAME, "auto");

    meta.put(CLUSTER_PROPERTIES, Collections.singletonMap(ANALYTICS_CAPTURE_BATCH_SIZE, new String[]{"The interval between OData cache wipes.", String.valueOf(ANALYTICS_CAPTURE_BATCH_SIZE_DEFAULT)}));

    meta.put(POLICY_NODE_CLASSNAME, "com.l7tech.external.assertions.analytics.console.AnalyticsAssertionPolicyNode");
    meta.put(POLICY_VALIDATOR_CLASSNAME, "com.l7tech.external.assertions.analytics.AnalyticsAssertionValidator");
    meta.put(WSP_TYPE_MAPPING_CLASSNAME, "com.l7tech.external.assertions.analytics.AnalyticsAssertionTypeMapping");

    meta.put(PALETTE_FOLDERS, new String[]{"internalAssertions"});
    // request default feature set name for our class name, since we are a known optional module
    // that is, we want our required feature set to be "assertion:OData" rather than "set:modularAssertions"
    //meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");
    meta.put(FEATURE_SET_NAME, "set:modularAssertions");

    meta.put(SERVER_ASSERTION_CLASSNAME, "com.l7tech.external.assertions.analytics.server.ServerAnalyticsAssertion");

    final TypeMapping typeMapping = new AnalyticsAssertionTypeMapping();
    meta.put(WSP_EXTERNAL_NAME, "Analytics");
    meta.put(WSP_SUBTYPE_FINDER, new SimpleTypeMappingFinder(Arrays.<TypeMapping>asList(new MapTypeMapping(), typeMapping)));


    meta.put(META_INITIALIZED, Boolean.TRUE);

    return meta;
  }

}
