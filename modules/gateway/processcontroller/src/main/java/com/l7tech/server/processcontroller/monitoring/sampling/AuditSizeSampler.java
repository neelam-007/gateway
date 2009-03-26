/*
 * Copyright (C) 2009 Layer 7 Technologies Inc.
 */
package com.l7tech.server.processcontroller.monitoring.sampling;

import com.l7tech.objectmodel.FindException;
import com.l7tech.server.management.api.monitoring.BuiltinMonitorables;
import com.l7tech.server.management.api.monitoring.MonitorableProperty;
import com.l7tech.server.management.api.node.NodeApi;
import com.l7tech.server.management.config.monitoring.ComponentType;
import com.l7tech.server.processcontroller.ConfigService;
import com.l7tech.util.ExceptionUtils;
import org.springframework.context.ApplicationContext;

import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AuditSizeSampler extends NodePropertySampler<Long> {
    private static final Logger logger = Logger.getLogger(AuditSizeSampler.class.getName());
    private static final MonitorableProperty AUDIT_SIZE = BuiltinMonitorables.AUDIT_SIZE;

    private final int apiConnectTimeout;
    private final int apiReadTimeout;
    private volatile NodeApi api;

    public AuditSizeSampler(String componentId, ApplicationContext spring) {
        super(ComponentType.CLUSTER, componentId, AUDIT_SIZE.getName(), spring);
        apiConnectTimeout = configService.getIntProperty(ConfigService.HOSTPROPERTIES_SAMPLER_TIMEOUT_SLOW_CONNECT, 20000);
        apiReadTimeout = configService.getIntProperty(ConfigService.HOSTPROPERTIES_SAMPLER_TIMEOUT_SLOW_READ, 20000);
    }

    @Override
    public Long sample() throws PropertySamplingException {
        try {
            return getValue();
        } catch (Exception e) {
            if (!isNetworkError(e))
                throw new PropertySamplingException("Couldn't get audit size", e, true);
            logger.log(Level.FINE, "Caught SocketException trying to get NodeApi; retrying", ExceptionUtils.getDebugException(e));
            clearApi();
            /* FALLTHROUGH and retry once using fresh API client */
        }

        try {
            return getValue();
        } catch (Exception e1) {
            throw new PropertySamplingException("Unsupported property", e1, false);
        }
    }

    private void clearApi() {
        api = null;
    }

    private static boolean isNetworkError(Exception e) {
        return ExceptionUtils.causedBy(e, SocketException.class);
    }

    private Long getValue() throws NodeApi.UnsupportedPropertyException, FindException {
        return Long.valueOf(getApi().getProperty(propertyName));
    }

    private NodeApi getApi() {
        if (api == null)
            api = makeApi();
        return api;
    }

    private NodeApi makeApi() {
        return processController.getNodeApi(null, apiReadTimeout, apiConnectTimeout);
    }
}
