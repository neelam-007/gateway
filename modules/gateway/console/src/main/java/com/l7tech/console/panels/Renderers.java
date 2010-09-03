package com.l7tech.console.panels;

import com.l7tech.gateway.common.security.RevocationCheckPolicy;

import javax.swing.*;
import java.awt.*;
import java.text.MessageFormat;
import java.util.ResourceBundle;

/**
 *
 */
public class Renderers {

    /**
     * Renderer for items keyed into given resource bundle.
     */
    public static final class KeyedResourceRenderer extends JLabel implements ListCellRenderer {
        private final ResourceBundle bundle;
        private final String keyFormat;

        public KeyedResourceRenderer( final ResourceBundle bundle,
                                      final String keyFormat ) {
            this.bundle = bundle;
            this.keyFormat = keyFormat;
        }

        @Override
        public Component getListCellRendererComponent( JList list,
                                                       Object value,
                                                       int index,
                                                       boolean isSelected,
                                                       boolean cellHasFocus)
        {
            Object[] keyFormatArgs = new Object[]{ value };

            String label = "";
            if ( value != null ) {
                label = bundle.getString( MessageFormat.format(keyFormat, keyFormatArgs));
            }

            setText(label);

            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
                setOpaque(true);
            } else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
                setOpaque(false);
            }

            setEnabled(list.isEnabled());
            setFont(list.getFont());

            return this;
        }
    }

    /**
     * Renderer for RevocationCheckPolicies
     */
    public static final class RevocationCheckPolicyRenderer extends JLabel implements ListCellRenderer {
        @Override
        public Component getListCellRendererComponent( JList list,
                                                       Object value,
                                                       int index,
                                                       boolean isSelected,
                                                       boolean cellHasFocus)
        {
            RevocationCheckPolicy revocationCheckPolicy = (RevocationCheckPolicy) value;

            String label = "";
            if ( revocationCheckPolicy != null ) {
                label = revocationCheckPolicy.getName();
                if (revocationCheckPolicy.isDefaultPolicy())  {
                    label += " [Default]";
                }
            }

            setText(label);

            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
                setOpaque(true);
            } else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
                setOpaque(false);
            }

            setEnabled(list.isEnabled());
            setFont(list.getFont());

            return this;
        }
    }
}
