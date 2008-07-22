package com.l7tech.console.util;

import com.l7tech.gui.util.SaveErrorStrategy;
import com.l7tech.gui.util.SheetHolder;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.common.http.GenericHttpClient;
import com.l7tech.common.http.SimpleHttpClient;
import com.l7tech.common.http.GenericHttpRequestParams;
import com.l7tech.common.http.prov.jdk.UrlConnectionHttpClient;
import com.l7tech.common.mime.ContentTypeHeader;

import javax.swing.*;
import java.applet.Applet;
import java.net.URL;
import java.io.IOException;

/**
 * @auther: ghuang
 */

/**
 * The class specifies a save-error strategy using a browser to save error into a file.
 * The super class is {@link com.l7tech.gui.util.SaveErrorStrategy}
 */
public class BrowserSaveErrorStrategy extends SaveErrorStrategy {

    public void saveErrorReportFile() {
        Applet applet;
        SheetHolder holder = DialogDisplayer.getSheetHolderAncestor(errorMessageDialog);
        if (holder != null && holder instanceof Applet) {
            applet = (Applet)holder;
        } else {
            JOptionPane.showMessageDialog(errorMessageDialog,
                    "Cannot save a report due to an internal error.  Please contact your to administrator.",
                    "Warning",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        String fileContent = getReportContent();
        String urlStr = getBasicURL(applet.getDocumentBase().toString()) + "/ssg/webadmin/filedownload";
        if (urlStr == null) {
            JOptionPane.showMessageDialog(errorMessageDialog,
                    "Cannot process the saving-report request due to the invalid URL.",
                    "Warning",
                    JOptionPane.WARNING_MESSAGE);
        }

        try {
            // Uploading the report
            URL url = new URL(urlStr + "?filename=" + getSuggestedFileName());
            GenericHttpClient client = new UrlConnectionHttpClient();
            SimpleHttpClient sClient = new SimpleHttpClient(client);
            GenericHttpRequestParams params = new GenericHttpRequestParams(url);
            params.setContentType(ContentTypeHeader.TEXT_DEFAULT);
            SimpleHttpClient.SimpleHttpResponse response = sClient.post(params, fileContent.getBytes("UTF-8"));
            // Check for a 200 response code (on POST)
            if (response.getStatus() != 200) {
                DialogDisplayer.showMessageDialog(applet, null, "The report cannot be saved.  Try it again.", null);
                return;
            }
            String key = new String(response.getBytes());

            // Downloading the report
            url = new URL(urlStr + "?key=" + key);
            applet.getAppletContext().showDocument(url, "_self");
        }
        catch (IOException ex) {
            JOptionPane.showMessageDialog(errorMessageDialog,
                    "Cannot process the saving-report request due to the communication problem.",
                    "Warning",
                    JOptionPane.WARNING_MESSAGE);
        }
    }

    private String getBasicURL(String currURL) {
        int idx = currURL.indexOf("9443");
        if (idx < 0) idx = currURL.indexOf("8443");
        if (idx < 0) return null;
        return currURL.substring(0, (idx + 4));
    }
}
