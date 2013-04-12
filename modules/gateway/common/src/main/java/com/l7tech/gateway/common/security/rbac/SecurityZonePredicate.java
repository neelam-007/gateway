package com.l7tech.gateway.common.security.rbac;

import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.SecurityZone;
import com.l7tech.objectmodel.ZoneableEntity;
import org.hibernate.annotations.Proxy;

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

    @ManyToOne(optional=false)
    @JoinColumn(name = "security_zone_oid")
    public SecurityZone getRequiredZone() {
        return requiredZone;
    }

    public void setRequiredZone(SecurityZone requiredZone) {
        this.requiredZone = requiredZone;
    }

    @Override
    public boolean matches(Entity entity) {
        if (requiredZone != null && entity instanceof ZoneableEntity && entityTypePermitted(requiredZone, entity)) {
            ZoneableEntity ze = (ZoneableEntity) entity;
            return requiredZone.equals(ze.getSecurityZone());
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
        sb.append(permission.getEntityType().getPluralName());
        sb.append(" in security zone ").append(requiredZone.getName());
        return sb.toString();
    }
}
