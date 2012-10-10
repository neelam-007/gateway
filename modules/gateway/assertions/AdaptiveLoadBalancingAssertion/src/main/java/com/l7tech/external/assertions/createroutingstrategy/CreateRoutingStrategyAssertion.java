package com.l7tech.external.assertions.createroutingstrategy;

import com.l7tech.external.assertions.adaptiveloadbalancing.AbstractAdaptiveLoadBalancing;
import com.l7tech.common.io.failover.FailoverStrategy;
import com.l7tech.common.io.failover.Service;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.policy.wsp.BeanTypeMapping;
import com.l7tech.policy.wsp.CollectionTypeMapping;
import com.l7tech.policy.wsp.SimpleTypeMappingFinder;
import com.l7tech.policy.wsp.TypeMapping;

import java.util.*;

import static com.l7tech.policy.assertion.AssertionMetadata.POLICY_NODE_NAME_FACTORY;

/**
 *
 */
public class CreateRoutingStrategyAssertion extends AbstractAdaptiveLoadBalancing implements UsesVariables, SetsVariables {


    public static final String ROUTE_LIST = ".routeList";
    private final static String baseName = "Create Routing Strategy";
    private static final int MAX_DISPLAY_LENGTH = 80;
    private List<Service> routes = new ArrayList<Service>();

    private String strategyName = null;
    private Map<String, String> strategyProperties = new HashMap();
    private String strategyDescription;

    public Map<String, String> getStrategyProperties() {
        return strategyProperties;
    }

    public void setStrategyProperties(Map<String, String> strategyProperties) {
        this.strategyProperties = strategyProperties;
    }

    public String getRouteList() {
        return getStrategy() + ROUTE_LIST;
    }

    /**
     * The route list
     * @return
     */
    public List<Service> getRoutes() {
        return routes;
    }

    public void setRoutes(List<Service> routes) {
        this.routes = routes;
    }

    /**
     * The selected strategy name
     * @return
     */
    public String getStrategyName() {
        return strategyName;
    }

    public void setStrategyName(String strategyName) {
        this.strategyName = strategyName;
    }

    public String getStrategyDescription() {
        return strategyDescription;
    }

    public void setStrategyDescription(String desc) {
        this.strategyDescription = desc;
    }

    //
    // Metadata
    //
    private static final String META_INITIALIZED = CreateRoutingStrategyAssertion.class.getName() + ".metadataInitialized";

    @Override
    public VariableMetadata[] getVariablesSet() {

        return new VariableMetadata[]{
                new VariableMetadata(getStrategy(), true, false, null, false, DataType.UNKNOWN),
                new VariableMetadata(getRouteList(), false, true, null, false, DataType.UNKNOWN),
        };
    }

    @Override
    public String[] getVariablesUsed() {
        final List<String> expressions = new ArrayList<String>();
        for (Service route: routes) {
            expressions.add(route.getName());
        }
        return Syntax.getReferencedNames(expressions.toArray(new String[expressions.size()]));
    }

    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        // Set description for GUI
        meta.put(AssertionMetadata.SHORT_NAME, baseName);
        meta.put(AssertionMetadata.LONG_NAME, "Create Routing Strategy Assertion");
        meta.put(AssertionMetadata.DESCRIPTION, "Creates a routing strategy using the defined route list and " +
                "stores the strategy for use by the Execute Routing Strategy and Process Routing Strategy Result assertions.");

        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{"policyLogic"});
        meta.put(AssertionMetadata.WSP_SUBTYPE_FINDER, new SimpleTypeMappingFinder(Arrays.<TypeMapping>asList(
                new CollectionTypeMapping(List.class, Service.class, ArrayList.class, "routes"),
                new BeanTypeMapping(Service.class, "route"),
                new BeanTypeMapping(FailoverStrategy.class, "securityObj")
        )));
        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");
        meta.put(POLICY_NODE_NAME_FACTORY, policyNameFactory);

        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/server16.gif");
        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.createroutingstrategy.console.CreateRoutingStrategyAssertionDialog");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME, "Create Routing Strategy Properties");

        meta.put(AssertionMetadata.FEATURE_SET_NAME, "set:modularAssertions");
        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

    final static AssertionNodeNameFactory policyNameFactory = new AssertionNodeNameFactory<CreateRoutingStrategyAssertion>(){
        @Override
        public String getAssertionName( final CreateRoutingStrategyAssertion assertion, final boolean decorate) {
            if(!decorate) return baseName;

            StringBuffer name = new StringBuffer(baseName + " ");
            name.append(assertion.getStrategyDescription());
            name.append(" as ${");
            name.append(assertion.getStrategy());
            name.append("}");
            if(name.length() > MAX_DISPLAY_LENGTH) {
                name = name.replace(MAX_DISPLAY_LENGTH - 1, name.length() - 1, "...");
            }
            return name.toString();
        }
    };
}
