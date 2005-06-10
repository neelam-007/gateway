/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.console.panels;

import com.l7tech.common.xml.TestDocuments;
import com.l7tech.console.tree.ServiceNode;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.AssertionTreeNodeFactory;
import com.l7tech.console.tree.policy.SchemaValidationTreeNode;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.registry.RegistryStub;
import com.l7tech.identity.StubDataStore;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.policy.assertion.xml.SchemaValidation;
import com.l7tech.service.PublishedService;

import javax.swing.*;
import java.util.Collection;

/**
 * @author mike
 */
public class SchemaValidationPropertiesDialogTest {

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

        StubDataStore dataStore = StubDataStore.defaultStore();
        Collection services = dataStore.getPublishedServices().values();
        if (services.isEmpty()) {
            throw new RuntimeException("Emtpy Stub DataStore");
        }
        PublishedService svc = (PublishedService)services.iterator().next();
        EntityHeader eh = new EntityHeader(Long.toString(svc.getOid()), EntityType.SERVICE, svc.getName(), "This is a test service");
        ServiceNode sn = new ServiceNode(eh);

        PublishedService ps = sn.getPublishedService();
        ps.setWsdlXml(TestDocuments.getTestDocumentAsXml(TestDocuments.WSDL_DOC_LITERAL));
        SchemaValidation sass = new SchemaValidation();
        AssertionTreeNode tn = AssertionTreeNodeFactory.asTreeNode(sass);

        SchemaValidationPropertiesDialog d = new SchemaValidationPropertiesDialog(null, (SchemaValidationTreeNode)tn, ps);

        d.setModal(true);
        d.pack();
        d.setSize(600, 800);
        d.setVisible(true);
    }
}
