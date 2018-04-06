package com.l7tech.external.assertions.email.console;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * This listener class helps in handling all document updates like insert, remove and change using 1 method call.
 */
public abstract class DocumentAdapter implements DocumentListener {

    public enum DocumentUpdateType {
        INSERT,
        REMOVE,
        CHANGED
    }

    @Override
    public void insertUpdate(DocumentEvent e) {
        documentUpdate(e, DocumentUpdateType.INSERT);
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        documentUpdate(e, DocumentUpdateType.REMOVE);
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
        documentUpdate(e, DocumentUpdateType.CHANGED);
    }

    /**
     * Handles insert, remove and change document updates.
     * @param e Docuemnt Event.
     * @param updateType Type of update like insert, remove, changed.
     */
    public abstract void documentUpdate(DocumentEvent e, DocumentUpdateType updateType);
}
