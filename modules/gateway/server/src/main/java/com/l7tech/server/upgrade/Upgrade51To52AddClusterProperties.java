package com.l7tech.server.upgrade;

import java.util.Collections;
import java.util.HashMap;

/**
 * 
 */
public class Upgrade51To52AddClusterProperties extends ClusterPropertyUpgradeTask {

    public Upgrade51To52AddClusterProperties() {
        super( Collections.<String>emptySet(), new HashMap<String,String>(){{
            put( "saml.generation.includeDNSAddress", "true" );
        }} );
    }

}
