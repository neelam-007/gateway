package com.l7tech.external.assertions.xmppassertion.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.util.Registry;
import com.l7tech.external.assertions.xmppassertion.XMPPConnectionEntity;
import com.l7tech.external.assertions.xmppassertion.XMPPConnectionEntityAdmin;
import com.l7tech.external.assertions.xmppassertion.XMPPOpenServerSessionAssertion;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 14/03/12
 * Time: 2:45 PM
 * To change this template use File | Settings | File Templates.
 */
public class XMPPOpenServerSessionAssertionPropertiesDialog extends AssertionPropertiesOkCancelSupport<XMPPOpenServerSessionAssertion> {
    private static class XMPPConnectionEntry {
        private String name;
        private Goid goid;

        public XMPPConnectionEntry(String name, Goid goid) {
            this.name = name;
            this.goid = goid;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    protected static final Logger logger = Logger.getLogger(XMPPOpenServerSessionAssertionPropertiesDialog.class.getName());

    private JPanel mainPanel;
    private JComboBox xmppConnectionComboBox;

    public XMPPOpenServerSessionAssertionPropertiesDialog(Window owner, XMPPOpenServerSessionAssertion assertion) {
        super(assertion.getClass(), owner, assertion, true);
        initComponents();
    }

    @Override
    public void initComponents() {
        try {
            Collection<XMPPConnectionEntity> entities = Registry.getDefault().getExtensionInterface(XMPPConnectionEntityAdmin.class, null).findAll();
            for(XMPPConnectionEntity entity : entities) {
                if(!entity.isInbound()) {
                    xmppConnectionComboBox.addItem(new XMPPConnectionEntry(entity.getName(), entity.getGoid()));
                }
            }
        } catch(FindException e) {
            logger.log(Level.WARNING, "Failed to load the XMPP connections.");
        }
        
        super.initComponents();
    }

    @Override
    protected JPanel createPropertyPanel() {
        return mainPanel;
    }

    @Override
    public XMPPOpenServerSessionAssertion getData(XMPPOpenServerSessionAssertion assertion) throws ValidationException {
        if(xmppConnectionComboBox.getSelectedItem() == null) {
            throw new ValidationException("The XMPP connection is required.");
        }

        XMPPConnectionEntry entry = (XMPPConnectionEntry)xmppConnectionComboBox.getSelectedItem();
        if(entry.goid == null || entry.goid.equals(XMPPConnectionEntity.DEFAULT_GOID)) {
            throw new ValidationException("The XMPP connection is required.");
        }

        XMPPConnectionEntry firstEntry = (XMPPConnectionEntry)xmppConnectionComboBox.getItemAt(0);
        if(firstEntry.goid == null || firstEntry.goid.equals(XMPPConnectionEntity.DEFAULT_GOID)) {
            xmppConnectionComboBox.removeItemAt(0);
        }

        assertion.setXMPPConnectionId(((XMPPConnectionEntry)xmppConnectionComboBox.getSelectedItem()).goid);
        return assertion;
    }

    @Override
    public void setData(XMPPOpenServerSessionAssertion assertion) {
        boolean connectionFound = false;
        for(int i = 0;i < xmppConnectionComboBox.getItemCount();i++) {
            XMPPConnectionEntry entry = (XMPPConnectionEntry)xmppConnectionComboBox.getItemAt(i);
            if(entry.goid.equals(assertion.getXMPPConnectionId())) {
                xmppConnectionComboBox.setSelectedIndex(i);
                connectionFound = true;
                break;
            }
        }

        if(!connectionFound) {
            xmppConnectionComboBox.insertItemAt(new XMPPConnectionEntry("", XMPPConnectionEntity.DEFAULT_GOID), 0);
            xmppConnectionComboBox.setSelectedIndex(0);
        }
    }
}
