package com.l7tech.server.util;

import com.l7tech.objectmodel.EntityType;
import com.l7tech.policy.assertion.UsesEntities;
import com.l7tech.util.Functions.Unary;
import static com.l7tech.util.Functions.equality;
import static com.l7tech.util.Functions.grepFirst;
import com.l7tech.util.Option;
import static com.l7tech.util.Option.optional;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.util.Arrays.asList;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Utility methods and annotations for UsesEntities
 */
public class EntityUseUtils {

    //- PUBLIC

    /**
     * Get a descriptive name for the given type of reference.
     *
     * @param usesEntities The source for the type
     * @param entityType The type.
     * @return The type name, which may be customized for a specific usage.
     */
    public static String getTypeName( final UsesEntities usesEntities,
                                      final EntityType entityType ) {
        try {
            final Option<EntityUse> entityUseOption =
                    optional( usesEntities.getClass().getMethod( "getEntitiesUsed" ).getAnnotation( EntityUse.class ) );

            return entityUseOption.map( nameFor( entityType ) ).orSome( entityType.getName() );
        } catch ( final NoSuchMethodException e ) {
            // fallback to entity type name
        } catch ( final SecurityException e ) {
            // fallback to entity type name
        }

        return entityType.getName();
    }

    @Documented
    @Retention(value = RUNTIME)
    @Target({})
    public static @interface EntityTypeOverride {
        EntityType type();
        String description();
    }

    @Documented
    @Retention(value = RUNTIME)
    @Target({METHOD})
    public static @interface EntityUse {
        EntityTypeOverride[] value();
    }

    //- PRIVATE

    private static Unary<String,EntityUse> nameFor( final EntityType entityType ) {
        return new Unary<String,EntityUse>(){
            @Override
            public String call( final EntityUse entityUse ) {
                return optional( grepFirst( asList(entityUse.value()), equality( type(), entityType ) ) ).map( description() ).toNull();
            }
        };
    }

    private static Unary<EntityType,EntityTypeOverride> type() {
        return new Unary<EntityType,EntityTypeOverride>(){
            @Override
            public EntityType call( final EntityTypeOverride entityTypeOverride ) {
                return entityTypeOverride.type();
            }
        };
    }

    private static Unary<String,EntityTypeOverride> description() {
        return new Unary<String,EntityTypeOverride>(){
            @Override
            public String call( final EntityTypeOverride entityTypeOverride ) {
                return entityTypeOverride.description();
            }
        };
    }
}
