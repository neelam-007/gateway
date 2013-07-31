package com.l7tech.gateway.common.security.rbac;

import com.l7tech.objectmodel.*;
import org.apache.commons.lang.ObjectUtils;
import org.hibernate.annotations.Proxy;
import org.jetbrains.annotations.Nullable;

import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * A scope predicate for restricting a permission to entities within a particular security zone.
 */
@javax.persistence.Entity
@Proxy(lazy=false)
@Table(name="rbac_predicate_security_zone")
public class SecurityZonePredicate extends ScopePredicate implements ScopeEvaluator  {
    private SecurityZone requiredZone;

    public SecurityZonePredicate(Permission permission, SecurityZone requiredZone) {
        super(permission);
        this.requiredZone = requiredZone;
    }

    // Only used for serialization purposes
    @Deprecated
    protected SecurityZonePredicate() {
    }

    /**
     * @return the SecurityZone that an entity must belong to in order to match this predicate. If null, an entity must be zoneable but not in a SecurityZone.
     */
    @ManyToOne(optional=false)
    @JoinColumn(name = "security_zone_goid")
    public SecurityZone getRequiredZone() {
        return requiredZone;
    }

    /**
     * @param requiredZone the SecurityZone that an entity must belong to in order to match this predicate. If null, an entity must be zoneable but not in a SecurityZone.
     */
    public void setRequiredZone(@Nullable final SecurityZone requiredZone) {
        this.requiredZone = requiredZone;
    }

    @Override
    public boolean matches(Entity entity) {
        if (entity instanceof PartiallyZoneableEntity) {
            final PartiallyZoneableEntity pze = (PartiallyZoneableEntity) entity;
            if (!pze.isZoneable())
                return false;
        }
        if (entity instanceof ZoneableEntity && (requiredZone == null || entityTypePermitted(requiredZone, entity))) {
            final ZoneableEntity ze = (ZoneableEntity) entity;
            // if required zone is null, the entity security zone must also be null
            return ObjectUtils.equals(requiredZone, ze.getSecurityZone());
        } else if (entity != null && !(entity instanceof ZoneableEntity) && requiredZone == null) {
            // non-zoneable entity matches no zone
            return true;
        }
        return false;
    }

    private static boolean entityTypePermitted(SecurityZone requiredZone, Entity entity) {
        return entity != null && requiredZone.permitsEntityType(EntityType.findTypeByEntity(entity.getClass()));
    }

    @Override
    public ScopePredicate createAnonymousClone() {
        SecurityZonePredicate copy = new SecurityZonePredicate(null, requiredZone);
        copy.setOid(this.getOid());
        return copy;
    }

    @SuppressWarnings("RedundantIfStatement")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SecurityZonePredicate)) return false;
        if (!super.equals(o)) return false;

        SecurityZonePredicate that = (SecurityZonePredicate) o;

        if (requiredZone != null ? !requiredZone.equals(that.requiredZone) : that.requiredZone != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (requiredZone != null ? requiredZone.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        if (permission != null) {
            sb.append(permission.getEntityType().getPluralName());
        }
        if (requiredZone != null) {
            sb.append(" in security zone ").append(requiredZone.getName());
        } else {
            sb.append(" not in any security zone");
        }
        return sb.toString();
    }
}
