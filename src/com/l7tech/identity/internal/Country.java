package com.l7tech.identity.internal;

import com.l7tech.objectmodel.NamedEntity;

import java.util.Set;

public interface Country extends NamedEntity {
    String getCode();
    void setCode( String code );
    Set getStates();
    void setStates( Set states );
}
