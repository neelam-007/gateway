package com.l7tech.console.panels;

import com.l7tech.console.event.PolicyEvent;
import com.l7tech.console.event.PolicyListener;
import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.RoutingAssertion;
import com.l7tech.common.gui.util.Utilities;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.EventListenerList;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.EventListener;


/**
 * <code>RoutingAssertionDialog</code> is the protected service
 * policy edit dialog.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class RoutingAssertionDialog extends JDialog {
    private JCheckBox anonymousAccessCheckBox;
    private RoutingAssertion assertion;
    private JButton cancelButton;
    private JPanel buttonPanel;
    private JButton okButton;
    private EventListenerList listenerList = new EventListenerList();

    /** Creates new form ServicePanel */
    public RoutingAssertionDialog(Frame owner, RoutingAssertion a) {
        super(owner, true);
        setTitle("Edit Routing Assertion");
        assertion = a;
        initComponents();
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
     * @param a the assertion
     */
    private void fireEventAssertionChanged(final Assertion a) {
        SwingUtilities.invokeLater(
          new Runnable() {
              public void run() {
                  int[] indices = new int[a.getParent().getChildren().indexOf(a)];
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
        GridBagConstraints gridBagConstraints;
        mainPanel = new JPanel();
        credentialsAndTransportPanel = new JPanel();

        getContentPane().setLayout(new BorderLayout());
        getServiceUrlPanel().setBorder(BorderFactory.createEmptyBorder(10, 5, 5, 10));
        getContentPane().add(getServiceUrlPanel(), BorderLayout.NORTH);

        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(new EmptyBorder(new Insets(10, 10, 10, 10)));


        credentialsAndTransportPanel.setLayout(new GridBagLayout());
        credentialsAndTransportPanel.setBorder(BorderFactory.createTitledBorder("Protected service authentication"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new Insets(0, 0, 20, 0);
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        credentialsAndTransportPanel.add(getCredentialsPanel(), gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        credentialsAndTransportPanel.add(Box.createGlue(), gridBagConstraints);

        mainPanel.add(credentialsAndTransportPanel);

        // Add buttonPanel
        mainPanel.add(getButtonPanel());


        getContentPane().add(mainPanel, BorderLayout.CENTER);
    }

    /**
     * get the credentials that were entered.
     *
     * todo: deal with certificates too
     * @return the credentialsd byte array
     */
    private byte[] getCredentials() {
        char[] cpass = identityPasswordField.getPassword();
        return String.valueOf(cpass).getBytes();
    }

    private JPanel getServiceUrlPanel() {
        if (serviceUrlPanel != null)
            return serviceUrlPanel;

        serviceUrlPanel = new JPanel();
        serviceUrlPanel.setLayout(new BoxLayout(serviceUrlPanel, BoxLayout.X_AXIS));

        JLabel serviceUrlLabel = new JLabel();
        serviceUrlLabel.setText("Service URL");
        serviceUrlLabel.setBorder(new EmptyBorder(new Insets(1, 1, 1, 5)));
        serviceUrlPanel.add(serviceUrlLabel);

        serviceUrlTextField = new JTextField();
        serviceUrlTextField.setText(assertion.getProtectedServiceUrl());
        serviceUrlTextField.setPreferredSize(new Dimension(200, 20));
        serviceUrlPanel.add(serviceUrlTextField);

        JButton buttonDefaultUrl = new JButton();
        buttonDefaultUrl.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                 serviceUrlTextField.setText(assertion.getProtectedServiceUrl());
            }
        });
        buttonDefaultUrl.setText("Reset");
        serviceUrlPanel.add(buttonDefaultUrl);

        return serviceUrlPanel;
    }


    private JComboBox getAuthenticationMethodComboBox() {
        if (authenticationMethodComboBox != null)
            return authenticationMethodComboBox;
        authenticationMethodComboBox =
          Components.getCredentialsLocationComboBox();

        return authenticationMethodComboBox;
    }

    private JPanel getCredentialsPanel() {
        JPanel credentialsPanel = new JPanel();

        credentialsPanel.setLayout(new BoxLayout(credentialsPanel, BoxLayout.Y_AXIS));
        credentialsPanel.setBorder(new EmptyBorder(new Insets(5, 5, 5, 0)));

        JPanel authMethodPanel = new JPanel();
        authMethodPanel.setLayout(new BoxLayout(authMethodPanel, BoxLayout.X_AXIS));
        JLabel credentialsLabel = new JLabel();
        credentialsLabel.setText("Credentials");
        authMethodPanel.add(credentialsLabel);
        authMethodPanel.add(Box.createRigidArea(new Dimension(20, 10)));

//        final JComboBox acBox = getAuthenticationMethodComboBox();
//        acBox.addActionListener( new ActionListener() {
//            public void actionPerformed(ActionEvent e) {
//                boolean enable = !isAnonymous();
//                identityTextField.setEnabled(enable);
//                identityPasswordField.setEnabled(enable);
//                realmTextField.setEnabled(enable);
//            }
//        });
     //   authMethodPanel.add(Box.createGlue());
        // default disable
//             SwingUtilities.invokeLater(new Runnable() {
//                 public void run() {
//                     acBox.setSelectedIndex(0);
//                 }
//             });

   //     authMethodPanel.add(Box.createRigidArea(new Dimension(20, 10)));

   //     authMethodPanel.add(Box.createGlue());
        credentialsPanel.add(authMethodPanel);

        credentialsPanel.add(Box.createRigidArea(new Dimension(20, 10)));

        JPanel identityPanel = new JPanel();
        identityPanel.setLayout(new BoxLayout(identityPanel, BoxLayout.X_AXIS));
        JLabel identityLabel = new JLabel();
        identityLabel.setText("Identity");
        identityPanel.add(identityLabel);

        identityPanel.add(Box.createRigidArea(new Dimension(20, 10)));

        identityTextField = new JTextField();
        identityTextField.setText(assertion.getLogin());
        identityTextField.setPreferredSize(new Dimension(50, 20));
        identityPanel.add(identityTextField);

        identityPanel.add(Box.createGlue());
        credentialsPanel.add(identityPanel);

        credentialsPanel.add(Box.createRigidArea(new Dimension(20, 10)));


        JPanel passwordPanel = new JPanel();
        passwordPanel.setLayout(new BoxLayout(passwordPanel, BoxLayout.X_AXIS));

        JLabel passwordLabel = new JLabel();
        passwordLabel.setText("Password");
        passwordPanel.add(passwordLabel);

        passwordPanel.add(Box.createRigidArea(new Dimension(20, 10)));

        identityPasswordField = new JPasswordField();
        identityPasswordField.setPreferredSize(new Dimension(50, 20));
        passwordPanel.add(identityPasswordField);
        passwordPanel.add(Box.createGlue());
        credentialsPanel.add(passwordPanel);

        credentialsPanel.add(Box.createRigidArea(new Dimension(20, 10)));

        JPanel realmPanel = new JPanel();
        realmPanel.setLayout(new BoxLayout(realmPanel, BoxLayout.X_AXIS));
        JLabel realmLabel = new JLabel();
        realmLabel.setText("Realm");
        realmPanel.add(realmLabel);

        realmPanel.add(Box.createRigidArea(new Dimension(20, 10)));

        realmTextField = new JTextField();
        realmTextField.setText(assertion.getRealm());
        realmTextField.setPreferredSize(new Dimension(50, 20));
        realmPanel.add(realmTextField);

        realmPanel.add(Box.createGlue());
        credentialsPanel.add(realmPanel);


        Utilities.equalizeComponentSizes(
          new JComponent[]{credentialsLabel,
                           realmLabel,
                           identityLabel,
                           passwordLabel});


        Utilities.equalizeComponentWidth(
          new JComponent[]{identityPasswordField,
                           realmTextField,
                           identityTextField,
                           getAuthenticationMethodComboBox()});

        return credentialsPanel;
    }


    /** Returns buttonPanel */
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


    /** Returns okButton */
    private JButton getOKButton() {
        // If button not already created
        if (null == okButton) {
            // Create button
            okButton = new JButton("Ok");

            // Register listener
            okButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    assertion.setProtectedServiceUrl(serviceUrlTextField.getText());
                    assertion.setLogin(identityTextField.getText());
                    assertion.setPassword(new String(getCredentials()));
                    assertion.setRealm(realmTextField.getText());
                    fireEventAssertionChanged(assertion);
                    RoutingAssertionDialog.this.dispose();
                }
            });
        }

        // Return button
        return okButton;
    }

    /** Returns cancelButton */
    private JButton getCancelButton() {
        // If button not already created
        if (null == cancelButton) {

            // Create button
            cancelButton = new JButton("Cancel");

            // Register listener
            cancelButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    RoutingAssertionDialog.this.dispose();
                }
            });
        }
        // Return button
        return cancelButton;
    }

    /**
     * @return whether the selection is anonymou
     */
    private boolean isAnonymous() {
        String name = (String)authenticationMethodComboBox.getSelectedItem();
        return "Anonymous".equals(name);
    }


    private JComboBox authenticationMethodComboBox;
    private JTextField identityTextField;
    private JTextField realmTextField;
    private JPasswordField identityPasswordField;
    private JPasswordField confitmPasswordField;
    private JPanel credentialsAndTransportPanel;
    private JPanel serviceUrlPanel;
    private JTextField serviceUrlTextField;
    private JPanel mainPanel;


}
