package com.l7tech.console.tree;

import com.l7tech.console.logging.ErrorManager;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.policy.exporter.PolicyExporter;

import javax.swing.tree.MutableTreeNode;
import java.io.*;
import java.util.Comparator;
import java.util.logging.Level;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;


/**
 * The class represents a node element in the TreeModel.
 * It represents the folder with policies.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.1
 */
public class PoliciesFolderNode extends AbstractTreeNode {
    public static final String NAME = "Policy Templates";
    public static final String TEMPLATES_DIR = "policy.templates";

    /** The entity name comparator  */
      protected static final Comparator FILENAME_COMPARATOR = new Comparator() {
          public int compare(Object o1, Object o2) {
              if (o1 instanceof PolicyTemplateNode && o2 instanceof PolicyTemplateNode) {
                  String name1 = ((PolicyTemplateNode)o1).getFile().getName();
                  String name2 = ((PolicyTemplateNode)o2).getFile().getName();
                  return name1.compareToIgnoreCase(name2);
              }
              throw new ClassCastException("Expected "+PolicyTemplateNode.class +
                                           " received "+o1.getClass() + " and "+o2.getClass());
          }
      };

    /**
     * construct the <CODE>PoliciesFolderNode</CODE> instance for
     * a given home path
     */
    public PoliciesFolderNode(String path) {
        super(path+File.separator+TEMPLATES_DIR, FILENAME_COMPARATOR);
        if (path == null)
            throw new IllegalArgumentException();
    }

    /**
     * Returns true if the receiver is a leaf.
     *
     * @return true if leaf, false otherwise
     */
    public boolean isLeaf() {
        return false;
    }

    /**
     * Returns true if the receiver allows children.
     */
    public boolean getAllowsChildren() {
        return true;
    }

    /**
     * subclasses override this method
     */
    protected void loadChildren() {
        try {
            File[] files = listPolicies();
            children = null;
            for (int i = 0; i < files.length; i++) {
                File file = files[i];
                PolicyTemplateNode ptn = new PolicyTemplateNode(file);
                insert((MutableTreeNode) ptn, getInsertPosition(ptn));
            }
        } catch (IOException e) {
            ErrorManager.getDefault().notify(Level.WARNING, e, "There was an error loading policy templates.");

        }
    }

    /**
     * Returns the node name.
     *
     * @return the name as a String
     */
    public String getName() {
        return NAME;
    }

    /**
     * subclasses override this method specifying the resource name
     *
     * @param open for nodes that can be opened, can have children
     */
    protected String iconResource(boolean open) {
        if (open)
            return "com/l7tech/console/resources/folderOpen.gif";

        return "com/l7tech/console/resources/folder.gif";
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
