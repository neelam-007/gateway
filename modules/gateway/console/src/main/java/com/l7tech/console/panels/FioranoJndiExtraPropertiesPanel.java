package com.l7tech.console.panels;

import com.l7tech.gateway.common.transport.jms.JmsConnection;

import javax.swing.*;
import javax.naming.Context;
import java.util.Properties;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.*;

/**
 * User: dlee
 * Date: Jun 2, 2008
 */
public class FioranoJndiExtraPropertiesPanel extends JmsExtraPropertiesPanel {
    private JPanel mainPanel;
    private JCheckBox sslCheckbox;

    /**
     * From fiorano.jms.runtime.naming.FioranoJNDIContext#SSL_SECURITY_MANAGER
     */
    private static final String PROP_SEC_MANAGER = "SecurityManager";

    /**
     * From fiorano.jms.runtime.naming.FioranoJNDIContext#PROTOCOL_JSSE_SSL
     */
    private static final String SECURITY_PROTOCOL_JSSE_SSL = "SUN_SSL";

    public FioranoJndiExtraPropertiesPanel(final Properties properties) {
        setLayout(new BorderLayout());
        add(mainPanel);

        //add action listener to the following checkboxes and dropdown lists
        sslCheckbox.addActionListener(enableDisableListener);

        setProperties(properties);
    }

    public Properties getProperties() {
        Properties properties = new Properties();

        if ( sslCheckbox.isSelected() ){
            properties.setProperty(Context.SECURITY_PROTOCOL, SECURITY_PROTOCOL_JSSE_SSL);
            properties.setProperty(PROP_SEC_MANAGER, "com.l7tech.server.transport.jms.prov.fiorano.FioranoSecurityManager");
            properties.setProperty(JmsConnection.PROP_CUSTOMIZER, "com.l7tech.server.transport.jms.prov.FioranoConnectionFactoryCustomizer");
        }

        return properties;
    }

    public boolean validatePanel() {
        return true;
    }

    public void setProperties(Properties properties) {
        if ( properties != null ) {
            if ( SECURITY_PROTOCOL_JSSE_SSL.equals(properties.get(Context.SECURITY_PROTOCOL)) ) {
                sslCheckbox.setSelected(true);
            }
        }
    }

    /**
     * Define help method that will perform behaviour based on the action event
     */
    private final ActionListener enableDisableListener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            enableOrDisableComponents();
        }
    };

    /**
     * The logic of components based on the avaialable selection from the GUI
     */
    private void enableOrDisableComponents() {
    }
}
