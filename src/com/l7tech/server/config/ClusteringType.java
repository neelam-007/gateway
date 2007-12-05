package com.l7tech.server.config;

/**
 * User: megery
 * Date: Aug 29, 2006
 * Time: 1:29:22 PM
 */
public enum ClusteringType {
    CLUSTER_MASTER("Create a cluster using this node as the configuration source"),
    CLUSTER_CLONE("Clone an existing cluster node"),
//    CLUSTER_JOIN("I would like this SSG to join an existing cluster"),
    UNDEFINED("")
    ;

    private String description;


    ClusteringType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public String toString() {
        return description;
    }
}
