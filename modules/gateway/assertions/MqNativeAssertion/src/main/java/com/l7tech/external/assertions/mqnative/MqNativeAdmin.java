package com.l7tech.external.assertions.mqnative;

import com.l7tech.gateway.common.security.rbac.Secured;
import org.springframework.transaction.annotation.Transactional;

import static com.l7tech.objectmodel.EntityType.GENERIC;

/**
 * Admin interface for SSM code to perform MQ Native functions in server-side
 *
 * @author ghuang
 */

@Secured(types=GENERIC)
public interface MqNativeAdmin {

    /**
     * Get the default value of the MQ message max bytes defined in "io.mqMessageMaxBytes"
     * @return the maximum number of bytes permitted for a MQ Native Queue  message, or 0 for unlimited (Integer)
     */
    @Transactional(readOnly=true)
    long getDefaultMqMessageMaxBytes();
}