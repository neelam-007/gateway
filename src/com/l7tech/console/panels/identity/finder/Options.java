package com.l7tech.console.panels.identity.finder;

import com.l7tech.identity.IdentityProviderConfigManager;

import javax.swing.*;

/**
 * The <code>FindIdentitiesDialog</code> Options
 * Default options are
 * <ul>
 * <li>delete disabled
 * <li>allow multiple elements selection
 * <li>initial provider id
 * </ul>
 */
public class Options {

    /**
     * Enable the delete action
     * @param enableDeleteAction
     */
    public void setEnableDeleteAction(boolean enableDeleteAction) {
        this.enableDeleteAction = enableDeleteAction;
    }

    /**
     * Disable the properties opening for the selected item
     * Enabled by default.
     * @param disableOpenProperties
     */
    public void setDisableOpenProperties(boolean disableOpenProperties) {
        enableOpenProperties = disableOpenProperties;
    }

    /**
     * Dispose on select Disabled by default.
     * @param dispose
     */
    public void setDisposeOnSelect(boolean dispose) {
        disposeOnSelect = dispose;
    }

    public boolean isEnableDeleteAction() {
        return enableDeleteAction;
    }

    public boolean isEnableOpenProperties() {
        return enableOpenProperties;
    }

    public int getSelectionMode() {
        return selectionMode;
    }

    public long getInitialProviderOid() {
        return initialProviderOid;
    }

    /**
     * Set the selection mode. Use one of the values from
     * <code>ListSelectionModel</code>
     *
     * @param selectionMode
     * @see javax.swing.ListSelectionModel
     */
    public void setSelectionMode(int selectionMode) {
        this.selectionMode = selectionMode;
    }

    /**
     * Set the intial provider id whed the dialog is displayed
     *
     * @param id the provider id
     */
    public void setInitialProvider(long id) {
        this.initialProviderOid = id;
    }

    public SearchType getSearchType() {
        return searchType;
    }

    public void setSearchType(SearchType searchType) {
        this.searchType = searchType;
    }

    public boolean isDisposeOnSelect() {
        return disposeOnSelect;
    }

    public boolean isInternalOnly() {
        return internalOnly;
    }

    public void setInternalOnly(boolean internalOnly) {
        this.internalOnly = internalOnly;
    }

    SearchType searchType = SearchType.ALL;
    boolean enableDeleteAction = false;
    boolean enableOpenProperties = true;
    boolean internalOnly = false;
    private boolean disposeOnSelect = false;
    int selectionMode = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION;
    long initialProviderOid = IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_OID;
}
