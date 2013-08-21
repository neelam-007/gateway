package com.l7tech.objectmodel;

/**
 * Thrown if a folder cannot be deleted because it is not empty.
 */
public class NonEmptyFolderDeletionException extends DeleteException {
    public NonEmptyFolderDeletionException(final String message) {
        super(message);
    }
}
