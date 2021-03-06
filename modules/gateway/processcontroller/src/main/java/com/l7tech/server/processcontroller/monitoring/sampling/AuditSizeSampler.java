package com.l7tech.server.processcontroller.monitoring.sampling;

import com.l7tech.server.management.api.monitoring.BuiltinMonitorables;
import com.l7tech.server.management.api.monitoring.MonitorableProperty;
import org.springframework.context.ApplicationContext;

import java.util.logging.Logger;

public class AuditSizeSampler extends NamedPropertySampler<Long> {
    private static final Logger logger = Logger.getLogger(AuditSizeSampler.class.getName());
    private static final MonitorableProperty AUDIT_SIZE = BuiltinMonitorables.AUDIT_SIZE;

    public AuditSizeSampler(String componentId, ApplicationContext spring) {
        super(componentId, spring, AUDIT_SIZE, logger);
    }

    @Override
    protected Long cast( final String value ) {
        return Long.valueOf( value );
    }
}
