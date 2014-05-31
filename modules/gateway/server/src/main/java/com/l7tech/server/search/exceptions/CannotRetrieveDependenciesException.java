package com.l7tech.server.search.exceptions;

/**
 * This is thrown by the dependency analyzer if there is a problem attempting to retrieve a dependency
 */
public class CannotRetrieveDependenciesException extends Exception {
    private static final long serialVersionUID = -2631230044964517889L;

    private static final String exceptionMessage = "Cannot retrieve dependency %1$s of type %2$s on object %3$s. %4$s";

    public CannotRetrieveDependenciesException(String dependencyName, Class dependencyType, Class dependentObject, String reason){
        super(String.format(exceptionMessage, dependencyName, dependencyType.getSimpleName(), dependentObject.getSimpleName(), reason));
    }

    public CannotRetrieveDependenciesException(String dependencyName, Class dependencyType, Class dependentObject, String reason, Throwable throwable){
        super(String.format(exceptionMessage, dependencyName, dependencyType.getSimpleName(), dependentObject.getSimpleName(), reason), throwable);
    }
}
