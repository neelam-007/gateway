package com.l7tech.ssg.objectmodel;


public interface IdentityProviderType extends NamedEntity {
    String getDescription();
    String getClassName();

    void setDescription( String description );
    void setClassName( String className );
}
