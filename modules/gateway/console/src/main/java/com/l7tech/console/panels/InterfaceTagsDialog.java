package com.l7tech.console.panels;

import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.gateway.common.transport.InterfaceTag;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.OkCancelDialog;
import com.l7tech.gui.widgets.ValidatedPanel;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;

import javax.swing.*;
import java.awt.*;
import java.text.ParseException;
import java.util.Collections;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class InterfaceTagsDialog extends OkCancelDialog<Set<InterfaceTag>> {
    private static final Logger logger = Logger.getLogger(InterfaceTagsDialog.class.getName());

    public InterfaceTagsDialog(Frame owner, String title, boolean modal, Set<InterfaceTag> model) {
        super(owner, title, modal, makePanel(model));
    }

    public InterfaceTagsDialog(Frame owner, String title, boolean modal, Set<InterfaceTag> model, boolean readOnly) {
        super(owner, title, modal, makePanel(model), readOnly);
    }

    public InterfaceTagsDialog(Dialog owner, String title, boolean modal, Set<InterfaceTag> model) {
        super(owner, title, modal, makePanel(model));
    }

    public InterfaceTagsDialog(Dialog owner, String title, boolean modal, Set<InterfaceTag> model, boolean readOnly) {
        super(owner, title, modal, makePanel(model), readOnly);
    }

    private static ValidatedPanel makePanel(Set<InterfaceTag> model) {
        return new InterfaceTagsPanel(model);
    }

    /**
     * Display the Manage Interface Tags dialog, and invoke the specified callback
     * asynchronously if dialog is OK'ed with changes.
     *
     * @param parent parent component or null.
     * @param callback  callback to invoke if the dialog is confirmed, or null.  When the callback is invoked, the
     *                  interface tags have already been updated on the server.  The callback is passed
     *                  a single argument: the new value of the interfaceTags cluster property.
     */
    public static void show(Frame parent, Functions.UnaryVoid<String> callback) {
        doShow(new InterfaceTagsDialog(parent, "Manage Interface Tags", true, loadCurrentInterfaceTagsFromServer(), false), callback);
    }

    /**
     * Display the Manage Interface Tags dialog, and invoke the specified callback
     * asynchronously if dialog is OK'ed with changes.
     *
     * @param parent parent component or null.
     * @param callback  callback to invoke if the dialog is confirmed, or null.  When the callback is invoked, the
     *                  interface tags have already been updated on the server.  The callback is passed
     *                  a single argument: the new value of the interfaceTags cluster property.
     */
    public static void show(Dialog parent, Functions.UnaryVoid<String> callback) {
        doShow(new InterfaceTagsDialog(parent, "Manage Interface Tags", true, loadCurrentInterfaceTagsFromServer(), false), callback);
    }

    private static void reportError(String title, String message, Throwable t) {
        message = message + ": " + ExceptionUtils.getMessage(t);
        logger.log(Level.WARNING, message, ExceptionUtils.getDebugException(t));
        DialogDisplayer.showMessageDialog(TopComponents.getInstance().getTopParent(), message, title, JOptionPane.ERROR_MESSAGE, null);
    }

    private static void doShow(final InterfaceTagsDialog dlg, final Functions.UnaryVoid<String> callback) {
        final String oldStringForm = InterfaceTag.toString(dlg.getValue());
        dlg.setModal(true);
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            public void run() {
                if (dlg.wasOKed()) {
                    Set<InterfaceTag> newTags = dlg.getValue();
                    String stringForm = InterfaceTag.toString(newTags);
                    if (oldStringForm != null && oldStringForm.equals(stringForm))
                        return;
                    try {
                        ClusterProperty cprop = Registry.getDefault().getClusterStatusAdmin().findPropertyByName(InterfaceTag.PROPERTY_NAME);
                        if (cprop == null) cprop = new ClusterProperty(InterfaceTag.PROPERTY_NAME, stringForm);
                        else cprop.setValue(stringForm);
                        Registry.getDefault().getClusterStatusAdmin().saveProperty(cprop);
                        if (callback != null)
                            callback.call(stringForm);
                    } catch (SaveException e) {
                        reportError("Unable to Save Interface Tags", "Unable to save interface tags", e);
                    } catch (UpdateException e) {
                        reportError("Unable to Save Interface Tags", "Unable to save interface tags", e);
                    } catch (DeleteException e) {
                        reportError("Unable to Save Interface Tags", "Unable to save interface tags", e);
                    } catch (FindException e) {
                        reportError("Unable to Save Interface Tags", "Unable to save interface tags", e);
                    }
                }
            }
        });
    }

    private static Set<InterfaceTag> loadCurrentInterfaceTagsFromServer() {
        try {
            ClusterProperty property = Registry.getDefault().getClusterStatusAdmin().findPropertyByName(InterfaceTag.PROPERTY_NAME);
            if (property == null)
                return Collections.emptySet();
            return InterfaceTag.parseMultiple(property.getValue());
        } catch (FindException e) {
            reportError("Unable to Load Interface Tags", "Unable to load interface tags", e);
            return Collections.emptySet();
        } catch (ParseException e) {
            reportError("Unable to Load Interface Tags", "Unable to load interface tags", e);
            return Collections.emptySet();
        }
    }
}
