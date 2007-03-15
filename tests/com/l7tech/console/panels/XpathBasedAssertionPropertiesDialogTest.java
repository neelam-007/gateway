/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.console.panels;

import com.l7tech.common.xml.TestDocuments;
import com.l7tech.console.tree.ServiceNode;
import com.l7tech.console.tree.policy.RequestXpathPolicyTreeNode;
import com.l7tech.console.tree.policy.XpathBasedAssertionTreeNode;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.registry.RegistryStub;
import com.l7tech.identity.StubDataStore;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.policy.assertion.RequestXpathAssertion;
import com.l7tech.service.PublishedService;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;

/**
 * @author mike
 */
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
            throw new RuntimeException("Emtpy Stub DataStore");
        }
        PublishedService svc = (PublishedService)services.iterator().next();
        EntityHeader eh = new EntityHeader(Long.toString(svc.getOid()), EntityType.SERVICE, svc.getName(), "This is a test service");
        ServiceNode sn = new ServiceNode(eh);

        PublishedService ps = sn.getPublishedService();
        ps.setWsdlXml(TestDocuments.getTestDocumentAsXml(TestDocuments.WSDL));

        ActionListener okListener = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
            }
        };

        XpathBasedAssertionTreeNode treeNode = new RequestXpathPolicyTreeNode(ass);
        XpathBasedAssertionPropertiesDialog d = new XpathBasedAssertionPropertiesDialog(null, true, treeNode, okListener, true);

        d.setModal(true);
        d.pack();
        d.setVisible(true);
    }
}
