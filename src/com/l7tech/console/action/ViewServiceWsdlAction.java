package com.l7tech.console.action;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.event.ConnectionEvent;
import com.l7tech.console.event.ConnectionListener;
import com.l7tech.console.tree.ServiceNode;
import com.l7tech.console.util.ComponentRegistry;
import com.l7tech.objectmodel.FindException;
import com.l7tech.service.PublishedService;
import org.syntax.jedit.JEditTextArea;
import org.syntax.jedit.tokenmarker.XMLTokenMarker;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * The <code>PublishServiceAction</code> action invokes the pubish
 * service wizard.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class ViewServiceWsdlAction extends BaseAction implements ConnectionListener {
    static final Logger log = Logger.getLogger(ViewServiceWsdlAction.class.getName());
    private ServiceNode serviceNode;

    public ViewServiceWsdlAction(ServiceNode sn) {
        if (sn == null) {
            throw new IllegalArgumentException();
        }
        serviceNode = sn;
    }

    /**
     * @return the action name
     */
    public String getName() {
        return "View Service Wsdl";
    }

    /**
     * @return the aciton description
     */
    public String getDescription() {
        return "View the service definition (WSDL)";
    }

    /**
     * specify the resource name for this action
     */
    protected String iconResource() {
        // todo: find better icon
        return "com/l7tech/console/resources/xmlsignature.gif";
    }

    /** Actually perform the action.
     * This is the method which should be called programmatically.

     * note on threading usage: do not access GUI components
     * without explicitly asking for the AWT event thread!
     */
    public void performAction() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    final PublishedService ps = serviceNode.getPublishedService();
                    new WsdlViewDialog(ps.getWsdlXml(), ps.getName());
                } catch (FindException e) {
                    log.log(Level.WARNING, "error retrieving service wsdl", e);
                } catch (IOException e) {
                    log.log(Level.WARNING, "error retrieving service wsdl", e);
                }
            }
        });
    }

    private class WsdlViewDialog extends JDialog {
        private JEditTextArea wsdlTextArea;

        private WsdlViewDialog(String wsdlString, String title) {
            super(ComponentRegistry.getInstance().getMainWindow(), title);

            wsdlTextArea = new JEditTextArea();
            wsdlTextArea.setEditable(false);
            wsdlTextArea.setTokenMarker(new XMLTokenMarker());
            wsdlTextArea.setText(wsdlString);
            wsdlTextArea.setCaretPosition(0);
            JPanel panel = new JPanel(new BorderLayout());
            panel.add(wsdlTextArea, BorderLayout.CENTER);

            getContentPane().add(panel, BorderLayout.CENTER);

            addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    dispose();
                }
            });
            pack();
            setSize(600, 600);
            Utilities.centerOnScreen(this);
            setVisible(true);
        }
    }


    public void onConnect(ConnectionEvent e) {
            setEnabled(true);
    }

    public void onDisconnect(ConnectionEvent e) {
            setEnabled(false);
    }
}
