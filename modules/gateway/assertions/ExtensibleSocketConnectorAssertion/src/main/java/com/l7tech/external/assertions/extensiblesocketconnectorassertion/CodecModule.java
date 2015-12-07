package com.l7tech.external.assertions.extensiblesocketconnectorassertion;

import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * User: njordan
 * Date: 15/11/12
 * Time: 1:34 PM
 * To change this template use File | Settings | File Templates.
 */
public class CodecModule implements Serializable {
    private String dialogClassName;
    private String defaultContentType;
    private String configurationClassName;
    private String displayName;
    private String codecClassName;

    public CodecModule(String dialogClassName, String defaultContentType, String configurationClassName, String displayName,
                       String codecClassName) {
        this.dialogClassName = dialogClassName;
        this.defaultContentType = defaultContentType;
        this.configurationClassName = configurationClassName;
        this.displayName = displayName;
        this.codecClassName = codecClassName;
    }

    public String getDialogClassName() {
        return dialogClassName;
    }

    public String getDefaultContentType() {
        return defaultContentType;
    }

    public String getConfigurationClassName() {
        return configurationClassName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getCodecClassName() {
        return codecClassName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
