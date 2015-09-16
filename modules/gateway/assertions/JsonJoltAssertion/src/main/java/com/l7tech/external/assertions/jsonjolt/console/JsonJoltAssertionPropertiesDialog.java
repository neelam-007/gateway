package com.l7tech.external.assertions.jsonjolt.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.external.assertions.jsonjolt.JsonJoltAssertion;
import com.l7tech.gui.util.ClipboardActions;
import com.l7tech.gui.util.Utilities;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class JsonJoltAssertionPropertiesDialog extends AssertionPropertiesOkCancelSupport<JsonJoltAssertion> {
    private JPanel contentPane;
    private JTextArea expressionTextArea;

    private void createUIComponents() {
        RSyntaxTextArea rsta = new RSyntaxTextArea();

        Utilities.attachClipboardKeyboardShortcuts( rsta );
        ActionMap am = rsta.getActionMap();
        am.put( "paste-from-clipboard", ClipboardActions.getPasteAction() );
        am.put( "copy-to-clipboard", ClipboardActions.getCopyAction() );
        am.put( "cut-to-clipboard", ClipboardActions.getCutAction() );
        am.put( "paste", ClipboardActions.getPasteAction() );
        am.put( "copy", ClipboardActions.getCopyAction() );
        am.put( "cut", ClipboardActions.getCutAction() );
        rsta.setSyntaxEditingStyle( SyntaxConstants.SYNTAX_STYLE_JSON );

        this.expressionTextArea = rsta;
    }

    public JsonJoltAssertionPropertiesDialog( final Frame parent, final JsonJoltAssertion assertion ) {
        super( JsonJoltAssertion.class, parent, assertion, true );
        initComponents();
    }

    @Override
    public void setData( JsonJoltAssertion assertion ) {
        expressionTextArea.setText( assertion.getSchemaExpression() );
    }

    @Override
    public JsonJoltAssertion getData( JsonJoltAssertion assertion ) throws ValidationException {
        assertion.setSchemaExpression( expressionTextArea.getText() );
        return assertion;
    }

    @Override
    protected JPanel createPropertyPanel() {
        return contentPane;
    }
}
