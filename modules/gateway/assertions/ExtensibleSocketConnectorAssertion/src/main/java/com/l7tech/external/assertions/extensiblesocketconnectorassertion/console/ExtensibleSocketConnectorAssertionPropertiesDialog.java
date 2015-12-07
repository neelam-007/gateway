package com.l7tech.external.assertions.extensiblesocketconnectorassertion.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.panels.TargetVariablePanel;
import com.l7tech.console.policy.SsmPolicyVariableUtils;
import com.l7tech.console.util.Registry;
import com.l7tech.external.assertions.extensiblesocketconnectorassertion.ExtensibleSocketConnectorAssertion;
import com.l7tech.external.assertions.extensiblesocketconnectorassertion.ExtensibleSocketConnectorEntity;
import com.l7tech.external.assertions.extensiblesocketconnectorassertion.ExtensibleSocketConnectorEntityAdmin;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.MessageTargetableSupport;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.VariableMetadata;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 08/12/11
 * Time: 2:02 PM
 * To change this template use File | Settings | File Templates.
 */
public class ExtensibleSocketConnectorAssertionPropertiesDialog extends AssertionPropertiesOkCancelSupport<ExtensibleSocketConnectorAssertion> {
    private JPanel mainPanel;
    private JComboBox socketConnectorComboBox;
    private JComboBox requestTargetComboBox;
    private JComboBox responseTargetComboBox;
    private JTextField responseTargetField;
    private TargetVariablePanel sessionIdVariable;
    private TargetVariablePanel sessionIdStoreVariable;
    private JCheckBox failOnNoSession;
    private JPanel optionsPanel;
    private JPanel mandatory;

    private static class SocketConnectorEntry {
        private String name;
        private Goid goid;

        public SocketConnectorEntry(String name, Goid goid) {
            this.name = name;
            this.goid = goid;
        }

        public Goid getGoid() {
            return goid;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static class RequestSourceComboBoxItem {
        private boolean undefined = false;

        private final MessageTargetableSupport target;

        public RequestSourceComboBoxItem(MessageTargetableSupport target) {
            this.target = target;
        }

        public MessageTargetableSupport getTarget() {
            return target;
        }

        public void setUndefined(boolean undefined) {
            this.undefined = undefined;
        }

        public boolean isUndefined() {
            return undefined;
        }

        @Override
        public String toString() {
            return (undefined ? "Undefined: " : "") + target.getTargetName();
        }
    }

    public ExtensibleSocketConnectorAssertionPropertiesDialog(Window owner, ExtensibleSocketConnectorAssertion assertion) {
        super(assertion.getClass(), owner, assertion, true);
        initComponents();
    }

    @Override
    protected void initComponents() {
        try {
            Collection<ExtensibleSocketConnectorEntity> configs = getEntityManager().findAll();
            Vector<SocketConnectorEntry> entries = new Vector<SocketConnectorEntry>();

            for (ExtensibleSocketConnectorEntity config : configs) {
                if (!config.isIn()) {
                    entries.add(new SocketConnectorEntry(config.getName(), config.getGoid()));
                }
            }

            socketConnectorComboBox.setModel(new DefaultComboBoxModel(entries));
        } catch (FindException e) {
            //
        }

        responseTargetComboBox.setModel(new DefaultComboBoxModel(TargetMessageType.values()));
        responseTargetComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                responseTargetField.setEnabled(TargetMessageType.OTHER == responseTargetComboBox.getSelectedItem());
            }
        });

        sessionIdVariable.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {

                if (sessionIdVariable.getVariable().trim().isEmpty()) {

                    if (failOnNoSession.isEnabled()) {
                        failOnNoSession.setSelected(false);
                        failOnNoSession.setEnabled(false);
                    }

                } else {

                    if (!failOnNoSession.isEnabled()) {
                        failOnNoSession.setEnabled(true);
                    }
                }

            }
        });

        super.initComponents();
    }

    @Override
    protected JPanel createPropertyPanel() {
        return mainPanel;
    }

    @Override
    public void setData(ExtensibleSocketConnectorAssertion assertion) {
        if (assertion.getSocketConnectorGoid() != null) {
            for (int i = 0; i < socketConnectorComboBox.getItemCount(); i++) {
                SocketConnectorEntry entry = (SocketConnectorEntry) socketConnectorComboBox.getItemAt(i);

                if (assertion.getSocketConnectorGoid().equals(entry.getGoid())) {
                    socketConnectorComboBox.setSelectedIndex(i);
                    break;
                }
            }
        }

        populateReqMsgSrcComboBox(assertion);

        if (assertion.getResponseTarget() != null) {
            responseTargetComboBox.setSelectedItem(assertion.getResponseTarget().getTarget());
            if (TargetMessageType.OTHER == assertion.getResponseTarget().getTarget()) {
                responseTargetField.setText(assertion.getResponseTarget().getOtherTargetMessageVariable() == null ? "" :
                        assertion.getResponseTarget().getOtherTargetMessageVariable());
            }
            responseTargetField.setEnabled(TargetMessageType.OTHER == assertion.getResponseTarget().getTarget());
        } else {
            responseTargetComboBox.setSelectedItem(TargetMessageType.RESPONSE);
            responseTargetField.setEnabled(false);
        }

        sessionIdVariable.setVariable(assertion.getSessionIdVariable());
        sessionIdStoreVariable.setVariable(assertion.getSessionIdStoreVariable());
        failOnNoSession.setSelected(assertion.isFailOnNoSession());
    }

    @Override
    public ExtensibleSocketConnectorAssertion getData(ExtensibleSocketConnectorAssertion assertion) throws ValidationException {
        if (socketConnectorComboBox.getSelectedItem() == null) {
            throw new ValidationException("No socket connector was selected.");
        }

        assertion.setSocketConnectorGoid(((SocketConnectorEntry) socketConnectorComboBox.getSelectedItem()).getGoid());

        assertion.setRequestTarget(((RequestSourceComboBoxItem) requestTargetComboBox.getSelectedItem()).getTarget());

        MessageTargetableSupport mts = new MessageTargetableSupport((TargetMessageType) responseTargetComboBox.getSelectedItem());
        if (TargetMessageType.OTHER == responseTargetComboBox.getSelectedItem()) {
            mts.setOtherTargetMessageVariable(responseTargetField.getText().trim());
        }
        assertion.setResponseTarget(mts);

        assertion.setSessionIdVariable(sessionIdVariable.getVariable());
        assertion.setSessionIdStoreVariable(sessionIdStoreVariable.getVariable());
        assertion.setFailOnNoSession(failOnNoSession.isSelected());

        return assertion;
    }

    private void populateReqMsgSrcComboBox(ExtensibleSocketConnectorAssertion assertion) {
        requestTargetComboBox.removeAllItems();
        requestTargetComboBox.setSelectedIndex(-1);

        MessageTargetableSupport currentMessageSource = assertion.getRequestTarget();
        TargetMessageType sourceTarget = currentMessageSource != null ? currentMessageSource.getTarget() : null;
        String contextVariableSourceTarget = sourceTarget == TargetMessageType.OTHER ? currentMessageSource.getOtherTargetMessageVariable() : null;

        requestTargetComboBox.addItem(new RequestSourceComboBoxItem(new MessageTargetableSupport(TargetMessageType.REQUEST, false)));
        requestTargetComboBox.addItem(new RequestSourceComboBoxItem(new MessageTargetableSupport(TargetMessageType.RESPONSE, false)));

        if (sourceTarget == TargetMessageType.REQUEST)
            requestTargetComboBox.setSelectedIndex(0);
        else if (sourceTarget == TargetMessageType.RESPONSE)
            requestTargetComboBox.setSelectedIndex(1);

        final Map<String, VariableMetadata> predecessorVariables = SsmPolicyVariableUtils.getVariablesSetByPredecessors(assertion);
        final SortedSet<String> predecessorVariableNames = new TreeSet<String>(predecessorVariables.keySet());
        for (String variableName : predecessorVariableNames) {
            if (predecessorVariables.get(variableName).getType() == DataType.MESSAGE) {
                final RequestSourceComboBoxItem item = new RequestSourceComboBoxItem(new MessageTargetableSupport(variableName));
                requestTargetComboBox.addItem(item);
                if (variableName.equals(contextVariableSourceTarget)) {
                    requestTargetComboBox.setSelectedItem(item);
                }
            }
        }

        if (contextVariableSourceTarget != null && !predecessorVariableNames.contains(contextVariableSourceTarget)) {
            RequestSourceComboBoxItem current = new RequestSourceComboBoxItem(new MessageTargetableSupport(contextVariableSourceTarget));
            current.setUndefined(true);
            requestTargetComboBox.addItem(current);
            requestTargetComboBox.setSelectedItem(current);
        }
    }

    private static ExtensibleSocketConnectorEntityAdmin getEntityManager() {
        return Registry.getDefault().getExtensionInterface(ExtensibleSocketConnectorEntityAdmin.class, null);
    }
}
