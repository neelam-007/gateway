package com.l7tech.server.config;

/**
 * User: megery
 * Date: Dec 30, 2006
 * Time: 9:41:00 AM
 */
public interface PartitionActionListener {
    boolean getPartitionActionsConfirmation(String message);
    void showPartitionActionErrorMessage(String message);
    void showPartitionActionMessage(String message);
}
