package com.l7tech.console.panels;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class SsgConnectorPropertiesDialogTest {

    @Mock
    private PrivateKeysComboBox privateKeyComboBox;

    @Mock
    private CipherSuiteListModel cipherSuiteListModel;

    SsgConnectorPropertiesDialog ssgConnectorPropertiesDialog;

    @Test
    @Ignore
    //Default Cipher list + "EC" key selected. All ciphers(RSA and EC) are selected by default
    public void testIsEccKeyButRsaCiphersChecked_When_CipherListIsDefault() {
        Mockito.when(privateKeyComboBox.getSelectedKeyAlgorithm()).thenReturn("EC");
        Mockito.when(cipherSuiteListModel.asCipherListStringOrNullIfDefault()).thenReturn(null);

        ssgConnectorPropertiesDialog = new SsgConnectorPropertiesDialog(privateKeyComboBox, cipherSuiteListModel);
        boolean isEccKeyButRsaCiphersChecked = ssgConnectorPropertiesDialog.isEccKeyButRsaCiphersChecked();

        //Default list will have RSA and EC ciphers. So when "EC" private key is selected with "RSA ciphers"
        //isEccKeyButRsaCiphersChecked() method should return true
        assertEquals(isEccKeyButRsaCiphersChecked, true);
    }


    @Test
    @Ignore
    //"EC" private key selected and only one TLS_RSA_WITH_AES_256_CBC_SHA256 ciphers are checked
    public void testIsEccKeyButRsaCiphersChecked_When_Only_One_TLS_RSA_Cipher_Checked() {
        Mockito.when(privateKeyComboBox.getSelectedKeyAlgorithm()).thenReturn("EC");
        Mockito.when(cipherSuiteListModel.asCipherListStringOrNullIfDefault()).thenReturn("TLS_RSA_WITH_AES_256_CBC_SHA256");

        ssgConnectorPropertiesDialog = new SsgConnectorPropertiesDialog(privateKeyComboBox, cipherSuiteListModel);
        boolean isEccKeyButRsaCiphersChecked = ssgConnectorPropertiesDialog.isEccKeyButRsaCiphersChecked();

        //TLS_RSA_WITH_AES_256_CBC_SHA256 works only with RSA cipher
        assertEquals(isEccKeyButRsaCiphersChecked, true);
    }

    @Test
    @Ignore
    //Customized Cipher list + "EC" key and only TLS_ECDH_RSA_WITH_AES_128_CBC_SHA cipher checked
    public void testIsRsaKeyButECCCiphersChecked_When_Only_TLS_ECDH_RSA_WITH_AES_128_CBC_SHA_Checked() {
        Mockito.when(privateKeyComboBox.getSelectedKeyAlgorithm()).thenReturn("EC");
        Mockito.when(cipherSuiteListModel.asCipherListStringOrNullIfDefault()).thenReturn("TLS_ECDH_RSA_WITH_AES_128_CBC_SHA");

        ssgConnectorPropertiesDialog = new SsgConnectorPropertiesDialog(privateKeyComboBox, cipherSuiteListModel);
        boolean isRsaKeyButEccCiphersChecked = ssgConnectorPropertiesDialog.isRsaKeyButEccCiphersChecked();

        //TLS_ECDH_RSA_WITH_AES_128_CBC_SHA cipher works with EC key so this method should return false
        assertEquals(isRsaKeyButEccCiphersChecked, false);
    }

    @Test
    @Ignore
    //"EC" private key selected and mutiple RSA ciphers are checked
    public void testIsEccKeyButRsaCiphersChecked_When_Multiple_RSA_Cipher_Checked() {
        Mockito.when(privateKeyComboBox.getSelectedKeyAlgorithm()).thenReturn("EC");
        Mockito.when(cipherSuiteListModel.asCipherListStringOrNullIfDefault()).thenReturn(
                        "TLS_RSA_WITH_AES_256_CBC_SHA256," +
                        "TLS_RSA_WITH_AES_256_CBC_SHA," +
                        "TLS_RSA_WITH_AES_128_GCM_SHA256," +
                        "TLS_RSA_WITH_AES_128_CBC_SHA256," +
                        "TLS_RSA_WITH_AES_128_CBC_SHA," +
                        "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384");

        ssgConnectorPropertiesDialog = new SsgConnectorPropertiesDialog(privateKeyComboBox, cipherSuiteListModel);
        boolean isEccKeyButRsaCiphersChecked = ssgConnectorPropertiesDialog.isEccKeyButRsaCiphersChecked();

        //RSA ciphers don't work with EC key
        assertEquals(isEccKeyButRsaCiphersChecked, true);
    }

    @Test
    @Ignore
    //"EC" private key selected and mutiple ECC ciphers are checked
    public void testIsEccKeyButRsaCiphersChecked_When_ONLY_ECC_Ciphers_Checked() {
        Mockito.when(privateKeyComboBox.getSelectedKeyAlgorithm()).thenReturn("EC");
        Mockito.when(cipherSuiteListModel.asCipherListStringOrNullIfDefault()).thenReturn(
                        "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384," +
                        "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384," +
                        "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA," +
                        "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256," +
                        "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256," +
                        "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA," +
                        "TLS_ECDH_ECDSA_WITH_AES_256_GCM_SHA384," +
                        "TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA384," +
                        "TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA," +
                        "TLS_ECDH_ECDSA_WITH_AES_128_GCM_SHA256," +
                        "TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA256," +
                        "TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA" +
                        "TLS_ECDH_RSA_WITH_AES_256_GCM_SHA384," +
                        "TLS_ECDH_RSA_WITH_AES_256_CBC_SHA384," +
                        "TLS_ECDH_RSA_WITH_AES_256_CBC_SHA," +
                        "TLS_ECDH_RSA_WITH_AES_128_GCM_SHA256," +
                        "TLS_ECDH_RSA_WITH_AES_128_CBC_SHA256," +
                        "TLS_ECDH_RSA_WITH_AES_128_CBC_SHA");

        ssgConnectorPropertiesDialog = new SsgConnectorPropertiesDialog(privateKeyComboBox, cipherSuiteListModel);
        boolean isEccKeyButRsaCiphersChecked = ssgConnectorPropertiesDialog.isEccKeyButRsaCiphersChecked();

        // Private key is Elliptic curve and all "ECC" ciphers checked. So this method should return false
        assertEquals(isEccKeyButRsaCiphersChecked, false);
    }

    @Test
    @Ignore
    //RSA private key and all ciphers(RSA and ECC) are selected by default
    public void testIsRsaKeyButECC_CiphersChecked_When_CipherListIsDefault() {
        Mockito.when(privateKeyComboBox.getSelectedKeyAlgorithm()).thenReturn("RSA");
        Mockito.when(cipherSuiteListModel.asCipherListStringOrNullIfDefault()).thenReturn(null);

        ssgConnectorPropertiesDialog = new SsgConnectorPropertiesDialog(privateKeyComboBox, cipherSuiteListModel);
        boolean isRsaKeyButEccCiphersChecked = ssgConnectorPropertiesDialog.isRsaKeyButEccCiphersChecked();

        //Default list will have RSA and EC ciphers. isRsaKeyButEccCiphersChecked() method should return true
        //as "RSA" private key because and ECC ciphers are selected
        assertEquals(isRsaKeyButEccCiphersChecked, true);
    }

    @Test
    @Ignore
    //Customized Cipher list and only one ECC cipher is checked
    public void testIsRsaKeyButECCCiphersChecked_When_Only_One_TLS_ECDH_RSA_Checked() {
        Mockito.when(privateKeyComboBox.getSelectedKeyAlgorithm()).thenReturn("RSA");
        Mockito.when(cipherSuiteListModel.asCipherListStringOrNullIfDefault()).thenReturn("TLS_ECDH_RSA_WITH_AES_128_CBC_SHA");

        ssgConnectorPropertiesDialog = new SsgConnectorPropertiesDialog(privateKeyComboBox, cipherSuiteListModel);
        boolean isRsaKeyButEccCiphersChecked = ssgConnectorPropertiesDialog.isRsaKeyButEccCiphersChecked();

        //TLS_ECDH_RSA_WITH_AES_128_CBC_SHA cipher works only with EC key so this method should return false
        assertEquals(isRsaKeyButEccCiphersChecked, true);
    }

    @Test
    @Ignore
    //This is when user has modified Cipher list and multiple ECC ciphers are checked
    public void testIsRSAKeyButECCCiphersChecked_When_Multiple_ECC_Cipher_Checked() {
        Mockito.when(privateKeyComboBox.getSelectedKeyAlgorithm()).thenReturn("RSA");
        Mockito.when(cipherSuiteListModel.asCipherListStringOrNullIfDefault()).thenReturn(
                        "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384," +
                        "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384," +
                        "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA," +
                        "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256," +
                        "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256," +
                        "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA," +
                        "TLS_ECDH_RSA_WITH_AES_256_GCM_SHA384," +
                        "TLS_ECDH_ECDSA_WITH_AES_256_GCM_SHA384," +
                        "TLS_ECDH_RSA_WITH_AES_256_CBC_SHA384," +
                        "TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA384," +
                        "TLS_ECDH_RSA_WITH_AES_256_CBC_SHA," +
                        "TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA," +
                        "TLS_ECDH_RSA_WITH_AES_128_GCM_SHA256," +
                        "TLS_ECDH_ECDSA_WITH_AES_128_GCM_SHA256," +
                        "TLS_ECDH_RSA_WITH_AES_128_CBC_SHA256," +
                        "TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA256," +
                        "TLS_ECDH_RSA_WITH_AES_128_CBC_SHA," +
                        "TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA");

        ssgConnectorPropertiesDialog = new SsgConnectorPropertiesDialog(privateKeyComboBox, cipherSuiteListModel);
        boolean isRsaKeyButEccCiphersChecked = ssgConnectorPropertiesDialog.isRsaKeyButEccCiphersChecked();

        //RSA key selected and all ECC ciphers checked. isRsaKeyButEccCiphersChecked() method should return true
        assertEquals(isRsaKeyButEccCiphersChecked, true);
    }

    @Test
    @Ignore
    //Customized cipher list + RSA key amd and multiple TLS_RSA ciphers are checked
    public void testIsRSAKeyButECCCiphersChecked_When_ONLY_ECC_Ciphers_Checked() {
        Mockito.when(privateKeyComboBox.getSelectedKeyAlgorithm()).thenReturn("RSA");
        Mockito.when(cipherSuiteListModel.asCipherListStringOrNullIfDefault()).thenReturn(
                        "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384," +
                        "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384," +
                        "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA," +
                        "TLS_DHE_RSA_WITH_AES_256_GCM_SHA384," +
                        "TLS_DHE_RSA_WITH_AES_256_CBC_SHA256," +
                        "TLS_DHE_RSA_WITH_AES_256_CBC_SHA," +
                        "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256," +
                        "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256," +
                        "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA," +
                        "TLS_DHE_RSA_WITH_AES_128_GCM_SHA256," +
                        "TLS_DHE_RSA_WITH_AES_128_CBC_SHA256," +
                        "TLS_DHE_RSA_WITH_AES_128_CBC_SHA," +
                        "TLS_RSA_WITH_AES_256_GCM_SHA384," +
                        "TLS_RSA_WITH_AES_256_CBC_SHA256," +
                        "TLS_RSA_WITH_AES_256_CBC_SHA," +
                        "TLS_RSA_WITH_AES_128_GCM_SHA256," +
                        "TLS_RSA_WITH_AES_128_CBC_SHA256," +
                        "TLS_RSA_WITH_AES_128_CBC_SHA" );

        ssgConnectorPropertiesDialog = new SsgConnectorPropertiesDialog(privateKeyComboBox, cipherSuiteListModel);
        boolean isRsaKeyButEccCiphersChecked = ssgConnectorPropertiesDialog.isRsaKeyButEccCiphersChecked();

        // Private key is RSA and only "RSA" ciphers are checked. So this method should return false
        assertEquals(isRsaKeyButEccCiphersChecked, false);
    }


}