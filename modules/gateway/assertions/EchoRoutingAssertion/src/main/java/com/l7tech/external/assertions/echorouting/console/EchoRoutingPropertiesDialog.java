package com.l7tech.external.assertions.echorouting.console;

import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.EventListener;
import javax.swing.*;
import javax.swing.event.EventListenerList;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.AssertionPath;
import com.l7tech.console.event.PolicyListener;
import com.l7tech.console.event.PolicyEvent;
import com.l7tech.console.panels.AssertionPropertiesEditorSupport;
import com.l7tech.gui.util.Utilities;
import com.l7tech.external.assertions.echorouting.EchoRoutingAssertion;


/**
 * Dialog for editing the EchoRoutingAssertion.
 *
 * @author $Author$
 * @version $Revision$
 */
public class EchoRoutingPropertiesDialog extends AssertionPropertiesEditorSupport<EchoRoutingAssertion> {

    //- PUBLIC

    /**
     * Creates new form ServicePanel
     * @param owner  parent for dialog
     * @param a      assertion to edit
     */
    public EchoRoutingPropertiesDialog(Frame owner, EchoRoutingAssertion a) {
        super(owner, "Echo Routing Properties", true);
        assertion = a;
        initComponents();
        initFormData();
    }

    /**
     * @return true unless the dialog was exited via the OK button.
     */
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

    //- PRIVATE

    // model, etc
    private EchoRoutingAssertion assertion;
    private boolean wasOkButtonPressed = false;
    private EventListenerList listenerList = new EventListenerList();

    // form items
    private JPanel mainPanel;
    private JButton okButton;
    private JButton cancelButton;
    private JRadioButton securityHeaderRemoveRadioButton;
    private JRadioButton securityHeaderLeaveRadioButton;

    /**
     * Notify the listeners
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
                  for (EventListener listener : listeners) {
                      ((PolicyListener)listener).assertionsChanged(event);
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

        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(securityHeaderRemoveRadioButton);
        buttonGroup.add(securityHeaderLeaveRadioButton);

        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                getData(assertion);
                fireEventAssertionChanged(assertion);
                wasOkButtonPressed = true;
                dispose();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                EchoRoutingPropertiesDialog.this.dispose();
            }
        });
    }

    private void initFormData() {
        if (assertion.getCurrentSecurityHeaderHandling() == EchoRoutingAssertion.REMOVE_CURRENT_SECURITY_HEADER)
            securityHeaderRemoveRadioButton.setSelected(true);
        else
            securityHeaderLeaveRadioButton.setSelected(true);
    }

    public boolean isConfirmed() {
        return wasOkButtonPressed;
    }

    public void setData(EchoRoutingAssertion assertion) {
        this.assertion = assertion;
        initFormData();
    }

    public EchoRoutingAssertion getData(EchoRoutingAssertion assertion) {
        // copy view into model
        if (securityHeaderRemoveRadioButton.isSelected())
            assertion.setCurrentSecurityHeaderHandling(EchoRoutingAssertion.REMOVE_CURRENT_SECURITY_HEADER);
        else
            assertion.setCurrentSecurityHeaderHandling(EchoRoutingAssertion.LEAVE_CURRENT_SECURITY_HEADER_AS_IS);
        assertion.setGroupMembershipStatement(false);
        assertion.setAttachSamlSenderVouches(false);
        return assertion;
    }

    @Override
    protected void configureView() {
        okButton.setEnabled( !isReadOnly() );
    }
}
