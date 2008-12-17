package com.l7tech.console.tree.servicesAndPolicies;

import com.l7tech.console.security.LogonListener;
import com.l7tech.console.event.WeakEventListenerList;
import com.l7tech.gateway.common.audit.LogonEvent;

import javax.swing.*;
import javax.swing.event.EventListenerList;

/**
 * Essentially a class that will encapsulate sorting selections made on the Service and Policy tree.
 *
 * User: dlee
 * Date: Nov 25, 2008
 */
public class SortComponents implements LogonListener {
    private JRadioButtonMenuItem nameAsc;
    private JRadioButtonMenuItem nameDesc;
    private JRadioButtonMenuItem typeAsc;
    private JRadioButtonMenuItem typeDesc;

    private JRadioButtonMenuItem all;
    private JRadioButtonMenuItem services;
    private JRadioButtonMenuItem policies;

    private EventListenerList listenerList = new WeakEventListenerList();

    public SortComponents(JLabel filterLabel) {
        initFilterMenu(filterLabel);
        initSortMenu();
    }

    private void initFilterMenu(JLabel filterLabel) {
        //init actions
        AlterFilterAction filterAllAction = new AlterFilterAction(AlterFilterAction.FilterType.ALL, filterLabel);
        AlterFilterAction filterServiceAction = new AlterFilterAction(AlterFilterAction.FilterType.SERVICES, filterLabel);
        AlterFilterAction filterPolicyAction = new AlterFilterAction(AlterFilterAction.FilterType.POLICY_FRAGMENT, filterLabel);

        //set default action state
        filterAllAction.setEnabled(true);
        filterServiceAction.setEnabled(true);
        filterPolicyAction.setEnabled(true);

        //add to logon listener for the list of actions
        listenerList.add(LogonListener.class, filterAllAction);
        listenerList.add(LogonListener.class, filterServiceAction);
        listenerList.add(LogonListener.class, filterPolicyAction);

        all = new JRadioButtonMenuItem(filterAllAction);
        services = new JRadioButtonMenuItem(filterServiceAction);
        policies = new JRadioButtonMenuItem(filterPolicyAction);

        //should only allow one selection on filtering
        ButtonGroup btnGroup = new ButtonGroup();
        btnGroup.add(all);
        btnGroup.add(services);
        btnGroup.add(policies);

        //set default
        all.setSelected(true);
    }

    private void initSortMenu() {
        //init actions
        AlterDefaultSortAction sortNameAscAction = new SortAscendingAction(AlterDefaultSortAction.SortType.NAME);
        AlterDefaultSortAction sortNameDescAction = new SortDescendingAction(AlterDefaultSortAction.SortType.NAME);
        AlterDefaultSortAction sortTypeAscAction = new SortAscendingAction(AlterDefaultSortAction.SortType.TYPE);
        AlterDefaultSortAction sortTypeDescAction = new SortDescendingAction(AlterDefaultSortAction.SortType.TYPE);

        sortNameAscAction.setEnabled(true);
        sortNameDescAction.setEnabled(true);
        sortTypeAscAction.setEnabled(true);
        sortTypeDescAction.setEnabled(true);

        //add actions to logon listeners
        listenerList.add(LogonListener.class, sortNameAscAction);
        listenerList.add(LogonListener.class, sortNameDescAction);
        listenerList.add(LogonListener.class, sortTypeAscAction);
        listenerList.add(LogonListener.class, sortTypeDescAction);

        //create sub menu for sort by name
        JMenu byName = new JMenu("Name");
        nameAsc = new JRadioButtonMenuItem(sortNameAscAction);
        nameDesc = new JRadioButtonMenuItem(sortNameDescAction);
        ButtonGroup byNameGroup = new ButtonGroup();
        byNameGroup.add(nameAsc);
        byNameGroup.add(nameDesc);
        byName.add(nameAsc);
        byName.add(nameDesc);
        nameAsc.setSelected(true);

        //create sub menu for sort by type
        JMenu byType = new JMenu("Type");
        typeAsc = new JRadioButtonMenuItem(sortTypeAscAction);
        typeDesc = new JRadioButtonMenuItem(sortTypeDescAction);
        ButtonGroup byTypeGroup = new ButtonGroup();
        byTypeGroup.add(typeAsc);
        byTypeGroup.add(typeDesc);
        byType.add(typeAsc);
        byType.add(typeDesc);
        typeDesc.setSelected(true);
    }

    /**
     * Appends all the current filter selection state to the provided menu so that the menu will display the correct
     * selections.
     *
     * @param filterMenu    Filter menu to be appended with the filter state
     * @return  JMenu containing possible selections for filtering
     */
    public JMenu addFilterMenu(JMenu filterMenu) {
        filterMenu.add(all);
        filterMenu.add(services);
        filterMenu.add(policies);
        return filterMenu;
    }

    /**
     * Appends all the current sorting selection state to the provided menu so that the menu will display the correct
     * selections.
     *
     * @param sortMenu  Sort menu to be appended with the sorting state
     * @return  JMenu containing possible selections for sorting
     */
    public JMenu addSortMenu(JMenu sortMenu) {
        JMenu byName = new JMenu("Name");
        byName.add(nameAsc);
        byName.add(nameDesc);
        JMenu byType = new JMenu("Type");
        byType.add(typeAsc);
        byType.add(typeDesc);
        sortMenu.add(byName);
        sortMenu.add(byType);
        return sortMenu;
    }

    public void onLogoff(LogonEvent e) {
        LogonListener[] listeners = listenerList.getListeners(LogonListener.class);
        for (LogonListener listener : listeners) {
            listener.onLogoff(e);
        }
    }

    public void onLogon(LogonEvent e) {
        resetToDefaultSelection();
        LogonListener[] listeners = listenerList.getListeners(LogonListener.class);
        for (LogonListener listener : listeners) {
            listener.onLogon(e);
        }
    }

    /**
     * For filtering, we should always reset to display ALL upon connection to gateway.
     */
    private void resetToDefaultSelection() {
        nameAsc.setSelected(true);
        typeDesc.setSelected(true);
        ((AlterDefaultSortAction) nameAsc.getAction()).performAction();
        ((AlterDefaultSortAction) typeDesc.getAction()).performAction();

        all.setSelected(true);
        ((AlterFilterAction) all.getAction()).performAction();
    }

    /**
     * Updates the filter selection accordingly and execute the filter action.
     * Default filter means no filter is applied to the tree.
     */
    public void selectDefaultFilter() {
        all.setSelected(true);
        ((AlterFilterAction) all.getAction()).performAction();
    }
}
