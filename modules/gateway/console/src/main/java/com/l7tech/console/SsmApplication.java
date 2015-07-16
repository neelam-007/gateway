package com.l7tech.console;

import com.l7tech.console.util.ClusterPropertyCrud;
import com.l7tech.gui.util.HelpUtil;
import com.l7tech.util.ConfigFactory;
import com.l7tech.gui.util.FileChooserUtil;
import com.l7tech.console.util.TopComponents;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.SyspropUtil;
import org.springframework.context.support.ApplicationObjectSupport;

import javax.swing.*;
import javax.swing.plaf.metal.MetalTheme;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.security.AccessControlException;

/**
 * @author mike
 */
public abstract class SsmApplication extends ApplicationObjectSupport {
    private static final Logger logger = Logger.getLogger(SsmApplication.class.getName());
    private static final String KUNSTSTOFF_CLASSNAME = "com.incors.plaf.kunststoff.KunststoffLookAndFeel";
    private static final String KUNSTSTOFF_THEME_CLASSNAME = "com.incors.plaf.kunststoff.themes.KunststoffDesktopTheme";
    private static final boolean SUPPRESS_AUTO_LNF = ConfigFactory.getBooleanProperty( "com.l7tech.console.SuppressAutoLookAndFeel", false );
    private static final String PROP_HELP_URL = "help.url";

    private String resourcePath;
    private boolean trusted = true;
    protected MainWindow mainWindow;

    protected MainWindow getMainWindow() {
        return mainWindow;
    }

    public abstract void run();

    public String getResourcePath() {
        return resourcePath;
    }

    public void setResourcePath(String resourcePath) {
        this.resourcePath = resourcePath;
    }

    public abstract boolean isApplet();

    public boolean isTrusted() {
        return trusted;    
    }

    void setTrusted(boolean trusted) {
        this.trusted = trusted;
    }

    /** @return true if a custom look and feel should be honored.  False to do normal automatic look-and-feel selection. */
    public static boolean isSuppressAutoLookAndFeel() {
        return SUPPRESS_AUTO_LNF;
    }

    /**
     * Create a new JFileChooser and try to use it, failing with a graceful error message if running as an untrusted applet.
     * <p/>
     * The difference between this method and {@link FileChooserUtil#doWithJFileChooser} is that this method
     * displays an error message if we are running as an untrusted applet, while the other
     * method throws an exception instead.
     *
     * @param fcu  the code that will be used by the JFileChooser, or null if you intend to use the returned
     *             JFileChooser yourself.
     *             Will not be invoked if no JFileChooser can be created.
     */
    public static void doWithJFileChooser(FileChooserUtil.FileChooserUser fcu) {
        try {
            FileChooserUtil.doWithJFileChooser(fcu);
        } catch (AccessControlException ace) {
            TopComponents.getInstance().showNoPrivilegesErrorMessage();
        }
    }

    private static interface LnfSetter {
        void setLnf() throws Exception;
    }

    /**
     * Try to set the Kunststoff look and feel.
     */
    private static LnfSetter kunststoffLnfSetter = new LnfSetter() {
        public void setLnf() throws Exception {
            final Class kunststoffClass = Class.forName( KUNSTSTOFF_CLASSNAME );
            final Object kunststoffLnF = kunststoffClass.newInstance();
            try {
                final Class themeClass = Class.forName( KUNSTSTOFF_THEME_CLASSNAME );
                final Object theme = themeClass.newInstance();
                kunststoffClass.getMethod("setCurrentTheme", MetalTheme.class).invoke(kunststoffLnF, theme);
            } catch ( Exception e ) {
                // eat it, themes not make one great
            }
            UIManager.setLookAndFeel( (LookAndFeel) kunststoffLnF );
        }
    };

    /**
     * Try to set the Windows look and feel.
     */
    private static LnfSetter windowsLnfSetter = new LnfSetter() {
        public void setLnf() throws Exception {
            boolean wasSet = false;
            UIManager.LookAndFeelInfo[] feels = UIManager.getInstalledLookAndFeels();
            for (UIManager.LookAndFeelInfo feel : feels) {
                if (feel.getName().indexOf("Windows") >= 0) {
                    UIManager.setLookAndFeel(feel.getClassName());
                    wasSet = true;
                    break;
                }
            }
            if (!wasSet)
                throw new Exception("No XP LNF");
        }
    };

    /**
     * Otherwise, system look and feel.
     */
    private static LnfSetter systemLnfSetter = new LnfSetter() {
        public void setLnf() throws Exception {
            UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
        }
    };

    /**
     * Automatically pick the best look and feel and enable it.
     * This method currently tries Windows, then Kunststoff, then System; unless
     * os.name contains "mac os x" in which case it tries System, then Kunststoff.
     */
    protected void setAutoLookAndFeel() {
        LnfSetter[] order;

        boolean isMac = SyspropUtil.getString( "os.name", "" ).toLowerCase().contains( "mac os x" );
        if ( isMac ) {
            order = new LnfSetter[] { systemLnfSetter, kunststoffLnfSetter };
        } else {
            order = new LnfSetter[] { windowsLnfSetter, kunststoffLnfSetter, systemLnfSetter };
        }

        for (LnfSetter lnfSetter : order) {
            try {
                lnfSetter.setLnf();
            } catch (Exception e) {
                continue;
            }
            break;
        }

        try {
            // incors.org Kunststoff faq says we need the following line if we ever want to use Java Web Start:
            UIManager.getLookAndFeelDefaults().put( "ClassLoader", getClass().getClassLoader() );
        } catch ( Exception e ) {
            logger.log( Level.WARNING, "Unable to update look-and-feel classloader", e );
        }
    }

    public void updateHelpUrl() {
        try {
            String customUrl = ClusterPropertyCrud.getClusterProperty( PROP_HELP_URL );
            HelpUtil.setHelpUrl( customUrl );
        } catch ( Exception e ) {
            logger.log( Level.INFO, "Unable to look up custom help URL: " + ExceptionUtils.getMessage( e ), ExceptionUtils.getDebugException( e ) );
        }
    }

    public abstract void showHelpTopicsRoot();
}
