package com.l7tech.console.action;

import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.tree.PolicyTemplateNode;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.util.ComponentRegistry;
import com.l7tech.service.PublishedService;

import javax.swing.*;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;
import java.util.logging.Level;


/**
 * The <code>AddAssertionAction</code> action assigns
 * the current assertion  to the target policy.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class AddAssertionAction extends BaseAction {
    private static final Logger log =
      Logger.getLogger(AddAssertionAction.class.getName());

    protected AbstractTreeNode paletteNode;
    protected AssertionTreeNode assertionNode;

    /**
     * @return the action name
     */
    public String getName() {
        return "Add assertion";
    }

    /**
     * @return the aciton description
     */
    public String getDescription() {
        return "Add assertion to the policy assertion tree";
    }

    /**
     * subclasses override this method specifying the resource name
     */
    protected String iconResource() {
        return "com/l7tech/console/resources/assign.gif";
    }

    /** Actually perform the action.
     * This is the method which should be called programmatically.

     * note on threading usage: do not access GUI components
     * without explicitly asking for the AWT event thread!
     */
    public void performAction() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (paletteNode == null || assertionNode == null) {
                    throw new IllegalStateException();
                }
                if (paletteNode instanceof PolicyTemplateNode) {
                    assignPolicyTemplate((PolicyTemplateNode)paletteNode);
                } else {
                    assertionNode.receive(paletteNode);
                }
            }
        });
    }

    private void assignPolicyTemplate(PolicyTemplateNode pn) {
        JTree tree = ComponentRegistry.getInstance().getPolicyTree();
        PublishedService svc = (PublishedService)tree.getClientProperty("service");
        if (svc == null)
            throw new IllegalArgumentException("No edited service specified");
        ByteArrayOutputStream bo = null;
        InputStream fin = null;
        try {
            bo = new ByteArrayOutputStream();
            fin = new FileInputStream(pn.getFile());

            byte[] buff = new byte[1024];
            int nread = -1;
            while ((nread = fin.read(buff)) != -1) {
                bo.write(buff, 0, nread);
            }
            svc.setPolicyXml(bo.toString());
        } catch (IOException e) {

        } finally {
            if (bo != null) {
                try {
                    bo.close();
                } catch (IOException e) {
                    log.log(Level.WARNING, "Error closing stream", e);
                }
            }
            if (fin != null) {
                try {
                    fin.close();
                } catch (IOException e) {
                    log.log(Level.WARNING, "Error closing stream", e);
                }
            }
        }

    }
}
