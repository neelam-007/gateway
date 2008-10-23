package com.l7tech.gateway.config.manager.db;

import java.util.Map;
import java.net.PasswordAuthentication;

/**
 * User: megery
 * Date: Apr 4, 2006
 * Time: 4:22:39 PM
 */
public interface DBActionsListener {
    void showErrorMessage(String errorMsg);

    void hideErrorMessage();

    boolean getOverwriteConfirmationFromUser(String dbName);

    void showSuccess(String message);

    boolean getGenericUserConfirmation(String msg);

    PasswordAuthentication getPrivelegedCredentials(String message, String usernamePrompt, String passwordPrompt, String defaultUsername);
}
