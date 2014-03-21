package com.l7tech.external.assertions.gatewaymanagement.server.rest.exceptions;

import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityType;

import java.text.MessageFormat;

/**
 * This exception is used to capture invalid privileges or permission errors
 */
public class InsufficientPermissionsException extends RuntimeException {
    public InsufficientPermissionsException(String message) {
        super(message);
    }

    public InsufficientPermissionsException(String message, Throwable e) {
        super(message, e);
    }

    public InsufficientPermissionsException(User user, Entity entity, OperationType operationType, String otherOperationName) {
        super(MessageFormat.format("Permission denied for user {0} on entity {1} with id {2}. Requested operation {3}", user.getLogin(), EntityType.findTypeByEntity(entity.getClass()), entity.getId(), OperationType.OTHER.equals(operationType) ? otherOperationName : operationType));
    }

    public InsufficientPermissionsException(User user, Entity entity, OperationType operationType, String otherOperationName, Throwable e) {
        super(MessageFormat.format("Permission denied for user {0} on entity {1} with id {2}. Requested operation {3}", user.getLogin(), EntityType.findTypeByEntity(entity.getClass()), entity.getId(), OperationType.OTHER.equals(operationType) ? otherOperationName : operationType), e);
    }
}
