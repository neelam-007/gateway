/**
 * Copyright (C) 2006-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.gateway.common.security.rbac;

import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityType;

import javax.persistence.Table;
import javax.persistence.Column;
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
@Table(name="rbac_predicate_attribute")
public class AttributePredicate extends ScopePredicate implements ScopeEvaluator {
    private static final Logger logger = Logger.getLogger(AttributePredicate.class.getName());

    private String attribute;
    private String value;
    private transient Method getter;

    public AttributePredicate(Permission permission, String attribute, String value) {
        super(permission);
        this.value = value.trim().intern();
        setAttribute(attribute);
    }

    protected AttributePredicate() { }

    protected void setAttribute(String attribute) {
        this.attribute = attribute;
        setupGetter();
    }

    private void setupGetter() {
        if (getter != null || attribute == null) return;

        String uname = Character.toUpperCase(attribute.charAt(0)) +
                (attribute.length() > 1 ? attribute.substring(1) : "");
        String getname = "get" + uname;
        String isname = "is" + uname;

        EntityType etype = permission.getEntityType();
        if (etype == null || etype == EntityType.ANY)
            throw new IllegalStateException("Can't evaluate an AttributePredicate without a specific EntityType");

        Class entityClass = etype.getEntityClass();
        if (entityClass == null) throw new IllegalArgumentException();
        Method[] meths = entityClass.getMethods();
        for (Method method : meths) {
            String name = method.getName();
            if (name.equals(getname) || name.equals(isname)) {
                Class[] types = method.getParameterTypes();
                if (types.length != 0) continue;
                Class rtype = method.getReturnType();
                if (Number.class.isAssignableFrom(rtype) || rtype == Long.TYPE || rtype == Integer.TYPE || rtype == Byte.TYPE || rtype == Short.TYPE ||
                    CharSequence.class.isAssignableFrom(rtype) || 
                    rtype == Boolean.TYPE || Boolean.class.isAssignableFrom(rtype))
                {
                    this.getter = method;
                    break;
                } else {
                    throw new IllegalArgumentException("Return type of " + entityClass.getName() + "." + method.getName() + " must be CharSequence, Long, long, Boolean or boolean");
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

    public boolean matches(Entity entity) {
        setupGetter();
        try {
            Object got = getter.invoke(entity);
            if (got == null) return value == null;
            return got.toString().trim().equals(value);
        } catch (IllegalAccessException e) {
            logger.log(Level.SEVERE, "Couldn't invoke " + entity.getClass().getName() + "." + getter.getName(), e);
            return false;
        } catch (InvocationTargetException e) {
            logger.log(Level.SEVERE, "Couldn't invoke " + entity.getClass().getName() + "." + getter.getName(), e);
            return false;
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(permission.getEntityType().getPluralName());
        sb.append(" with ").append(attribute).append(" = ").append(value);
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

        return true;
    }

    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (attribute != null ? attribute.hashCode() : 0);
        result = 31 * result + (value != null ? value.hashCode() : 0);
        result = 31 * result + (getter != null ? getter.hashCode() : 0);
        return result;
    }
}
