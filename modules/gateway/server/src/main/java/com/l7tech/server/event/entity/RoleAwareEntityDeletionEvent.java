package com.l7tech.server.event.entity;

import com.l7tech.objectmodel.Entity;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationEvent;

/**
 * Published when an Entity which may be referenced by a Role is going to be deleted.
 */
public class RoleAwareEntityDeletionEvent extends ApplicationEvent {
    private final Entity entity;

    public RoleAwareEntityDeletionEvent(@NotNull final Object source, @NotNull final Entity entity) {
        super(source);
        this.entity = entity;
    }

    public Entity getEntity() {
        return entity;
    }
}
