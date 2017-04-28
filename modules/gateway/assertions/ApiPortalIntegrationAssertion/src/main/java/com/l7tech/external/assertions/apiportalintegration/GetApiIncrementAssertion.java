package com.l7tech.external.assertions.apiportalintegration;

import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.VariableMetadata;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * This assertion will query the portal database via the supplied jdbc and retrieve the increment.  The output of this
 * assertion will be the json payload with the increment.
 *
 * @author wlui
 */
public class GetApiIncrementAssertion extends Assertion implements UsesVariables, SetsVariables {
  protected static final Logger logger = Logger.getLogger(GetApiIncrementAssertion.class.getName());

  private static final String VARIABLE_PREFIX = "portal.sync.api.increment";

  public static final String SUFFIX_JDBC_CONNECTION = "jdbc";
  public static final String SUFFIX_NODE_ID = "nodeId";
  public static final String SUFFIX_TYPE = "type";
  public static final String SUFFIX_JSON = "json";
  public static final String SUFFIX_TENANT_ID = "tenantId";


  public String getVariablePrefix() {
    return VARIABLE_PREFIX;
  }

  @Override
  public VariableMetadata[] getVariablesSet() {
    return new VariableMetadata[]{
        new VariableMetadata(VARIABLE_PREFIX + "." + SUFFIX_JSON, false, false, null, true, DataType.STRING)
    };
  }

  @Override
  public String[] getVariablesUsed() {
    return VariableUseSupport.variables(VARIABLE_PREFIX + "." + SUFFIX_NODE_ID,
        VARIABLE_PREFIX + "." + SUFFIX_JDBC_CONNECTION,
        VARIABLE_PREFIX + "." + SUFFIX_TYPE,
        VARIABLE_PREFIX + "." + SUFFIX_TENANT_ID).asArray();
  }

  //
  // Metadata
  //
  private static final String META_INITIALIZED = GetApiIncrementAssertion.class.getName() + ".metadataInitialized";

  public AssertionMetadata meta() {
    DefaultAssertionMetadata meta = super.defaultMeta();
    if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
      return meta;

    // Ensure inbound transport gets wired up
    meta.put(AssertionMetadata.MODULE_LOAD_LISTENER_CLASSNAME, "com.l7tech.external.assertions.apiportalintegration.server.ModuleLoadListener");

    // Cluster properties used by this assertion
    Map<String, String[]> props = new HashMap<>();
    meta.put(AssertionMetadata.CLUSTER_PROPERTIES, props);

    // Set description for GUI
    meta.put(AssertionMetadata.SHORT_NAME, "Portal Get Api V2 Sync");
    meta.put(AssertionMetadata.LONG_NAME, "Portal Get Api V2 Sync Json message");

    //Adding internalAssertions in to PALETTE_FOLDERS
    meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{"internalAssertions"});
    meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/server16.gif");

    // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
    meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");

    // request default feature set name for our class name, since we are a known optional module
    // that is, we want our required feature set to be "assertion:ApiPortalIntegration" rather than "set:modularAssertions"
    meta.put(AssertionMetadata.FEATURE_SET_NAME, "set:modularAssertions");

    meta.put(META_INITIALIZED, Boolean.TRUE);
    return meta;
  }

}
