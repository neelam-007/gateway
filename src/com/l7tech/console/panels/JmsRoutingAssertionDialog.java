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
import javax.swing.event.EventListenerList;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.EventListener;
import java.util.logging.Logger;
import java.util.logging.Level;


/**
 * <code>JmsRoutingAssertionDialog</code> is the protected service
 * policy edit dialog for JMS routing assertions.
 *
 * @author <a href="mailto:mlyons@layer7-tech.com">Mike Lyons</a>
 * @version 1.0
 */
public class JmsRoutingAssertionDialog extends JDialog {

    //- PUBLIC

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

    public void dispose() {
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
            logger.log(Level.WARNING, "Unable to roll back newly-created JMS Queue", e);
        }
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(JmsRoutingAssertionDialog.class.getName());

    // model, etc
    private JmsRoutingAssertion assertion;
    private boolean wasOkButtonPressed = false;
    private EventListenerList listenerList = new EventListenerList();

    private JmsConnection newlyCreatedConnection = null;
    private JmsEndpoint newlyCreatedEndpoint = null;
    private JmsUtilities.QueueItem[] queueItems;

    // form items
    private JPanel mainPanel;
    private JButton okButton;
    private JButton cancelButton;
    private JButton newQueueButton;
    private JComboBox queueComboBox;
    private JRadioButton securityHeaderRemoveRadioButton;
    private JRadioButton securityHeaderLeaveRadioButton;

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

        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(mainPanel, BorderLayout.CENTER);
        Utilities.setEscKeyStrokeDisposes(this);

        queueComboBox.setModel(new DefaultComboBoxModel(getQueueItems()));

        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(securityHeaderRemoveRadioButton);
        buttonGroup.add(securityHeaderLeaveRadioButton);

        newQueueButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JmsEndpoint ep = newlyCreatedEndpoint;
                JmsConnection conn = newlyCreatedConnection;
                JmsQueuePropertiesDialog pd = JmsQueuePropertiesDialog.createInstance(getOwner(), conn, ep, true);
                Utilities.centerOnScreen(pd);
                pd.setVisible(true);
                if (!pd.isCanceled()) {
                    newlyCreatedEndpoint = pd.getEndpoint();
                    newlyCreatedConnection = pd.getConnection();
                    getQueueComboBox().setModel(new DefaultComboBoxModel(loadQueueItems()));
                    JmsUtilities.selectEndpoint(getQueueComboBox(), newlyCreatedEndpoint);
                }
            }
        });

        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // copy view into model
                if (securityHeaderRemoveRadioButton.isSelected())
                    assertion.setCurrentSecurityHeaderHandling(JmsRoutingAssertion.REMOVE_CURRENT_SECURITY_HEADER);
                else
                    assertion.setCurrentSecurityHeaderHandling(JmsRoutingAssertion.LEAVE_CURRENT_SECURITY_HEADER_AS_IS);

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
                dispose();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                JmsRoutingAssertionDialog.this.dispose();
            }
        });
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
        return queueComboBox;
    }

    private void initFormData() {
        if (assertion.getCurrentSecurityHeaderHandling() == JmsRoutingAssertion.REMOVE_CURRENT_SECURITY_HEADER)
            securityHeaderRemoveRadioButton.setSelected(true);
        else
            securityHeaderLeaveRadioButton.setSelected(true);

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
