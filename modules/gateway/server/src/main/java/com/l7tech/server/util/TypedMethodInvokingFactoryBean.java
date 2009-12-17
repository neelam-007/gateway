package com.l7tech.server.util;

import org.springframework.beans.factory.config.MethodInvokingFactoryBean;

/**
 * MethodInvokingFactoryBean with a configured type.
 *
 * <p>This should be used when invoking <code>void</code> methods, as
 * specifying a type prevents spring performing repeated instantiation of some
 * beans.</p>
 */
public class TypedMethodInvokingFactoryBean extends MethodInvokingFactoryBean {

    //- PUBLIC

    @Override
    public Class getObjectType() {
        return objectType;
    }

    public void setObjectType( final Class objectType ) {
        this.objectType = objectType;
    }

    //- PRIVATE

    private Class objectType = Object.class;
}
