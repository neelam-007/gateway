package com.l7tech.console.panels;

import com.l7tech.console.action.Actions;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * Dialog for viewing and editing a RemoteIpRange assertion.
 *
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Feb 20, 2004<br/>
 * $Id$<br/>
 *
 */
public class RemoteIpRangePropertiesDialog extends JDialog {
    public RemoteIpRangePropertiesDialog(Frame owner, boolean modal) {
        super(owner, modal);
        initialize();
    }

    private void initialize() {
        setTitle("Remote IP Range Assertion Properties");

        Container contents = getContentPane();
        contents.setLayout(new BorderLayout(0,0));
        contents.add(makeGlobalPanel(), BorderLayout.CENTER);
        contents.add(makeBottomButtonsPanel(), BorderLayout.SOUTH);

        setCallbacks();
    }

    private void cancel() {
        RemoteIpRangePropertiesDialog.this.dispose();
    }

    private void ok() {
        cancel();
    }

    private void setCallbacks() {
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ok();
            }
        });
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                cancel();
            }
        });
        helpButton.addActionListener( new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Actions.invokeHelp(RemoteIpRangePropertiesDialog.this);
            }
        });
    }

    private JPanel makeGlobalPanel() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayout(3, 1, 0, CONTROL_SPACING));

        mainPanel.add(makeRulePanel());
        mainPanel.add(makeNotePanel());
        mainPanel.add(makeIPRangePanel());

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weightx = 1.0;
        constraints.weighty = 1.0;
        constraints.insets = new Insets(BORDER_PADDING, BORDER_PADDING, 0, BORDER_PADDING);
        JPanel bordered = new JPanel();
        bordered.setLayout(new GridBagLayout());
        bordered.add(mainPanel, constraints);
        return bordered;
    }

    private JPanel makeRulePanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));
        includeExcludeCombo = new JComboBox(new String[] {"Include", "Exclude"});
        panel.add(includeExcludeCombo);
        return panel;
    }

    private JPanel makeNotePanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));
        panel.add(new JLabel("the following IP range", null, JLabel.LEFT));
        return panel;
    }

    private JPanel makeIPRangePanel() {
        add1 = new JTextField("888");
        add2 = new JTextField("888");
        add3 = new JTextField("888");
        add4 = new JTextField("888");
        suffix = new JTextField("888");

        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        Insets insets = new Insets(0, 0, 0, CONTROL_SPACING);

        panel.add(add1, new GridBagConstraints(0,0,1,1,0.2,0,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,insets,0,0));
        panel.add(new JLabel("."), new GridBagConstraints(1,0,1,1,0,0,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,insets,0,0));
        panel.add(add2, new GridBagConstraints(2,0,1,1,0.2,0,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,insets,0,0));
        panel.add(new JLabel("."), new GridBagConstraints(3,0,1,1,0,0,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,insets,0,0));
        panel.add(add3, new GridBagConstraints(4,0,1,1,0.2,0,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,insets,0,0));
        panel.add(new JLabel("."), new GridBagConstraints(5,0,1,1,0,0,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,insets,0,0));
        panel.add(add4, new GridBagConstraints(6,0,1,1,0.2,0,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,insets,0,0));
        panel.add(new JLabel("/"), new GridBagConstraints(7,0,1,1,0,0,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,insets,0,0));
        panel.add(suffix, new GridBagConstraints(8,0,1,1,0.2,0,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,insets,0,0));

        return panel;
    }

    private JPanel makeBottomButtonsPanel() {
        // construct buttons
        helpButton = new JButton();
        helpButton.setText("Help");
        okButton = new JButton();
        okButton.setText("Ok");
        cancelButton = new JButton();
        cancelButton.setText("Cancel");

        // construct the bottom panel and wrap it with a border
        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new FlowLayout(FlowLayout.TRAILING, CONTROL_SPACING, 0));
        buttonsPanel.add(helpButton);
        buttonsPanel.add(okButton);
        buttonsPanel.add(cancelButton);

        //  make this panel align to the right
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BorderLayout());
        rightPanel.add(buttonsPanel, BorderLayout.EAST);

        // wrap this with border settings
        JPanel output = new JPanel();
        output.setLayout(new FlowLayout(FlowLayout.TRAILING, BORDER_PADDING-CONTROL_SPACING, BORDER_PADDING));
        output.add(rightPanel);

        return output;
    }

    /**
     * for dev purposes only, to view dlg's layout
     */
    public static void main(String[] args) {
        RemoteIpRangePropertiesDialog me = new RemoteIpRangePropertiesDialog(null, true);
        me.pack();
        me.show();
        System.exit(0);
    }

    private JButton helpButton;
    private JButton okButton;
    private JButton cancelButton;

    private JComboBox includeExcludeCombo;
    private JTextField add1, add2, add3, add4, suffix;

    private final static int BORDER_PADDING = 20;
    private final static int CONTROL_SPACING = 5;
}
