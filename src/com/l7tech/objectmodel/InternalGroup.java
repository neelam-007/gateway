package com.l7tech.ssg.objectmodel;

import java.util.Collection;


public interface InternalGroup extends NamedEntity {
    String getDescription();
    
    Collection getMembers();

    void setDescription( String description );
}
