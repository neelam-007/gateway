package com.l7tech.console.panels;

import com.l7tech.console.event.PolicyEvent;
import com.l7tech.console.event.PolicyListener;
import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.xmlsec.XmlDsigAssertion;
import com.l7tech.policy.assertion.xmlsec.XmlEncAssertion;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.EventListenerList;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.EventListener;
import java.util.logging.Logger;


/**
 * <code>XmlEncAssertionDialog</code> is the XML digital signature
 * assertion edit dialog.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class XmlEncAssertionDialog extends JDialog {
    static final Logger log = Logger.getLogger(XmlEncAssertionDialog.class.getName());

    private XmlEncAssertion assertion;
    private JButton cancelButton;
    private JPanel buttonPanel;
    private JButton okButton;
    private EventListenerList listenerList = new EventListenerList();

    /** Creates new form ServicePanel */
    public XmlEncAssertionDialog(Frame owner, XmlEncAssertion a) {
        super(owner, true);
        setTitle("XML Encryption");
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
                  PolicyEvent(this, new AssertionPath(a.getPath()), indices, new Assertion[] {a});
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
        JPanel editXmlEncPanel = new JPanel();

        getContentPane().setLayout(new BorderLayout());
        getDescriptionPanel().setBorder(BorderFactory.createEmptyBorder(10, 5, 5, 10));
        getContentPane().add(getDescriptionPanel(), BorderLayout.NORTH);

        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(new EmptyBorder(new Insets(10, 10, 10, 10)));


        editXmlEncPanel.setLayout(new GridBagLayout());
        editXmlEncPanel.setBorder(BorderFactory.createTitledBorder(""));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new Insets(0, 0, 20, 0);
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        editXmlEncPanel.add(getEncDirectionEditPanel(), gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        editXmlEncPanel.add(Box.createGlue(), gridBagConstraints);

        mainPanel.add(editXmlEncPanel);

        // Add buttonPanel
        mainPanel.add(getButtonPanel());


        getContentPane().add(mainPanel, BorderLayout.CENTER);
    }

    private JPanel getDescriptionPanel() {
        if (descriptionPanel !=null)
            return descriptionPanel;

        descriptionPanel = new JPanel();
        descriptionPanel.setLayout(new BoxLayout(descriptionPanel, BoxLayout.X_AXIS));

        JLabel descLabel = new JLabel();
        descLabel.setText("Specify the XML encryption direction");
        descriptionPanel.add(descLabel);

        return descriptionPanel;
    }


    private JPanel getEncDirectionEditPanel() {
        JPanel directionPanel = new JPanel();

        directionPanel.setLayout(new BoxLayout(directionPanel, BoxLayout.Y_AXIS));
        directionPanel.setBorder(new EmptyBorder(new Insets(5, 5, 5, 0)));

        JPanel incomingMessagePanel = new JPanel();
        incomingMessagePanel.setLayout(new BoxLayout(incomingMessagePanel, BoxLayout.X_AXIS));
        encInRadioButton = new JRadioButton("Incoming message");

        incomingMessagePanel.add(encInRadioButton);

        incomingMessagePanel.add(Box.createGlue());
        directionPanel.add(incomingMessagePanel);
        directionPanel.add(Box.createRigidArea(new Dimension(20, 10)));


        JPanel outgoingMessagePanel = new JPanel();
        outgoingMessagePanel.setLayout(new BoxLayout(outgoingMessagePanel, BoxLayout.X_AXIS));

        encOutRadioButton = new JRadioButton("Outgoing message");
        outgoingMessagePanel.add(encOutRadioButton);

        outgoingMessagePanel.add(Box.createGlue());
        directionPanel.add(outgoingMessagePanel);

        directionPanel.add(Box.createRigidArea(new Dimension(20, 10)));


        JPanel inOutMessagePanel = new JPanel();
        inOutMessagePanel.setLayout(new BoxLayout(inOutMessagePanel, BoxLayout.X_AXIS));

        encInOutRadioButton = new JRadioButton("In/Out message");

        inOutMessagePanel.add(encInOutRadioButton);
        inOutMessagePanel.add(Box.createGlue());
        directionPanel.add(inOutMessagePanel);


        Utilities.equalizeComponentWidth(
          new JComponent[]{encInOutRadioButton,
                           encOutRadioButton,
                           encInRadioButton});

        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(encOutRadioButton);
        buttonGroup.add(encInRadioButton);
        buttonGroup.add(encInOutRadioButton);
        SwingUtilities.invokeLater( new Runnable() {
            public void run() {
                updateRadioButtons();
            }
        });

        return directionPanel;
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
            okButton = new JButton("Save");

            // Register listener
            okButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    updateAssertion();
                    fireEventAssertionChanged(assertion);
                    XmlEncAssertionDialog.this.dispose();
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
            cancelButton = new JButton("cancel");

            // Register listener
            cancelButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    XmlEncAssertionDialog.this.dispose();
                }
            });
        }
        // Return button
        return cancelButton;
    }

    private void updateRadioButtons() {
        int d = assertion.getDirection();
        if (d == XmlDsigAssertion.IN) {
            encInRadioButton.setSelected(true);
        } else if(d == XmlDsigAssertion.OUT) {
            encOutRadioButton.setSelected(true);
        } else if (d == XmlDsigAssertion.INOUT) {
            encInOutRadioButton.setSelected(true);
        } else {
            log.warning("Uknonwn direction value "+d);
        }

    }

    private void updateAssertion() {
        if (encInRadioButton.isSelected()) {
            assertion.setDirection(XmlDsigAssertion.IN);
        } else if (encOutRadioButton.isSelected()) {
            assertion.setDirection(XmlDsigAssertion.OUT);
        } else if (encInOutRadioButton.isSelected()) {
            assertion.setDirection(XmlDsigAssertion.INOUT);
        }
    }


    private JRadioButton encInRadioButton;
    private JRadioButton encOutRadioButton;
    private JRadioButton encInOutRadioButton;

    private JPanel descriptionPanel;
    private JPanel mainPanel;


}
