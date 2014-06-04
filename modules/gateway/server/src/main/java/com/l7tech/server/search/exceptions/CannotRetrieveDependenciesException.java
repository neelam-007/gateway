package com.l7tech.server.search.exceptions;

import org.jetbrains.annotations.NotNull;

/**
 * This is thrown by the dependency analyzer if there is a problem attempting to retrieve a dependency
 */
public class CannotRetrieveDependenciesException extends Exception {
    private static final long serialVersionUID = -2631230044964517889L;

    private static final String exceptionMessageSimple = "Cannot retrieve dependency of type %1$s. %2$s";
    private static final String exceptionMessage = "Cannot retrieve dependency %1$s of type %2$s. %3$s";
    private static final String exceptionMessageNoName = "Cannot retrieve dependency of type %1$s on object %2$s. %3$s";
    private static final String exceptionMessageFull = "Cannot retrieve dependency %1$s of type %2$s on object %3$s. %4$s";

    public CannotRetrieveDependenciesException(@NotNull final Class dependencyType, @NotNull final String reason){
        super(String.format(exceptionMessageSimple, dependencyType.getSimpleName(), reason));
    }

    public CannotRetrieveDependenciesException(@NotNull final Class dependencyType, @NotNull final String reason, @NotNull final Throwable throwable){
        super(String.format(exceptionMessageSimple, dependencyType.getSimpleName(), reason), throwable);
    }

    public CannotRetrieveDependenciesException(@NotNull final Class dependencyType, @NotNull final Class dependentObject, @NotNull final String reason, @NotNull final Throwable throwable){
        super(String.format(exceptionMessageNoName, dependencyType.getSimpleName(), dependentObject.getSimpleName(), reason), throwable);
    }

    public CannotRetrieveDependenciesException(@NotNull final String dependencyName, @NotNull final Class dependencyType, @NotNull final String reason){
        super(String.format(exceptionMessage, dependencyName, dependencyType.getSimpleName(), reason));
    }

    public CannotRetrieveDependenciesException(@NotNull final String dependencyName, @NotNull final Class dependencyType, @NotNull final Class dependentObject, @NotNull final String reason){
        super(String.format(exceptionMessageFull, dependencyName, dependencyType.getSimpleName(), dependentObject.getSimpleName(), reason));
    }

    public CannotRetrieveDependenciesException(@NotNull final String dependencyName, @NotNull final Class dependencyType, @NotNull final Class dependentObject, @NotNull final String reason, @NotNull final Throwable throwable){
        super(String.format(exceptionMessageFull, dependencyName, dependencyType.getSimpleName(), dependentObject.getSimpleName(), reason), throwable);
    }
}
