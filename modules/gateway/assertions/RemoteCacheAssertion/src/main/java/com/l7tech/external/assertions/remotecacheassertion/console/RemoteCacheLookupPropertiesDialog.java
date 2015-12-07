package com.l7tech.external.assertions.remotecacheassertion.console;

import com.l7tech.console.panels.AssertionPropertiesEditorSupport;
import com.l7tech.console.util.Registry;
import com.l7tech.external.assertions.remotecacheassertion.*;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.gateway.common.cluster.ClusterStatusAdmin;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.variable.Syntax;
import org.apache.commons.codec.binary.Base64;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 10/11/11
 * Time: 10:25 AM
 * To change this template use File | Settings | File Templates.
 */
public class RemoteCacheLookupPropertiesDialog extends AssertionPropertiesEditorSupport<RemoteCacheLookupAssertion> {
    private JPanel mainPanel;
    private JComboBox remoteCacheComboBox;
    private JTextField cacheEntryKeyField;
    private JButton okButton;
    private JButton cancelButton;

    public static final String TITLE = "Remote Cache Lookup Properties";

    /** @noinspection ThisEscapedInObjectConstruction*/
    private final InputValidator validator = new InputValidator(this, TITLE);
    private boolean confirmed = false;

    private static class RemoteCacheEntry {
        private Goid goid;
        private String name;

        public RemoteCacheEntry(Goid goid, String name) {
            this.goid = goid;
            this.name = name;
        }

        public Goid getGoid() {
            return goid;
        }

        public String getName() {
            return name;
        }

        public String toString() {
            return name;
        }
    }

    public RemoteCacheLookupPropertiesDialog(Window owner) {
        super(owner, TITLE);
        init();
    }

    private void init() {
        setContentPane(mainPanel);
        setModal(true);

        loadRemoteCacheList();
        
        getRootPane().setDefaultButton(okButton);

        validator.attachToButton(okButton, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        validator.addRule(new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                RemoteCacheEntry entry = (RemoteCacheEntry)remoteCacheComboBox.getSelectedItem();

                if(entry == null) {
                    return "No servers were specified.";
                }

                return null;
            }
        });

        validator.constrainTextFieldToBeNonEmpty("Cache entry key", cacheEntryKeyField, new InputValidator.ComponentValidationRule(cacheEntryKeyField) {
            @Override
            public String getValidationError() {
                String[] refs = Syntax.getReferencedNames(cacheEntryKeyField.getText());
                if (refs == null || refs.length < 1)
                    return "The cache entry key must contain at least one interpolated context variable.";
                return null;
            }
        });
        Utilities.setEscKeyStrokeDisposes(this);
    }

    private void onOK() {
        confirmed = true;
        dispose();
    }

    private void onCancel() {
        confirmed = false;
        dispose();
    }

    @Override
    public boolean isConfirmed() {
        return confirmed;
    }

    @Override
    public void setData(RemoteCacheLookupAssertion ass) {

        for(int i = 0;i < remoteCacheComboBox.getItemCount();i++) {
            RemoteCacheEntry entry = (RemoteCacheEntry)remoteCacheComboBox.getItemAt(i);
            if(entry != null && entry.getGoid().equals(ass.getRemoteCacheGoid())) {
                remoteCacheComboBox.setSelectedIndex(i);
                break;
            }
        }

        cacheEntryKeyField.setText(ass.getCacheEntryKey() == null ? "" : ass.getCacheEntryKey());
    }

    @Override
    public RemoteCacheLookupAssertion getData(RemoteCacheLookupAssertion ass) {
        ass.setRemoteCacheGoid(((RemoteCacheEntry)remoteCacheComboBox.getSelectedItem()).getGoid());
        ass.setCacheEntryKey(cacheEntryKeyField.getText());

        return ass;
    }


    private void loadRemoteCacheList() {
        try {
            DefaultComboBoxModel model = new DefaultComboBoxModel();
            RemoteCacheEntityAdmin remoteCacheAdmin = Registry.getDefault().getExtensionInterface(RemoteCacheEntityAdmin.class, null);

            //add empty first element to combo box
            model.addElement(null);

            for(RemoteCacheEntity remoteCache :remoteCacheAdmin.findAll()) {
                model.addElement(new RemoteCacheEntry(remoteCache.getGoid(), remoteCache.getName()));
            }

            remoteCacheComboBox.setModel(model);
        } catch(FindException fe) {
        }
    }

}
