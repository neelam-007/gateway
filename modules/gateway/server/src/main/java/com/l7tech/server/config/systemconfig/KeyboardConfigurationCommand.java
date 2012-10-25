package com.l7tech.server.config.systemconfig;

import com.l7tech.common.io.ProcResult;
import com.l7tech.common.io.ProcUtils;
import com.l7tech.server.config.commands.BaseConfigurationCommand;
import com.l7tech.util.ExceptionUtils;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author jbufu
 */
public class KeyboardConfigurationCommand extends BaseConfigurationCommand<KeyboardConfigurationBean> {

    // - PUBLIC

    @Override
    public boolean execute() {
        final String keymap = configBean.getKeymap();
        if (keymap == null) {
            // user selected not to configure the keyboard.
            logger.log(Level.INFO, "Keyboard configuration not changed.");
            return true;
        }

        return loadKeys() && grubby() && writeConfigFile(KEYBOARD_CONFIG_FILE, configBean.getKeyboardConfig());
    }


    // - PROTECTED

    protected KeyboardConfigurationCommand(KeyboardConfigurationBean bean) {
        super(bean);
    }

    // - PRIVATE

    private static final Logger logger = Logger.getLogger(KeyboardConfigurationCommand.class.getName());

    private static final String KEYBOARD_CONFIG_FILE = "keyboard";
    private static final String LOADKEYS = "/usr/bin/sudo /bin/loadkeys";
    private static final String GRUBBY = "/usr/bin/sudo /sbin/grubby";

    /** runs the system "loadkeys" command */
    private boolean loadKeys() {
        String command = LOADKEYS + " " + configBean.getKeymap();
        try {
            ProcResult result = ProcUtils.exec(command);
            logger.log(Level.INFO, "Output of: `" + command + "` : \n" + new String(result.getOutput()));
            return true;
        } catch (IOException e) {
            logger.log(Level.WARNING, "Error running " + command + " : " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            return false;
        }
    }

    /** runs the system "grubby" command to update /etc/grub.conf with the new keymap */
    private boolean grubby() {
        /*
        We should be able to do:
        String[] commands = {
            GRUBBY + " --update-kernel=ALL --remove-args=\"KEYBOARDTYPE KEYTABLE\"",
            GRUBBY + " --update-kernel=ALL --args=\"KEYBOARDTYPE=pc KEYTABLE=" + configBean.getKeymap() + "\""
        };
        but because of how ProcUtils tokenizes arguments to commands (that is, it doesn't properly handle quoted strings)
        we have to do this instead:
        */
        String[] commands = {
            GRUBBY + " --update-kernel=ALL --remove-args=KEYBOARDTYPE",
            GRUBBY + " --update-kernel=ALL --remove-args=KEYTABLE",
            GRUBBY + " --update-kernel=ALL --args=KEYBOARDTYPE=pc",
            GRUBBY + " --update-kernel=ALL --args=KEYTABLE=" + configBean.getKeymap()
        };
        for (String command : commands){
            try {
                ProcResult result = ProcUtils.exec(command);
                logger.log(Level.INFO, "Output of: `" + command + "` : \n" + new String(result.getOutput()));
            }
            catch (IOException e) {
                logger.log(Level.WARNING, "Error running " + command + " : " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                return false;
            }
        }
        return true;
    }
}

