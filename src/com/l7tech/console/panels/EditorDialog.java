package com.l7tech.console.panels;

import com.l7tech.console.action.Actions;

import javax.swing.*;
import java.awt.*;

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
    Actions.setEscKeyStrokeDisposes(this);  
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


