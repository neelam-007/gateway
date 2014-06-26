package com.l7tech.external.assertions.gatewaymanagement.server.rest.exceptions;

import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    public InsufficientPermissionsException(@Nullable final User user, Throwable e) {
        super(MessageFormat.format("Permission denied for user {0}.", user != null ? user.getLogin() : "<unauthenticated>"), e);
    }

    public InsufficientPermissionsException(@Nullable final User user, @NotNull final Entity entity, @Nullable final OperationType operationType, @Nullable final String otherOperationName) {
        super(MessageFormat.format("Permission denied for user {0} on entity {1} with id {2}. Requested operation {3}", user != null ? user.getLogin() : "<unauthenticated>", EntityType.findTypeByEntity(entity.getClass()), entity.getId(), OperationType.OTHER.equals(operationType) ? otherOperationName : operationType));
    }

    public InsufficientPermissionsException(@Nullable final User user, @NotNull final Entity entity, @NotNull final OperationType operationType, @Nullable final String otherOperationName, @NotNull final Throwable e) {
        super(MessageFormat.format("Permission denied for user {0} on entity {1} with id {2}. Requested operation {3}", user != null ? user.getLogin() : "<unauthenticated>", EntityType.findTypeByEntity(entity.getClass()), entity.getId(), OperationType.OTHER.equals(operationType) ? otherOperationName : operationType), e);
    }
}
