package com.l7tech.console.action;

import com.japisoft.xmlpad.PopupModel;
import com.japisoft.xmlpad.UIAccessibility;
import com.japisoft.xmlpad.XMLContainer;
import com.japisoft.xmlpad.editor.XMLEditor;
import com.japisoft.xmlpad.action.ActionModel;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.gui.widgets.ContextMenuTextField;
import com.l7tech.console.tree.ServiceNode;
import com.l7tech.console.util.TopComponents;
import com.l7tech.identity.Group;
import com.l7tech.objectmodel.FindException;
import com.l7tech.service.PublishedService;

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
    private String description = "View the WSDL defined for the Web service";
    private PublishedService publishedService;

    public ViewServiceWsdlAction(ServiceNode sn) {
        super(sn);
        if (sn == null) {
            throw new IllegalArgumentException();
        }
        serviceNode = sn;
        setEnabled(false);
        try {
            publishedService = serviceNode.getPublishedService();
            String wsdlXml = publishedService.getWsdlXml();
            if (wsdlXml != null && !"".equals(wsdlXml)) {
                setEnabled(true);
            } else {
                description = "The service has no WSDL";
            }
        } catch (FindException e) {
            description = "Error obtaining WSDL";
            log.log(Level.WARNING, "error retrieving service wsdl", e);
        } catch (IOException e) {
            description = "Error obtaining WSDL";
            log.log(Level.WARNING, "error retrieving service wsdl", e);
        }
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
        return description;
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
                new WsdlViewDialog(publishedService).setVisible(true);
            }
        });
    }

    private class WsdlViewDialog extends JFrame {
        private XMLContainer xmlContainer;
        private UIAccessibility uiAccessibility;

        private WsdlViewDialog(final PublishedService ps) {
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
                BorderFactory.createEtchedBorder(), BorderFactory.createEmptyBorder(5, 5, 5, 5));
            wsdlPanel.setBorder(border);
            // configure xml editing widget
            xmlContainer = new XMLContainer(true);
            uiAccessibility = xmlContainer.getUIAccessibility();
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    XMLEditor editor = uiAccessibility.getEditor();
                    editor.setText(ps.getWsdlXml());
                    Action reformatAction = ActionModel.getActionByName(ActionModel.FORMAT_ACTION);
                    reformatAction.actionPerformed(null);
                }
            });

            uiAccessibility.setTreeAvailable(false);
            uiAccessibility.setTreeToolBarAvailable(false);
            xmlContainer.setEditable(false);
            uiAccessibility.setToolBarAvailable(false);
            xmlContainer.setStatusBarAvailable(false);
            PopupModel popupModel = xmlContainer.getPopupModel();
            // remove the unwanted actions
            popupModel.removeAction(ActionModel.getActionByName(ActionModel.PARSE_ACTION));
            popupModel.removeAction(ActionModel.getActionByName(ActionModel.FORMAT_ACTION));
            popupModel.removeAction(ActionModel.getActionByName(ActionModel.LOAD_ACTION));
            popupModel.removeAction(ActionModel.getActionByName(ActionModel.SAVE_ACTION));
            popupModel.removeAction(ActionModel.getActionByName(ActionModel.SAVEAS_ACTION));
            popupModel.removeAction(ActionModel.getActionByName(ActionModel.NEW_ACTION));
            popupModel.removeAction(ActionModel.getActionByName(ActionModel.INSERT_ACTION));
            popupModel.removeAction(ActionModel.getActionByName(ActionModel.COMMENT_ACTION));

            panel.add(xmlContainer.getView(), BorderLayout.CENTER);

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
