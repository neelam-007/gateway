package com.l7tech.console;

import com.l7tech.console.util.TopComponents;
import com.l7tech.gui.util.HelpUtil;
import com.l7tech.security.prov.ProviderUtil;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.Pair;
import com.l7tech.util.SyspropUtil;

import javax.swing.*;
import java.io.File;
import java.security.Provider;
import java.security.Security;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Logger;

/**
 * Thick-client version of SsmApplication.
 */
public class SsmApplicationHeavy extends SsmApplication  {
    private final Logger log = Logger.getLogger(getClass().getName());
    private static final String PROP_DISABLE_BLACKLISTED_SERVICES = "com.l7tech.security.prov.rsa.disableServices";
    private static final boolean DISABLE_BLACKLISTED_SERVICES = ConfigFactory.getBooleanProperty( PROP_DISABLE_BLACKLISTED_SERVICES, true );
    private static final Collection<Pair<String,String>> SERVICE_BLACKLIST = Collections.unmodifiableCollection(Arrays.asList(
            new Pair<String, String>("CertificateFactory", "X.509")
    ));

    //the property name for the current applications home directory. If not set, this is defaulted to null by code
    // that uses it
    private static final String APPLICATION_HOME_PROPERTY = "com.l7tech.applicationHome";

    private static SsmApplication ssmApplication;
    private boolean running = false;

    public SsmApplicationHeavy() {
        if (ssmApplication != null) {
            throw new IllegalStateException("Already initalized");
        }
        ssmApplication = this;
    }

    public synchronized void run() {
        if (running) {
            throw new IllegalStateException("Policy Manager already running");
        }

        installAdditionalSecurityProviders();

        if (!isSuppressAutoLookAndFeel()) setAutoLookAndFeel();
        mainWindow = new MainWindow(this);
        TopComponents.getInstance().registerComponent("mainWindow", mainWindow);

        // Window listener
        mainWindow.setVisible(true);
        mainWindow.toFront();
        running = true;
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                mainWindow.checkConfiguration();
                mainWindow.activateLogonDialog();
            }
        });
    }

    public boolean isApplet() {
        return false;
    }

    /**
     * set the look and feel
     *
     * @param lookAndFeel a string specifying the name of the class that implements
     *                    the look and feel
     */
    protected void setLookAndFeel
      (String
      lookAndFeel)
    {
        if (!isSuppressAutoLookAndFeel()) {
            setAutoLookAndFeel();
            return;
        }

        if (lookAndFeel == null) return;
        boolean lfSet = true;

        // if same look and feel quick exit
        if (lookAndFeel.
          equals(UIManager.getLookAndFeel().getClass().getName())) {
            return;
        }

        try {
            Object lafObject =
              Class.forName(lookAndFeel).newInstance();
            UIManager.setLookAndFeel((LookAndFeel)lafObject);
        } catch (Exception e) {
            lfSet = false;
        }
        // there was a problem setting l&f, try crossplatform one (best bet)
        if (!lfSet) {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception e) {
                return;
            }
        }
        // update panels with new l&f
        MainWindow mainWindow = getMainWindow();
        SwingUtilities.updateComponentTreeUI(mainWindow);
        mainWindow.validate();
    }

    /**
     * The "Help Topics".
     * This procedure displays the help contents in the preferred browser for the system on which the SSM is running.
     */
    public void showHelpTopicsRoot() {
        HelpUtil.showHelpTopicsRoot( TopComponents.getInstance().getTopParent() );
    }

    private void installAdditionalSecurityProviders() {

        String cj = SyspropUtil.getString("com.l7tech.console.security.cryptoj.install", "first").trim().toLowerCase();
        final Provider jsafeProvider = getJsafeProvider();
        if (DISABLE_BLACKLISTED_SERVICES) {
            ProviderUtil.configureProvider(SERVICE_BLACKLIST, jsafeProvider);
        }
        if ("first".equals(cj)) {
            log.info("Registering Crypto-J as most-preferred crypto provider");
            Security.insertProviderAt(jsafeProvider, 1);
        } else if ("last".equals(cj) || Boolean.valueOf(cj)) {
            log.info("Registering Crypto-J as additional crypto provider");
            Security.addProvider(jsafeProvider);
        }
    }

    private Provider getJsafeProvider() {
        try {
            return (Provider) getClass().getClassLoader().loadClass("com.rsa.jsafe.provider.JsafeJCE").newInstance();
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
