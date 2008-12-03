package com.l7tech.server.ems.gateway;

import com.l7tech.server.management.api.node.GatewayApi;
import com.l7tech.server.ems.enterprise.SsgCluster;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.util.ExceptionUtils;

import javax.xml.ws.soap.SOAPFaultException;
import java.util.List;
import java.util.Collection;

/**
 * Implementation of {@link GatewayClusterClient}.
 */
class GatewayClusterClientImpl implements GatewayClusterClient {
    private final SsgCluster ssgCluster;
    private final List<GatewayContext> nodeContexts;

    public GatewayClusterClientImpl(SsgCluster ssgCluster, List<GatewayContext> nodeContexts) {
        if (ssgCluster == null) throw new NullPointerException("ssgCluster");
        if (nodeContexts == null) throw new NullPointerException("nodeContexts");
        this.ssgCluster = ssgCluster;
        this.nodeContexts = nodeContexts;
    }

    public SsgCluster getCluster() {
        return ssgCluster;
    }

    public Collection<GatewayApi.EntityInfo> getEntityInfo(final Collection<EntityType> entityTypes) throws GatewayException {
        if (entityTypes == null) throw new NullPointerException();
        return callApi(new GatewayApiCall<Collection<GatewayApi.EntityInfo>>() {
            public Collection<GatewayApi.EntityInfo> call(GatewayApi api) throws GatewayApi.GatewayException {
                return api.getEntityInfo(entityTypes);
            }
        });
    }

    public GatewayApi.ClusterInfo getClusterInfo() throws GatewayException {
        return callApi(new GatewayApiCall<GatewayApi.ClusterInfo>() {
            public GatewayApi.ClusterInfo call(GatewayApi api) throws GatewayApi.GatewayException, SOAPFaultException {
                return api.getClusterInfo();
            }
        });
    }

    public Collection<GatewayApi.GatewayInfo> getGatewayInfo() throws GatewayException {
        return callApi(new GatewayApiCall<Collection<GatewayApi.GatewayInfo>>() {
            public Collection<GatewayApi.GatewayInfo> call(GatewayApi api) throws GatewayApi.GatewayException, SOAPFaultException {
                return api.getGatewayInfo();
            }
        });
    }

    private interface GatewayApiCall<R> {
        R call(GatewayApi api) throws GatewayApi.GatewayException, SOAPFaultException;
    }

    private <R> R callApi(GatewayApiCall<R> call) throws GatewayException {
        SOAPFaultException lastNetworkException = null;
        for (GatewayContext context : nodeContexts) {
            try {
                // TODO memoize this call, cache results in this instance for a time
                return call.call(context.getApi());
            } catch (GatewayApi.GatewayException e) {
                throw new GatewayException(e);
            } catch (SOAPFaultException sfe) {
                if (GatewayContext.isNetworkException(sfe)) {
                    lastNetworkException = sfe;
                    // Failover to next node
                    // TODO ordered sticky failover strategy
                    /* FALLTHROUGH and try next node */
                } else {
                    throw new GatewayException(sfe);
                }
            }
        }

        throw new GatewayNetworkException("Unable to find any working cluster node to talk to: " + ExceptionUtils.getMessage(lastNetworkException), lastNetworkException);
    }
}
