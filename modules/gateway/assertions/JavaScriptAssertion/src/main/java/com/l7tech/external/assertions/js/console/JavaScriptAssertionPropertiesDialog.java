package com.l7tech.external.assertions.js.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.external.assertions.js.JavaScriptAssertion;
import com.l7tech.gui.util.ClipboardActions;
import com.l7tech.gui.util.Utilities;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

import javax.swing.*;
import java.awt.*;

public class JavaScriptAssertionPropertiesDialog extends AssertionPropertiesOkCancelSupport<JavaScriptAssertion> {
    private JPanel contentPane;
    private JTextArea scriptEditor;

    public JavaScriptAssertionPropertiesDialog( Frame owner, final JavaScriptAssertion assertion ) {
        super( JavaScriptAssertion.class, owner, assertion, true );
        initComponents();
    }

    @Override
    protected JPanel createPropertyPanel() {
        return contentPane;
    }

    private void createUIComponents() {
        RSyntaxTextArea rs = new RSyntaxTextArea();
        scriptEditor = rs;

        Utilities.attachClipboardKeyboardShortcuts( scriptEditor );
        ActionMap am = scriptEditor.getActionMap();
        am.put( "paste-from-clipboard", ClipboardActions.getPasteAction() );
        am.put( "copy-to-clipboard", ClipboardActions.getCopyAction() );
        am.put( "cut-to-clipboard", ClipboardActions.getCutAction() );
        am.put( "paste", ClipboardActions.getPasteAction() );
        am.put( "copy", ClipboardActions.getCopyAction() );
        am.put( "cut", ClipboardActions.getCutAction() );

        rs.setSyntaxEditingStyle( SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT );
    }

    @Override
    public void setData( JavaScriptAssertion assertion ) {
        scriptEditor.setText( assertion.decodeScript() );
    }

    @Override
    public JavaScriptAssertion getData( JavaScriptAssertion assertion ) throws ValidationException {
        assertion.encodeScript( scriptEditor.getText() );
        return assertion;
    }
}
