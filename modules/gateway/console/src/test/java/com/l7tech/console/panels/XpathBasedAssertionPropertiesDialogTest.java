package com.l7tech.console.panels;

import com.l7tech.common.TestDocuments;
import com.l7tech.console.tree.ServiceNode;
import com.l7tech.console.tree.policy.RequestXpathPolicyTreeNode;
import com.l7tech.console.tree.policy.XpathBasedAssertionTreeNode;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.registry.RegistryStub;
import com.l7tech.gateway.common.StubDataStore;
import com.l7tech.policy.assertion.RequestXpathAssertion;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceHeader;
import org.junit.Ignore;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;

/**
 * @author mike
 */
@Ignore("Developer Test")
public class XpathBasedAssertionPropertiesDialogTest {

    public static void main(String[] args) {
        try {
            Registry.setDefault(new RegistryStub());
            realMain();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.exit(0);
    }

    private static void realMain() throws Exception {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

        RequestXpathAssertion ass = new RequestXpathAssertion();
        StubDataStore dataStore = StubDataStore.defaultStore();
        Collection services = dataStore.getPublishedServices().values();
        if (services.isEmpty()) {
            throw new RuntimeException("Empty Stub DataStore");
        }
        PublishedService svc = (PublishedService)services.iterator().next();
        ServiceHeader sh = new ServiceHeader(svc);
        ServiceNode sn = new ServiceNode(sh);

        PublishedService ps = sn.getEntity();
        ps.setWsdlXml(TestDocuments.getTestDocumentAsXml(TestDocuments.WSDL));

        ActionListener okListener = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
            }
        };

        XpathBasedAssertionTreeNode treeNode = new RequestXpathPolicyTreeNode(ass);
        XpathBasedAssertionPropertiesDialog d = new XpathBasedAssertionPropertiesDialog(null, true, treeNode, okListener, true, false);

        d.setModal(true);
        d.pack();
        d.setVisible(true);
    }
}
