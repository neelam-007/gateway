package com.l7tech.console.action;

import com.l7tech.console.panels.MigrateNamespacesDialog;
import com.l7tech.console.poleditor.PolicyEditorPanel;
import com.l7tech.console.tree.EntityWithPolicyNode;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.validator.XpathBasedAssertionValidator;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import com.l7tech.xml.NamespaceMigratable;
import com.l7tech.xml.soap.SoapVersion;

import javax.swing.*;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The XpathBasedAssertionSoapVersionFixificationRepairalyzer pops up a "Migrate Namespaces" dialog,
 * ready to confirm to repair a mismatched SOAP namespace version issue.
 */
@SuppressWarnings({"UnusedDeclaration"}) // Used reflectively; returned as validator warning remedial action classname
public class NamespaceMigratableSoapVersionMigrator implements Functions.UnaryVoid<AssertionTreeNode> {
    private static final Logger logger = Logger.getLogger(NamespaceMigratableSoapVersionMigrator.class.getName());

    @Override
    public void call(AssertionTreeNode node) {
        Assertion assertion = node.asAssertion();
        if (!(assertion instanceof NamespaceMigratable))
            return;
        final NamespaceMigratable migratable = (NamespaceMigratable) assertion;

        PolicyEditorPanel pep = TopComponents.getInstance().getPolicyEditorPanel();
        if (pep == null)
            return;

        EntityWithPolicyNode pn = pep.getPolicyNode();
        if (pn == null)
            return;

        final Entity entity;
        try {
            entity = pn.getEntity();
        } catch (FindException e) {
            //noinspection ThrowableResultOfMethodCallIgnored
            logger.log(Level.WARNING, "Unable to look up policy node entity: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            return;
        }

        if (!(entity instanceof PublishedService))
            return;

        PublishedService service = (PublishedService) entity;
        SoapVersion serviceSoapVersion = service.getSoapVersion();
        if (serviceSoapVersion == null || serviceSoapVersion.getNamespaceUri() == null)
            return;

        SoapVersion unwantedSoapVersion = XpathBasedAssertionValidator.checkForUnwantedSoapVersion(migratable, serviceSoapVersion);
        if (unwantedSoapVersion == null)
            return;                

        JDialog dlg = new MigrateNamespacesDialog(TopComponents.getInstance().getTopParent(), Arrays.asList(node), unwantedSoapVersion.getNamespaceUri(), serviceSoapVersion.getNamespaceUri());
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg, null);
    }
}
