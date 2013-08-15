/**
 * Copyright (C) 2006-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.gateway.common.security.rbac;

import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.NamedEntity;
import org.hibernate.annotations.Proxy;

import javax.persistence.Column;
import javax.persistence.Table;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Matches an {@link com.l7tech.objectmodel.Entity} if a specified {@link #attribute} of that entity matches a
 * predetermined value.
 * <p>
 * The values of the attribute in question <em>must</em> be of type <code>long</code>, {@link Long}
 * or {@link CharSequence}.
 */
@javax.persistence.Entity
@Proxy(lazy=false)
@Table(name="rbac_predicate_attribute")
public class AttributePredicate extends ScopePredicate implements ScopeEvaluator {
    private static final Logger logger = Logger.getLogger(AttributePredicate.class.getName());
    public static final String EQUALS = "eq";
    public static final String STARTS_WITH = "sw";

    private String attribute;
    private String value;
    private String mode;
    private transient volatile Method getter;

    public AttributePredicate(Permission permission, String attribute, String value) {
        super(permission);
        this.value = value.trim().intern();
        setAttribute(attribute);
    }

    protected AttributePredicate() { }

    @Override
    public ScopePredicate createAnonymousClone() {
        Permission anonymousPermission = null;
        if (permission != null) {
            anonymousPermission = new Permission();
            // want to keep the entity type
            anonymousPermission.setEntityType(getPermission().getEntityType());
        }
        AttributePredicate copy = new AttributePredicate(anonymousPermission, this.attribute, this.value);
        copy.setGoid(this.getGoid());
        copy.setMode(this.mode);
        copy.getter = this.getter;
        return copy;
    }

    @Override
    protected void setPermission(Permission permission) {
        super.setPermission(permission);
        setupGetter();
    }

    protected void setAttribute(String attribute) {
        this.attribute = attribute;
        setupGetter();
    }

    private void setupGetter() {
        if (getter != null || attribute == null || permission == null)
            return;

        String uname = Character.toUpperCase(attribute.charAt(0)) +
                (attribute.length() > 1 ? attribute.substring(1) : "");
        String getname = "get" + uname;
        String isname = "is" + uname;

        EntityType etype = permission.getEntityType();
        if (etype == null)
            throw new IllegalStateException("Can't evaluate an AttributePredicate without a specific EntityType");

        Class entityClass;
        if (EntityType.ANY == etype) {
            // Allow attempt to access "name" property as a special case
            entityClass = NamedEntity.class;
        } else {
            entityClass = etype.getEntityClass();
        }
        if (entityClass == null)
            throw new IllegalArgumentException();

        Method[] meths = entityClass.getMethods();
        for (Method method : meths) {
            String name = method.getName();
            if (name.equals(getname) || name.equals(isname)) {
                Class[] types = method.getParameterTypes();
                if (types.length != 0) continue;
                Class rtype = method.getReturnType();
                if (Number.class.isAssignableFrom(rtype) || rtype == Long.TYPE || rtype == Integer.TYPE || rtype == Byte.TYPE || rtype == Short.TYPE ||
                    CharSequence.class.isAssignableFrom(rtype) || 
                    rtype == Boolean.TYPE || Boolean.class.isAssignableFrom(rtype) ||
                    Enum.class.isAssignableFrom(rtype) ||
                    Goid.class.isAssignableFrom(rtype))
                {
                    synchronized (this) {
                        if (this.getter == null)
                            this.getter = method;
                    }
                    break;
                } else {
                    throw new IllegalArgumentException("Return type of " + entityClass.getName() + "." + method.getName() + " must be CharSequence, enum, Long, long, Boolean or boolean");
                }
            }
        }
        if (getter == null)
            throw new IllegalArgumentException("Class " + entityClass + " has no getter for attribute " + attribute);
    }

    @Column(name="attribute", nullable=false, length=255)
    public String getAttribute() {
        return attribute;
    }

    @Column(name="value", length=255)
    public String getValue() {
        return value;
    }

    protected void setValue(String value) {
        this.value = value;
    }

    /**
     * Get the comparison mode:  "eq" is full equality (after the current value is converted to a string and trimmed).
     * "sw" tests whether the current value starts with the expected value (after the current value is converted to a string and trimmed).
     * A value of null shall be treated as the same as "eq", for backward compatibility.
     *
     * @return the comparison mode (e.g. "sw" or "eq").  A value of null is to be treated as the same as "eq".
     */
    @Column(name="mode", length=255)
    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public boolean matches(Entity entity) {
        if (attribute == null) {
            logger.log(Level.SEVERE, "Couldn't check RBAC attribute predicate for  " + entity.getClass().getName() + ":  null attibute");
            return false;
        }
        if (permission == null) {
            logger.log(Level.SEVERE, "Couldn't check RBAC attribute predicate for  " + entity.getClass().getName() + "." + attribute + ":  null permission");
            return false;
        }

        setupGetter();

        Object got;
        try {
            got = getter.invoke(entity);
        } catch (IllegalAccessException e) {
            logger.log(Level.SEVERE, "Couldn't invoke " + entity.getClass().getName() + "." + getter.getName(), e);
            return false;
        } catch (InvocationTargetException e) {
            logger.log(Level.SEVERE, "Couldn't invoke " + entity.getClass().getName() + "." + getter.getName(), e);
            return false;
        } catch (IllegalArgumentException e) {
            logger.log(Level.SEVERE, "Couldn't invoke " + entity.getClass().getName() + "." + getter.getName(), e);
            return false;
        }

        if (got == null)
            return value == null;

        if (mode == null || mode.equals(EQUALS)) {
            return got.toString().trim().equals(value);
        } else if (mode.equals(STARTS_WITH)) {
            return got.toString().trim().startsWith(value);
        } else {
            logger.log(Level.SEVERE, "Unrecognized RBAC predicate attribute comparison mode \"" + mode + "\" for " + entity.getClass().getName() + "." + getter.getName());
            return false;
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (permission != null) {
            sb.append(permission.getEntityType().getPluralName());
        }
        String oper = "sw".equals(mode) ? " starting with " : " = ";
        sb.append(" with ").append(attribute).append(oper).append(value);
        return sb.toString(); 
    }

    @SuppressWarnings({"RedundantIfStatement"})
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        AttributePredicate that = (AttributePredicate) o;

        if (attribute != null ? !attribute.equals(that.attribute) : that.attribute != null) return false;
        if (getter != null ? !getter.equals(that.getter) : that.getter != null) return false;
        if (value != null ? !value.equals(that.value) : that.value != null) return false;
        if (mode != null ? !mode.equals(that.mode) : that.mode != null) return false;

        return true;
    }

    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (attribute != null ? attribute.hashCode() : 0);
        result = 31 * result + (value != null ? value.hashCode() : 0);
        result = 31 * result + (getter != null ? getter.hashCode() : 0);
        result = 31 * result + (mode != null ? mode.hashCode() : 0);
        return result;
    }
}
