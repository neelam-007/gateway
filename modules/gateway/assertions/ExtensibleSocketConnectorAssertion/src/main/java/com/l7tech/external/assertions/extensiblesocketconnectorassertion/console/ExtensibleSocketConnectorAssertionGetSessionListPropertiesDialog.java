package com.l7tech.external.assertions.extensiblesocketconnectorassertion.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.panels.TargetVariablePanel;
import com.l7tech.console.util.Registry;
import com.l7tech.external.assertions.extensiblesocketconnectorassertion.ExtensibleSocketConnectorEntity;
import com.l7tech.external.assertions.extensiblesocketconnectorassertion.ExtensibleSocketConnectorEntityAdmin;
import com.l7tech.external.assertions.extensiblesocketconnectorassertion.ExtensibleSocketConnectorGetSessionListAssertion;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;

/**
 * Created with IntelliJ IDEA.
 * User: abjorge
 * Date: 24/01/14
 * Time: 2:59 PM
 * To change this template use File | Settings | File Templates.
 */
public class ExtensibleSocketConnectorAssertionGetSessionListPropertiesDialog extends AssertionPropertiesOkCancelSupport<ExtensibleSocketConnectorGetSessionListAssertion> {
    private JPanel mainPanel;
    private JComboBox connectorDropDown;
    private TargetVariablePanel targetVariable;

    private static class SocketConnectorEntry {
        private String name = "";
        private Goid goid = Goid.DEFAULT_GOID;

        public SocketConnectorEntry(String name, Goid goid) {
            this.name = name;
            this.goid = goid;
        }

        public String getName() {
            return name;
        }

        public Goid getGoid() {
            return goid;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public ExtensibleSocketConnectorAssertionGetSessionListPropertiesDialog(Window owner, ExtensibleSocketConnectorGetSessionListAssertion assertion) {
        super(assertion.getClass(), owner, assertion, true);
        initComponents();
    }

    @Override
    protected void initComponents() {
        super.initComponents();    //To change body of overridden methods use File | Settings | File Templates.

        //set combo box list
        DefaultComboBoxModel<SocketConnectorEntry> defaultComboBoxModel = new DefaultComboBoxModel<SocketConnectorEntry>();
        try {
            Collection<ExtensibleSocketConnectorEntity> configs = getEntityManager().findAll();

            for (ExtensibleSocketConnectorEntity config : configs) {
                if (!config.isIn()) {
                    defaultComboBoxModel.addElement(new SocketConnectorEntry(config.getName(), config.getGoid()));
                }
            }

            connectorDropDown.setModel(defaultComboBoxModel);
        } catch (FindException e) {
            //
        }
    }

    @Override
    protected JPanel createPropertyPanel() {
        return mainPanel;
    }

    @Override
    public ExtensibleSocketConnectorGetSessionListAssertion getData(ExtensibleSocketConnectorGetSessionListAssertion assertion) throws ValidationException {

        //save selected socket connector
        if (connectorDropDown.getSelectedItem() == null) {
            throw new ValidationException("No socket connector was selected.");
        }
        assertion.setSocketConnectorGoid(((SocketConnectorEntry) connectorDropDown.getSelectedItem()).getGoid());

        assertion.setTargetVariable(targetVariable.getVariable());

        return assertion;
    }

    @Override
    public void setData(ExtensibleSocketConnectorGetSessionListAssertion assertion) {

        if (assertion.getSocketConnectorGoid() != null && !assertion.getSocketConnectorGoid().equals(Goid.DEFAULT_GOID)) {
            for (int i = 0; i < connectorDropDown.getItemCount(); i++) {
                SocketConnectorEntry entry = (SocketConnectorEntry) connectorDropDown.getItemAt(i);

                if (assertion.getSocketConnectorGoid().equals(entry.getGoid())) {
                    connectorDropDown.setSelectedIndex(i);
                    break;
                }
            }
        }

        targetVariable.setVariable(assertion.getTargetVariable());
    }

    private ExtensibleSocketConnectorEntityAdmin getEntityManager() {
        return Registry.getDefault().getExtensionInterface(ExtensibleSocketConnectorEntityAdmin.class, null);
    }
}
