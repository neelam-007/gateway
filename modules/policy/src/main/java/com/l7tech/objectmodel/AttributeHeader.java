/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.objectmodel;

import com.l7tech.objectmodel.imp.NamedGoidEntityImp;
import com.l7tech.policy.variable.DataType;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import static com.l7tech.objectmodel.AttributeHeader.Builtin.CUSTOM;
import static com.l7tech.objectmodel.UsersOrGroups.BOTH;
import static com.l7tech.objectmodel.UsersOrGroups.USERS;
import static com.l7tech.policy.variable.DataType.STRING;

/**
 * Basic information about an attribute; typically forms part of an {@Link AttributeConfig}.
 *
 * @author alex
 */
public class AttributeHeader extends NamedGoidEntityImp implements Serializable {
    /**
     * Only classes in this package can declare whether a variable is built-in (any header created by "outsiders" 
     * is considered "custom.")
     */
    public enum Builtin { BUILTIN, CUSTOM }

    private static final Map<String, AttributeHeader> nameCache = new HashMap<String, AttributeHeader>();

    public static final AttributeHeader ID = new AttributeHeader("id", "Unique ID", STRING, BOTH, Builtin.BUILTIN);
    public static final AttributeHeader PROVIDER_GOID = new AttributeHeader("providerId", "Identity Provider GOID", DataType.INTEGER, BOTH, Builtin.BUILTIN);
    public static final AttributeHeader NAME = new AttributeHeader("name", "Common Name", STRING, BOTH, Builtin.BUILTIN);
    public static final AttributeHeader SUBJECT_DN = new AttributeHeader("subjectDn", "X.500 Subject DN", STRING, BOTH, Builtin.BUILTIN);

    public static final AttributeHeader EMAIL = new AttributeHeader("email", "E-mail Address", STRING, USERS, Builtin.BUILTIN);
    public static final AttributeHeader LOGIN = new AttributeHeader("login", "User Login", STRING, USERS, Builtin.BUILTIN);
    public static final AttributeHeader FIRST_NAME = new AttributeHeader("firstName", "First Name", DataType.STRING, USERS, Builtin.BUILTIN);
    public static final AttributeHeader LAST_NAME = new AttributeHeader("lastName", "Last Name", STRING, USERS, Builtin.BUILTIN);
    public static final AttributeHeader DEPARTMENT = new AttributeHeader("department", "Department", STRING, USERS, Builtin.BUILTIN);

    public static final AttributeHeader DESCRIPTION = new AttributeHeader("description", "Description", DataType.STRING, UsersOrGroups.GROUPS, Builtin.BUILTIN);

    private String variableName;
    private String description;
    private DataType type;
    private UsersOrGroups usersOrGroups;
    private final Builtin builtin;

    public AttributeHeader() {
        this.builtin = CUSTOM;
    }

    public AttributeHeader(String variableName, String name, DataType type, UsersOrGroups uog) {
        this(variableName, name, type, uog, CUSTOM);
    }

    public AttributeHeader(String variableName, String name, DataType type, UsersOrGroups uog, Builtin builtin) {
        if (nameCache.get(variableName) != null) throw new IllegalArgumentException(MessageFormat.format("the variable name {0} is already in use; variable names must be globally unique", variableName));
        this.variableName = variableName;
        this._name = name;
        this.type = type;
        this.usersOrGroups = uog;
        this.builtin = builtin;
        nameCache.put(variableName, this);
    }

    public String getVariableName() {
        return variableName;
    }

    public void setVariableName(String variableName) {
        if (builtin == Builtin.BUILTIN) throw new UnsupportedOperationException("Can't set properties on built-in attribute headers");
        this.variableName = variableName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        if (builtin == Builtin.BUILTIN) throw new UnsupportedOperationException("Can't set properties on built-in attribute headers");
        this.description = description;
    }

    public DataType getType() {
        return type;
    }

    public void setType(DataType type) {
        if (builtin == Builtin.BUILTIN) throw new UnsupportedOperationException("Can't set properties on built-in attribute headers");
        this.type = type;
    }

    public UsersOrGroups getUsersOrGroups() {
        return usersOrGroups;
    }

    public void setUsersOrGroups(UsersOrGroups usersOrGroups) {
        if (builtin == Builtin.BUILTIN) throw new UnsupportedOperationException("Can't set properties on built-in attribute headers");
        this.usersOrGroups = usersOrGroups;
    }

    public boolean isBuiltin() {
        return builtin == Builtin.BUILTIN;
    }

    @SuppressWarnings({"RedundantIfStatement"})
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        AttributeHeader that = (AttributeHeader) o;

        if (builtin != that.builtin) return false;
        if (description != null ? !description.equals(that.description) : that.description != null) return false;
        if (type != null ? !type.equals(that.type) : that.type != null) return false;
        if (usersOrGroups != that.usersOrGroups) return false;
        if (variableName != null ? !variableName.equals(that.variableName) : that.variableName != null) return false;

        return true;
    }

    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (variableName != null ? variableName.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (usersOrGroups != null ? usersOrGroups.hashCode() : 0);
        result = 31 * result + (builtin != null ? builtin.hashCode() : 0);
        return result;
    }

    protected Object readResolve() throws ObjectStreamException {
        Object builtin = nameCache.get(_name);
        if (builtin != null) return builtin;
        return this;
    }

    public static AttributeHeader forName(String name) {
        return nameCache.get(name);
    }

    @Override
    public String toString() {
        return _name == null ? variableName : _name;
    }
}
