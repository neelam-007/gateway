package com.l7tech.objectmodel;

public interface Address extends StandardEntity {
    String getAddress();
    String getAddress2();
    String getCity();
    State getState();
    Country getCountry();
    String getPostalCode();

    void setAddress( String address );
    void setAddress2( String address2 );
    void setCity( String city );
    void setState( State state );
    void setCountry( Country country );
    void setPostalCode( String postalCode );
}
