/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server;

import com.l7tech.policy.AllAssertions;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.server.identity.cert.CSRHandler;
import com.l7tech.server.policy.PolicyServlet;
import com.l7tech.gateway.common.License;
import org.junit.Test;
import static org.junit.Assert.*;


import java.io.PrintStream;
import java.util.*;
import java.util.logging.Logger;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * @author mike
 * @noinspection JavaDoc
 */
public class GatewayFeatureSetsTest {
    /** @noinspection UnusedDeclaration*/
    private static Logger log = Logger.getLogger(GatewayFeatureSetsTest.class.getName());

    @Test
    public void testEmitWikiDocs_ServiceNames() throws Exception {
        emit(System.out, "Service Names", collectServices(Arrays.asList(ALL_SERVICES)), new HashSet<String>(), false);
    }

    @Test
    public void testEmitWikiDocs_UiFeatures() throws Exception {
        emit(System.out, "UI Features", collectServices(Arrays.asList(ALL_UI)), new HashSet<String>(), false);
    }

    @Test
    public void testEmitWikiDocs_BuldingBlocks() throws Exception {
        emit(System.out, "Building Blocks", GatewayFeatureSets.getRootFeatureSets(), new HashSet<String>(), true);
    }

    @Test
    public void testEmitWikiDocs_ProductProfiles() throws Exception {
        emit(System.out, "Product Profiles", GatewayFeatureSets.getProductProfiles(), new HashSet<String>(), true);
    }

    private Map<String, GatewayFeatureSet> collectServices(List<String> names) {
        Map<String, GatewayFeatureSet> services = new HashMap<String, GatewayFeatureSet>();
        Set<String> allServNames = new LinkedHashSet<String>(names);
        for (String servName : allServNames) {
            GatewayFeatureSet serv = GatewayFeatureSets.getAllFeatureSets().get(servName);
            assertNotNull(serv);
            services.put(servName, serv);
        }
        return services;
    }

    private void emit(PrintStream out, String what, Map<String, GatewayFeatureSet> sets, Set<String> visited, boolean includeLastTwoColumns) {
        out.println("\n\n<!-- Begin generated by " + getClass().getName() + " -- do not edit below this line -- " + what + " -->");
        String lasttwoHeaders = includeLastTwoColumns ? "!! Included Feature Sets !! Notes" : "";
        out.println("{| border=\"1\" cellpadding=\"3\" cellspacing=\"0\" style=\"font-size: 90%; border: gray solid 2px; border-collapse: collapse; text-align: left; width: 95%\" \n" +
                "|- \n" +
                "! Name !! Short Description " + lasttwoHeaders);
        for (Map.Entry<String, GatewayFeatureSet> entry : sets.entrySet()) {
            String name = entry.getKey();
            GatewayFeatureSet fs = entry.getValue();
            if (visited.contains(name)) continue;
            visited.add(name);
            out.println("|-");
            out.println("| " + name + " || " + fs.desc + (includeLastTwoColumns ? " || " : ""));
            for (GatewayFeatureSet dep : fs.sets) {
                out.println("* " + dep.name);
            }
            if (includeLastTwoColumns)
                out.println("|| " + fs.getNote());
        }
        out.println("|}");
        out.println("<!-- End generated by " + getClass().getName() + " -- do not edit above this line -- " + what + " -->\n");
    }

    @Test
    public void testAllAssertionsReachableThroughProfileAll() throws Exception {
        GatewayFeatureSet profileAll = GatewayFeatureSets.getBestProductProfile();

        Assertion[] allAssertions = AllAssertions.SERIALIZABLE_EVERYTHING;
        for (Assertion assertion : allAssertions) {
            String name = GatewayFeatureSets.getFeatureSetNameForAssertion(assertion.getClass());
            if (!profileAll.contains(name))
                throw new RuntimeException("Assertion is not enabled by the full-featured Product Profile: " +
                        assertion.getClass() + " (feature set name would be " + name + ")");
        }
    }

    @Test
    public void testAllOptionalAssertionsHaveAccessToModuleLoading() throws Exception {
        Map<String, GatewayFeatureSet> profiles = GatewayFeatureSets.getProductProfiles();

        License.FeatureSetExpander expander = GatewayFeatureSets.getFeatureSetExpander();
        for (String profileName : profiles.keySet()) {
            //noinspection unchecked
            Set<String> expanded = expander.getAllEnabledFeatures(new HashSet<String>(Arrays.asList(profileName)));

            boolean allowsModuleLoading = expanded.contains(GatewayFeatureSets.SERVICE_MODULELOADER);

            for (String name : expanded) {
                if (GatewayFeatureSets.isOptionalModularAssertion(name))
                    assertTrue("Product profile " + profileName + " that enables optional modular assertion " + name + " must also enable " + GatewayFeatureSets.SERVICE_MODULELOADER, allowsModuleLoading);
            }

            if (expanded.contains("set:modularAssertions"))
                assertTrue(allowsModuleLoading);
        }
    }

    @Test
    public void testEverythingMapped() throws Exception {
        Map<String, GatewayFeatureSet> allSets = GatewayFeatureSets.getAllFeatureSets();

        Assertion[] allAssertions = AllAssertions.SERIALIZABLE_EVERYTHING;
        for (Assertion assertion : allAssertions) {
            String name = GatewayFeatureSets.getFeatureSetNameForAssertion(assertion.getClass());
            if (!allSets.containsKey(name))
                throw new RuntimeException("Assertion is not present in any Feature Set: " +
                        assertion.getClass() + " (feature set name would be " + name + ")");
        }

        for (String name : ALL_SERVICES) {
            if (!allSets.containsKey(name))
                throw new RuntimeException("Service is not present in any Feature Set: " + name);
        }

        for (String name : ALL_UI) {
            if (!allSets.containsKey(name))
                throw new RuntimeException("UI feature is not present in any Feature Set: " + name);
        }
    }

    private static final String[] EXTRA_SERVICES = {
            "service:CSRHandler",       GatewayFeatureSets.getFeatureSetNameForServlet(CSRHandler.class),
            "service:Passwd",           GatewayFeatureSets.getFeatureSetNameForServlet(PasswdServlet.class),
            "service:Policy",           GatewayFeatureSets.getFeatureSetNameForServlet(PolicyServlet.class),
            "service:TokenService",     GatewayFeatureSets.getFeatureSetNameForServlet(TokenServiceServlet.class),
            "service:SnmpQuery",        GatewayFeatureSets.getFeatureSetNameForServlet(SnmpQueryServlet.class),
            "service:WsdlProxy",        GatewayFeatureSets.getFeatureSetNameForServlet(WsdlProxyServlet.class),
    };
    private static final String[] ALL_SERVICES = findStaticStringValuesWithPrefix("SERVICE_", EXTRA_SERVICES);
    private static final String[] EXTRA_FEATURES = {};
    private static final String[] ALL_FEATURES = findStaticStringValuesWithPrefix("FEATURE_", EXTRA_FEATURES);
    private static final String[] EXTRA_UI = {};
    private static final String[] ALL_UI = findStaticStringValuesWithPrefix("UI_", EXTRA_UI);

    private static String[] findStaticStringValuesWithPrefix(String prefix, String[] extra) {
        Set<String> servs = new HashSet<String>(Arrays.asList(extra));

        Field[] fields = GatewayFeatureSets.class.getFields();
        for (Field field : fields) {
            if (Modifier.isStatic(field.getModifiers()) &&
                field.getName().startsWith(prefix) &&
                    field.getType().equals(String.class))
            {
                try {
                    servs.add((String)field.get(null));
                } catch (IllegalAccessException e) {
                    throw new Error(e);
                }
            }
        }

        return servs.toArray(new String[servs.size()]);
    }

    /** Makes sure that all registered services are included in ALL_SERVICES.  Dual of testAllServicesMapped. */
    @Test
    public void testAllServicesKnown() throws Exception {
        GatewayFeatureSet profileAll = GatewayFeatureSets.getBestProductProfile();

        Set<String> allServs = new HashSet<String>(Arrays.asList(ALL_SERVICES));
        Set<String> names = new HashSet<String>();
        profileAll.collectAllFeatureNames(names);

        for (String name : names) {
            if (name.startsWith("service:") && !allServs.contains(name))
                throw new RuntimeException("Service is registered as a feature but is not present in ALL_SERVICES: " + name);
        }
    }

    /** Makes sure that all registered services are included in ALL_SERVICES.  Dual of testAllServicesMapped. */
    @Test
    public void testAllFeaturesKnown() throws Exception {
        GatewayFeatureSet profileAll = GatewayFeatureSets.getBestProductProfile();

        Set<String> allFeatures = new HashSet<String>(Arrays.asList(ALL_FEATURES));
        Set<String> names = new HashSet<String>();
        profileAll.collectAllFeatureNames(names);

        for (String name : names) {
            if (name.startsWith("feature:") && !allFeatures.contains(name))
                throw new RuntimeException("Feature is registered as a feature but is not present in ALL_FEATURES: " + name);
        }
    }

    @Test
    public void testAllUiKnown() throws Exception {
        GatewayFeatureSet profileAll = GatewayFeatureSets.getBestProductProfile();

        Set<String> allServs = new HashSet<String>(Arrays.asList(ALL_UI));
        Set<String> names = new HashSet<String>();
        profileAll.collectAllFeatureNames(names);

        for (String name : names) {
            if (name.startsWith("ui:") && !allServs.contains(name))
                throw new RuntimeException("UI feature is registered as a feature but is not present in ALL_UI: " + name);
        }
    }

    /** Makes sure that all services in ALL_SERVICES are registered.  Dual of testAllServicesKnown. */
    @Test
    public void testAllServicesMapped() throws Exception {
        GatewayFeatureSet profileAll = GatewayFeatureSets.getBestProductProfile();

        Assertion[] allAssertions = AllAssertions.SERIALIZABLE_EVERYTHING;
        for (String name : ALL_SERVICES) {
            if (!profileAll.contains(name))
                throw new RuntimeException("Servlet is not enabled by the full-featured Product Profile: " + name);
        }

        for (String name : ALL_FEATURES) {
            if (!profileAll.contains(name))
                throw new RuntimeException("Feature is not enabled by the full-featured Product Profile: " + name);
        }

        for (String name : ALL_UI) {
            if (!profileAll.contains(name))
                throw new RuntimeException("UI feature is not enabled by the full-featured Product Profile: " + name);
        }

        for (Assertion assertion : allAssertions) {
            String name = GatewayFeatureSets.getFeatureSetNameForAssertion(assertion.getClass());
            assertNotNull(name);
        }
    }

    @Test
    public void testAllAssertionsProfiled() throws Exception {
        final Collection<GatewayFeatureSet> allProfiles = GatewayFeatureSets.getProductProfiles().values();
        GatewayFeatureSet allProfilesSet =
                new GatewayFeatureSet("EVERYTHING_PROFILED", "", null,
                        allProfiles.toArray(new GatewayFeatureSet[allProfiles.size()]));

        Assertion[] allAssertions = AllAssertions.SERIALIZABLE_EVERYTHING;
        for (Assertion assertion : allAssertions) {
            String name = GatewayFeatureSets.getFeatureSetNameForAssertion(assertion.getClass());
            if (!allProfilesSet.contains(name))
                throw new RuntimeException("Assertion is not enabled by any root-level Product Profile: " +
                        assertion.getClass() + " (feature set name would be " + name + ")");
        }
    }
}
