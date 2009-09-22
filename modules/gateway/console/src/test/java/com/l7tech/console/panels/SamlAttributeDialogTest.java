/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.console.panels;

import com.l7tech.console.beaneditor.BeanListener;
import com.l7tech.console.panels.saml.EditAttributeDialog;
import com.l7tech.policy.assertion.xmlsec.SamlAttributeStatement;

import javax.swing.*;
import java.beans.PropertyChangeEvent;

import org.junit.Ignore;

/**
 * @author emil
 * @version Mar 22, 2005
 */
@Ignore
public class SamlAttributeDialogTest {
    public static void main(String[] args)
      throws Exception {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        SamlAttributeStatement.Attribute sba = new SamlAttributeStatement.Attribute();
        sba.setName("foo");
        sba.setNamespace("urn:bar");
        sba.setValue("baz");
        EditAttributeDialog dlg = new EditAttributeDialog(new JDialog(), sba, 2, true);
        dlg.addBeanListener(new BeanListener() {
            public void onEditAccepted(Object source, Object bean) {
                System.out.println("OK " + bean);
            }

            public void onEditCancelled(Object source, Object bean) {
                System.out.println("Cancel " + bean);
            }

            public void propertyChange(PropertyChangeEvent evt) {
                System.out.println("Change " + evt.getOldValue() + " to " + evt.getNewValue());
            }
        });

        dlg.pack();
        dlg.setVisible(true);
    }
}