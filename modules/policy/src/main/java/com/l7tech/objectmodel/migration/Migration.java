package com.l7tech.objectmodel.migration;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Migration {

    /**
     * Specifies if the annotated property is a dependency or not.
     *
     * @return true if the property is a dependency, false otherwise.
     */
    boolean dependency() default true;

    /**
     * Specifies the mappable quality of the annotated property's NAME.
     *
     * @see MigrationMappingSelection for the available mapping selection modes.
     */
    MigrationMappingSelection mapName() default MigrationMappingSelection.OPTIONAL;

    /**
     * Specifies the mappable quality of the annotated property's VALUE.
     *
     * @see MigrationMappingSelection for the available mapping selection modes.
     */
    MigrationMappingSelection mapValue() default MigrationMappingSelection.OPTIONAL;

    /**
     * Specifies the property resolver type that must be used to extract and apply
     * dependencies to the annotated property.
     */
    PropertyResolver.Type resolver() default PropertyResolver.Type.DEFAULT;

    /**
     * Specifies if the value of a dependency should be serialized and added to the exported bundle.
     */
    boolean export() default true;
}
