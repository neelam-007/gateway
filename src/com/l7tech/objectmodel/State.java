package com.l7tech.objectmodel;

public interface State extends NamedEntity {
    Country getCountry();
    String getCode();

    void setCountry( Country country );
    void setCode( String code );
}
