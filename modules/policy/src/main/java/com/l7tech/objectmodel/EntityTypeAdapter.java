/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.objectmodel;

import javax.xml.bind.annotation.adapters.XmlAdapter;

/** @author alex */
public class EntityTypeAdapter extends XmlAdapter<String, EntityType> {
    @Override
    public EntityType unmarshal(String v) throws Exception {
        return EntityType.valueOf(v);
    }

    @Override
    public String marshal(EntityType v) throws Exception {
        return v.toString();
    }
}
