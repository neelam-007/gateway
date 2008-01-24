package com.l7tech.console.panels;

import com.l7tech.policy.assertion.SqlAttackAssertion;
import com.l7tech.common.gui.util.Utilities;

import javax.swing.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Set;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Created by IntelliJ IDEA.
 * User: megery
 * Date: Sep 28, 2005
 * Time: 9:56:59 AM
 * To change this template use File | Settings | File Templates.
 */
public class SqlAttackDialogTest {

    private JFrame initialFrame = new JFrame();
    private JPanel panel = new JPanel();
    private JLabel label = new JLabel("Select which protections will be tested in the dialog");
    private JButton okButton = new JButton("OK");
    private SqlAttackAssertion sqlAssertion;

    private void doDialogTest() {
        initialFrame.dispose();
        Component[] comps = panel.getComponents();
        for (int i = 0; i < comps.length; i++) {
            Component component = comps[i];
            if (component instanceof JCheckBox && ((JCheckBox)component).isSelected()) {
                sqlAssertion.setProtection(((JCheckBox)component).getText());
            }
        }

        //here's what was selected
        Set selected = sqlAssertion.getProtections();
        Iterator iter = selected.iterator();

        System.out.println("Selected Items Before Dialog");
        while(iter.hasNext()) {
            System.out.println((String)iter.next());
        }

        //assertion should be prepared now
        JFrame parentFrame = new JFrame();
        parentFrame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        parentFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        SqlAttackDialog dlg = new SqlAttackDialog(parentFrame, sqlAssertion, true, false);
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        dlg.setVisible(true);

        selected = sqlAssertion.getProtections();
        iter = selected.iterator();
        System.out.println("Selected Items After Dialog");
        while(iter.hasNext()) {
            System.out.println((String)iter.next());
        }
    }

    public SqlAttackDialogTest() throws IllegalAccessException, UnsupportedLookAndFeelException, InstantiationException, ClassNotFoundException {
        sqlAssertion = new SqlAttackAssertion();
        initialFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        List allProtections = sqlAssertion.getAllProtections();
        Iterator iter = allProtections.iterator();
        ArrayList cbList = new ArrayList();
        while(iter.hasNext()) {
            String s = (String) iter.next();
            JCheckBox cb = new JCheckBox(s);
            cbList.add(cb);
        }

        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doDialogTest();
            }
        });

        initialFrame.setBounds(50,50,240,200);
        initialFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Container con = initialFrame.getContentPane();
        con.add(panel);
        panel.setLayout(new GridLayout(allProtections.size() + 2, 1));
        panel.add(label);

        iter = cbList.iterator();
        while(iter.hasNext()) {
            panel.add((JCheckBox)iter.next());
        }
        panel.add(okButton);
    }

    void doIt() {
        initialFrame.setVisible(true);
    }

    public static void main(String[] args) throws Exception {
        SqlAttackDialogTest test = new SqlAttackDialogTest();
        test.doIt();
    }
}
