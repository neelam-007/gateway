package com.l7tech.external.assertions.extensiblesocketconnectorassertion.console;

import com.l7tech.external.assertions.extensiblesocketconnectorassertion.codecconfigurations.MarkerTerminatedCodecConfiguration;

import javax.swing.*;

/**
 * Created with IntelliJ IDEA.
 * User: njordan
 * Date: 19/12/12
 * Time: 3:25 PM
 * To change this template use File | Settings | File Templates.
 */
public class MarkerTerminatedCodecSettingsPanel implements CodecSettingsPanel<MarkerTerminatedCodecConfiguration> {
    private JPanel mainPanel;
    private JTextField terminatorField;

    public MarkerTerminatedCodecSettingsPanel() {
    }

    @Override
    public boolean validateView() {
        String terminatorString = terminatorField.getText().trim().toLowerCase();
        return terminatorString.matches("(?:[abcdef0-9][abcdef0-9])+");
    }

    @Override
    public void updateModel(MarkerTerminatedCodecConfiguration model) {
        String terminatorString = terminatorField.getText().trim().toLowerCase();
        byte[] terminatorBytes = new byte[terminatorString.length() / 2];
        int terminatorBytesPos = 0;

        for (int i = 0; i < terminatorString.length(); i = i + 2) {
            byte byteValue = (byte) 0;
            switch (terminatorString.charAt(i)) {
                case '0':
                    byteValue = (byte) 0x00;
                    break;
                case '1':
                    byteValue = (byte) 0x10;
                    break;
                case '2':
                    byteValue = (byte) 0x20;
                    break;
                case '3':
                    byteValue = (byte) 0x30;
                    break;
                case '4':
                    byteValue = (byte) 0x40;
                    break;
                case '5':
                    byteValue = (byte) 0x50;
                    break;
                case '6':
                    byteValue = (byte) 0x60;
                    break;
                case '7':
                    byteValue = (byte) 0x70;
                    break;
                case '8':
                    byteValue = (byte) 0x80;
                    break;
                case '9':
                    byteValue = (byte) 0x90;
                    break;
                case 'a':
                    byteValue = (byte) 0xa0;
                    break;
                case 'b':
                    byteValue = (byte) 0xb0;
                    break;
                case 'c':
                    byteValue = (byte) 0xc0;
                    break;
                case 'd':
                    byteValue = (byte) 0xd0;
                    break;
                case 'e':
                    byteValue = (byte) 0xe0;
                    break;
                case 'f':
                    byteValue = (byte) 0xf0;
                    break;
            }

            switch (terminatorString.charAt(i + 1)) {
                case '0':
                    byteValue |= (byte) 0x00;
                    break;
                case '1':
                    byteValue |= (byte) 0x01;
                    break;
                case '2':
                    byteValue |= (byte) 0x02;
                    break;
                case '3':
                    byteValue |= (byte) 0x03;
                    break;
                case '4':
                    byteValue |= (byte) 0x04;
                    break;
                case '5':
                    byteValue |= (byte) 0x05;
                    break;
                case '6':
                    byteValue |= (byte) 0x06;
                    break;
                case '7':
                    byteValue |= (byte) 0x07;
                    break;
                case '8':
                    byteValue |= (byte) 0x08;
                    break;
                case '9':
                    byteValue |= (byte) 0x09;
                    break;
                case 'a':
                    byteValue |= (byte) 0x0a;
                    break;
                case 'b':
                    byteValue |= (byte) 0x0b;
                    break;
                case 'c':
                    byteValue |= (byte) 0x0c;
                    break;
                case 'd':
                    byteValue |= (byte) 0x0d;
                    break;
                case 'e':
                    byteValue |= (byte) 0x0e;
                    break;
                case 'f':
                    byteValue |= (byte) 0x0f;
                    break;
            }

            terminatorBytes[terminatorBytesPos++] = byteValue;
        }

        model.setTermnatorBytes(terminatorBytes);
    }

    @Override
    public void updateView(MarkerTerminatedCodecConfiguration model) {
        StringBuilder sb = new StringBuilder();

        for (byte byteValue : model.getTermnatorBytes()) {
            switch (byteValue & (byte) 0xf0) {
                case (byte) 0x00:
                    sb.append("0");
                    break;
                case (byte) 0x10:
                    sb.append("1");
                    break;
                case (byte) 0x20:
                    sb.append("2");
                    break;
                case (byte) 0x30:
                    sb.append("3");
                    break;
                case (byte) 0x40:
                    sb.append("4");
                    break;
                case (byte) 0x50:
                    sb.append("5");
                    break;
                case (byte) 0x60:
                    sb.append("6");
                    break;
                case (byte) 0x70:
                    sb.append("7");
                    break;
                case (byte) 0x80:
                    sb.append("8");
                    break;
                case (byte) 0x90:
                    sb.append("9");
                    break;
                case (byte) 0xa0:
                    sb.append("a");
                    break;
                case (byte) 0xb0:
                    sb.append("b");
                    break;
                case (byte) 0xc0:
                    sb.append("c");
                    break;
                case (byte) 0xd0:
                    sb.append("d");
                    break;
                case (byte) 0xe0:
                    sb.append("e");
                    break;
                case (byte) 0xf0:
                    sb.append("f");
                    break;
            }

            switch (byteValue & (byte) 0x0f) {
                case (byte) 0x00:
                    sb.append("0");
                    break;
                case (byte) 0x01:
                    sb.append("1");
                    break;
                case (byte) 0x02:
                    sb.append("2");
                    break;
                case (byte) 0x03:
                    sb.append("3");
                    break;
                case (byte) 0x04:
                    sb.append("4");
                    break;
                case (byte) 0x05:
                    sb.append("5");
                    break;
                case (byte) 0x06:
                    sb.append("6");
                    break;
                case (byte) 0x07:
                    sb.append("7");
                    break;
                case (byte) 0x08:
                    sb.append("8");
                    break;
                case (byte) 0x09:
                    sb.append("9");
                    break;
                case (byte) 0x0a:
                    sb.append("a");
                    break;
                case (byte) 0x0b:
                    sb.append("b");
                    break;
                case (byte) 0x0c:
                    sb.append("c");
                    break;
                case (byte) 0x0d:
                    sb.append("d");
                    break;
                case (byte) 0x0e:
                    sb.append("e");
                    break;
                case (byte) 0x0f:
                    sb.append("f");
                    break;
            }
        }

        terminatorField.setText(sb.toString());
    }

    @Override
    public JPanel getPanel() {
        return mainPanel;
    }
}
