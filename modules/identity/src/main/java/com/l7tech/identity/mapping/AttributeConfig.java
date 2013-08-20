package com.l7tech.identity.mapping;

import com.l7tech.objectmodel.AttributeHeader;
import com.l7tech.objectmodel.imp.NamedGoidEntityImp;
import com.l7tech.policy.variable.DataType;

/**
 * A description of a generic attribute that users and/or credentials might have.
 * (e.g. email address, membership status, account balance...)
 */
public class AttributeConfig extends NamedGoidEntityImp {
    private AttributeHeader header;
    private String description;

    /**
     * Overrides the default variable name in {@link #header}.
     */
    private String variableName;

    public AttributeConfig() {
        this.header = new AttributeHeader();
    }

    public AttributeConfig(AttributeHeader header) {
        if (header == null) throw new NullPointerException();
        this.header = header;
    }

    /**
     * A short, unique and unambiguous name for this attribute, for use in templates.  Required.
     * @return the variable name.
     */
    public synchronized String getVariableName() {
        if (variableName != null) return variableName;
        return header.getVariableName();
    }

    /**
     * A short, unique and unambiguous name for this attribute, for use in
     * {@link com.l7tech.server.policy.variable.ExpandVariables} expressions.  Required.
     * @param variableName the variable name
     */
    public synchronized void setVariableName(String variableName) {
        if (variableName != null && variableName.equals(header.getVariableName())) return;
        this.variableName = variableName;
    }

    public AttributeHeader getHeader() {
        return header;
    }

    public void setHeader(AttributeHeader header) {
        if (header == null) throw new NullPointerException();
        this.header = header;
    }

    /**
     * @return optional, human-readable text describing this attribute and its uses.
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description optional, human-readable text describing this attribute and its uses.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    public DataType getType() {
        return header.getType();
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        AttributeConfig that = (AttributeConfig) o;

        if (description != null ? !description.equals(that.description) : that.description != null) return false;
        if (header != null ? !header.equals(that.header) : that.header != null) return false;
        if (variableName != null ? !variableName.equals(that.variableName) : that.variableName != null) return false;

        return true;
}

    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (header != null ? header.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (variableName != null ? variableName.hashCode() : 0);
        return result;
    }
}
