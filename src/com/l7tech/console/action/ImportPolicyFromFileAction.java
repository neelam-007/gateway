package com.l7tech.console.action;

import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.console.logging.ErrorManager;
import com.l7tech.console.tree.PoliciesFolderNode;
import com.l7tech.console.util.Preferences;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.exporter.ExporterConstants;
import com.l7tech.policy.exporter.ExternalReference;
import com.l7tech.policy.exporter.RemoteReferenceResolver;
import com.l7tech.policy.exporter.PolicyImporter;
import com.l7tech.policy.wsp.WspConstants;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.policy.wsp.InvalidPolicyStreamException;
import com.l7tech.policy.assertion.Assertion;
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

        try {
            Assertion newRoot = PolicyImporter.importPolicy(chooser.getSelectedFile());
            // for some reason, the PublishedService class does not allow to set a policy
            // directly, it must be set through the XML
            if (newRoot != null) {
                pubService.setPolicyXml(WspWriter.getPolicyXml(newRoot));
            }
        } catch (IOException e) {
            log.log(Level.WARNING, "could not localize or read policy from " + chooser.getSelectedFile().getPath(), e);
        }
    }
}
