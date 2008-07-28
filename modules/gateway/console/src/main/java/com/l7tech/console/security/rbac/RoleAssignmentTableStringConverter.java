package com.l7tech.console.security.rbac;

import com.l7tech.identity.Identity;
import com.l7tech.identity.User;
import com.l7tech.identity.Group;

import javax.swing.table.TableStringConverter;
import javax.swing.table.TableModel;
import javax.swing.*;

/**
 * Created by IntelliJ IDEA.
 * User: darmstrong
 * Date: Jul 25, 2008
 * Time: 2:47:03 PM
 * This is what allows for the behaviour described in the funcspec:
 * http://sarek.l7tech.com/mediawiki/index.php?title=Role_Assignment_to_Group#Role_Management_UI
 * The a and b below ensure that users are always sorted before groups
 * This does not show in the view, what happens is that the TableRowSorter finds a JLabel in the
 * column to be sorted, it comes to it's TableStringConverter, which we set, to find out
 * how to sort these non String objects.
 * */

public class RoleAssignmentTableStringConverter extends TableStringConverter{

    public String toString(TableModel model, int row, int column) {
        Object val = model.getValueAt(row, column);
        if(val instanceof Identity){
            Identity id = (Identity) val;
            if(id instanceof User){
                User u = (User)id;
                return id.getProviderId()+"a"+u.getLogin() +" [" + u.getFirstName()+ " " + u.getLastName() + "]";
            }else if(id instanceof Group){
                Group g = (Group)id;
                return id.getProviderId()+"b"+g.getName();
            }
        }
        //If not a user or group
        return val.toString();
    }
}
