package com.l7tech.external.assertions.extensiblesocketconnectorassertion.console;

import com.l7tech.external.assertions.extensiblesocketconnectorassertion.codecconfigurations.CodecConfiguration;

import javax.swing.*;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 02/12/11
 * Time: 3:10 PM
 * To change this template use File | Settings | File Templates.
 */
public interface CodecSettingsPanel<T extends CodecConfiguration> {
    public boolean validateView();

    public void updateModel(T model);

    public void updateView(T model);

    public JPanel getPanel();
}
