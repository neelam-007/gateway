/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.console.panels;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;

/**
 *
 * @author mike
 * @version 1.0
 */
public class CancelableOperationDialog extends JDialog {
    private CancelableOperation cancelableOperation;

    public static class OperationCanceledException extends Exception {
        public OperationCanceledException() {
        }

        public OperationCanceledException(String message) {
            super(message);
        }

        public OperationCanceledException(String message, Throwable cause) {
            super(message, cause);
        }

        public OperationCanceledException(Throwable cause) {
            super(cause);
        }
    }

    public static interface CancelableOperation {
        void runOperation() throws Exception;
    }

    public CancelableOperationDialog(CancelableOperation cancelableOperation) {
        this.cancelableOperation = cancelableOperation;
    }

    public void runOperation() throws OperationCanceledException, InvocationTargetException {
        // TODO: Run operation in seperate thread.  If operation continues for longer than 400ms,
        // display a dialog with a Cancel button.  In any case do not return until either the operation
        // finishes or the cancel button is pressed.
        try {
            cancelableOperation.runOperation();
        } catch (OperationCanceledException e) {
            throw e;
        } catch (Exception e) {
            throw new InvocationTargetException(e);
        }
    }
}
