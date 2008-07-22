package com.l7tech.console.util;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.text.SimpleDateFormat;
import java.text.DateFormat;
import java.util.Date;

/**
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version  1.0
 */
public class ProgressBar extends JProgressBar {
    public static final String NAME = "progress.bar";
    private Timer timer = null;
    private int delay = 0;
    private long actionStartAt = 0;

    /**
     *
     * @param min    specifies progress bar minimum
     * @param max    specifies progress bar maximum
     *
     * @param delay  The number of milliseconds between progress
     *               bar chnages.
     */
    public ProgressBar(int min, int max, int delay) {
        super(JProgressBar.HORIZONTAL, min, max);
        // sanity check
        if (delay <= 0) {
            throw new IllegalArgumentException("Invalid delay value " + delay);
        }
        if (!(min < max)) {
            throw new IllegalArgumentException("min < max ");
        }
        this.setStringPainted(false);
        this.delay = delay;
        initializeTimer();
    }


    /**
     * start the progress bar.
     * If the progress bar is already running, this method does
     * not perform any action.
     */
    public synchronized void start() {
        if (timer.isRunning()) return;

        Runnable runnable = new Runnable() {
            public void run() {
                actionStartAt = System.currentTimeMillis();
                timer.start();
                setToolTipText("In progress...");
            }
        };

        if (SwingUtilities.isEventDispatchThread()) {
            runnable.run();
        } else {
            SwingUtilities.invokeLater(runnable);
        }
    }

    /**
     * stop the progress bar.
     * If the progress bar is not running, this method does
     * not perform any action.
     */
    public synchronized void stop() {
        if (!timer.isRunning()) return;

        Runnable runnable = new Runnable() {
            public void run() {
                timer.stop();
                final long timeElapsed = System.currentTimeMillis() - actionStartAt;

                setValue(getMinimum());
                setToolTipText("Last action : " + formatter.format(new Date(timeElapsed)));
            }
        };

        if (SwingUtilities.isEventDispatchThread()) {
            runnable.run();
        } else {
            SwingUtilities.invokeLater(runnable);
        }
    }

    /* initialize the Timer for this progress bar */
    private void initializeTimer() {
        timer = new Timer(delay,
          new ActionListener() {
              boolean increasing = true;

              public void actionPerformed(ActionEvent evt) {
                  int val = ProgressBar.this.getValue();
                  if (increasing) {
                      ProgressBar.this.setValue(++val);
                      if (val == ProgressBar.this.getMaximum()) {
                          increasing = false;
                      }
                  } else {
                      ProgressBar.this.setValue(--val);
                      if (val == ProgressBar.this.getMinimum()) {
                          increasing = true;
                      }
                  }
              }
          });
        timer.setRepeats(true);
    }

    DateFormat formatter = new SimpleDateFormat("mm:ss");
}
