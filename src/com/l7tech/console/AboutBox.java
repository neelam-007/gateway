package com.l7tech.console;

import com.l7tech.common.BuildInfo;
import com.l7tech.console.action.Actions;
import com.l7tech.console.table.MapBackedTableModel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.util.TreeMap;


/**
 * About box showing logo, memory stats and environment
 * properties.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.2
 */
public class AboutBox extends JDialog implements ActionListener {
    private static final String LOGO_IMAGE = "help-panel.png";
    /* this class classloader */
    private final ClassLoader cl = getClass().getClassLoader();

    JTabbedPane tabPanel = new JTabbedPane();
    JLabel logoLabel =
      new JLabel(new ImageIcon(cl.getResource(MainWindow.RESOURCE_PATH + "/" + LOGO_IMAGE)), JLabel.CENTER);
    JLabel resLabel = new JLabel("", JLabel.CENTER);
    JLabel urlLabel = new JLabel("HomePage -- http://www.layer7tech.com", JLabel.CENTER);

    JPanel infoPanel = new JPanel(new GridBagLayout());
    String product = "SecureSpan Manager";
    String version = BuildInfo.getProductVersion() + " build " + BuildInfo.getBuildNumber();
    ResourceThread rThread = new ResourceThread();
    JProgressBar resourceMeter = new JProgressBar();
    JTable systemProperties = new
      JTable(new MapBackedTableModel("Property Name", "Value", new TreeMap(System.getProperties())));

    /**
     * Convenience static method. Instantiate the AboutBox
     * dialog with parameter Frame as owner.
     *
     * @param Owner the owner of this dialog
     */
    public static void showDialog(Frame Owner) {
        AboutBox box = new AboutBox(Owner);
        box.show();
    }

    /**
     * private constructor
     *
     * @param parent parnt Frame
     */
    private AboutBox(Frame parent) {
        super(parent, "", true);
        enableEvents(AWTEvent.WINDOW_EVENT_MASK);
        initUI();
        pack();
        setLocationRelativeTo(parent);
        rThread.start();
    }

    /**
     * initialize the UI components
     */
    private void initUI() {
        Insets ins = new Insets(1, 1, 1, 1);
        JScrollPane jsp = new JScrollPane(systemProperties);

        this.setTitle("About the" + product /*+ " " + "Version" + " " + version*/);
        this.getContentPane().add(tabPanel, BorderLayout.CENTER);
        infoPanel.add(new JLabel("Product: " + product),
          new GridBagConstraints(0, 1, 1, 1, 0d, 0d,
            GridBagConstraints.WEST,
            GridBagConstraints.NONE, ins, 0, 0));
        infoPanel.add(resLabel,
          new GridBagConstraints(1, 1, 1, 1, 1d, 0d,
            GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, ins, 0, 0));

        infoPanel.add(new JLabel("Version: " + version),
          new GridBagConstraints(0, 2, 1, 1, 0d, 0d,
            GridBagConstraints.WEST,
            GridBagConstraints.NONE, ins, 0, 0));
        infoPanel.add(resourceMeter,
          new GridBagConstraints(1, 2, 1, 1, 1d, 0d,
            GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, ins, 0, 0));


        infoPanel.add(new JLabel("System Properties", JLabel.CENTER),
          new GridBagConstraints(0, 3, 3, 1, 1d, 0d,
            GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, ins, 0, 0));

        infoPanel.add(jsp,
          new GridBagConstraints(0, 4, 3, 1, 1d, 1d,
            GridBagConstraints.CENTER,
            GridBagConstraints.BOTH, new Insets(1, 1, 1, 1), 0, 0));
        infoPanel.add(urlLabel,
          new GridBagConstraints(0, 5, 3, 1, 0d, 0d,
            GridBagConstraints.CENTER,
            GridBagConstraints.NONE, ins, 0, 0));

        jsp.setPreferredSize(logoLabel.getPreferredSize());
        jsp.setMaximumSize(logoLabel.getPreferredSize());
        jsp.setMinimumSize(logoLabel.getPreferredSize());

        JPanel pnel = new JPanel(new BorderLayout());
        pnel.add(logoLabel, BorderLayout.CENTER);
        pnel.setBackground(Color.decode("#FFFFFF"));

        tabPanel.addTab("About", pnel);
        tabPanel.addTab("Info", infoPanel);

        logoLabel.setBorder(BorderFactory.createBevelBorder(1));

        resourceMeter.setStringPainted(true);
        resourceMeter.setToolTipText("Memory being used outside the allocated heap");
        resLabel.setPreferredSize(resourceMeter.getPreferredSize());
        resLabel.setMinimumSize(resourceMeter.getPreferredSize());
        resLabel.setMaximumSize(resourceMeter.getPreferredSize());
        setResizable(true);
        Actions.setEscKeyStrokeDisposes(this);
    }


    /**
     * window event handling, terminate the resource meter
     * thread when window closing.
     *
     * @param e the WindowEvent
     */
    protected void processWindowEvent(WindowEvent e) {
        if (e.getID() == WindowEvent.WINDOW_CLOSING) {
            cancel();
        }
        super.processWindowEvent(e);
    }

    /**
     * terminate the resource meter thread
     */
    void cancel() {
        rThread.halt();
        dispose();
    }

    /**
     * ActionListener interface contract
     *
     * @param e ActionEvent
     */
    public void actionPerformed(ActionEvent e) {
        ;
    }

    /**
     * The resource meter thread
     */
    class ResourceThread extends Thread {
        boolean stop = false;
        java.text.NumberFormat Nf =
          java.text.NumberFormat.getNumberInstance();
        double MB = Math.pow(2d, 20d);

        public ResourceThread() {
            super("ResourceMonitorThread");
            Nf.setMaximumFractionDigits(2);
            Nf.setMinimumFractionDigits(2);
            Nf.setMaximumIntegerDigits(8);
        }

        public void halt() {
            stop = true;
        }

        public void run() {
            System.gc();
            System.runFinalization();
            try {
                while (!stop) {
                    Runtime runtime = Runtime.getRuntime();
                    long UsedMemory =
                      runtime.totalMemory() - runtime.freeMemory();
                    if (resourceMeter.getMaximum() != (int)runtime.totalMemory())
                        resourceMeter.setMaximum((int)runtime.totalMemory());
                    if (resourceMeter.getValue() != (int)UsedMemory) {
                        resourceMeter.setValue((int)UsedMemory);
                        resourceMeter.setString(Nf.format(UsedMemory / MB) + " " + "MB");
                        resLabel.setText("Java Heap : " +
                          Nf.format(Runtime.getRuntime().totalMemory() / MB) + " MB Allocated");
                        System.runFinalization();
                    }
                    sleep(2000);
                }
            } catch (Exception e) {
            }
        }
    }
}
