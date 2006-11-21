package com.l7tech.console.action;

import com.japisoft.xmlpad.PopupModel;
import com.japisoft.xmlpad.UIAccessibility;
import com.japisoft.xmlpad.XMLContainer;
import com.japisoft.xmlpad.editor.XMLEditor;
import com.japisoft.xmlpad.action.ActionModel;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.gui.util.DialogDisplayer;
import com.l7tech.common.gui.widgets.ContextMenuTextField;
import com.l7tech.console.tree.ServiceNode;
import com.l7tech.console.util.TopComponents;
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

    private String description = "View the WSDL defined for the Web service";
    private PublishedService publishedService;

    public ViewServiceWsdlAction(ServiceNode sn) {
        super(sn, LIC_AUTH_ASSERTIONS, null);
        if (sn == null) {
            throw new IllegalArgumentException();
        }
        setEnabled(false);
        try {
            publishedService = sn.getPublishedService();
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
                DialogDisplayer.display(new WsdlViewDialog(publishedService));
            }
        });
    }

    private class WsdlViewDialog extends JDialog {
        private XMLContainer xmlContainer;

        private WsdlViewDialog(final PublishedService ps) {
            super(TopComponents.getInstance().getTopParent(), true);

            // TODO reenable this as soon as the problem with the XML viewer widget NPEing is straightened out
            DialogDisplayer.suppressSheetDisplay(this);

            setTitle(ps.getName());
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
            final UIAccessibility uiAccessibility = xmlContainer.getUIAccessibility();
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
            popupModel.removeAction(ActionModel.getActionByName(ActionModel.TREE_SELECTNODE_ACTION));
            popupModel.removeAction(ActionModel.getActionByName(ActionModel.TREE_COMMENTNODE_ACTION));
            popupModel.removeAction(ActionModel.getActionByName(ActionModel.TREE_COPYNODE_ACTION));
            popupModel.removeAction(ActionModel.getActionByName(ActionModel.TREE_CUTNODE_ACTION));
            popupModel.removeAction(ActionModel.getActionByName(ActionModel.TREE_EDITNODE_ACTION));
            popupModel.removeAction(ActionModel.getActionByName(ActionModel.TREE_CLEANHISTORY_ACTION));
            popupModel.removeAction(ActionModel.getActionByName(ActionModel.TREE_ADDHISTORY_ACTION));
            popupModel.removeAction(ActionModel.getActionByName(ActionModel.TREE_PREVIOUS_ACTION));
            popupModel.removeAction(ActionModel.getActionByName(ActionModel.TREE_NEXT_ACTION));
            if (TopComponents.getInstance().isApplet()) {
                // Search action tries to get the class loader
                popupModel.removeAction(ActionModel.getActionByName(ActionModel.SEARCH_ACTION));
            }

            boolean lastWasSeparator = true; // remove trailing separator
            for (int i=popupModel.size()-1; i>=0; i--) {
                boolean isSeparator = popupModel.isSeparator(i);
                if (isSeparator && (i==0 || lastWasSeparator)) {
                    popupModel.removeSeparator(i);
                } else {
                    lastWasSeparator = isSeparator;
                }
            }

            panel.add(xmlContainer.getView(), BorderLayout.CENTER);

            getContentPane().add(panel, BorderLayout.CENTER);
            Utilities.setEscKeyStrokeDisposes(this);
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

        public void dispose() {
            xmlContainer.dispose();
            super.dispose();
        }
    }

}
