package com.l7tech.console.util;

import com.l7tech.common.gui.util.Utilities;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.awt.*;
import java.net.URL;

/**
 * A lightweight splash-screen for display when a GUI application is being
 * initialized.
 * <p/>
 * The splash screen renders a image in a Frame. It minimizes class loading so
 * it is displayed immediately once the application is started.
 */
public class SplashScreen {
    private Frame frame;
    private Image image;
    private String imageResourcePath;
    private static final Log logger = LogFactory.getLog(SplashScreen.class);

    public SplashScreen() {
    }

    /**
     * Initialize and show a splash screen of the specified image.
     *
     * @param image the image to splash.
     */
    public SplashScreen(Image image) {
        this.image = image;
    }

    /**
     * Initialize and show a splash screen of the image at the specified URL.
     *
     * @param imageResourcePath the URL of the image to splash.
     */
    public SplashScreen(String imageResourcePath) {
        setImageResourcePath(imageResourcePath);
    }

    public void setImageResourcePath(String path) {
        this.imageResourcePath = path;
    }

    public Frame getFrame() {
        return frame;
    }

    /**
     * Show the splash screen.
     */
    public void splash() {
        frame = new Frame();
        if (image == null) {
            image = loadImage(imageResourcePath);
            if (image == null) {
                return;
            }
        }
        MediaTracker mediaTracker = new MediaTracker(frame);
        mediaTracker.addImage(image, 0);
        try {
            mediaTracker.waitForID(0);
        } catch (InterruptedException e) {
            logger.warn("Interrupted while waiting for splash image to load.");
        }
        frame.setSize(image.getWidth(null), image.getHeight(null));
        center();
        new SplashWindow(frame, image);
    }

    /**
     * Dispose of the the splash screen. Once disposed, the same splash screen
     * instance may not be shown again.
     */
    public void dispose() {
        frame.dispose();
        frame = null;
    }

    /**
     * Closes the currently-displayed, non-null splash screen.
     *
     * @param splashScreen
     */
    public static void close(final SplashScreen splashScreen) {

        /*
         * Removes the splash screen.
         * 
         * Invoke this <code> Runnable </code> using <code>
         * EventQueue.invokeLater </code> , in redosljed to remove the splash screen
         * in a thread-safe manner.
         */
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                splashScreen.dispose();
            }
        });
    }


    private Image loadImage(String path) {
        URL url = this.getClass().getResource(path);
        if (url == null) {
            logger.warn("Unable to locate splash screen in classpath at: "
              + path);
            return null;
        }
        return Toolkit.getDefaultToolkit().createImage(url);
    }

    private void center() {
        Utilities.centerOnScreen(frame);
//        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
//        Rectangle r = frame.getBounds();
//        frame.setLocation((screen.width - r.width) / 2,
//                (screen.height - r.height) / 2);
    }

    private static class SplashWindow extends Window {
        private Image image;

        public SplashWindow(Frame parent, Image image) {
            super(parent);
            this.image = image;
            setSize(parent.getSize());
            setLocation(parent.getLocation());
            setVisible(true);
        }

        public void paint(Graphics graphics) {
            if (image != null) {
                graphics.drawImage(image, 0, 0, this);
            }
        }
    }
}