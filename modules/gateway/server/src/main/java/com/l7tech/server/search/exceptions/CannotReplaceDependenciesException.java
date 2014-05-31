package com.l7tech.server.search.exceptions;

/**
 * This is throw by the dependency analyzer id there is a problem attempting to replace dependencies
 */
public class CannotReplaceDependenciesException extends Exception {
    private static final long serialVersionUID = 6156878844989874457L;

    private static final String exceptionMessage = "Cannot replace dependency '%1$s' of type '%2$s' on object '%3$s'. Replacement dependency id: %4$s. %5$s";

    public CannotReplaceDependenciesException(String dependencyName, String dependencyID, Class dependencyType, Class dependentObject, String reason){
        super(String.format(exceptionMessage, dependencyName, dependencyType.getSimpleName(), dependentObject.getSimpleName(), dependencyID, reason));
    }

    public CannotReplaceDependenciesException(String dependencyName, String dependencyID, Class dependencyType, Class dependentObject, String reason, Throwable throwable){
        super(String.format(exceptionMessage, dependencyName, dependencyType.getSimpleName(), dependentObject.getSimpleName(), dependencyID, reason), throwable);
    }
}
