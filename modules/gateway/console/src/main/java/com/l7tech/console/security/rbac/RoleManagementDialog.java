package com.l7tech.console.security.rbac;

import com.l7tech.console.action.Actions;
import com.l7tech.console.util.Filter;
import com.l7tech.console.util.FilterListModel;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.PauseListenerAdapter;
import com.l7tech.gui.util.TextComponentPauseListenerManager;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gateway.common.security.rbac.*;
import com.l7tech.util.Functions;
import com.l7tech.console.panels.PermissionFlags;
import com.l7tech.console.util.Registry;
import com.l7tech.objectmodel.EntityType;
import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.*;
import java.text.MessageFormat;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class RoleManagementDialog extends JDialog {
    private JPanel mainPanel;
    private JButton buttonHelp;
    private JButton buttonClose;
    private JList roleList;
    private JButton addRole;
    private JButton editRole;
    private JButton removeRole;
    private JTextPane propertiesPane;
    private JTable roleAssigneeTable;
    private JTextField filterTextField;
    private JLabel filterWarningLabel;

    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.resources.RbacGui");

    private final PermissionFlags flags;
    private final RbacAdmin rbacAdmin = Registry.getDefault().getRbacAdmin();
    private final ActionListener roleActionListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            doUpdateRoleAction(e);
        }
    };
    private final DefaultListModel roleListModel = new DefaultListModel();
    private final FilterListModel<RoleModel> filteredListRoleModel = new FilterListModel<RoleModel>(roleListModel);
    private RoleAssignmentTableModel roleAssignmentTableModel;

    public RoleManagementDialog(final Window parent) throws HeadlessException {
        super(parent, resources.getString("manageRoles.title"), JDialog.DEFAULT_MODALITY_TYPE);
        flags = PermissionFlags.get(EntityType.RBAC_ROLE);
        initialize();
    }

    private void initialize() {
        add(mainPanel);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        enableRoleManagmentButtons(RbacUtilities.isEnableRoleEditing());

        // reset cached identity provider names
        IdentityHolder.reset();

        populateList();
        setupButtonListeners();
        setupActionListeners();

        roleList.setModel( filteredListRoleModel );
        roleList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        enableEditRemoveButtons();

        pack();
        getRootPane().setDefaultButton(buttonClose);
        Utilities.setButtonAccelerator(this, buttonHelp, KeyEvent.VK_F1);
        Utilities.centerOnParentWindow(this);
        Utilities.setEscKeyStrokeDisposes(this);
    }

    private void enableRoleManagmentButtons(boolean enable) {
        addRole.setVisible(flags.canCreateSome() && enable);
        removeRole.setVisible(flags.canDeleteSome() && enable);
    }

    private void setupActionListeners() {
        roleList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                    //disable this code for now since we are not allowing the editing of roles in 3.6.
                    //Uncomment this to allow double click editing and enable/disable of the buttons
                    if (e.getClickCount() == 1)
                        enableEditRemoveButtons();
                    else if (e.getClickCount() >= 2) {
                        showEditDialog(getSelectedRole(), new Functions.UnaryVoid<Role>() {
                            @Override
                            public void call(Role role) {
                                updatePropertiesSummary();
                            }
                        });
                    }
            }

        });

        roleList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                enableEditRemoveButtons();
                updatePropertiesSummary();
            }
        });

        TextComponentPauseListenerManager.registerPauseListener( filterTextField, new PauseListenerAdapter() {
            @Override
            public void textEntryPaused( final JTextComponent component, final long msecs ) {
                resetFilter();
            }
        }, 700 );

        filterTextField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // consume the enter key event, do nothing
            }
        });

        filterWarningLabel.setVisible( false );
    }

    private void updatePropertiesSummary() {
        String message = null;

        RoleModel model = ((RoleModel) roleList.getSelectedValue());
        if (model != null) {
            Role role = model.role;
            String roleDescription = role.getDescription();

            StringBuilder sb = new StringBuilder();
            sb.append("<html>");

            if (StringUtils.isNotEmpty(roleDescription)) {
                sb.append(RbacUtilities.getDescriptionString(role, true));
                sb.append("<br>");
            } else {
                //sb.append("<strong>Permissions:</strong><br>");
                sb.append("Users assigned to the ");
                sb.append(role.getName());
                sb.append(" role have the ability to ");
                Set<String> sorted = new TreeSet<String>();
                for(Permission p: role.getPermissions()){
                    StringBuilder sb1 = new StringBuilder();
                    sb1.append(p.getOperation().toString());
                    
                    EntityType etype = p.getEntityType();
                    switch(p.getScope().size()) {
                        case 0:
                            sb1.append("[Any");
                            if (etype == EntityType.ANY)
                                sb1.append(" Object");
                            else {
                                sb1.append(" ").append(etype.getName());
                            }
                            sb1.append("]");
                            break;
                        case 1:
                            break;
                        default:
                            sb1.append("[Complex Scope]");
                    }
                    sorted.add(sb1.toString());
                }
                String [] p = sorted.toArray(new String[sorted.size()]);
                for (int i = 0; i < p.length; i++) {
                    //sb.append(MessageFormat.format("&nbsp&nbsp&nbsp{0}<br>\n", s));
                    if(i == p.length - 1){
                        sb.append(" and ");
                    }else if (i != 0){
                        sb.append(", ");
                    } 
                    sb.append(p[i]);
                }
                sb.append(" the ").append(role.getName());
            }

            sb.append("</html>");
            message = sb.toString();
            //Update the table of identity providers and user / group info
            if( roleAssignmentTableModel == null || roleAssignmentTableModel.getRole() == null || !roleAssignmentTableModel.getRole().equals(role)){
                setUpRoleAssignmentTable(role);
            }
        }else{
            setUpRoleAssignmentTable(null);
        }
        propertiesPane.setText(message);
        propertiesPane.getCaret().setDot(0);
    }

    private void setUpRoleAssignmentTable(Role role){
        try{
            roleAssignmentTableModel = new RoleAssignmentTableModel(role);
        }catch(Exception ex){
            throw new RuntimeException("Could not look up assignments for role", ex);
        }
        this.roleAssigneeTable.setModel(roleAssignmentTableModel);
        //don't allow the user to be able to reorder columns in the table
        this.roleAssigneeTable.getTableHeader().setReorderingAllowed(false);

        Utilities.setRowSorter(roleAssigneeTable, roleAssignmentTableModel, new int[]{0,1}, new boolean[]{true, true},
                new Comparator[]{null, RoleAssignmentTableModel.USER_GROUP_COMPARATOR});
        TableColumn tC = roleAssigneeTable.getColumn(RoleAssignmentTableModel.USER_GROUPS);
        tC.setCellRenderer(new UserGroupTableCellRenderer(roleAssigneeTable));
    }
    
    private void setupButtonListeners() {
        editRole.addActionListener(roleActionListener);
        addRole.addActionListener(roleActionListener);
        removeRole.addActionListener(roleActionListener);

        buttonHelp.addActionListener( new ActionListener(){
            @Override
            public void actionPerformed( final ActionEvent e ) {
                Actions.invokeHelp( RoleManagementDialog.this);
            }
        } );
        buttonClose.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onClose();
            }
        });
    }

    private void enableEditRemoveButtons() {
        boolean validRowSelected = roleList.getModel().getSize() != 0 &&
                roleList.getSelectedValue() != null;

        boolean canRemovePermissions = flags.canDeleteSome();

        removeRole.setEnabled(canRemovePermissions && validRowSelected);
        editRole.setEnabled(validRowSelected);
    }

    private void doUpdateRoleAction(ActionEvent e) {
        Object source = e.getSource();
        if (source == null || !(source instanceof JButton)) {
            return;
        }

        JButton srcButton = (JButton) source;
        if (srcButton == addRole) {
            showEditDialog(new Role(), new Functions.UnaryVoid<Role>() {
                @Override
                public void call(Role newRole) {
                    if (newRole != null) populateList();
                    updatePropertiesSummary();
                }
            });
        } else if (srcButton == editRole) {
            showEditDialog(getSelectedRole(), new Functions.UnaryVoid<Role>() {
                @Override
                public void call(Role r) {
                    if (r != null)setUpRoleAssignmentTable(r);
                    updatePropertiesSummary();
                }
            });
        } else if (srcButton == removeRole) {
            final Role selectedRole = getSelectedRole();
            if (selectedRole == null) return;
            Utilities.doWithConfirmation(
                this,
                resources.getString("manageRoles.deleteTitle"),
                MessageFormat.format(resources.getString("manageRoles.deleteMessage"), selectedRole.getName()),
                new Runnable() {
                    @Override
                    public void run() {
                        try {
                            rbacAdmin.deleteRole(selectedRole);
                            populateList();
                        } catch (Exception e1) {
                            throw new RuntimeException("Couldn't delete Role", e1);
                        }
                    }
                });
            updatePropertiesSummary();
        }
    }

    private Role getSelectedRole() {
        return ((RoleModel)roleList.getSelectedValue()).role;
    }

    private void showEditDialog(Role selectedRole, final Functions.UnaryVoid<Role> result) {
        if (selectedRole == null) {
            result.call(null);
            return;
        }

        final EditRoleDialog dlg = new EditRoleDialog(selectedRole, this);
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            @Override
            public void run() {
                Role updated = dlg.getRole();
                result.call(updated);
            }
        });
    }

    private void populateList() {
        try {
            final Collection<Role> roleCollection = rbacAdmin.findAllRoles();
            final Role[] roles = roleCollection.toArray(new Role[roleCollection.size()]);
            Arrays.sort(roles);
            roleListModel.clear();
            for ( final Role role : roles ) {
                roleListModel.addElement( new RoleModel( role ) );
            }
        } catch (Exception e) {
            throw new RuntimeException("Couldn't get initial list of Roles", e);
        }
    }

    private void resetFilter() {
        final String filterString = filterTextField.getText();
        try {
            filteredListRoleModel.setFilter( getFilter( filterString ) );
            filterWarningLabel.setVisible( !filterString.isEmpty() );
            roleList.getSelectionModel().clearSelection();
        } catch (PatternSyntaxException e) {
            DialogDisplayer.showMessageDialog(
                    this,
                    "Invalid syntax for the regular expression, \"" + filterString + "\"",
                    "Role Filter",
                    JOptionPane.WARNING_MESSAGE,
                    null );
        }
    }

    private Filter<RoleModel> getFilter( final String filter ) {
        final Pattern pattern = Pattern.compile(filter, Pattern.CASE_INSENSITIVE);
        return new Filter<RoleModel>(){
            @Override
            public boolean accept( final RoleModel o ) {
                return pattern.matcher(o.name).find();
            }
        };
    }

    private static class RoleModel {
        private final Role role;
        private final String name;

        private RoleModel(Role role) {
            this.role = role;
            this.name = role.getDescriptiveName();
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private void onClose() {
        dispose();
    }
}
