package com.l7tech.console.action;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.gui.widgets.ContextMenuTextField;
import com.l7tech.console.tree.ServiceNode;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.xmlviewer.Viewer;
import com.l7tech.identity.Group;
import com.l7tech.objectmodel.FindException;
import com.l7tech.service.PublishedService;
import org.dom4j.DocumentException;
import org.xml.sax.SAXParseException;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The <code>PublishServiceAction</code> action invokes the pubish
 * service wizard.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class ViewServiceWsdlAction extends NodeAction {
    static final Logger log = Logger.getLogger(ViewServiceWsdlAction.class.getName());
    private ServiceNode serviceNode;

    public ViewServiceWsdlAction(ServiceNode sn) {
        super(sn);
        if (sn == null) {
            throw new IllegalArgumentException();
        }
        serviceNode = sn;
    }

    /**
     * @return the action name
     */
    public String getName() {
        return "View WSDL";
    }

    /**
     * @return the aciton description
     */
    public String getDescription() {
        return "View the WSDL defined for the Web service";
    }

    /**
     * specify the resource name for this action
     */
    protected String iconResource() {
        // todo: find better icon
        return "com/l7tech/console/resources/xmlsignature.gif";
    }

    /**
     * Actually perform the action.
     * This is the method which should be called programmatically.
     * <p/>
     * note on threading usage: do not access GUI components
     * without explicitly asking for the AWT event thread!
     */
    protected void performAction() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    final PublishedService ps = serviceNode.getPublishedService();
                    new WsdlViewDialog(ps).show();
                } catch (FindException e) {
                    log.log(Level.WARNING, "error retrieving service wsdl", e);
                } catch (IOException e) {
                    log.log(Level.WARNING, "error retrieving service wsdl", e);
                } catch (DocumentException e) {
                    log.log(Level.WARNING, "error retrieving service wsdl", e);
                } catch (SAXParseException e) {
                    log.log(Level.WARNING, "error retrieving service wsdl", e);
                }
            }
        });
    }

    private class WsdlViewDialog extends JFrame {

        private WsdlViewDialog(PublishedService ps) throws IOException, DocumentException, SAXParseException {
            setTitle(ps.getName());
            setIconImage(TopComponents.getInstance().getMainWindow().getIconImage());
            JPanel panel = new JPanel(new BorderLayout());
            JPanel wsdlPanel = new JPanel();
            wsdlPanel.setLayout(new BoxLayout(wsdlPanel, BoxLayout.X_AXIS));
            panel.add(wsdlPanel, BorderLayout.NORTH);

            final JLabel l = new JLabel("WSDL URL: ");
            l.setFont(l.getFont().deriveFont(Font.BOLD));
            wsdlPanel.add(l);

            final String wsdlUrl = ps.getWsdlUrl();
            final JTextField tf = new ContextMenuTextField(wsdlUrl);
            tf.setBorder(BorderFactory.createEmptyBorder());
            tf.setEditable(false);
            wsdlPanel.add(tf);

            final CompoundBorder border =
              BorderFactory.createCompoundBorder(
                BorderFactory.createEtchedBorder(), BorderFactory.createEmptyBorder(5,5,5,5));
            wsdlPanel.setBorder(border);
            Viewer messageViewer = Viewer.createMessageViewer(ps.getWsdlXml());
            panel.add(messageViewer, BorderLayout.CENTER);

            getContentPane().add(panel, BorderLayout.CENTER);
            Actions.setEscKeyStrokeDisposes(this);
            addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    dispose();
                }
            });
            pack();
            final int labelWidth = (int)(l.getSize().getWidth() + 20);
            setSize(Math.max(600, labelWidth), 800);
            Utilities.centerOnScreen(this);
        }
    }

    /**
     * Return the required roles for this action, one of the roles. The base
     * implementatoinm requires the strongest admin role.
     *
     * @return the list of roles that are allowed to carry out the action
     */
    protected String[] requiredRoles() {
        return new String[]{Group.ADMIN_GROUP_NAME, Group.OPERATOR_GROUP_NAME};
    }
}
