package com.l7tech.console.tree;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.console.logging.ErrorManager;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.exporter.PolicyExporter;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.util.Comparator;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Level;


/**
 * This class represents the folder in the assertion palette tree containing exported policies (aka Policy Templates)
 *
 * @author Emil Marceta
 */
public class PolicyTemplatesFolderNode extends AbstractPaletteFolderNode {
    public static final String NAME = "Policy Templates";
    public static final String TEMPLATES_DIR = "policy.templates";
    public static final String REFRESH_POLICY_TEMPLATES = "Refresh Policy Templates";

    private static final WeakHashMap<AbstractTreeNode,Object> instances = new WeakHashMap<>();
    private static final PropertyChangeListener RELOAD_CHILDREN_LISTENER = new PropertyChangeListener() {
        @Override
        public void propertyChange( PropertyChangeEvent evt ) {
            for ( AbstractTreeNode node : instances.keySet() ) {
                node.reloadChildren();
            }
        }
    };

    /**
     * The entity name comparator
     */
    @SuppressWarnings({"unchecked"})
    protected static final Comparator<TreeNode> FILENAME_COMPARATOR = new Comparator<TreeNode>() {
          @Override
          public int compare(TreeNode o1, TreeNode o2) {
              if (o1 instanceof PolicyTemplateNode && o2 instanceof PolicyTemplateNode) {
                  String name1 = ((PolicyTemplateNode)o1).getFile().getName();
                  String name2 = ((PolicyTemplateNode)o2).getFile().getName();
                  return name1.compareToIgnoreCase(name2);
              }
              throw new ClassCastException("Expected "+PolicyTemplateNode.class +
                                           " received "+ (o1 == null ? null : o1.getClass().getSimpleName()) +
                                           " and " + (o2 == null ? null : o2.getClass().getSimpleName()));
          }
      };

    public PolicyTemplatesFolderNode() {
        super(NAME, "policies", TopComponents.getInstance().getPreferences().getHomePath() + File.separator + TEMPLATES_DIR, FILENAME_COMPARATOR);

        // Add a refresh property.  If users change policy xml file names in the local directory, ".l7tech/policy.templates",
        // after Refresh clicked or F5 pressed, these policy templates will be reloaded in the assertion palette panel. (SSM-1632)
        // We use a static listener to avoid leaks due to retaining palette nodes, which retain assertion prototype instances,
        // which prevents modular assertion class loaders from unloading, which leaks tons of permgen. (SSM-4715)
        instances.put( this, null );
        JTree paletteTree = (JTree) TopComponents.getInstance().getComponent(AssertionsTree.NAME);
        paletteTree.removePropertyChangeListener( REFRESH_POLICY_TEMPLATES, RELOAD_CHILDREN_LISTENER );
        paletteTree.addPropertyChangeListener( REFRESH_POLICY_TEMPLATES, RELOAD_CHILDREN_LISTENER );
    }

    @Override
    public void reloadChildren() {
        super.reloadChildren();
        JTree paletteTree = (JTree) TopComponents.getInstance().getComponent(AssertionsTree.NAME);
        final DefaultTreeModel paletteModel = (DefaultTreeModel) paletteTree.getModel();
        paletteModel.nodeStructureChanged(this);
    }

    /**
     * subclasses override this method
     */
    @Override
    protected void doLoadChildren() {
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

    @Override
    protected boolean isEnabledByLicense() {
        // Policy templates always shown, regardless of license
        return true;
    }

    @Override
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
            @Override
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

    @Override
    public Action[] getActions() {
        return new Action[] {
                new RefreshTreeNodeAction(this)
        };
    }
}