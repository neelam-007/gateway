package com.l7tech.identity.internal;

import com.l7tech.identity.internal.Country;
import com.l7tech.objectmodel.NamedEntity;

public interface State extends NamedEntity {
    Country getCountry();
    String getCode();

    void setCountry( Country country );
    void setCode( String code );
}
