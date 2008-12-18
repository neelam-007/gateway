package com.l7tech.objectmodel.folder;

/**
 * @author jbufu
 */
public class InvalidParentFolderException extends Exception {
    public InvalidParentFolderException() {
        super();
    }

    public InvalidParentFolderException(String message) {
        super(message);
    }

    public InvalidParentFolderException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidParentFolderException(Throwable cause) {
        super(cause);
    }
}
