package com.l7tech.identity.internal;

import com.l7tech.objectmodel.NamedEntity;

public interface Country extends NamedEntity {
    String getCode();
    void setCode( String code );
}
