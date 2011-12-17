package com.l7tech.console.util;

import com.japisoft.xmlpad.PopupModel;
import com.japisoft.xmlpad.UIAccessibility;
import com.japisoft.xmlpad.XMLContainer;
import com.japisoft.xmlpad.action.ActionModel;
import com.japisoft.xmlpad.action.XMLAction;
import com.japisoft.xmlpad.action.XMLActionForSelection;
import com.japisoft.xmlpad.editor.XMLEditor;
import com.l7tech.gui.util.ClipboardActions;

import javax.swing.*;
import javax.swing.text.Caret;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ResourceBundle;


public class XMLContainerFactory {
    private static boolean clipboardActionsReplaced = false;
    private static ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.resources.XMLContainerFactory");

    public static XMLContainer createXmlContainer(boolean autoDisposeMode) {
        replaceClipboardActions();

        final XMLContainer xmlContainer =  new XMLContainer(autoDisposeMode);

        UIAccessibility uiAccessibility = xmlContainer.getUIAccessibility();
        uiAccessibility.setTreeAvailable(false);
        uiAccessibility.setTreeToolBarAvailable(false);
        uiAccessibility.setToolBarAvailable(false);
        xmlContainer.setStatusBarAvailable(false);
        xmlContainer.setErrorPanelAvailable(false);

        uiAccessibility.getEditor().addPropertyChangeListener("editable",new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                ActionModel.resetActionState(xmlContainer);
            }
        });

        PopupModel popupModel = xmlContainer.getPopupModel();
        popupModel.removeAction(ActionModel.getActionByName(ActionModel.LOAD_ACTION));
        popupModel.removeAction(ActionModel.getActionByName(ActionModel.PARSE_ACTION));
        popupModel.removeAction(ActionModel.getActionByName(ActionModel.SAVE_ACTION));
        popupModel.removeAction(ActionModel.getActionByName(ActionModel.SAVEAS_ACTION));
        popupModel.removeAction(ActionModel.getActionByName(ActionModel.NEW_ACTION));
        popupModel.removeAction(ActionModel.getActionByName(ActionModel.FORMAT_ACTION));
        popupModel.removeAction(ActionModel.getActionByName(ActionModel.TREE_SELECTNODE_ACTION));
        popupModel.removeAction(ActionModel.getActionByName(ActionModel.TREE_COMMENTNODE_ACTION));
        popupModel.removeAction(ActionModel.getActionByName(ActionModel.TREE_COPYNODE_ACTION));
        popupModel.removeAction(ActionModel.getActionByName(ActionModel.TREE_CUTNODE_ACTION));
        popupModel.removeAction(ActionModel.getActionByName(ActionModel.TREE_EDITNODE_ACTION));
        popupModel.removeAction(ActionModel.getActionByName(ActionModel.TREE_CLEANHISTORY_ACTION));
        popupModel.removeAction(ActionModel.getActionByName(ActionModel.TREE_ADDHISTORY_ACTION));
        popupModel.removeAction(ActionModel.getActionByName(ActionModel.TREE_PREVIOUS_ACTION));
        popupModel.removeAction(ActionModel.getActionByName(ActionModel.TREE_NEXT_ACTION));

        if(TopComponents.getInstance().isApplet()){
            // Search action tries to get the class loader
            popupModel.removeAction(ActionModel.getActionByName(ActionModel.INSERT_ACTION));
            popupModel.removeAction(ActionModel.getActionByName(ActionModel.SEARCH_ACTION));
            popupModel.removeAction(ActionModel.getActionByName(ActionModel.COMMENT_ACTION));
        }

        boolean lastWasSeparator = true; // remove trailing separator
        for (int i=popupModel.size()-1; i>=0; i--) {
            boolean isSeparator = popupModel.isSeparator(i);
            if (isSeparator && (i==0 || lastWasSeparator)) {
                popupModel.removeSeparator(i);
            } else {
                lastWasSeparator = isSeparator;
            }
        }

        return xmlContainer;
    }

    protected static void replaceClipboardActions() {
        // only need to be done once
        if (clipboardActionsReplaced) return;

        ActionModel.replaceActionByName(ActionModel.PASTE_ACTION, new XMLAction() {
            @Override
            protected String getDefaultLabel() {
                return resources.getString("paste");
            }

            @Override
            protected KeyStroke getDefaultAccelerator() {
                return KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_MASK);
            }

            @Override
            public void setXMLEditor(XMLEditor editor) {
                super.setXMLEditor(editor);
                if (editor != null){
                    editor.setAction(getDefaultAccelerator(), this);
                    setEnabled(editor.isEditable());
                }
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                Clipboard clipboard = ClipboardActions.getClipboard();
                String name = (String) getValue(Action.NAME);
                if (name == null) return;

                if (clipboard == null) {
                    return;
                }
                if (!clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor))
                    return;

                Transferable transferable = clipboard.getContents(null);
                try {
                    String text = (String) transferable.getTransferData(DataFlavor.stringFlavor);
                    if (text == null) {
                        return;
                    }
                    Caret pos = editor.getCaret();
                    editor.getDocument().remove(Math.min(pos.getDot(), pos.getMark()), Math.abs(pos.getDot() - pos.getMark()));
                    editor.getDocument().insertString(Math.min(pos.getDot(), pos.getMark()), text, null);
                } catch (Exception ex) {
                    return;
                }
            }

            @Override
            public boolean notifyAction() {
                return VALID_ACTION;
            }
        });
        ActionModel.replaceActionByName(ActionModel.COPY_ACTION, new XMLActionForSelection() {
            @Override
            protected String getDefaultLabel() {
                return resources.getString("copy");
            }


            @Override
            protected KeyStroke getDefaultAccelerator() {
                return KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_MASK);
            }

            @Override
            public boolean notifyAction() {
                return VALID_ACTION;
            }

            @Override
            public void setXMLEditor(XMLEditor editor) {
                if (editor != null)
                    editor.setAction(getDefaultAccelerator(), this);
                super.setXMLEditor(editor);
            }

            public void actionPerformed(ActionEvent e) {
                Clipboard clipboard = ClipboardActions.getClipboard();
                String name = (String) getValue(Action.NAME);
                if (name == null) return;

                if (clipboard == null) {
                    return;
                }
                try {
                    String text = editor.getSelectedText();
                    if (text == null) {
                        return;
                    }
                    clipboard.setContents(new StringSelection(text), null);
                } catch (Exception ex) {
                    return;
                }
            }
        });

        ActionModel.replaceActionByName(ActionModel.CUT_ACTION, new XMLActionForSelection() {

            @Override
            protected String getDefaultLabel() {
                return resources.getString("cut");
            }


            @Override
            protected KeyStroke getDefaultAccelerator() {
                return KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_MASK);
            }

            @Override
            public boolean notifyAction() {
                return VALID_ACTION;
            }

            @Override
            public void setXMLEditor(XMLEditor editor) {
                if (editor != null)
                    editor.setAction(getDefaultAccelerator(), this);
                super.setXMLEditor(editor);
            }

            @Override
            public void setEnabled(boolean newValue) {
                super.setEnabled( editor==null ? newValue: (editor.isEditable() && newValue));
            }

            public void actionPerformed(ActionEvent e) {
                Clipboard clipboard = ClipboardActions.getClipboard();
                String name = (String) getValue(Action.NAME);
                if (name == null) return;

                if (clipboard == null) {
                    return;
                }
                try {
                    String text = editor.getSelectedText();
                    if (text == null) {
                        return;
                    }
                    Caret pos = editor.getCaret();
                    editor.getDocument().remove(Math.min(pos.getDot(), pos.getMark()), Math.abs(pos.getDot() - pos.getMark()));
                    clipboard.setContents(new StringSelection(text), null);
                } catch (Exception ex) {
                    return;
                }
            }
        });
        clipboardActionsReplaced = true;
    }
}
