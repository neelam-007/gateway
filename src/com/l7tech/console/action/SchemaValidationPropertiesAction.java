package com.l7tech.console.action;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.event.PolicyEvent;
import com.l7tech.console.event.PolicyListener;
import com.l7tech.console.event.PolicyListenerAdapter;
import com.l7tech.console.panels.SchemaValidationPropertiesDialog;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.tree.policy.SchemaValidationTreeNode;
import com.l7tech.console.util.TopComponents;
import com.l7tech.service.PublishedService;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.IOException;

import org.dom4j.DocumentException;
import org.xml.sax.SAXParseException;

/**
 * Action for viewing or editing the properties of a Schema Validation Assertion node.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Feb 6, 2004<br/>
 * $Id$<br/>
 */
public class SchemaValidationPropertiesAction extends SecureAction {

    public SchemaValidationPropertiesAction(SchemaValidationTreeNode node, PublishedService service) {
        this.node = node;
        this.service = service;
    }

    public String getName() {
        return "Schema Validation Properties";
    }

    public String getDescription() {
        return "View and edit XML schema validation properties";
    }

    protected String iconResource() {
        return "com/l7tech/console/resources/Properties16.gif";
    }

    protected void performAction() {
        try {
            Frame f = TopComponents.getInstance().getMainWindow();
            SchemaValidationPropertiesDialog dlg = new SchemaValidationPropertiesDialog(f, node, service);
            dlg.addPolicyListener(listener);
            dlg.pack();
            dlg.setSize(600, 800);
            Utilities.centerOnScreen(dlg);
            dlg.show();
        } catch (DocumentException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (SAXParseException e) {
            throw new RuntimeException(e);
        }
    }

    private final PolicyListener listener = new PolicyListenerAdapter() {
        public void assertionsChanged(PolicyEvent e) {
            JTree tree = TopComponents.getInstance().getPolicyTree();
            if (tree != null) {
                PolicyTreeModel model = (PolicyTreeModel)tree.getModel();
                model.assertionTreeNodeChanged(node);
                log.finest("model invalidated");
            } else {
                log.log(Level.WARNING, "Unable to reach the palette tree.");
            }
        }
    };

    private final Logger log = Logger.getLogger(getClass().getName());
    private SchemaValidationTreeNode node;
    private PublishedService service;
}
