package com.l7tech.server.config.systemconfig;

import com.l7tech.server.config.beans.BaseConfigurationBean;

import java.util.ArrayList;
import java.util.List;

/**
 * @author jbufu
 */
public class KeyboardConfigurationBean extends BaseConfigurationBean {

    // - PUBLIC

    public KeyboardConfigurationBean(String name, String description) {
        super(name, description);
    }

    public void setKeymap(String keymap) {
        this.keymap = keymap;
    }

    public String getKeymap() {
        return keymap;
    }

    @Override
    public void reset() {
        keymap = null;
    }

    @Override
    protected void populateExplanations() {
        explanations.add("\nKeyboard configuration: \n\t");
        explanations.add(concatConfigLines(EOL + "\t", getConfigLines()));
    }

    public String getKeyboardConfig() {
        return concatConfigLines(EOL, getConfigLines());
    }

    // - PRIVATE

    private String keymap;

    private List<String> getConfigLines() {
        List<String> config = new ArrayList<String>();
        config.add("KEYBOARDTYPE=\"pc\"");
        config.add("KEYTABLE=\"" + keymap + "\"");
        return config;
    }
}
