package com.l7tech.console.panels;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.wsdl.BindingInfo;
import com.l7tech.common.wsdl.BindingOperationInfo;
import com.l7tech.common.wsdl.MimePartInfo;
import com.l7tech.common.xml.Wsdl;
import com.l7tech.console.event.PolicyEvent;
import com.l7tech.console.event.PolicyListener;
import com.l7tech.console.tree.ServiceNode;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.RequestSwAAssertion;

import javax.swing.*;
import javax.swing.event.EventListenerList;
import javax.wsdl.Binding;
import javax.wsdl.BindingOperation;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.mime.MIMEContent;
import javax.wsdl.extensions.mime.MIMEMultipartRelated;
import javax.wsdl.extensions.mime.MIMEPart;
import javax.wsdl.extensions.soap.SOAPBody;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.rmi.RemoteException;
import java.util.*;
import java.util.List;
import java.util.logging.Logger;


/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
public class RequestSwAAssertionDialog extends JDialog {
    static final Logger log = Logger.getLogger(RequestSwAAssertionDialog.class.getName());
    private RequestSwAAssertion assertion;
    private JButton cancelButton;
    private JButton okButton;
    private JPanel mainPanel;
    private JComboBox bindingsListComboxBox;
    private JScrollPane operationsScrollPane;
    private JScrollPane multipartScrollPane;
    private ServiceNode serviceNode;
    private EventListenerList listenerList = new EventListenerList();
    private BindingInfo bindingInfo = new BindingInfo();

    private static ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.resources.RequestSwAPropertiesDialog", Locale.getDefault());
    private static Logger logger = Logger.getLogger(RequestSwAAssertionDialog.class.getName());

    public RequestSwAAssertionDialog(JFrame parent, RequestSwAAssertion assertion, ServiceNode sn) {
        super(parent, resources.getString("window.title"), true);
        this.assertion = assertion;
        this.serviceNode = sn;


        initialize();
        populateData();
        pack();
        Utilities.centerOnScreen(this);
    }

    private void initialize() {
        Container p = getContentPane();
        p.setLayout(new BorderLayout());
        p.add(mainPanel, BorderLayout.CENTER);

        okButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                fireEventAssertionChanged(assertion);
                dispose();
            }
        });

        cancelButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                dispose();
            }
        });

        Utilities.equalizeButtonSizes(new JButton[]{cancelButton, okButton});

    }

    private void populateData() {
        // get the MIMEParts Info from WSDL
        loadMIMEPartsInfoFromWSDL();

        if (assertion.getBindingInfo().getBindingName().equals("")) {
            // this is the first time
            populateDataFromWSDL();
        } else {
            // check if the WSDL changed
            populateDataFromAssertion();
        }
    }

    private void populateDataFromAssertion() {

    }

    private void populateDataFromWSDL() {
        assertion.setBindingInfo(bindingInfo);
    }

    private void loadMIMEPartsInfoFromWSDL() {
        try {
            Wsdl parsedWsdl = serviceNode.getPublishedService().parsedWsdl();

            Collection bindingList = parsedWsdl.getBindings();
            HashMap operations = new HashMap();

            // for each binding
            for (Iterator iterator = bindingList.iterator(); iterator.hasNext();) {
                Binding binding = (Binding) iterator.next();

                //todo: should filter out non-SOAP binding
                Collection boList = binding.getBindingOperations();

                // for each operation
                for (Iterator iterator1 = boList.iterator(); iterator1.hasNext();) {
                    BindingOperation bo = (BindingOperation) iterator1.next();
                //    System.out.println(bo.getOperation().toString());

                    HashMap partList = new HashMap();
                    Collection elements = parsedWsdl.getInputParameters(bo);

                    for (Iterator itr = elements.iterator(); itr.hasNext();) {

                        Object o = (Object) itr.next();
                        if (o instanceof MIMEMultipartRelated) {

                            MIMEMultipartRelated multipart = (MIMEMultipartRelated) o;

                            List parts = multipart.getMIMEParts();
                            for (Iterator partsItr = parts.iterator(); partsItr.hasNext();) {

                                MIMEPart mimePart = (MIMEPart) partsItr.next();
                                Collection mimePartSubElements = parsedWsdl.getMimePartSubElements(mimePart);

                                for (Iterator subElementItr = mimePartSubElements.iterator(); subElementItr.hasNext();) {
                                    Object subElement = (Object) subElementItr.next();

                                    if (subElement instanceof MIMEContent) {
                                        MIMEContent mimeContent = (MIMEContent) subElement;
                                        MimePartInfo part = new MimePartInfo(mimeContent.getPart(), mimeContent.getType());

                                        //concat the content type if the part alreay exists
                                        MimePartInfo partInfo = (MimePartInfo) partList.get(mimeContent.getPart());
                                        if (partInfo != null) {
                                            partInfo.setContentType(partInfo.getContentType() + mimeContent.getPart());
                                        } else {
                                            partList.put(mimeContent.getPart(), part);
                                        }
                                    } else if (subElement instanceof SOAPBody) {
                                        // don't care about soapPart for now
                                        //SOAPBody soapBody = (SOAPBody) subElement;
                                    }
                                }
                            }
                        }
                        // create BindingOperationInfo
                        BindingOperationInfo operation = new BindingOperationInfo(bo.getOperation().getName(), partList);
                        operations.put(bo.getOperation().getName(), operation);
                    }
                }
                bindingInfo = new BindingInfo(binding.getQName().getLocalPart(), operations);
            }

        } catch (FindException e) {
            logger.warning("The service not found: " + serviceNode.getName());
        } catch (RemoteException re) {
            logger.severe("Remote exception");
        } catch (WSDLException e) {
            logger.warning("Unable to retrieve parse the WSDL of the service " + serviceNode.getName());
        }
    }

    /**
     * add the PolicyListener
     *
     * @param listener the PolicyListener
     */
    public void addPolicyListener(PolicyListener listener) {
        listenerList.add(PolicyListener.class, listener);
    }

    /**
     * remove the the PolicyListener
     *
     * @param listener the PolicyListener
     */
    public void removePolicyListener(PolicyListener listener) {
        listenerList.remove(PolicyListener.class, listener);
    }

    /**
     * notfy the listeners
     *
     * @param a the assertion
     */
    private void fireEventAssertionChanged(final Assertion a) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                int[] indices = new int[a.getParent().getChildren().indexOf(a)];
                PolicyEvent event = new
                        PolicyEvent(this, new AssertionPath(a.getPath()), indices, new Assertion[]{a});
                EventListener[] listeners = listenerList.getListeners(PolicyListener.class);
                for (int i = 0; i < listeners.length; i++) {
                    ((PolicyListener) listeners[i]).assertionsChanged(event);
                }
            }
        });
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// !!! IMPORTANT !!!
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * !!! IMPORTANT !!!
     * DO NOT edit this method OR call it in your code!
     */
    private void $$$setupUI$$$() {
        mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayoutManager(2, 1, new Insets(10, 10, 10, 10), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(panel1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel2.add(panel3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null));
        final JLabel label1 = new JLabel();
        label1.setText("Operations:");
        panel3.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        operationsScrollPane = new JScrollPane();
        panel3.add(operationsScrollPane, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(150, -1), new Dimension(150, 200), new Dimension(200, -1)));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel2.add(panel4, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null));
        final JLabel label2 = new JLabel();
        label2.setText("Attachments:");
        panel4.add(label2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        multipartScrollPane = new JScrollPane();
        panel4.add(multipartScrollPane, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(400, -1), new Dimension(400, 200), null));
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridLayoutManager(2, 1, new Insets(10, 0, 10, 0), -1, -1));
        panel1.add(panel5, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        final JLabel label3 = new JLabel();
        label3.setText("Bindings:");
        panel5.add(label3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        bindingsListComboxBox = new JComboBox();
        panel5.add(bindingsListComboxBox, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), new Dimension(200, -1)));
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(panel6, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null));
        cancelButton = new JButton();
        cancelButton.setText("Canel");
        panel6.add(cancelButton, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        final Spacer spacer1 = new Spacer();
        panel6.add(spacer1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null));
        okButton = new JButton();
        okButton.setText("OK");
        panel6.add(okButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
    }
}
