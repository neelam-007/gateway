package com.l7tech.gateway.api.impl;

import com.l7tech.gateway.api.AccessibleObject;
import com.l7tech.util.Functions;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import java.util.List;

/**
 * Represents a reference to a managed object.
 */
@XmlType(name="ReferenceType")
public class ManagedObjectReference extends ExtensionSupport {

    //- PUBLIC

    public ManagedObjectReference(){
    }

    public ManagedObjectReference( final Class<? extends AccessibleObject> typeClass,
                                   final String id ) {
        setResourceType( typeClass );
        setId( id );
    }

    @XmlAttribute(name="id", required=true)
    public String getId() {
        return id;
    }

    public void setId( final String id ) {
        this.id = id;
    }

    @XmlAttribute(name="resourceUri")
    public String getResourceUri() {
        return resourceUri;
    }

    public void setResourceUri( final String resourceUri ) {
        this.resourceUri = resourceUri;
    }

    public void setResourceType( final Class<? extends AccessibleObject> typeClass ) {
         if ( typeClass != null ) {
             setResourceUri( AccessorSupport.getResourceUri(typeClass) );
         } else {
             setResourceUri( null );
         }
    }

    /**
     * Create a list of references of the given type.
     *
     * @param typeClass The type to use (for the resource URI, optional)
     * @param identifiers The identifier values (will be converted to String values, required)
     * @return The list of references
     */
    public static List<ManagedObjectReference> asReferences( final Class<? extends AccessibleObject> typeClass,
                                                             final Iterable<?> identifiers ) {
        return Functions.map( identifiers, new Functions.Unary<ManagedObjectReference,Object>(){
            @Override
            public ManagedObjectReference call( final Object identifier ) {
                return new ManagedObjectReference( typeClass, identifier.toString() );
            }
        } );
    }

    /**
     * Create a list of references of the given type.
     *
     * @param identifiers The identifier values (will be converted to String values)
     * @return The list of references
     */
    public static List<ManagedObjectReference> asReferences( final Iterable<?> identifiers ) {
        return asReferences( null, identifiers );
    }

    //- PRIVATE

    private String resourceUri;
    private String id;
}
