package com.l7tech.console.panels;

import com.l7tech.console.util.ComponentRegistry;
import com.l7tech.console.util.Registry;
import com.l7tech.console.event.ConnectionEvent;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.ResourceBundle;

/**
 * Created by IntelliJ IDEA.
 * User: fpang
 * Date: Sep 26, 2003
 * Time: 12:18:13 PM
 * To change this template use Options | File Templates.
 */
public class MonitorPanel extends JTabbedPane {

    private final ComponentRegistry componentRegistry = Registry.getDefault().getComponentRegistry();
    private Action toggleShowLogAction = null;
    private Action toggleShowStatAction = null;
    private LogPanel logPane = null;
    private StatisticsPanel statisticsPane = null;
    private static
    ResourceBundle resapplication =
      java.util.ResourceBundle.getBundle("com.l7tech.console.resources.console");

    public MonitorPanel() {
        setTabPlacement(JTabbedPane.TOP);
        updateDisplay();
    }

    public void updateDisplay() {
        boolean visibleFlag = false;

        // if one of items is visible, make this tabbed pane visible
        for (int i = 0; i < getTabCount(); i++) {
            if (getComponentAt(i).isVisible()) {
                visibleFlag = true;
            }
        }

        setVisible(visibleFlag);
        validate();
        repaint();
    }

    public void removeComponent(JComponent component) {

        // select the first component in the tabbed pane
        if (getTabCount() > 1) {
            getComponentAt(0).setVisible(true);
            setSelectedIndex(0);
        }

        this.remove(component);

    }

    public void connectHandler(ConnectionEvent e) {

        if (componentRegistry.getMainWindow().getLogMenuItem().isSelected()) {
            restoreLogPane();
        }
        if (componentRegistry.getMainWindow().getStatMenuItem().isSelected()) {
            restoreStatPane();
        }

    }

    public void disconnectHandler(ConnectionEvent e) {

        getLogPane().stopRefreshTimer();
        getLogPane().clearMsgTable();

        getStatisticsPane().stopRefreshTimer();
        getStatisticsPane().clearStatiistics();
    }


    public Action getShowLogToggleAction() {
        if (toggleShowLogAction != null) return toggleShowLogAction;

        String atext = resapplication.getString("toggle.log.display");

        toggleShowLogAction =
                new AbstractAction(atext) {
                    public void actionPerformed(ActionEvent event) {
                        JCheckBoxMenuItem item = (JCheckBoxMenuItem) event.getSource();

                        getLogPane().setVisible(item.isSelected());
                        if (item.isSelected()) {
                            restoreLogPane();
                            //validate();
                            //repaint();
                        } else {
                            componentRegistry.getMainWindow().storeMainSplitPaneDividerLocation();

                            getLogPane().stopRefreshTimer();
                            removeComponent(getLogPane());
                            updateDisplay();
                            componentRegistry.getMainWindow().updateMainSplitPaneDividerLocation();
                        }
                    }


                };

        toggleShowLogAction.putValue(Action.SHORT_DESCRIPTION, atext);

        return toggleShowLogAction;
    }


    public Action getShowStatToggleAction() {
        if (toggleShowStatAction != null) return toggleShowStatAction;
        String atext = resapplication.getString("toggle.statistics.display");

        toggleShowStatAction =
                new AbstractAction(atext) {

                    public void actionPerformed(ActionEvent event) {

                        JCheckBoxMenuItem item = (JCheckBoxMenuItem) event.getSource();

                        getStatisticsPane().setVisible(item.isSelected());
                        if (item.isSelected()) {
                            restoreStatPane();
                        } else {

                            // store the current divider location
                            componentRegistry.getMainWindow().storeMainSplitPaneDividerLocation();
                            getStatisticsPane().stopRefreshTimer();
                            removeComponent(getStatisticsPane());
                            updateDisplay();
                            componentRegistry.getMainWindow().updateMainSplitPaneDividerLocation();
                        }
                    }


                };

        toggleShowStatAction.putValue(Action.SHORT_DESCRIPTION, atext);

        return toggleShowStatAction;
    }

    private StatisticsPanel getStatisticsPane() {
        if (statisticsPane != null) return statisticsPane;

        statisticsPane = new StatisticsPanel();
        return statisticsPane;
    }

    public LogPanel getLogPane() {
        if (logPane != null) return logPane;

        logPane = new LogPanel();
        return logPane;

    }

    /**
     * restore the log window
     */

    private void restoreLogPane() {

        getLogPane().refreshLogs();

        addTab("Logs", getLogPane());
        getLogPane().setVisible(true);
        updateDisplay();
        setSelectedComponent(getLogPane());

        componentRegistry.getMainWindow().updateMainSplitPaneDividerLocation();
    }

    private void restoreStatPane() {

        getStatisticsPane().refreshStatistics();

        addTab("Statistics", getStatisticsPane());
        getStatisticsPane().setVisible(true);
        updateDisplay();
        setSelectedComponent(getStatisticsPane());

        componentRegistry.getMainWindow().updateMainSplitPaneDividerLocation();
    }

    public void setActionEnabled(boolean enable) {

        componentRegistry.getMainWindow().getLogMenuItem().setEnabled(enable);
        componentRegistry.getMainWindow().getStatMenuItem().setEnabled(enable);
    }
}
