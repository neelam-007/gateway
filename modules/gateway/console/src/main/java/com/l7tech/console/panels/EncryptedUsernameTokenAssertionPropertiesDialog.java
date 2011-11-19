package com.l7tech.console.panels;

import com.l7tech.gui.util.Utilities;
import com.l7tech.policy.assertion.credential.wss.EncryptedUsernameTokenAssertion;
import com.l7tech.security.xml.XencUtil;
import com.l7tech.util.Functions;
import com.l7tech.util.Option;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings({"ToArrayCallWithZeroLengthArrayArgument"})
public class EncryptedUsernameTokenAssertionPropertiesDialog extends AssertionPropertiesOkCancelSupport<EncryptedUsernameTokenAssertion> {
    private JPanel contentPane;
    private JCheckBox specifyPermittedEncryptionMethodsCheckBox;
    private JCheckBox aes128CheckBox;
    private JCheckBox aes192CheckBox;
    private JCheckBox aes256CheckBox;
    private JCheckBox tripleDESCheckBox;
    private JCheckBox aes128GcmCheckBox;
    private JCheckBox aes256GcmCheckBox;

    public EncryptedUsernameTokenAssertionPropertiesDialog(Window owner, EncryptedUsernameTokenAssertion assertion) {
        super(assertion.getClass(), owner, assertion, true);
        initComponents();
        setData(assertion);
    }

    @Override
    public void setData(EncryptedUsernameTokenAssertion assertion) {
        List<String> algs = assertion.getXEncAlgorithmList();
        boolean custom = algs != null;
        specifyPermittedEncryptionMethodsCheckBox.setSelected(custom);
        setCipherSelected(true, custom ? algs.toArray(new String[algs.size()]) : ALGS);
        updateEnableState();
    }

    @Override
    public EncryptedUsernameTokenAssertion getData(EncryptedUsernameTokenAssertion assertion) throws ValidationException {
        assertion.setXEncAlgorithmList(specifyPermittedEncryptionMethodsCheckBox.isSelected() ? makeAlgList() : null);
        return assertion;
    }

    private void updateEnableState() {
        setCiphersEnabled(specifyPermittedEncryptionMethodsCheckBox.isSelected(), ALGS);
    }

    final Map<String, JCheckBox> ALGMAP = new LinkedHashMap<String, JCheckBox>() {{
        put(XencUtil.AES_128_GCM, aes128GcmCheckBox);
        put(XencUtil.AES_256_GCM, aes256GcmCheckBox);
        put(XencUtil.AES_128_CBC, aes128CheckBox);
        put(XencUtil.AES_192_CBC, aes192CheckBox);
        put(XencUtil.AES_256_CBC, aes256CheckBox);
        put(XencUtil.TRIPLE_DES_CBC, tripleDESCheckBox);
    }};

    final String[] ALGS = ALGMAP.keySet().toArray(new String[0]);
    final JCheckBox[] ALGCHECKBOXES = ALGMAP.values().toArray(new JCheckBox[0]);

    private void setCipherSelected(final boolean selected, String... uri) {
        forEachCipher(checkboxSelector(selected), uri);
    }

    private void setCiphersEnabled(final boolean enabled, String... uri) {
        forEachCipher(checkboxEnabler(enabled), uri);
    }

    private static Functions.UnaryVoid<JCheckBox> checkboxEnabler(final boolean enabled) {
        return new Functions.UnaryVoid<JCheckBox>() {
            @Override
            public void call(JCheckBox jCheckBox) {
                jCheckBox.setEnabled(enabled);
            }
        };
    }

    private static Functions.UnaryVoid<JCheckBox> checkboxSelector(final boolean selected) {
        return new Functions.UnaryVoid<JCheckBox>() {
            @Override
            public void call(JCheckBox cb) {
                cb.setSelected(selected);
            }
        };
    }

    private List<String> makeAlgList() {
        return Functions.grep(ALGMAP.keySet(), new Functions.Unary<Boolean, String>() {
            @Override
            public Boolean call(String uri) {
                return isCipherSelected(uri);
            }
        });
    }

    private boolean isCipherSelected(String uri) {
        final Option<JCheckBox> cb = Option.optional(ALGMAP.get(uri));
        return cb.isSome() && cb.some().isSelected();
    }

    private void forEachCipher(Functions.UnaryVoid<JCheckBox> action, String... uri) {
        for (String s : uri) {
            Option.optional(ALGMAP.get(s)).foreach(action);
        }
    }

    @Override
    protected JPanel createPropertyPanel() {
        Utilities.enableGrayOnDisabled(ALGCHECKBOXES);
        specifyPermittedEncryptionMethodsCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateEnableState();
            }
        });
        return contentPane;
    }
}
