package com.l7tech.server.util;

import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.hibernate.type.StringType;
import org.hibernate.type.Type;

import javax.persistence.Column;
import javax.persistence.JoinTable;
import javax.persistence.MapKeyColumn;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;

/**
 * Restriction criterion for working with Map properties.
 */
public final class MapRestriction {

    //- PUBLIC

    /**
     * Create a restriction instance for the given type/property.
     *
     * <p>The property must be annotated for persistence as a Map.</p>
     *
     * @param entityClass The entity class
     * @param getterName The name of the (annotated) getter (e.g. "getX")
     */
    public MapRestriction( final Class<?> entityClass, final String getterName ) {
        try {
            final Method getter = entityClass.getDeclaredMethod( getterName );
            final JoinTable joinTable = requireAnnotation(getter, JoinTable.class );
            final MapKeyColumn mapKeyColumn = requireAnnotation(getter, MapKeyColumn.class );
            final Column column = requireAnnotation(getter, Column.class );
            if ( joinTable.joinColumns().length != 1 ) {
                throw new IllegalStateException( "Unexpected join for Map property: " + getterName );
            }

            mapTable = joinTable.name();
            mapTableEntityKey = joinTable.joinColumns()[0].name();
            entityKey = joinTable.joinColumns()[0].referencedColumnName();
            mapKey = mapKeyColumn.name();
            mapValue = column.name();
        } catch ( NoSuchMethodException e ) {
            throw new IllegalStateException( "Method not found: " + getterName );
        }
    }

    /**
     * Create a criterion that restricts this property to contain the specified mapping.
     *
     * @param key The required key
     * @param value The required value
     * @return The entry criterion
     */
    public Criterion containsEntry( final String key, final Object value ) {
        final StringBuilder sqlBuilder = new StringBuilder( 256 )
            .append( "? IN (SELECT " )
            .append( mapTable ).append( "." ).append( mapValue )
            .append( " FROM " )
            .append( mapTable )
            .append( " WHERE {alias}." ).append( entityKey ).append( "=" ).append( mapTable ).append(".").append( mapTableEntityKey )
            .append( " AND " ).append( mapTable ).append( "." ).append( mapKey ).append( "=?)" );

        return Restrictions.sqlRestriction(
                sqlBuilder.toString(),
                new Object[]{ value, key },
                new Type[]{ StringType.INSTANCE, StringType.INSTANCE } );
    }

    //- PRIVATE

    private final String mapTable;
    private final String mapTableEntityKey;
    private final String entityKey;
    private final String mapKey;
    private final String mapValue;

    private <T extends Annotation> T requireAnnotation( final AnnotatedElement element, final Class<T> annotationClass ) {
        final T value = element.getAnnotation( annotationClass );
        if ( value == null ) {
            throw new IllegalStateException( "Missing require annotation for Map property: " + annotationClass.getName() );
        }
        return value;
    }
}
