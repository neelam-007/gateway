package com.l7tech.identity.mapping;

import com.l7tech.objectmodel.imp.NamedEntityImp;

public abstract class AttributeMapping extends NamedEntityImp {
    private AttributeConfig attributeConfig;
    private boolean multivalued = false;

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
