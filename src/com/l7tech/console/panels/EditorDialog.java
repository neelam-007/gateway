package com.l7tech.console.panels;

import javax.swing.JPanel;
import javax.swing.JFrame;
import javax.swing.JDialog;
import java.awt.GridBagConstraints;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.*;

import com.l7tech.console.panels.Utilities;

/**
 * The class for implementing panels that allow editing of bean data
 *
 * @author <a href="mailto:emarceta@l7tech.com">Emil Marceta</a>
 */
public class EditorDialog extends JDialog {
  
  private JPanel panel;
  
  public EditorDialog(JFrame parentFrame, JPanel panel) {
    super(parentFrame, true);
    this.panel = panel;
    
    initEditorDialog();
  }

  private void initEditorDialog() {

    setResizable(false);
    getContentPane().setLayout(new GridBagLayout());
    GridBagConstraints constraints
      = new  GridBagConstraints(0,  // gridx
                                0,  // gridy
                                1,  // widthx
                                1,  // widthy
                                1.0, // weightx
                                1.0, // weigthy
                                GridBagConstraints.NORTH, // anchor
                                GridBagConstraints.BOTH, //fill
                                new Insets(0,0, 0,0), // inses
                                0,  // padx
                                0); // pady
    getContentPane().add(panel, constraints);
  }
}


