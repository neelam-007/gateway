/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.identity.mapping;

import com.l7tech.objectmodel.imp.NamedEntityImp;

/**
 * Abstract superclass of {@link IdentityMapping} and {@link SecurityTokenMapping}. 
 */
public abstract class AttributeMapping extends NamedEntityImp {
    protected AttributeConfig attributeConfig;
    private boolean multivalued = false;

    protected AttributeMapping() {
    }

    public AttributeMapping(AttributeConfig parent) {
        this.attributeConfig = parent;
    }

    public AttributeConfig getAttributeConfig() {
        return attributeConfig;
    }

    public void setAttributeConfig(AttributeConfig attributeConfig) {
        this.attributeConfig = attributeConfig;
    }

    public boolean isMultivalued() {
        return multivalued;
    }

    public void setMultivalued(boolean multivalued) {
        this.multivalued = multivalued;
    }
}
