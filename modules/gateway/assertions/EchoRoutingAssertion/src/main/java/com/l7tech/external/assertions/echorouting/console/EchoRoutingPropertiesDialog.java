package com.l7tech.external.assertions.echorouting.console;

import com.l7tech.console.event.PolicyEvent;
import com.l7tech.console.event.PolicyListener;
import com.l7tech.console.panels.AssertionPropertiesEditorSupport;
import com.l7tech.console.panels.RoutingDialogUtils;
import com.l7tech.external.assertions.echorouting.EchoRoutingAssertion;
import com.l7tech.gui.util.Utilities;
import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;

import javax.swing.*;
import javax.swing.event.EventListenerList;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.EventListener;


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
    public EchoRoutingPropertiesDialog(Window owner, EchoRoutingAssertion a) {
        super(owner, a);
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
    private JRadioButton wssIgnoreRadio;
    private JRadioButton wssCleanupRadio;
    private JRadioButton wssRemoveRadio;

    private AbstractButton[] secHdrButtons = {wssIgnoreRadio, wssCleanupRadio, wssRemoveRadio, null };

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
              @Override
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
        for (AbstractButton button : secHdrButtons)
            buttonGroup.add(button);
        RoutingDialogUtils.tagSecurityHeaderHandlingButtons(secHdrButtons);

        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                getData(assertion);
                fireEventAssertionChanged(assertion);
                wasOkButtonPressed = true;
                dispose();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                EchoRoutingPropertiesDialog.this.dispose();
            }
        });
    }

    private void initFormData() {
        RoutingDialogUtils.configSecurityHeaderRadioButtons(assertion, -1, null, secHdrButtons);
    }

    @Override
    public boolean isConfirmed() {
        return wasOkButtonPressed;
    }

    @Override
    public void setData(EchoRoutingAssertion assertion) {
        this.assertion = assertion;
        initFormData();
    }

    @Override
    public EchoRoutingAssertion getData(EchoRoutingAssertion assertion) {
        RoutingDialogUtils.configSecurityHeaderHandling(assertion, -1, secHdrButtons);
        assertion.setGroupMembershipStatement(false);
        assertion.setAttachSamlSenderVouches(false);
        return assertion;
    }

    @Override
    protected void configureView() {
        okButton.setEnabled( !isReadOnly() );
    }
}
