package com.l7tech.gateway.api.impl;

import com.l7tech.gateway.api.AccessibleObject;
import com.l7tech.gateway.api.Accessor;
import com.l7tech.gateway.api.ManagementRuntimeException;

import javax.xml.bind.annotation.XmlSchema;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Accessor helper class with resource annotations and methods.
 *
 * <p>This class is used by both client and server.</p>
 */
public class AccessorSupport {

    public static String getResourceName( final Class<? extends AccessibleObject> managedObjectClass ) {
        final AccessibleResource resource = managedObjectClass.getAnnotation( AccessibleResource.class );
        if ( resource == null ) {
            throw new ManagementRuntimeException("Missing annotation for resource '"+managedObjectClass.getName()+"'.");
        }
        return resource.name();
    }

    public static String getResourceUri( final Class<? extends AccessibleObject> managedObjectClass ) {
        final XmlSchema schema = Accessor.class.getPackage().getAnnotation( XmlSchema.class );
        if ( schema == null ) {
            throw new ManagementRuntimeException("Missing annotation for API package.");
        }
        return schema.namespace() + "/" + getResourceName(managedObjectClass);
    }

    /**
     * Annotation for use on accessible objects.
     */
    @Retention(value = RUNTIME)
    @Target(TYPE)
    public static @interface AccessibleResource {

        /**
         * The name of the resource.
         *
         * @return The resource name.
         */
        public abstract String name();

        /**
         * The classname for the accessor for the resource.
         *
         * @return The accessor classname.
         */
        // avoids using class reference since in some deployments the client
        // classes may not be available
        public abstract String accessorClassname() default "com.l7tech.gateway.api.Accessor";
    }
}
