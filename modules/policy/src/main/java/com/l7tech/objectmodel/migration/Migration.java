package com.l7tech.objectmodel.migration;

import com.l7tech.objectmodel.EntityType;

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
     * Specifies the DependencyResolver implementation that can be used to extract dependencies
     * for the annotated property.
     *
     * @return DependencyResolver implementation for the annotated property.
     */
    Class<? extends PropertyResolver> resolver() default DefaultEntityPropertyResolver.class;

    /**
     * Specifies the entity type of the dependency that the annotated property points to.
     */
    EntityType targetType() default EntityType.ANY;

    /**
     * Specifies if the entity representing the target of the annotated property is uploaded
     * on the target cluster by the manager of the entity that depends on it.
     *
     * @return true if the dependency is uploaded by its parent, false otherwise.
     */
    boolean uploadedByParent() default false;
}
