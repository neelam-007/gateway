package com.l7tech.console.tree;

import com.l7tech.common.util.XmlUtil;
import com.l7tech.console.logging.ErrorManager;
import com.l7tech.console.policy.exporter.PolicyExporter;
import com.l7tech.console.util.TopComponents;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.*;
import java.util.Comparator;
import java.util.logging.Level;


/**
 * This class represents the folder in the assertion palette tree containing exported policies (aka Policy Templates)
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.1
 */
public class PolicyTemplatesFolderNode extends AbstractPaletteFolderNode {
    public static final String NAME = "Policy Templates";
    public static final String TEMPLATES_DIR = "policy.templates";

    /** The entity name comparator  */
      protected static final Comparator<PolicyTemplateNode> FILENAME_COMPARATOR = new Comparator<PolicyTemplateNode>() {
          public int compare(PolicyTemplateNode o1, PolicyTemplateNode o2) {
              if (o1 != null && o2 != null) {
                  String name1 = o1.getFile().getName();
                  String name2 = o2.getFile().getName();
                  return name1.compareToIgnoreCase(name2);
              }
              throw new ClassCastException("Expected "+PolicyTemplateNode.class +
                                           " received "+ (o1 == null ? null : o1.getClass().getSimpleName()) +
                                           " and " + (o2 == null ? null : o2.getClass().getSimpleName()));
          }
      };

    public PolicyTemplatesFolderNode() {
        super(NAME, "policies", TopComponents.getInstance().getPreferences().getHomePath() + File.separator + TEMPLATES_DIR, FILENAME_COMPARATOR);
    }


    /**
     * subclasses override this method
     */
    protected void loadChildren() {
        // This folder does not allow modular assertions to invite themselves into it, so we don't call
        // insertMatchingModularAssertions here.
        try {
            File[] files = listPolicies();
            children = null;
            for (File file : files) {
                PolicyTemplateNode ptn = new PolicyTemplateNode(file);
                insert(ptn, getInsertPosition(ptn));
            }
        } catch (IOException e) {
            ErrorManager.getDefault().notify(Level.WARNING, e, "There was an error loading policy templates.");

        }
    }

    protected boolean isEnabledByLicense() {
        // Policy templates always shown, regardless of license
        return true;
    }

    protected void filterChildren() {
        // Suppress filtering for policy templates
    }

    private String getTemplatesPath() {
        return (String)getUserObject();
    }

    private  File[] listPolicies() throws IOException {
        File f = new File(getTemplatesPath());
        if (!f.exists()) {
            if (!f.mkdir()) throw new IOException("Cannot create "+f.getPath());
        }
        return
        f.listFiles( new FilenameFilter() {
            /**
             * Tests if a specified file should be included in a file list.
             *
             * @param   dir    the directory in which the file was found.
             * @param   name   the name of the file.
             * @return  <code>true</code> if and only if the name should be
             * included in the file list; <code>false</code> otherwise.
             */
            public boolean accept(File dir, String name) {
                File f = new File(dir.getPath()+File.separator+name);
                if (f.isFile()) {
                    // check if it's an xml file
                    try {
                        FileInputStream fis = new FileInputStream(f);
                        Document doc = XmlUtil.parse(fis);
                        if (doc == null) {
                            logger.fine("File " + name + " is in templates folder but is not an exported policy");
                            return false;
                        }
                        // check if it's an exported policy
                        if (!PolicyExporter.isExportedPolicy(doc)) {
                            logger.fine("document name is xml but is not an exported document");
                            return false;
                        }
                        return true;
                    } catch (FileNotFoundException e) {
                        logger.log(Level.FINE, "Document " + name + " is in templates folder but is " +
                                               "not an exported policy", e);
                    } catch (IOException e) {
                        logger.log(Level.FINE, "Document " + name + " is in templates folder but is " +
                                               "not an exported policy", e);
                    } catch (SAXException e) {
                        logger.log(Level.FINE, "Document " + name + " is in templates folder but is " +
                                               "not an exported policy", e);
                    }
                    return false;
                }
                return false;
            }
        });
    }
}