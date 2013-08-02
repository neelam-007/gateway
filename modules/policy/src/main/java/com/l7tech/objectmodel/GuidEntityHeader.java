/*
 * Copyright (C) 2003-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.objectmodel;


import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlAttribute;

/**
 * 
 */
@XmlRootElement
public class GuidEntityHeader extends EntityHeader {

    //- PUBLIC

    public GuidEntityHeader() {
    }

    public GuidEntityHeader(String id, EntityType type, String name, String description) {
        super(id, type, name, description);
    }

    public GuidEntityHeader(String id, EntityType type, String name, String description, Integer version) {
        super(id, type, name, description, version);
    }

    public GuidEntityHeader(Goid goid, EntityType type, String name, String description) {
        super(goid, type, name, description);
    }

    public GuidEntityHeader(Goid goid, EntityType type, String name, String description, Integer version) {
        super(goid, type, name, description, version);
    }

    @XmlAttribute
    public String getGuid() {
        return guid;
    }

    public void setGuid( final String guid ) {
         this.guid = guid;
    }

    //- PROTECTED

    protected String guid;

}