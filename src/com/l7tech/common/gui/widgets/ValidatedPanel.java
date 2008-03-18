/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.common.gui.widgets;

import javax.swing.*;
import java.awt.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Note: every concrete implementation *must* call {@link #init} from each constructor!
 * @param <V> the payload value type (returned from {@link #getModel()})
 * @author alex
 */
public abstract class ValidatedPanel<V> extends JPanel {
    private static final Pattern SPACER = Pattern.compile("\\s+");

    protected String propertyName;
    protected volatile boolean syntaxOk;
    protected JLabel statusLabel;

    protected ValidatedPanel() {
        this("defaultPropertyName");
    }

    public ValidatedPanel(String propertyName) {
        this.propertyName = propertyName;
    }

    /**
     * Call this in your constructor to set the {@link JLabel} that should receive status messages.
     *
     * If null or not set, status messages will not be visible.
     */
    protected void setStatusLabel(JLabel statusLabel) {
        Font font = statusLabel.getFont();
        float size = Math.round(font.getSize() * 1.5) / 2f;
        statusLabel.setFont(font.deriveFont(size));
        statusLabel.setText(null);
        this.statusLabel = statusLabel;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    /**
     * Return an object representing your model, whether valid or not.
     * @return an object representing this panel's model.
     */
    protected abstract V getModel();

    /**
     * Validate the syntax of the entered model, and return null if it's valid, or an error message if
     * it's invalid.  Returning non-null will cause one {@link java.beans.PropertyChangeEvent} to be
     * fired on the property "ok" with the value {@link Boolean#FALSE}, and another to be fired with
     * the property {@link #getPropertyName()} with the value <code>null</code>.
     * @param model the model to validate
     * @return null if the text is valid, or an error message if it's invalid
     */
    protected String getSyntaxError(V model) {
        return null;
    }

    /**
     * Validate the entered model semantically, and return null if it's valid, or an error message if
     * it's invalid.  Returning non-null will cause the supplied message to be displayed in the GUI,
     * but will not prevent the value from being submitted to the caller.
     *
     * @param model the model to validate
     * @return null if the text is valid, or an error message if it's invalid
     */
    protected String getSemanticError(V model) {
        return null;
    }

    /**
     * Called when {@link #getSyntaxError} has returned true.  Components should be updated to display
     * their "correct" view.
     */
    protected void goodSyntax() { }

    /**
     * Called when {@link #getSyntaxError} has returned false.  Components should be updated to display
     * their "incorrect" view.
     */
    protected void badSyntax() { }

    /**
     * Called from the constructor to initialize components.  You probably want to add listeners that
     * will call checkSyntax(), and maybe more that will call checkSemantic(), when your model is likely
     * to have changed.
     */
    protected abstract void initComponents();

    /**
     * Checks the syntactic validity of the model.  Should run quickly enough to be called from
     * frequent events, e.g. {@link javax.swing.event.DocumentListener} for text components.
     */
    protected void checkSyntax() {
        V model = getModel();
        if (model == null) return;

        String statusString = getSyntaxError(model);
        if (statusString == null) {
            syntaxOk = true;
            goodSyntax();
            if (statusLabel != null) statusLabel.setText(null);
            fireGood(model);
        } else {
            syntaxOk = false;
            badSyntax();
            setStatusString(statusString, Font.BOLD);
            fireBad();
        }
    }



    /**
     * Checks the semantic validity of the model.  Might run slowly, so should only be called from
     * infrequent events, e.g. {@link com.l7tech.common.gui.util.PauseListener}.
     */
    protected void checkSemantic() {
        if (!syntaxOk) return;

        V model = getModel();
        if (model == null) return;

        String err = getSemanticError(model);
        if (err == null) {
            goodSyntax();
        } else {
            setStatusString(err, Font.PLAIN);
        }
    }

    /**
     * Sets the status label text, if {@link #setStatusLabel} has been set to a non-null {@link JLabel},
     * to the supplied string, with the specified font attribute (e.g. {@link Font#BOLD})
     * @param status the status label text to be set
     * @param fontAttrs the font attribute(s) to use (e.g. "{@link Font#ITALIC} | {@link Font#BOLD}")
     */
    protected void setStatusString(String status, int fontAttrs) {
        if (statusLabel != null) {
            Font font = statusLabel.getFont();
            statusLabel.setFont(font.deriveFont(fontAttrs));
            statusLabel.setText("<html>" + nospace(status));
        }
    }

    /**
     * This <em>must</em> be called at the end of your constructor(s), for unknown reasons related to the way the IDEA 
     * GUI builder works.
     */
    protected void init() {
        checkSyntax();
        setLayout(new BorderLayout());
        initComponents();
    }

    public boolean isSyntaxOk() {
        return syntaxOk;
    }

    private void fireGood(V model) {
        firePropertyChange(propertyName, null, model);
        firePropertyChange("ok", false, true);
    }

    private void fireBad() {
        firePropertyChange(propertyName, null, null);
        firePropertyChange("ok", true, false);
    }

    private String nospace(String s) {
        Matcher m = SPACER.matcher(s);
        return m.replaceAll("&nbsp;");
    }

    public abstract void focusFirstComponent();

    /**
     * Called when the OK button is pressed; it's now time to update the model from the view.
     */
    public void updateModel() {
        doUpdateModel();
    }

    protected abstract void doUpdateModel();
}
