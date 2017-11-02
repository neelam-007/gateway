package com.l7tech.server.cluster;

/**
 * Elects a single node as the "master" node in the cluster.
 */
public class ClusterMasterStub implements ClusterMaster {

    //- PUBLIC

    public boolean isMaster = true;

    @Override
    public boolean isMaster(){ return isMaster;}
}
