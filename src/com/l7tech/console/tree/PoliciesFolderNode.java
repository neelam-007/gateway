package com.l7tech.console.tree;

import com.l7tech.console.logging.ErrorManager;

import javax.swing.tree.MutableTreeNode;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.TreeSet;
import java.util.Set;
import java.util.Comparator;
import java.util.Arrays;


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

    /**
     * construct the <CODE>PoliciesFolderNode</CODE> instance for
     * a given home path
     */
    public PoliciesFolderNode(String path) {
        super(path+File.separator+TEMPLATES_DIR);
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

            Set sorted = new TreeSet(new Comparator() {
                public int compare(Object o1, Object o2) {
                    File f1 = (File)o1;
                    File f2 = (File)o2;
                    return f1.getName().compareTo(f2.getName());
                }
            });
            sorted.addAll(Arrays.asList(files));
            files = (File[])sorted.toArray(new File[] {});
            int index = 0;
            children = null;
            for (int i = 0; i < files.length; i++) {
                File file = files[i];
                insert((MutableTreeNode) new PolicyTemplateNode(file), index++);
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
                if (f.isFile()) return true;

                return false;
            }
        });
    }
}
