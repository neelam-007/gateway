package com.l7tech.console.panels;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.transport.jms.JmsConnection;
import com.l7tech.common.transport.jms.JmsEndpoint;
import com.l7tech.console.event.PolicyEvent;
import com.l7tech.console.event.PolicyListener;
import com.l7tech.console.util.JmsUtilities;
import com.l7tech.console.util.Registry;
import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.JmsRoutingAssertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.EventListenerList;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.EventListener;
import java.util.logging.Logger;


/**
 * <code>JmsRoutingAssertionDialog</code> is the protected service
 * policy edit dialog for JMS routing assertions.
 *
 * @author <a href="mailto:mlyons@layer7-tech.com">Mike Lyons</a>
 * @version 1.0
 */
public class JmsRoutingAssertionDialog extends JDialog {
    static final Logger log = Logger.getLogger(LogonDialog.class.getName());
    private JmsRoutingAssertion assertion;
    private boolean wasShown = false;
    private boolean wasOkButtonPressed = false;
    private JButton cancelButton;
    private JPanel buttonPanel;
    private JButton okButton;
    private EventListenerList listenerList = new EventListenerList();

    private JmsConnection newlyCreatedConnection = null;
    private JmsEndpoint newlyCreatedEndpoint = null;
    private JPanel serviceEndpointPanel;
    private JPanel mainPanel;
    private JPanel credentialsPanel;
    private JButton newQueueButton;
    private JComboBox queueComboBox;
    private JmsUtilities.QueueItem[] queueItems;

    /**
     * Creates new form ServicePanel
     */
    public JmsRoutingAssertionDialog(Frame owner, JmsRoutingAssertion a) {
        super(owner, true);
        setTitle("JMS Routing Properties");
        assertion = a;
        initComponents();
        initFormData();
    }

    /** @return true unless the dialog was exited via the OK button. */
    public boolean isCanceled() {
        return !wasOkButtonPressed;
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
        final CompositeAssertion parent = a.getParent();
        if (parent == null)
          return;

        SwingUtilities.invokeLater(
          new Runnable() {
              public void run() {
                  int[] indices = new int[parent.getChildren().indexOf(a)];
                  PolicyEvent event = new
                    PolicyEvent(this, new AssertionPath(a.getPath()), indices, new Assertion[]{a});
                  EventListener[] listeners = listenerList.getListeners(PolicyListener.class);
                  for (int i = 0; i < listeners.length; i++) {
                      ((PolicyListener)listeners[i]).assertionsChanged(event);
                  }
              }
          });
    }


    /**
     * This method is called from within the static factory to
     * initialize the form.
     */
    private void initComponents() {
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        getContentPane().setLayout(new BorderLayout());
        getServiceEndpointPanel().setBorder(BorderFactory.createEmptyBorder(10, 5, 5, 10));
        getContentPane().add(getServiceEndpointPanel(), BorderLayout.NORTH);

        mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(new EmptyBorder(new Insets(10, 10, 10, 10)));

        credentialsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.weightx = 0;
        gridBagConstraints.weighty = 0;

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.weighty = 1;
        gridBagConstraints.fill = GridBagConstraints.VERTICAL;
        credentialsPanel.add(Box.createGlue(), gridBagConstraints);


        mainPanel.add(credentialsPanel,
                      new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0,
                                             GridBagConstraints.WEST,
                                             GridBagConstraints.BOTH,
                                             new Insets(0, 0, 0, 0), 0, 0));

        // Add buttonPanel
        mainPanel.add(getButtonPanel(),
                      new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                                             GridBagConstraints.EAST,
                                             GridBagConstraints.HORIZONTAL,
                                             new Insets(0, 0, 0, 0), 0, 0));
        getContentPane().add(mainPanel, BorderLayout.CENTER);
    }

    private JPanel getServiceEndpointPanel() {
        if (serviceEndpointPanel != null)
            return serviceEndpointPanel;

        serviceEndpointPanel = new JPanel();
        serviceEndpointPanel.setLayout(new GridBagLayout());

        JLabel serviceUrlLabel = new JLabel();
        serviceUrlLabel.setText("JMS Queue: ");
        serviceUrlLabel.setBorder(new EmptyBorder(new Insets(1, 1, 1, 5)));
        serviceEndpointPanel.add(serviceUrlLabel,
                                 new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                                                        GridBagConstraints.WEST,
                                                        GridBagConstraints.NONE,
                                                        new Insets(0, 0, 0, 0), 0, 0));

        serviceEndpointPanel.add(getQueueComboBox(),
                                 new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0,
                                                        GridBagConstraints.WEST,
                                                        GridBagConstraints.HORIZONTAL,
                                                        new Insets(0, 6, 0, 5), 0, 0));

        serviceEndpointPanel.add(getNewQueueButton(),
                                 new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
                                                        GridBagConstraints.EAST,
                                                        GridBagConstraints.NONE,
                                                        new Insets(0, 0, 0, 0), 0, 0));


        return serviceEndpointPanel;
    }

    private JmsUtilities.QueueItem[] loadQueueItems() {
        return queueItems = JmsUtilities.loadQueueItems();
    }

    private JmsUtilities.QueueItem[] getQueueItems() {
        if (queueItems == null)
            queueItems = loadQueueItems();
        return queueItems;
    }

    private JComboBox getQueueComboBox() {
        if (queueComboBox == null) {
            queueComboBox = new JComboBox(new DefaultComboBoxModel(getQueueItems()));
            queueComboBox.setPreferredSize(new Dimension(400, 20));
        }
        return queueComboBox;
    }

    private JButton getNewQueueButton() {
        if (newQueueButton == null) {
            newQueueButton = new JButton("New Queue");
            newQueueButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    JmsEndpoint ep = newlyCreatedEndpoint;
                    JmsConnection conn = newlyCreatedConnection;
                    JmsQueuePropertiesDialog pd = JmsQueuePropertiesDialog.createInstance(getOwner(), conn, ep, true);
                    Utilities.centerOnScreen(pd);
                    pd.show();
                    if (!pd.isCanceled()) {
                        newlyCreatedEndpoint = pd.getEndpoint();
                        newlyCreatedConnection = pd.getConnection();
                        getQueueComboBox().setModel(new DefaultComboBoxModel(loadQueueItems()));
                        JmsUtilities.selectEndpoint(getQueueComboBox(), newlyCreatedEndpoint);
                    }
                }
            });
        }
        return newQueueButton;
    }

    /** Force disposal after dialog has been used once. */
    public void show() {
        if (wasShown)
            throw new IllegalStateException("This dialog has already been shown and/or disposed.");
        wasShown = true;
        super.show();
        hide();
        dispose();
    }

    /**
     * Returns buttonPanel
     */
    private JPanel getButtonPanel() {
        if (buttonPanel == null) {
            buttonPanel = new JPanel();
            buttonPanel.setLayout(new GridBagLayout());

            Component hStrut = Box.createHorizontalStrut(8);

            // add components
            buttonPanel.add(hStrut,
              new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0,
                GridBagConstraints.CENTER,
                GridBagConstraints.BOTH,
                new Insets(0, 0, 0, 0), 0, 0));

            buttonPanel.add(getOKButton(),
              new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER,
                GridBagConstraints.NONE,
                new Insets(5, 5, 5, 5), 0, 0));

            buttonPanel.add(getCancelButton(),
              new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER,
                GridBagConstraints.NONE,
                new Insets(5, 5, 5, 5), 0, 0));

            JButton buttons[] = new JButton[]
            {
                getOKButton(),
                getCancelButton()
            };
            Utilities.equalizeButtonSizes(buttons);
        }
        return buttonPanel;
    }


    /**
     * Returns okButton
     */
    private JButton getOKButton() {
        // If button not already created
        if (null == okButton) {
            // Create button
            okButton = new JButton("OK");

            // Register listener
            okButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    // copy view into model
                    JmsUtilities.QueueItem item = (JmsUtilities.QueueItem)getQueueComboBox().getSelectedItem();

                    if ( item == null ) {
                        assertion.setEndpointOid(null);
                        assertion.setEndpointName(null);
                    } else {
                        JmsEndpoint endpoint = item.getQueue().getEndpoint();
                        assertion.setEndpointOid(new Long(endpoint.getOid()));
                        assertion.setEndpointName(endpoint.getName());
                    }

                    assertion.setGroupMembershipStatement(false);
                    assertion.setAttachSamlSenderVouches(false);
                    fireEventAssertionChanged(assertion);
                    wasOkButtonPressed = true;
                    newlyCreatedConnection = null; // prevent disposal from deleting our new serviceQueue
                    newlyCreatedEndpoint = null;
                    JmsRoutingAssertionDialog.this.dispose();
                }
            });
        }

        // Return button
        return okButton;
    }

    /**
     * Returns cancelButton
     */
    private JButton getCancelButton() {
        // If button not already created
        if (null == cancelButton) {

            // Create button
            cancelButton = new JButton("Cancel");

            // Register listener
            cancelButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    JmsRoutingAssertionDialog.this.dispose();
                }
            });
        }
        // Return button
        return cancelButton;
    }

    public void dispose() {
        wasShown = true;
        super.dispose();

        try {
            if (newlyCreatedEndpoint != null) {
                Registry.getDefault().getJmsManager().deleteEndpoint(newlyCreatedEndpoint.getOid());
                newlyCreatedEndpoint = null;
            }
            if (newlyCreatedConnection != null) {
                Registry.getDefault().getJmsManager().deleteConnection(newlyCreatedConnection.getOid());
                newlyCreatedConnection = null;
            }
        } catch (Exception e) {
            // TODO log this somewhere instead
            throw new RuntimeException("Unable to roll back newly-created JMS Queue", e);
        }
    }

    private void initFormData() {
        Long endpointOid = assertion.getEndpointOid();
        try {
            JmsEndpoint serviceEndpoint = null;
            if (endpointOid != null) {
                serviceEndpoint = Registry.getDefault().getJmsManager().findEndpointByPrimaryKey(endpointOid.longValue());
            }
            JmsUtilities.selectEndpoint(getQueueComboBox(), serviceEndpoint);
        } catch (Exception e) {
            throw new RuntimeException("Unable to look up JMS Queue for this routing assertion", e);
        }
    }
}
