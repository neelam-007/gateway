package com.l7tech.console.action;

import com.l7tech.common.util.XmlUtil;
import com.l7tech.console.logging.ErrorManager;
import com.l7tech.console.tree.PoliciesFolderNode;
import com.l7tech.console.util.Preferences;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.exporter.ExporterConstants;
import com.l7tech.policy.wsp.WspConstants;
import com.l7tech.service.PublishedService;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * The SSM action type that imports a policy from a file.
 *
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jul 21, 2004<br/>
 */
public class ImportPolicyFromFileAction extends BaseAction {
    static final Logger log = Logger.getLogger(ImportPolicyFromFileAction.class.getName());
    protected PublishedService pubService;

    public ImportPolicyFromFileAction() {
    }

    public ImportPolicyFromFileAction(PublishedService svc) {
        if (svc == null) {
            throw new IllegalArgumentException();
        }
        this.pubService = svc;
    }

    /**
     * @return the action name
     */
    public String getName() {
        return "Import Policy";
    }

    /**
     * @return the aciton description
     */
    public String getDescription() {
        return "Import a policy from a file along with external references.";
    }

    /**
     * subclasses override this method specifying the resource name
     */
    protected String iconResource() {
        return "com/l7tech/console/resources/saveTemplate.gif";
    }

    /** Actually perform the action.
     * This is the method which should be called programmatically.

     * note on threading usage: do not access GUI components
     * without explicitly asking for the AWT event thread!
     */
    public void performAction() {
        if (pubService == null) {
            throw new IllegalStateException("no service specified");
        }
        // get file from user
        File templateDir = null;
        try {
            templateDir = new File(Preferences.getPreferences().getHomePath() +
                                   File.separator + PoliciesFolderNode.TEMPLATES_DIR);
            if (!templateDir.exists()) {
                if (!templateDir.mkdir()) {
                    throw new IOException("Cannot create " + templateDir.getPath());
                }
            }
        } catch (IOException e) {
            ErrorManager.getDefault().notify(Level.WARNING,
                                             e,
                                             "The system reported problem in accessing or creating" +
                                             "the policy template directory " + templateDir.getPath() + "\n" +
                                             "The policy template is not saved.");
            return;
        }

        JFileChooser chooser = new JFileChooser(templateDir);
        chooser.setDialogTitle("Import from ...");
        chooser.setMultiSelectionEnabled(false);
        chooser.setFileFilter(new FileFilter() {
            public boolean accept(File f) {
                if (f.getAbsolutePath().endsWith(".xml") || f.getAbsolutePath().endsWith(".XML")) {
                    return true;
                }
                if (f.isDirectory()) return true;
                return false;
            }
            public String getDescription() {
                return "XML Files";
            }
        });
        int ret = chooser.showOpenDialog(TopComponents.getInstance().getMainWindow());
        if (JFileChooser.APPROVE_OPTION != ret) return;
        String name = chooser.getSelectedFile().getPath();
        // Read XML document from this
        Document readDoc = null;
        try {
            readDoc = XmlUtil.parse(new FileInputStream(chooser.getSelectedFile()));
        } catch (IOException e) {
            log.log(Level.WARNING, "Could not read xml document from " + name, e);
        } catch (SAXException e) {
            log.log(Level.WARNING, "Could not read xml document from " + name, e);
        }
        // Import policy references first
        Element references = XmlUtil.findFirstChildElementByName(readDoc.getDocumentElement(),
                                                                 ExporterConstants.EXPORTED_POL_NS,
                                                                 ExporterConstants.EXPORTED_REFERENCES_ELNAME);
        if (references != null) {
            // TODO
            System.out.println("\n\n\nBIG TODO HERE!!\n\n\n");
            // todo, check those references
            // if not resolveable, interact with administrator to fix the problem
        } else {
            log.warning("The policy document " + name + " did not contain exported references. Maybe this is " +
                        "an old-school style policy export.");
        }
        Element policy = XmlUtil.findFirstChildElementByName(readDoc.getDocumentElement(),
                                                             WspConstants.POLICY_NS,
                                                             WspConstants.POLICY_ELNAME);
        if (policy != null) {
            try {
                pubService.setPolicyXml(XmlUtil.nodeToString(policy));
            } catch (IOException e) {
                log.log(Level.WARNING, "could not read policy from " + name, e);
            }
        } else {
            log.warning("The document " + name + " did not contain a policy at all.");
        }
    }
}
