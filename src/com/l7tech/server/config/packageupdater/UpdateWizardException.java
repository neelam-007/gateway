package com.l7tech.server.config.packageupdater;

/**
 * User: megery
 * Date: Mar 23, 2007
 * Time: 4:49:43 PM
 */
public class UpdateWizardException extends Exception {

    public UpdateWizardException() {
        super();
    }

    public UpdateWizardException(String message) {
        super(message);
    }

    public UpdateWizardException(String message, Throwable cause) {
        super(message, cause);
    }

    public UpdateWizardException(Throwable cause) {
        super(cause);
    }
}
