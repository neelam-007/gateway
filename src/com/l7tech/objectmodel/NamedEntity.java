package com.l7tech.objectmodel;

public interface NamedEntity extends Entity {
    String getName();
    void setName( String name );
    int getVersion();
    void setVersion( int version );
    long getLoadTime();
    void setLoadTime( long loadTime );
}
