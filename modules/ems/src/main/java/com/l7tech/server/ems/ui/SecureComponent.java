package com.l7tech.server.ems.ui;

import com.l7tech.gateway.common.security.rbac.AttemptedOperation;

/**
 * Interface implemented by any WicketComponent that is secure.
 */
public interface SecureComponent {

    /**
     * Get the attempted operation for this component.
     *
     * @return The attempted operation or null for unsecured.
     */
    AttemptedOperation getAttemptedOperation();
}
