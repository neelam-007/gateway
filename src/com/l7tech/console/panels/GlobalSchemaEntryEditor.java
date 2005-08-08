/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Aug 8, 2005<br/>
 */
package com.l7tech.console.panels;

import com.japisoft.xmlpad.XMLContainer;
import com.japisoft.xmlpad.PopupModel;
import com.japisoft.xmlpad.UIAccessibility;
import com.japisoft.xmlpad.action.ActionModel;

import javax.swing.*;
import java.awt.*;

/**
 * Called by the GlobalSchemaDialog, this is used to edit an existing or add a new global schema entry.
 *
 * @author flascelles@layer7-tech.com
 */
public class GlobalSchemaEntryEditor extends JDialog {
    private JPanel xmlDisplayPanel;
    private JTextField schemanametxt;
    private JButton helpbutton;
    private JButton cancelbutton;
    private JButton okbutton;
    private JPanel mainPanel;
    private XMLContainer xmlContainer;
    private UIAccessibility uiAccessibility;

    public GlobalSchemaEntryEditor(JDialog owner) {
        super(owner, true);
        initialize();
    }

    private void initialize() {
        setContentPane(mainPanel);
        setTitle("Edit Global Schema");
        // set xml view
        xmlContainer = new XMLContainer(true);
        uiAccessibility = xmlContainer.getUIAccessibility();
        uiAccessibility.setTreeAvailable(false);
        uiAccessibility.setToolBarAvailable(false);
        xmlContainer.setStatusBarAvailable(false);
        PopupModel popupModel = xmlContainer.getPopupModel();
        // remove the unwanted actions
        popupModel.removeAction(ActionModel.getActionByName(ActionModel.LOAD_ACTION));
        popupModel.removeAction(ActionModel.getActionByName(ActionModel.SAVE_ACTION));
        popupModel.removeAction(ActionModel.getActionByName(ActionModel.SAVEAS_ACTION));
        popupModel.removeAction(ActionModel.getActionByName(ActionModel.NEW_ACTION));
        xmlContainer.getTreePopupModel().removeAction(ActionModel.getActionByName(ActionModel.TREE_SELECTNODE_ACTION));
        xmlContainer.getTreePopupModel().removeAction(ActionModel.getActionByName(ActionModel.TREE_COMMENTNODE_ACTION));
        xmlContainer.getTreePopupModel().removeAction(ActionModel.getActionByName(ActionModel.TREE_COPYNODE_ACTION));
        xmlContainer.getTreePopupModel().removeAction(ActionModel.getActionByName(ActionModel.TREE_CUTNODE_ACTION));
        xmlContainer.getTreePopupModel().removeAction(ActionModel.getActionByName(ActionModel.TREE_EDITNODE_ACTION));
        xmlContainer.getTreePopupModel().removeAction(ActionModel.getActionByName(ActionModel.TREE_CLEANHISTORY_ACTION));
        xmlContainer.getTreePopupModel().removeAction(ActionModel.getActionByName(ActionModel.TREE_ADDHISTORY_ACTION));
        xmlContainer.getTreePopupModel().removeAction(ActionModel.getActionByName(ActionModel.TREE_PREVIOUS_ACTION));
        xmlContainer.getTreePopupModel().removeAction(ActionModel.getActionByName(ActionModel.TREE_NEXT_ACTION));
        xmlDisplayPanel.setLayout(new BorderLayout());
        xmlDisplayPanel.add(xmlContainer.getView(), BorderLayout.CENTER);
    }
}
