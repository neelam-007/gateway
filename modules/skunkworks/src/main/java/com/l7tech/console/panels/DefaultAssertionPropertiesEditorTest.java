package com.l7tech.console.panels;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.RoutingAssertion;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 *
 */
public class DefaultAssertionPropertiesEditorTest {
    public static class HowdyAssertion extends RoutingAssertion {
        private String name;
        private String description;
        private int age;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }
    }
    
    public static void main(String[] args) {
        final HowdyAssertion assertion = new HowdyAssertion();

        final JFrame frame = new JFrame("Test");
        final Container cp = frame.getContentPane();
        cp.setLayout(new GridBagLayout());
        cp.add(new JButton("Edit") {{
            addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    final DefaultAssertionPropertiesEditor<HowdyAssertion> editor =
                            new DefaultAssertionPropertiesEditor<HowdyAssertion>(frame, assertion);
                    JDialog dialog = editor.getDialog();
                    dialog.pack();
                    Utilities.centerOnScreen(dialog);
                    DialogDisplayer.display(dialog, new Runnable() {
                        public void run() {
                            if (editor.isConfirmed())
                                editor.getData(assertion);
                        }
                    });

                }
            });
        }}, new GridBagConstraints() {{
            anchor = CENTER;
            fill = NONE;
        }});
        frame.pack();
        frame.setSize(600, 500);
        Utilities.centerOnScreen(frame);
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.out.println("Closing");
                frame.dispose();
                System.exit(0);
            }
        });
        frame.setVisible(true);
    }
}
