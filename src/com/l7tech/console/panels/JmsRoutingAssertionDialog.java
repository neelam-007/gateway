package com.l7tech.console.panels;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.transport.jms.JmsEndpoint;
import com.l7tech.common.transport.jms.JmsConnection;
import com.l7tech.console.event.PolicyEvent;
import com.l7tech.console.event.PolicyListener;
import com.l7tech.console.util.Registry;
import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.JmsRoutingAssertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.EventListener;
import java.util.logging.Logger;


/**
 * <code>JmsRoutingAssertionDialog</code> is the protected service
 * policy edit dialog for JMS routing assertions.
 * TODO: this class is bad cut-and-paste from HttpRoutingAssertion.  Refactor out common code (mostly SAML)
 * 
 * @author <a href="mailto:mlyons@layer7-tech.com">Mike Lyons</a>
 * @version 1.0
 */
public class JmsRoutingAssertionDialog extends JDialog {
    static final Logger log = Logger.getLogger(LogonDialog.class.getName());
    private JmsRoutingAssertion assertion;
    private boolean wasOkButtonPressed = false;
    private JButton cancelButton;
    private JPanel buttonPanel;
    private JButton okButton;
    private EventListenerList listenerList = new EventListenerList();

    private JmsEndpoint serviceEndpoint;
    private JPanel serviceEndpointPanel;
    private JLabel serviceEndpointName;
    private JPanel mainPanel;
    private JPanel credentialsPanel;
    private JButton changeButton;

    private JRadioButton passwordMethod;
    private JRadioButton samlMethod;
    private JSpinner expirySpinner;
    private JCheckBox memebershipStatementCheck;
    private JLabel expirySpinLabel;

    /**
     * Creates new form ServicePanel
     */
    public JmsRoutingAssertionDialog(Frame owner, JmsRoutingAssertion a) {
        super(owner, true);
        setTitle("Edit JMS Routing Assertion");
        assertion = a;
        initComponents();
        initFormData();
    }

    public void show() {
        if (isModal() && !isVisible()) {
            // Automatically run the Change Endpoint dialog
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    runChangeEndpointConnectionWindow();
                }
            });
        }
        super.show();
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
     * This method is called from within the constructor to
     * initialize the form.
     */
    private void initComponents() {

        getContentPane().setLayout(new BorderLayout());
        getServiceEndpointPanel().setBorder(BorderFactory.createEmptyBorder(10, 5, 5, 10));
        getContentPane().add(getServiceEndpointPanel(), BorderLayout.NORTH);

        mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(new EmptyBorder(new Insets(10, 10, 10, 10)));

        ButtonGroup methodGroup = new ButtonGroup();

        credentialsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.weightx = 0;
        gridBagConstraints.weighty = 0;

        passwordMethod = new JRadioButton("Authenticate through JMS Endpoint");
        methodGroup.add(passwordMethod);
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new Insets(0, 0, 5, 10);
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        credentialsPanel.add(passwordMethod, gridBagConstraints);

        samlMethod = new JRadioButton("Authenticate using SAML assertion");
        methodGroup.add(samlMethod);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new Insets(0, 0, 5, 0);
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        credentialsPanel.add(samlMethod, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(0, 30, 20, 10);
        credentialsPanel.add(getSamlAssertionPanel(), gridBagConstraints);

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

        // listeners
        samlMethod.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                updateEnableDisable();
            }
        });
        passwordMethod.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                updateEnableDisable();
            }
        });

    }

    private void updateEnableDisable() {
        boolean saml = false;
        if (samlMethod.isSelected()) {
            saml = true;
        }

        expirySpinner.setEnabled(saml);
        expirySpinLabel.setEnabled(saml);
        memebershipStatementCheck.setEnabled(saml);
    }

    private JPanel getServiceEndpointPanel() {
        if (serviceEndpointPanel != null)
            return serviceEndpointPanel;

        serviceEndpointPanel = new JPanel();
        serviceEndpointPanel.setLayout(new GridBagLayout());

        JLabel serviceUrlLabel = new JLabel();
        serviceUrlLabel.setText("JMS Endpoint: ");
        serviceUrlLabel.setBorder(new EmptyBorder(new Insets(1, 1, 1, 5)));
        serviceEndpointPanel.add(serviceUrlLabel,
                                 new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                                                        GridBagConstraints.WEST,
                                                        GridBagConstraints.NONE,
                                                        new Insets(0, 0, 0, 0), 0, 0));

        serviceEndpointName = new JLabel();
        serviceEndpointName.setPreferredSize(new Dimension(300, 20));
        serviceEndpointPanel.add(serviceEndpointName,
                                 new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0,
                                                        GridBagConstraints.WEST,
                                                        GridBagConstraints.HORIZONTAL,
                                                        new Insets(0, 0, 0, 0), 0, 0));

        serviceEndpointPanel.add(getChangeButton(),
                                 new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
                                                        GridBagConstraints.EAST,
                                                        GridBagConstraints.NONE,
                                                        new Insets(0, 0, 0, 0), 0, 0));


        return serviceEndpointPanel;
    }

    private JButton getChangeButton() {
        if (changeButton == null) {
            changeButton = new JButton("Change Endpoint");
            changeButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    runChangeEndpointConnectionWindow();
                }
            });
        }
        return changeButton;
    }

    /**
     * Run the "Change Endpoint" dialog sequence.
     */
    private void runChangeEndpointConnectionWindow() {
        ChangeEndpointConnectionWindow checw = new ChangeEndpointConnectionWindow();
        Utilities.centerOnScreen(checw);
        checw.show();
    }

    private JPanel getSamlAssertionPanel() {
        JPanel samlPanel = new JPanel();
        samlPanel.setLayout(new BoxLayout(samlPanel, BoxLayout.Y_AXIS));
        samlPanel.setBorder(new EmptyBorder(new Insets(5, 5, 5, 0)));

        JPanel expiresPanel = new JPanel();
        expiresPanel.setLayout(new BoxLayout(expiresPanel, BoxLayout.X_AXIS));

        expirySpinner = new JSpinner();
        Integer value = new Integer(5);
        Integer min = new Integer(1);
        Integer max = new Integer(120);
        Integer step = new Integer(1);
        SpinnerNumberModel spinModel = new SpinnerNumberModel(value, min, max, step);
        expirySpinner.setModel(spinModel);
        expirySpinLabel = new JLabel("Ticket expiry (in minutes):");
        expirySpinLabel.setLabelFor(expirySpinner);

        expiresPanel.add(expirySpinLabel);
        expiresPanel.add(expirySpinner);
        expiresPanel.add(Box.createGlue());

        samlPanel.add(expiresPanel);
        samlPanel.add(Box.createRigidArea(new Dimension(20, 10)));

        JPanel includeGroupsPanel = new JPanel();
        includeGroupsPanel.setLayout(new BoxLayout(includeGroupsPanel, BoxLayout.X_AXIS));
        memebershipStatementCheck = new JCheckBox("Group membership statement");
        includeGroupsPanel.add(memebershipStatementCheck);
        includeGroupsPanel.add(Box.createGlue());
        samlPanel.add(includeGroupsPanel);
        samlPanel.add(Box.createGlue());
        return samlPanel;
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
            okButton = new JButton("Ok");

            // Register listener
            okButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    // copy view into model
                    if (serviceEndpoint == null) {
                        assertion.setEndpointOid(null);
                        assertion.setEndpointName(null);
                    } else {
                        assertion.setEndpointOid(new Long(serviceEndpoint.getOid()));
                        assertion.setEndpointName(serviceEndpoint.getName());
                    }

                    final Integer sv = (Integer)expirySpinner.getValue();
                    assertion.setSamlAssertionExpiry(sv.intValue());
                    assertion.setGroupMembershipStatement(memebershipStatementCheck.isSelected());
                    assertion.setAttachSamlSenderVouches(samlMethod.isSelected());
                    fireEventAssertionChanged(assertion);
                    JmsRoutingAssertionDialog.this.dispose();
                    wasOkButtonPressed = true;
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
                public void actionPerformed(ActionEvent e) {
                    JmsRoutingAssertionDialog.this.dispose();
                }
            });
        }
        // Return button
        return cancelButton;
    }

    private void initFormData() {
        Long endpointOid = assertion.getEndpointOid();
        try {
            if (endpointOid != null)
                serviceEndpoint = Registry.getDefault().getJmsManager().findEndpointByPrimaryKey(endpointOid.longValue());
        } catch (Exception e) {
            throw new RuntimeException("Unable to look up JMS Endpoint for this routing assertion", e);
        }

        if (serviceEndpoint != null)
            serviceEndpointName.setText(serviceEndpoint.getName());

        memebershipStatementCheck.setSelected(assertion.isGroupMembershipStatement());
        int expiry = assertion.getSamlAssertionExpiry();
        if (expiry == 0) {
            expiry = 5;
        }
        expirySpinner.setValue(new Integer(expiry));
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                boolean saml = assertion.isAttachSamlSenderVouches();
                samlMethod.setSelected(saml);
                passwordMethod.setSelected(!saml);
            }
        });
    }

    private void changeJmsEndpoint(JmsEndpoint endpoint) {
        serviceEndpoint = endpoint;
        serviceEndpointName.setText(endpoint.getName());
    }

    private class ChangeEndpointConnectionWindow extends JDialog {
        ChangeEndpointConnectionWindow() {
            super(JmsRoutingAssertionDialog.this, "Select JMS Connection", true);

            final JButton okButton = new JButton("Ok");
            final JButton cancelButton = new JButton("Cancel");
            final JmsConnectionListPanel jpl = new JmsConnectionListPanel(JmsRoutingAssertionDialog.this);

            jpl.addJmsConnectionListSelectionListener(new JmsConnectionListPanel.JmsConnectionListSelectionListener() {
                public void onSelected(JmsConnection selected) {
                    okButton.setEnabled(selected != null);
                }
            });

            okButton.setEnabled(false);
            okButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    JmsConnection connection = jpl.getSelectedJmsConnection();
                    if (connection == null)
                        return;
                    try {
                        ChangeEndpointConnectionWindow.this.hide();
                        ChangeEndpointConnectionWindow.this.dispose();
                        ChangeEndpointWindow chew = new ChangeEndpointWindow(connection);
                        Utilities.centerOnScreen(chew);
                        chew.show();
                    } catch (Exception e1) {
                        throw new RuntimeException("Unable to use this JMS connection", e1);
                    }
                }
            });

            cancelButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    ChangeEndpointConnectionWindow.this.hide();
                    ChangeEndpointConnectionWindow.this.dispose();
                }
            });

            Container p = getContentPane();
            p.setLayout(new GridBagLayout());
            p.add(new JLabel("Which JMS Connection will be providing the JMS Endpoint?"),
                  new GridBagConstraints(0, 0, 3, 1, 0.0, 0.0,
                                         GridBagConstraints.WEST,
                                         GridBagConstraints.NONE,
                                         new Insets(15, 15, 0, 15), 0, 0));

            p.add(jpl,
                  new GridBagConstraints(0, 1, 3, 1, 1.0, 1.0,
                                         GridBagConstraints.CENTER,
                                         GridBagConstraints.BOTH,
                                         new Insets(0, 0, 0, 0), 0, 0));

            p.add(Box.createGlue(),
                  new GridBagConstraints(0, 2, 1, 1, 100.0, 1.0,
                                         GridBagConstraints.EAST,
                                         GridBagConstraints.HORIZONTAL,
                                         new Insets(0, 0, 0, 0), 0, 0));

            p.add(okButton,
                  new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0,
                                         GridBagConstraints.EAST,
                                         GridBagConstraints.NONE,
                                         new Insets(0, 0, 11, 5), 0, 0));

            p.add(cancelButton,
                  new GridBagConstraints(2, 2, 1, 1, 0.0, 0.0,
                                         GridBagConstraints.EAST,
                                         GridBagConstraints.NONE,
                                         new Insets(0, 0, 11, 11), 0, 0));

            Utilities.equalizeButtonSizes(new JButton[] { okButton, cancelButton });

            pack();
        }
    }

    private class ChangeEndpointWindow extends JDialog {
        ChangeEndpointWindow(JmsConnection connection) {
            super(JmsRoutingAssertionDialog.this, "Select JMS Endpoint", true);

            final JButton okButton = new JButton("Ok");
            final JButton cancelButton = new JButton("Cancel");
            final JmsEndpointListPanel jpl = new JmsEndpointListPanel(JmsRoutingAssertionDialog.this, connection);

            jpl.addJmsEndpointListSelectionListener(new JmsEndpointListPanel.JmsEndpointListSelectionListener() {
                public void onSelected(JmsEndpoint selected) {
                    okButton.setEnabled(selected != null);
                }
            });

            okButton.setEnabled(false);
            okButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    JmsEndpoint endpoint = jpl.getSelectedJmsEndpoint();
                    if (endpoint == null)
                        return;
                    try {
                        ChangeEndpointWindow.this.hide();
                        ChangeEndpointWindow.this.dispose();
                        changeJmsEndpoint(endpoint);
                    } catch (Exception e1) {
                        throw new RuntimeException("Unable to use this JMS Endpoint", e1);
                    }
                }
            });

            cancelButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    ChangeEndpointWindow.this.hide();
                    ChangeEndpointWindow.this.dispose();
                }
            });

            Container p = getContentPane();
            p.setLayout(new GridBagLayout());
            p.add(new JLabel("Which JMS Endpoint on this Connection leads to the protected service?"),
                  new GridBagConstraints(0, 0, 3, 1, 0.0, 0.0,
                                         GridBagConstraints.WEST,
                                         GridBagConstraints.NONE,
                                         new Insets(15, 15, 0, 15), 0, 0));

            p.add(jpl,
                  new GridBagConstraints(0, 1, 3, 1, 1.0, 1.0,
                                         GridBagConstraints.CENTER,
                                         GridBagConstraints.BOTH,
                                         new Insets(0, 0, 0, 0), 0, 0));

            p.add(Box.createGlue(),
                  new GridBagConstraints(0, 2, 1, 1, 100.0, 1.0,
                                         GridBagConstraints.EAST,
                                         GridBagConstraints.HORIZONTAL,
                                         new Insets(0, 0, 0, 0), 0, 0));

            p.add(okButton,
                  new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0,
                                         GridBagConstraints.EAST,
                                         GridBagConstraints.NONE,
                                         new Insets(0, 0, 11, 5), 0, 0));

            p.add(cancelButton,
                  new GridBagConstraints(2, 2, 1, 1, 0.0, 0.0,
                                         GridBagConstraints.EAST,
                                         GridBagConstraints.NONE,
                                         new Insets(0, 0, 11, 11), 0, 0));

            Utilities.equalizeButtonSizes(new JButton[] { okButton, cancelButton });

            pack();
        }
    }
}
