package com.l7tech.console.panels;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

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
        serviceUrljTextField = new JTextField();
        resolvejButton = new JButton();
        serviceOperationsjPanel = new JPanel();
        methodsjScrollPane = new JScrollPane();
        methodsjList = new JList();
        rigidAreatjPanel = new JPanel();

        setLayout(new BorderLayout());

        serviceUrljPanel.setLayout(new BoxLayout(serviceUrljPanel, BoxLayout.X_AXIS));

        serviceUrljPanel.setBorder(new EmptyBorder(new Insets(5, 5, 5, 5)));
        serviceUrljLabel.setText("Service WSDL location");
        serviceUrljLabel.setBorder(new EmptyBorder(new Insets(1, 1, 1, 5)));
        serviceUrljPanel.add(serviceUrljLabel);

        serviceUrljTextField.setText("http://localhost/urn:QuoteService?wsdl");
        serviceUrljTextField.setPreferredSize(new Dimension(150, 20));
        serviceUrljPanel.add(serviceUrljTextField);

        resolvejButton.setText("Resolve");
        resolvejButton.setMargin(new Insets(0, 14, 0, 14));
        resolvejButton.setMinimumSize(new Dimension(79, 32));
        resolvejButton.setPreferredSize(new Dimension(79, 25));
        serviceUrljPanel.add(resolvejButton);

        add(serviceUrljPanel, BorderLayout.NORTH);

        serviceOperationsjPanel.setLayout(new BoxLayout(serviceOperationsjPanel, BoxLayout.X_AXIS));

        serviceOperationsjPanel.setBorder(new EmptyBorder(new Insets(10, 10, 10, 10)));
        methodsjScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        methodsjScrollPane.setPreferredSize(new Dimension(200, 150));
        methodsjList.setModel(new AbstractListModel() {
            String[] strings = { "getQuote", "placeOrder", "cancelOrder", "viewPendingOrders" };
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
        });
        methodsjScrollPane.setViewportView(methodsjList);

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
    private JList methodsjList;
    private JPanel serviceUrljPanel;
    private JPanel serviceOperationsjPanel;
    private JTextField serviceUrljTextField;
    // End of variables declaration//GEN-END:variables
    
}
