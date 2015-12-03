package com.l7tech.external.assertions.remotecacheassertion.console;

import javax.swing.*;
import java.util.HashMap;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 16/11/11
 * Time: 10:50 AM
 * To change this template use File | Settings | File Templates.
 */
public interface RemoteCacheConfigPanel {
    public HashMap<String, String> getData();

    public JPanel getMainPanel();

    public void removeValidators();
}
