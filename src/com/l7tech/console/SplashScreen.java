package com.l7tech.console;

import java.awt.*;
import java.awt.event.WindowAdapter;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JWindow;
/**
 * The splash screen class
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.2
 */
class SplashScreen extends JWindow {
  /* this class classloader */
  private final ClassLoader cl = getClass().getClassLoader();
  private JPanel ivjJWindowContentPane = null;
  private JLabel ivjJLabelSplash = null;

  /**
   * SplashScreen constructor comment.
   */
  public SplashScreen() {
    super();
    initialize();
  }

  /**
   * SplashScreen constructor comment.
   * @param owner java.awt.Frame
   */
  public SplashScreen(Frame owner) {
    super(owner);
  }

  /**
   * SplashScreen constructor comment.
   * @param owner java.awt.Window
   */
  public SplashScreen(Window owner) {
    super(owner);
  }

  /**
   * Return the JLabel1 property value.
   * @return javax.swing.JLabel
   */
  private JLabel getJLabelSplash() {
    if (ivjJLabelSplash == null) {
      ivjJLabelSplash = new javax.swing.JLabel();
      ivjJLabelSplash.setName("JLabelSplash");
      ivjJLabelSplash.setIcon(new javax.swing.ImageIcon(cl.getResource(MainWindow.RESOURCE_PATH+"/rooster.gif")));
      ivjJLabelSplash.setText("");
      ivjJLabelSplash.setMaximumSize(new java.awt.Dimension(293, 247));
      ivjJLabelSplash.setMinimumSize(new java.awt.Dimension(293, 247));
    }
    return ivjJLabelSplash;
  }

  /**
   * Return the JWindowContentPane property value.
   * @return javax.swing.JPanel
   */
  private javax.swing.JPanel getJWindowContentPane() {
    if (ivjJWindowContentPane == null) {
      ivjJWindowContentPane = new javax.swing.JPanel();
      ivjJWindowContentPane.setName("JWindowContentPane");
      //ivjJWindowContentPane.setBorder(new javax.swing.border.EtchedBorder());
      ivjJWindowContentPane.setLayout(new java.awt.BorderLayout());
      getJWindowContentPane().add(getJLabelSplash(), "Center");
    }
    return ivjJWindowContentPane;
  }

  /**
   * @param e java.awt.event.WindowEvent
   */
  private void windowClosingHandler(java.awt.event.WindowEvent e) {
    this.dispose();
  }

  /**
   * Initializes listeners
   */
  private void initializeListeners() {

    this.addWindowListener(
                          new WindowAdapter() {
                            public void windowClosing(java.awt.event.WindowEvent e) {
                              windowClosingHandler(e);
                            }
                          });
  }

  /**
   * Initialize the class.
   */
  private void initialize() {
    setName("SplashScreen");
    setSize(300, 220);
    setContentPane(getJWindowContentPane());
    initializeListeners();
  }

  /**
   * main entrypoint - starts the part when it is run as an application
   * @param args java.lang.String[]
   */
  public static void main(java.lang.String[] args) {
    try {
      SplashScreen aSplashScreen;
      aSplashScreen = new SplashScreen();
      aSplashScreen.initialize();
      aSplashScreen.addWindowListener(new java.awt.event.WindowAdapter() {
                                        public void windowClosing(java.awt.event.WindowEvent e) {
                                          System.exit(0);
                                        }
                                      });
      aSplashScreen.show();
      java.awt.Insets insets = aSplashScreen.getInsets();
      aSplashScreen.setSize(aSplashScreen.getWidth() + insets.left + insets.right, aSplashScreen.getHeight() + insets.top + insets.bottom);
      aSplashScreen.setVisible(true);
    } catch (Throwable exception) {
      System.err.println("Exception occurred in main() of javax.swing.JWindow");
      exception.printStackTrace(System.out);
    }
  }
}
