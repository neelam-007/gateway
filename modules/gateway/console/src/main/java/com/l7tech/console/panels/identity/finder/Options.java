package com.l7tech.console.panels.identity.finder;

import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.objectmodel.Goid;

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

    public Options() {
    }

    public Options( final Options original ) {
        this.searchType = original.searchType;
        this.enableDeleteAction = original.enableDeleteAction;
        this.enableOpenProperties = original.enableOpenProperties;
        this.adminOnly = original.adminOnly;
        this.disposeOnSelect = original.disposeOnSelect;
        this.selectionMode = original.selectionMode;
        this.initialProviderOid = original.initialProviderOid;
    }

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

    public Goid getInitialProviderOid() {
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
    public void setInitialProvider(Goid id) {
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

    public boolean isAdminOnly() {
        return adminOnly;
    }

    /**
     * True to offer only identity providers with  {@link com.l7tech.identity.IdentityProviderConfig#isAdminEnabled()}
     * == true, false to show all identity providers
     */
    public void setAdminOnly(boolean adminOnly) {
        this.adminOnly = adminOnly;
    }


    // TODO Update copy constructor if adding fields
    // TODO Update copy constructor if adding fields
    // TODO Update copy constructor if adding fields
    private SearchType searchType = SearchType.ALL;
    private boolean enableDeleteAction = false;
    private boolean enableOpenProperties = true;
    private boolean adminOnly = false;
    private boolean disposeOnSelect = false;
    private int selectionMode = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION;
    private Goid initialProviderOid = IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_GOID;
    // TODO Update copy constructor if adding fields
    // TODO Update copy constructor if adding fields
    // TODO Update copy constructor if adding fields
}
