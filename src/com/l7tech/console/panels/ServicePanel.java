package com.l7tech.console.panels;

import com.l7tech.console.util.Registry;
import com.l7tech.console.tree.WsdlTreeNode;
import com.l7tech.service.Wsdl;

import javax.swing.*;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreeModel;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.border.EmptyBorder;
import javax.wsdl.WSDLException;
import javax.wsdl.PortType;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.rmi.RemoteException;
import java.io.StringReader;
import java.util.Iterator;
import java.util.ArrayList;

/**
 *
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 * @version 1.2

 */
public class ServicePanel extends WizardStepPanel {
    
    /** Creates new form ServicePanel */
    public ServicePanel() {
        initComponents();
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     */
    private void initComponents() {
        serviceUrljPanel = new JPanel();
        serviceUrljLabel = new JLabel();
        wsdlUrljTextField = new JTextField();
        resolvejButton = new JButton();
        serviceOperationsjPanel = new JPanel();
        methodsjScrollPane = new JScrollPane();
        wsdlJTree = new JTree();
        rigidAreatjPanel = new JPanel();

        setLayout(new BorderLayout());

        serviceUrljPanel.setLayout(new BoxLayout(serviceUrljPanel, BoxLayout.X_AXIS));

        serviceUrljPanel.setBorder(new EmptyBorder(new Insets(5, 5, 5, 5)));
        serviceUrljLabel.setText("Service WSDL location");
        serviceUrljLabel.setBorder(new EmptyBorder(new Insets(1, 1, 1, 5)));
        serviceUrljPanel.add(serviceUrljLabel);

        wsdlUrljTextField.setText("http://localhost/urn:QuoteService?wsdl");
        wsdlUrljTextField.setPreferredSize(new Dimension(150, 20));
        serviceUrljPanel.add(wsdlUrljTextField);

        resolvejButton.setText("Resolve");
        resolvejButton.setMargin(new Insets(0, 14, 0, 14));
        resolvejButton.setMinimumSize(new Dimension(79, 32));
        resolvejButton.setPreferredSize(new Dimension(79, 25));

        resolvejButton.addActionListener(new ActionListener() {
            /** Invoked when an action occurs. */
            public void actionPerformed(ActionEvent e) {
                try {
                    String sw =
                            Registry.getDefault().getServiceManager().resolveWsdlTarget(wsdlUrljTextField.getText());
                    Wsdl wsdl = Wsdl.newInstance(null, new StringReader(sw));
                    TreeNode node = WsdlTreeNode.newInstance(wsdl);
                    wsdlJTree.setModel(new DefaultTreeModel(node));
                } catch (RemoteException e1) {
                    e1.printStackTrace();
                    JOptionPane.showMessageDialog(null,
                                        "Unable to resolve the WSDL at location '"+wsdlUrljTextField.getText()+"'\n",
                                        "Error",
                                        JOptionPane.ERROR_MESSAGE);

                } catch (WSDLException e1) {
                     e1.printStackTrace();
                    JOptionPane.showMessageDialog(null,
                            "Unable to parse the WSDL at location '"+wsdlUrljTextField.getText()+"'\n",
                                                            "Error",
                                                            JOptionPane.ERROR_MESSAGE);
                }
            }

        });
        serviceUrljPanel.add(resolvejButton);

        add(serviceUrljPanel, BorderLayout.NORTH);

        serviceOperationsjPanel.setLayout(new BoxLayout(serviceOperationsjPanel, BoxLayout.X_AXIS));

        serviceOperationsjPanel.setBorder(new EmptyBorder(new Insets(10, 10, 10, 10)));
        methodsjScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        methodsjScrollPane.setPreferredSize(new Dimension(200, 150));


        methodsjScrollPane.setViewportView(wsdlJTree);

        serviceOperationsjPanel.add(methodsjScrollPane);

        rigidAreatjPanel.setLayout(new GridBagLayout());

        serviceOperationsjPanel.add(rigidAreatjPanel);

        add(serviceOperationsjPanel, BorderLayout.CENTER);

    }
        
    public String getDescription() {
        return "Retrieve the protected service description."
        +" Specify the service WSDL URL. Note that the request is performed on SSG (server side)";
    }

    /** @return the wizard step label    */
    public String getStepLabel() {
        return "Protected service";
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private JButton resolvejButton;
    private JLabel serviceUrljLabel;
    private JPanel rigidAreatjPanel;
    private JScrollPane methodsjScrollPane;
    private JTree wsdlJTree;
    private JPanel serviceUrljPanel;
    private JPanel serviceOperationsjPanel;
    private JTextField wsdlUrljTextField;

}
