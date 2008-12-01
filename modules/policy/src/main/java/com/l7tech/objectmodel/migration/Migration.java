package com.l7tech.objectmodel.migration;

import com.l7tech.objectmodel.migration.PropertyResolver;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
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
     * Specifies the DependencyResolver implementation that can be used to extract dependencies
     * for the annotated property.
     *
     * @return DependencyResolver implementation for the annotated property.
     */
    Class<? extends PropertyResolver> resolver() default DefaultEntityPropertyResolver.class;

    boolean uploadedByParent() default false;
}
