package com.l7tech.console.policy.exporter;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.export.ExternalReferenceFactory;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.policy.Policy;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.Include;
import com.l7tech.policy.assertion.PolicyReference;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.exporter.PolicyExporter;
import com.l7tech.policy.exporter.PolicyImportCancelledException;
import com.l7tech.policy.exporter.PolicyImporter;
import com.l7tech.policy.wsp.PolicyConflictException;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.util.ResourceUtils;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.swing.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class PolicyExportUtils {

    //- PUBLIC

    /**
     * Import a policy from file.
     *
     * @param policy The policy that is the target of the import.
     * @param exportFile The policy export to import.
     * @return True if the policy was imported.
     */
    public static boolean importPolicyFromFile( final Policy policy,
                                                final File exportFile ) {
        boolean imported = false;

        try {
            final Document readDoc;
            InputStream in = null;
            try {
                in = new FileInputStream( exportFile );
                readDoc = XmlUtil.parse(in);
            } catch ( SAXException e) {
                throw new IOException(e);
            } finally {
                ResourceUtils.closeQuietly(in);
            }

            final WspReader wspReader = TopComponents.getInstance().getApplicationContext().getBean("wspReader", WspReader.class);
            final ConsoleExternalReferenceFinder finder = new ConsoleExternalReferenceFinder();
            final PolicyImporter.PolicyImporterResult result = PolicyImporter.importPolicy(policy, readDoc, getExternalReferenceFactories(), wspReader, finder, finder, finder, finder );
            final Assertion newRoot = (result != null) ? result.assertion : null;
            // for some reason, the PublishedService class does not allow to set a policy
            // directly, it must be set through the XML
            if (newRoot != null) {
                final String newPolicyXml = WspWriter.getPolicyXml(newRoot);
                policy.setXml(newPolicyXml);
                addPoliciesToPolicyReferenceAssertions(policy.getAssertion(), result.policyFragments);
                imported = true;
            } else {
                DialogDisplayer.showMessageDialog(TopComponents.getInstance().getTopParent(),
                                          "The policy being imported is not a valid policy, or is empty.",
                                          "Invalid/Empty Policy",
                                          JOptionPane.WARNING_MESSAGE, null);
            }
        } catch ( PolicyConflictException e) {
            final String existingPolicyName = e.getExistingPolicyName();
            final String importedPolicyName = e.getImportedPolicyName();

            // Generate an error message depending on if the name of the imported policy fragment is the same as
            // the name of the existing policy fragment or not.
            StringBuilder errorMessage = new StringBuilder("The policy fragment " + importedPolicyName + " in the imported file is different from the existing policy fragment");
            if (! importedPolicyName.equals(existingPolicyName)) {
                errorMessage.append(" ").append(existingPolicyName).append(", but they have the same ID");
            }
            errorMessage.append(".\n");
            errorMessage.append("Due to this conflict, the policy fragment ").append(importedPolicyName).append(" cannot be imported.");

            logger.log( Level.WARNING, "Could not import the policy from " + exportFile.getPath() + "\n" + errorMessage, e);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Could not localize or read policy from " + exportFile.getPath(), e);
            DialogDisplayer.showMessageDialog(TopComponents.getInstance().getTopParent(),
                                          "Could not find policy export in the selected file or the imported policy contains errors",
                                          "Policy Not Found/Not Valid",
                                          JOptionPane.WARNING_MESSAGE, null);
        } catch ( PolicyImportCancelledException e) {
            logger.log( Level.FINE, "Import from file \"" + exportFile.getPath() + "\" was cancelled", e);
        }

        return imported;
    }

    /**
     * Export a policy to file.
     *
     * @param assertion The assertion to export.
     * @param exportFile The file to export to.
     * @return True if the policy was exported
     * @throws IOException If an IO error occurs
     * @throws SAXException If there is an error processing the policy XML
     */
    public static boolean exportPolicyToFile( final Assertion assertion,
                                              final File exportFile ) throws IOException, SAXException {
        final ConsoleExternalReferenceFinder finder = new ConsoleExternalReferenceFinder();
        PolicyExporter exporter = new PolicyExporter( finder, finder );
        exporter.exportToFile(assertion, exportFile, getExternalReferenceFactories());
        return true;
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( PolicyExportUtils.class.getName() );

    public static void addPoliciesToPolicyReferenceAssertions( final @Nullable Assertion rootAssertion,
                                                               final HashMap<String, Policy> fragments ) throws IOException {
        if(rootAssertion instanceof CompositeAssertion ) {
            CompositeAssertion compAssertion = (CompositeAssertion)rootAssertion;
            for( Iterator it = compAssertion.children();it.hasNext();) {
                Assertion child = (Assertion)it.next();
                addPoliciesToPolicyReferenceAssertions(child, fragments);
            }
        } else if(rootAssertion instanceof PolicyReference) {
            PolicyReference policyReference = (PolicyReference)rootAssertion;
            Policy fragment = fragments.get(policyReference.retrievePolicyGuid());
            if(fragment != null) {
                policyReference.replaceFragmentPolicy(fragment);
                if(rootAssertion instanceof Include ) {
                    ((Include)rootAssertion).setPolicyName(fragment.getName());
                }
                addPoliciesToPolicyReferenceAssertions(fragment.getAssertion(), fragments);
            }
        }
    }
    
    public static Set<ExternalReferenceFactory> getExternalReferenceFactories() {
        Registry registry = Registry.getDefault();
        if (! registry.isAdminContextPresent()) {
            logger.warning("Cannot get Policy Exporter and Importer Admin due to no Admin Context present.");
            return null;
        } else {
            return registry.getPolicyAdmin().findAllExternalReferenceFactories();
        }
    }
}