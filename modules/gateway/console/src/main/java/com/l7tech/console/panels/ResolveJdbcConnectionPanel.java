package com.l7tech.console.panels;

import javax.swing.*;
import java.awt.*;

/**
 * @author: ghuang
 */
public class ResolveJdbcConnectionPanel extends WizardStepPanel {
    private JPanel mainPanel;
    private JComboBox connectionsComboBox;
    private JButton manageConnectionsButton;

    public ResolveJdbcConnectionPanel(WizardStepPanel next) {
        super(next);
        initialize();
    }

    public String getDescription() {
        return getStepLabel();
    }

    public boolean canFinish() {
        return !hasNextPanel();
    }

    public String getStepLabel() {
        return "Unresolved JDBC Connection " + "XXX";
    }

    private void initialize() {
        setLayout(new BorderLayout());
        add(mainPanel);
        connectionsComboBox.addItem("Conn1-MySQL");
        connectionsComboBox.addItem("Conn2-MsSQL");
    }
}
