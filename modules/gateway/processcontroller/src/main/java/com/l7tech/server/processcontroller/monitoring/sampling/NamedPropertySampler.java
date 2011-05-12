package com.l7tech.server.processcontroller.monitoring.sampling;

import com.l7tech.objectmodel.FindException;
import com.l7tech.server.management.api.monitoring.MonitorableProperty;
import com.l7tech.server.management.api.node.NodeApi;
import com.l7tech.server.management.config.monitoring.ComponentType;
import com.l7tech.server.processcontroller.ConfigService;
import com.l7tech.util.ExceptionUtils;
import org.springframework.context.ApplicationContext;

import java.io.Serializable;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Node property samlper for named "cluster" scoped properties.
 */
abstract class NamedPropertySampler<T extends Serializable> extends NodePropertySampler<T> {
    private final Logger logger;
    private final int apiConnectTimeout;
    private final int apiReadTimeout;
    private volatile NodeApi api;

    NamedPropertySampler( final String componentId,
                          final ApplicationContext spring,
                          final MonitorableProperty property,
                          final Logger logger ) {
        super( ComponentType.CLUSTER, componentId, property.getName(), spring);
        this.logger = logger;
        this.apiConnectTimeout = configService.getIntProperty( ConfigService.HOSTPROPERTIES_SAMPLER_TIMEOUT_SLOW_CONNECT, 20000);
        this.apiReadTimeout = configService.getIntProperty(ConfigService.HOSTPROPERTIES_SAMPLER_TIMEOUT_SLOW_READ, 20000);
    }

    @Override
    public T sample() throws PropertySamplingException {
        try {
            return getValue();
        } catch (Exception e) {
            if (!isNetworkError(e))
                throw new PropertySamplingException("Couldn't get " + propertyName, e, true);
            logger.log( Level.FINE, "Caught SocketException trying to get NodeApi; retrying", ExceptionUtils.getDebugException( e ));
            clearApi();
            /* FALLTHROUGH and retry once using fresh API client */
        }

        try {
            return getValue();
        } catch (Exception e1) {
            throw new PropertySamplingException("Unsupported property", e1, false);
        }
    }

    /**
     * Unserialize the string value.
     *
     * @param value The property value
     * @return The converted value
     */
    protected abstract T cast( final String value );

    private void clearApi() {
        api = null;
    }

    private static boolean isNetworkError(Exception e) {
        return ExceptionUtils.causedBy(e, SocketException.class);
    }

    private T getValue() throws NodeApi.UnsupportedPropertyException, FindException {
        return cast( getApi().getProperty( propertyName ) );
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
