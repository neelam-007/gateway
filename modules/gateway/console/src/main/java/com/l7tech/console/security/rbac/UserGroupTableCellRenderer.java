package com.l7tech.console.security.rbac;

import com.l7tech.gui.util.Utilities;
import com.l7tech.console.panels.GroupPanel;
import com.l7tech.console.panels.UserPanel;
import com.l7tech.identity.Identity;
import com.l7tech.identity.User;
import com.l7tech.identity.Group;

import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import javax.swing.*;
import java.awt.*;

/**
 * Created by IntelliJ IDEA.
 * User: darmstrong
 * Date: Jul 25, 2008
 * Time: 2:49:30 PM
 * To change this template use File | Settings | File Templates.
 *
 * Class to renender a Column in a JTable which contains an a User / Group icon and text
 */
public class UserGroupTableCellRenderer implements TableCellRenderer {

    private Icon groupIcon = new ImageIcon(Utilities.loadImage(GroupPanel.GROUP_ICON_RESOURCE));
    private Icon userIcon = new ImageIcon(Utilities.loadImage(UserPanel.USER_ICON_RESOURCE));
    private JTable table;

    public UserGroupTableCellRenderer(JTable table){
        this.table = table;
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        TableModel tM = table.getModel();
        //what row does the param row reference in the model??
        //...row is from the JTable's view of the data which can have changed if the table has been sorted
        int modelRow = table.convertRowIndexToModel(row);

        Object colVal = tM.getValueAt(modelRow, column);
        if(colVal instanceof Identity){
            Identity id = (Identity) colVal;
            Icon icon = null;
            String labelValue = null;
            if(id instanceof User){
                User u = (User)id;
                icon = userIcon;
                labelValue =  u.getLogin() +" [" + u.getFirstName()+ " " + u.getLastName() + "]";
            }else if(id instanceof Group){
                Group g = (Group)id;
                icon = groupIcon;
                labelValue = g.getName();
            }
            JLabel label = new JLabel(labelValue, icon, SwingConstants.LEFT);
            if(isSelected){
                label.setBackground(table.getSelectionBackground());
                label.setForeground(table.getSelectionForeground());
                label.setOpaque(true);
            }else{
                label.setBackground(table.getBackground());
                label.setForeground(table.getForeground());
                label.setOpaque(false);
            }

            return label;
        }else{
            return new JLabel("Identity could not be identified");
        }
    }
}
