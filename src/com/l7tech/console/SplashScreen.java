package com.l7tech.console;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.*;

/**
 * The splash screen class
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.2
 */
class SplashScreen extends JWindow implements MouseListener {
    /* this class classloader */
    private final ClassLoader cl = getClass().getClassLoader();
    private JPanel windowContentPane = null;
    private JLabel labelSplash = null;

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
        if (labelSplash == null) {
            labelSplash = new javax.swing.JLabel();
            labelSplash.setName("JLabelSplash");
            labelSplash.setIcon(new javax.swing.ImageIcon(cl.getResource(MainWindow.RESOURCE_PATH + "/splash-screen.gif")));
            labelSplash.setText("");
            labelSplash.setMaximumSize(new java.awt.Dimension(293, 247));
            labelSplash.setMinimumSize(new java.awt.Dimension(293, 247));
        }
        return labelSplash;
    }

    /**
     * Return the JWindowContentPane property value.
     * @return javax.swing.JPanel
     */
    private javax.swing.JPanel getJWindowContentPane() {
        if (windowContentPane == null) {
            windowContentPane = new javax.swing.JPanel();
            windowContentPane.setName("JWindowContentPane");
            //windowContentPane.setBorder(new javax.swing.border.EtchedBorder());
            windowContentPane.setLayout(new java.awt.BorderLayout());
            getJWindowContentPane().add(getJLabelSplash(), "Center");
        }
        return windowContentPane;
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
        // egg
        this.addMouseListener(this);
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

    public void mouseClicked(MouseEvent e) {
        ImageIcon roosterIcon = new ImageIcon(cl.getResource(MainWindow.RESOURCE_PATH + "/rooster.gif"));
        int width = roosterIcon.getIconWidth();
        int height = roosterIcon.getIconHeight();
        setSize(width, height);
        labelSplash.setMaximumSize(new java.awt.Dimension(width, height));
        labelSplash.setMinimumSize(new java.awt.Dimension(width, height));
        labelSplash.setIcon(roosterIcon);
    }

    public void mousePressed(MouseEvent e) {}
    public void mouseReleased(MouseEvent e) {}
    public void mouseEntered(MouseEvent e) {}
    public void mouseExited(MouseEvent e) {}

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
