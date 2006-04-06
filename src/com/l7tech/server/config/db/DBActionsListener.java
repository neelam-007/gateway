package com.l7tech.server.config.db;

/**
 * User: megery
 * Date: Apr 4, 2006
 * Time: 4:22:39 PM
 */
public interface DBActionsListener {
    void showErrorMessage(String errorMsg);
    void hideErrorMessage();

    boolean getOverwriteConfirmationFromUser(String dbName);

    void confirmCreateSuccess();

    char[] getPrivilegedPassword();

    boolean getGenericUserConfirmation(String msg);
}
