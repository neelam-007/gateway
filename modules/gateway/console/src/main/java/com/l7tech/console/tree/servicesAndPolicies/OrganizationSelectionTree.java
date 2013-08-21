package com.l7tech.console.tree.servicesAndPolicies;

import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.OrganizationHeader;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.objectmodel.folder.FolderHeader;
import com.l7tech.util.Functions;
import com.l7tech.util.Functions.Unary;
import com.l7tech.util.Resolver;
import com.l7tech.util.ResolvingComparator;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

import static com.l7tech.util.CollectionUtils.iterable;

/**
 * Tree for selection of organizational headers / folders.
 */
public class OrganizationSelectionTree<LEH extends OrganizationHeader> extends JTree  {

    private CheckBoxNode rootNode;
    private DefaultTreeModel organizationTreeModel;
    private final boolean leafsSelectable;

    /**
     * Create empty policy tree
     *
     * @param leafsSelectable True if the displayed OrganizationHeaders are selectable.
     */
    public OrganizationSelectionTree( final boolean leafsSelectable ) {
        this.rootNode = new CheckBoxNode( new FolderHeader(Folder.ROOT_FOLDER_ID, "/", null, null, "/", null), false);
        this.organizationTreeModel = new DefaultTreeModel(rootNode);
        this.leafsSelectable = leafsSelectable;
        setModel( organizationTreeModel );
        CheckBoxNodeRenderer renderer = new CheckBoxNodeRenderer();
        setCellRenderer(renderer);
        setShowsRootHandles(true);
        setRootVisible(true);

        setCellEditor( new CheckBoxNodeEditor( this, leafsSelectable ) );
        setEditable( true );
    }

    public void initTree( @NotNull final Collection<FolderHeader> folders,
                          @NotNull final Collection<LEH> leafEntityHeaders ){
        rootNode.removeAllChildren();

        final List<LEH> leafHeadersList = new ArrayList<LEH>( leafEntityHeaders );
        Collections.sort( leafHeadersList, new ResolvingComparator<LEH, String>(new Resolver<LEH, String>() {
            @Override
            public String resolve(final LEH key) {
                return key.getDisplayName().toLowerCase();
            }
        }, false));

        final List<FolderHeader> allFolderHeaders = new ArrayList<FolderHeader>( folders );
        Collections.sort( allFolderHeaders, new ResolvingComparator<FolderHeader,String>( new Resolver<FolderHeader,String>(){
            @Override
            public String resolve( final FolderHeader key ) {
                return key.getName().toLowerCase();
            }
        }, false ) );
        FolderHeader root = null;
        for(FolderHeader folder : allFolderHeaders ) {
            if(folder.getParentFolderGoid() == null) {
                root = folder;
            }
        }

        //if this user has no permission to view any folders they will see an empty tree
        if (root == null) return;

        //noinspection unchecked
        insertNodes( iterable( allFolderHeaders, leafHeadersList ), rootNode, root.getGoid() );
        organizationTreeModel.reload(rootNode);

        expandPath(new TreePath(rootNode.getPath()));
    }

    private void insertNodes( Iterable<? extends EntityHeader> contents, AbstractTreeNode target, Goid folderGoid ) {
        for( final EntityHeader entityHeader : contents ) {
            if( getParentFolderGoid(entityHeader) != null && Goid.equals(getParentFolderGoid(entityHeader), folderGoid) ) {
                final boolean isFolder = entityHeader instanceof FolderHeader;
                final AbstractTreeNode childNode = isFolder || leafsSelectable ?
                        new CheckBoxNode(entityHeader, false) :
                        new OrganizationHeaderNode( (OrganizationHeader)entityHeader );
                organizationTreeModel.insertNodeInto(childNode, target, target.getChildCount());
                if ( isFolder ) {
                    insertNodes( contents, childNode, entityHeader.getGoid() );
                    // hide empty folders when not selectable
                    if ( leafsSelectable && childNode.getChildCount()==0 ) {
                        organizationTreeModel.removeNodeFromParent( childNode );
                    }
                }
            }
        }
    }

    @Override
    public String convertValueToText( final Object value, final boolean selected, final boolean expanded, final boolean leaf, final int row, final boolean hasFocus ) {
        if ( value instanceof OrganizationHeaderNode ) {
            return ((OrganizationHeaderNode)value).getName();
        }
        return super.convertValueToText( value, selected, expanded, leaf, row, hasFocus );
    }

    private Goid getParentFolderGoid(final EntityHeader entityHeader) {
        Goid folderId = null;
        if ( entityHeader instanceof FolderHeader ) {
            folderId = ((FolderHeader)entityHeader).getParentFolderGoid();
        } else if ( entityHeader instanceof OrganizationHeader ) {
            folderId = ((OrganizationHeader)entityHeader).getFolderId();
        }
        return folderId;
    }

    public Collection<LEH> getSelectedEntities() {
        return getSelections( new Unary<LEH,CheckBoxNode>(){
            @SuppressWarnings({ "unchecked" })
            @Override
            public LEH call( final CheckBoxNode node ) {
                return node.isLeaf() && !(node.header instanceof FolderHeader) ? (LEH) node.header : null;
            }
        } );
    }

    public Collection<FolderHeader> getSelectedFolders() {
        return getSelections( new Unary<FolderHeader,CheckBoxNode>(){
            @SuppressWarnings({ "unchecked" })
            @Override
            public FolderHeader call( final CheckBoxNode node ) {
                return !(node.header instanceof FolderHeader) ? null : (FolderHeader) node.header;
            }
        } );
    }

    @SuppressWarnings({ "unchecked" })
    private List<AbstractTreeNode> toNodeList( final Enumeration enumeration ) {
        return Collections.list((Enumeration<AbstractTreeNode>)enumeration);
    }

    private <T> Collection<T> getSelections( final Unary<T,CheckBoxNode> matchExtractor ) {
        return Functions.reduce(
                toNodeList(rootNode.preorderEnumeration()),
                new ArrayList<T>(),
                new Functions.Binary<List<T>,List<T>,AbstractTreeNode>(){
                    @Override
                    public List<T> call( final List<T> result,
                                         final AbstractTreeNode abstractTreeNode ) {
                        if ( abstractTreeNode instanceof CheckBoxNode ) {
                            final CheckBoxNode node = (CheckBoxNode) abstractTreeNode;
                            if ( node.checked && matchExtractor.call( node ) != null ) {
                                result.add( matchExtractor.call( node ) );
                            }
                        }
                        return result;
                    }
                });

    }

    private static class OrganizationHeaderNode extends AbstractTreeNode {
        private final OrganizationHeader header;

        private OrganizationHeaderNode( final OrganizationHeader header ) {
            super(header);
            this.header = header;
        }

        @Override
        public String getName() {
            return header.getDisplayName();
        }

        @Override
        protected String iconResource( final boolean open ) {
            return null;
        }
    }

    private static class CheckBoxNode extends AbstractTreeNode {
        private final EntityHeader header;
        private boolean checked;

        private CheckBoxNode(EntityHeader header, boolean checked) {
            super( header.getName() );
            this.header = header;
            this.checked = checked;
        }

        @Override
        public String getName() {
            if(header instanceof OrganizationHeader )
                return ((OrganizationHeader)header).getDisplayName();
            return header.getName();
        }

        @Override
        protected String iconResource(boolean open) {
            return null;
        }

        @Override
        public String toString() {
            return getName();
        }
    }

    private static class CheckBoxNodeRenderer implements TreeCellRenderer {

        private JCheckBox leafRenderer = new JCheckBox();
        private CheckBoxNode leafNode;

        private DefaultTreeCellRenderer defaultLeafRenderer;

        Color selectionBorderColor, selectionForeground, selectionBackground,
                textForeground, textBackground;

        protected JCheckBox getLeafRenderer() {
            return leafRenderer;
        }

        protected CheckBoxNode getLeafRendererNode(){
            return leafNode;
        }

        private CheckBoxNodeRenderer() {
            Font fontValue;
            fontValue = UIManager.getFont("Tree.font");
            if (fontValue != null) {
                leafRenderer.setFont(fontValue);
            }
            Boolean booleanValue = (Boolean) UIManager.get("Tree.drawsFocusBorderAroundIcon");
            leafRenderer.setFocusPainted( booleanValue != null && booleanValue );

            defaultLeafRenderer = new DefaultTreeCellRenderer();
            defaultLeafRenderer.setLeafIcon( null );
            defaultLeafRenderer.setBorder( BorderFactory.createEmptyBorder( 4, 4, 4, 4 ) );

            selectionBorderColor = UIManager.getColor("Tree.selectionBorderColor");
            selectionForeground = UIManager.getColor("Tree.selectionForeground");
            selectionBackground = UIManager.getColor("Tree.selectionBackground");
            textForeground = UIManager.getColor("Tree.textForeground");
            textBackground = UIManager.getColor("Tree.textBackground");

        }

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value,
                                                      boolean selected, boolean expanded, boolean leaf, int row,
                                                      boolean hasFocus) {

            Component returnValue;
            if (value instanceof CheckBoxNode) {
                CheckBoxNode node = (CheckBoxNode)value;
                leafNode = node;
                leafRenderer.setText(node.getName());
                leafRenderer.setSelected(node.checked);

                leafRenderer.setEnabled( tree.isEnabled() );

                if (selected) {
                    leafRenderer.setForeground(selectionForeground);
                    leafRenderer.setBackground(selectionBackground);
                } else {
                    leafRenderer.setForeground(textForeground);
                    leafRenderer.setBackground(textBackground);
                }

                returnValue = leafRenderer;
            } else {
                returnValue = defaultLeafRenderer.getTreeCellRendererComponent(tree,
                        value, selected, expanded, leaf, row, hasFocus);
            }
            return returnValue;
        }
    }

    private static class CheckBoxNodeEditor extends AbstractCellEditor implements TreeCellEditor {
        private final JTree tree;
        private final CheckBoxNodeRenderer renderer;
        private final boolean leafsSelectable;

        CheckBoxNodeEditor(final JTree tree,
                           final boolean leafsSelectable ) {
            this.tree = tree;
            this.leafsSelectable = leafsSelectable;
            this.renderer = new CheckBoxNodeRenderer();
        }

        @Override
        public Object getCellEditorValue() {
            JCheckBox checkbox = renderer.getLeafRenderer();
            CheckBoxNode checkBoxNode =  renderer.getLeafRendererNode();
            checkBoxNode.checked = checkbox.isSelected();
            return checkBoxNode;
        }

        @Override
        public boolean isCellEditable(EventObject event) {
            boolean returnValue = false;
            if (event instanceof MouseEvent) {
                MouseEvent mouseEvent = (MouseEvent) event;
                TreePath path = tree.getPathForLocation(mouseEvent.getX(),
                        mouseEvent.getY());
                if (path != null) {
                    Object node = path.getLastPathComponent();
                    if ((node != null) && (node instanceof DefaultMutableTreeNode)) {
                        DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) node;
                        returnValue = treeNode instanceof CheckBoxNode;
                    }
                }
            }
            return returnValue;
        }

        @Override
        public Component getTreeCellEditorComponent(JTree tree,Object value,
                                                    boolean selected, boolean expanded, boolean leaf, int row) {

            Component editor = renderer.getTreeCellRendererComponent(tree, value,
                    true, expanded, leaf, row, true);

            // editor always selected / focused
            ItemListener itemListener = new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent itemEvent) {
                    if (stopCellEditing()) {
                        fireEditingStopped();
                    }
                }
            };

            if (editor instanceof JCheckBox) {
                ((JCheckBox) editor).addItemListener( itemListener );
                ((JCheckBox) editor).addActionListener( new ActionListener() {
                    @Override
                    public void actionPerformed( ActionEvent e ) {
                        // deselect all parents if unselected
                        JCheckBox checkBox = (JCheckBox) e.getSource();
                        CheckBoxNode checkBoxNode = renderer.getLeafRendererNode();
                        if ( leafsSelectable ) {
                            if ( checkBox.isSelected() ) {
                                setChildrenSelectedState( checkBoxNode, true );
                            } else {
                                TreeNode parent = (checkBoxNode).getParent();
                                while ( parent != null && parent instanceof CheckBoxNode ) {
                                    ((CheckBoxNode) parent).checked = false;
                                    ((DefaultTreeModel) CheckBoxNodeEditor.this.tree.getModel()).nodeChanged( parent );
                                    parent = parent.getParent();
                                }
                                // toggle selection
                                if ( allChildrenSelected( checkBoxNode ) ) {
                                    setChildrenSelectedState( checkBoxNode, false );
                                }
                            }
                        }
                    }
                } );
            }

            return editor;
        }

        private boolean allChildrenSelected( final TreeNode node ){
            boolean allSelected = true;
            for(int i = 0 ; i < node.getChildCount() ; ++i){
                TreeNode child = node.getChildAt(i);
                if ( child instanceof CheckBoxNode ) {
                    if ( !((CheckBoxNode)child).checked ||
                         !allChildrenSelected(child) ) {
                        allSelected = false;
                        break;
                    }
                }
            }
            return allSelected;
        }

        private void setChildrenSelectedState( final TreeNode node, final boolean select ){
            for(int i = 0 ; i < node.getChildCount() ; ++i){
                TreeNode child = node.getChildAt(i);
                if ( child instanceof CheckBoxNode ) {
                    ((CheckBoxNode)child).checked = select;
                    ((DefaultTreeModel)CheckBoxNodeEditor.this.tree.getModel()).nodeChanged(child);
                    setChildrenSelectedState( child, select );
                }
            }
        }
    }
}
