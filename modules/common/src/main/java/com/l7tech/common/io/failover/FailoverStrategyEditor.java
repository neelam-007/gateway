package com.l7tech.common.io.failover;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

/**
 * Abstract class for custom failover strategy editor.
 * The Failover Strategy can provide a custom user interface for user to configure the strategy properties.
 * the strategy properties has to transform to a Map<String, String> for serialization purpose.
 *
 */
public abstract class FailoverStrategyEditor extends JDialog {

    /**
     * The properties of the strategy
     */
    protected Map<String, String> properties;

    /**
     * Constructor for FailoverStrategyEditor, subclass has to create a constructor with same parameters,
     * This constructor will be involved from reflection during creation of the editor.
     *
     * @param frame The parent frame
     * @param properties The properties of the strategy.
     */
    protected FailoverStrategyEditor(Frame frame, Map<String, String> properties) {
        super(frame);
        this.properties = properties;
    }

}
