package com.l7tech.server.policy;

import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.event.system.Started;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import java.util.*;
import java.util.logging.Logger;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The Gateway's AssertionRegistry, which extends the default registry with the ability to look for
 * modular ServerConfig properties in the new assertions and register them with ServerConfig.
 */
public class ServerAssertionRegistry extends AssertionRegistry implements ApplicationListener {
    protected static final Logger logger = Logger.getLogger(ServerAssertionRegistry.class.getName());

    // Install the default getters that are specified to the Gateway
    private static final AtomicBoolean gatewayMetadataDefaultsInstalled = new AtomicBoolean(false);
    static {
        installGatewayMetadataDefaults();
    }

    private final ServerConfig serverConfig;
    private Map<String, String[]> newClusterProps; // do not initialize -- clobbers info collected during super c'tor

    public ServerAssertionRegistry(ServerConfig serverConfig) {
        installGatewayMetadataDefaults();
        this.serverConfig = serverConfig;
    }

    public Assertion registerAssertion(Class<? extends Assertion> assertionClass) {
        Assertion prototype = super.registerAssertion(assertionClass);

        // Check if the new assertion requires any new serverConfig properties.
        AssertionMetadata meta = prototype.meta();
        //noinspection unchecked
        Map<String, String[]> newProps = (Map<String, String[]>)meta.get(AssertionMetadata.CLUSTER_PROPERTIES);
        if (newProps != null) {
            // We may be called during superclass c'tor, so may need to initialize our own field here
            if (newClusterProps == null) newClusterProps = new ConcurrentHashMap<String, String[]>();

            for (Map.Entry<String, String[]> entry : newProps.entrySet()) {
                final String name = entry.getKey();
                final String[] tuple = entry.getValue();
                String desc = tuple != null && tuple.length > 0 ? tuple[0] : null;
                String dflt = tuple != null && tuple.length > 1 ? tuple[1] : null;
                newClusterProps.put(name, new String[] { desc, dflt });
            }
        }

        return prototype;
    }

    private static final Pattern findMidDots = Pattern.compile("\\.([a-zA-Z0-9_])");

    /**
     * Converts a cluster property name like "foo.bar.blatzBloof.bargleFoomp" into a ServerConfig property
     * root like "fooBarBlatzBlofBargleFoomp".
     *
     * @param clusterPropertyName the cluster property name to convert
     * @return the corresponding serverConfig property name.  Never null.
     */
    private static String makeServerConfigPropertyName(String clusterPropertyName) {
        Matcher matcher = findMidDots.matcher(clusterPropertyName);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, matcher.group(1).toUpperCase());
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /** Scan modular assertions for new cluster properties. */
    private void scanModularAssertions() {
        Map<String,String> namesToDesc =  serverConfig.getClusterPropertyNames();
        Set<String> knownNames = namesToDesc.keySet();

        List<String[]> toAdd = new ArrayList<String[]>();

        if (newClusterProps == null)
            return;
        
        for (Map.Entry<String, String[]> entry : newClusterProps.entrySet()) {
            String clusterPropertyName = entry.getKey();
            String[] tuple = entry.getValue();
            if (!knownNames.contains(clusterPropertyName)) {
                // Dynamically register this new cluster property
                String desc = tuple[0];
                String dflt = tuple[1];
                String serverConfigName = makeServerConfigPropertyName(clusterPropertyName);

                toAdd.add(new String[] { serverConfigName, clusterPropertyName, desc, dflt });
                logger.info("Dynamically registering cluster property " + clusterPropertyName);
            }
        }
        if (!toAdd.isEmpty()) serverConfig.registerServerConfigProperties(toAdd.toArray(new String[][] {}));
    }

    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        if (applicationEvent instanceof Started) {
            scanModularAssertions();
        }
    }

    private static void installGatewayMetadataDefaults() {
        if (gatewayMetadataDefaultsInstalled.get())
            return;

        DefaultAssertionMetadata.putDefaultGetter(AssertionMetadata.CLUSTER_PROPERTIES, new DefaultAssertionMetadata.Getter() {
            public Object get(AssertionMetadata meta, String key) {
                return DefaultAssertionMetadata.cache(meta, key, new HashMap());
            }
        });

        gatewayMetadataDefaultsInstalled.set(true);
    }
}
