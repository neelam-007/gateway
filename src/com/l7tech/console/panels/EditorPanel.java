package com.l7tech.console.panels;

import javax.swing.JPanel;
import com.l7tech.console.panels.PanelListener;

/**
 * abstract class for implementing panels that allow editing of bean data
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public abstract class EditorPanel extends JPanel {

  protected EditorPanel() {
  }

  public abstract void edit(Object dirObject, boolean readWrite);

  public void setPanelListener(PanelListener listener) {
    this.panelListener = listener;
  }

  protected PanelListener panelListener;
}
