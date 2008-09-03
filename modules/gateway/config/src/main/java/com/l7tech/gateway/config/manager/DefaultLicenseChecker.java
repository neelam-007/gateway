package com.l7tech.gateway.config.manager;

import com.l7tech.util.BuildInfo;
import com.l7tech.util.ResourceUtils;
import com.l7tech.gateway.common.InvalidLicenseException;
import com.l7tech.gateway.common.License;
import com.l7tech.common.io.CertUtils;
import org.apache.commons.lang.StringUtils;

import java.security.cert.X509Certificate;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.logging.Logger;

/**
 * User: Mike
 * Date: Mar 6, 2007
 * Time: 9:15:57 PM
 */
public class DefaultLicenseChecker implements LicenseChecker {
    private static final Logger logger = Logger.getLogger(DefaultLicenseChecker.class.getName());
    private static final String licensePem = "-----BEGIN CERTIFICATE-----\n" +
            "MIIC6DCCAdACBEM41hwwDQYJKoZIhvcNAQEEBQAwOTEdMBsGA1UEChMUTGF5ZXIg\n" +
            "NyBUZWNobm9sb2dpZXMxGDAWBgNVBAMTD0xpY2Vuc2UgT2ZmaWNlcjAeFw0wNTA5\n" +
            "MjcwNTE4MjBaFw0yNTA5MjYwNTE4MjBaMDkxHTAbBgNVBAoTFExheWVyIDcgVGVj\n" +
            "aG5vbG9naWVzMRgwFgYDVQQDEw9MaWNlbnNlIE9mZmljZXIwggEhMA0GCSqGSIb3\n" +
            "DQEBAQUAA4IBDgAwggEJAoIBAH6Zu7CyJnp/UqHlZ3WNEy4OKXzms7movyd4Bpqb\n" +
            "6DRRzOq/qZfMMnoKCZ5tEpAODw9DPPJHoE3bXV67dDWTnDNwCU67r1fHBFqTqJaB\n" +
            "WgU1Gzgy+Ve7N6BaoeAXVJgEXR5b9MVFabfG1FYsqEbvKwUvOVqow1XGLoPWqAKP\n" +
            "3fdBDUPOJgGUnrzY1pBvBSLlQoKzGR+fHVrMn1zQRS9MFalwzIgrgvEUxeTA72DF\n" +
            "G3ZJJ47ek+OmYP7q5Nzz1rCSBilv7CTW8TCZMKLJSBHfB0pPDaIMLdPdZqOes3ng\n" +
            "9jXuWpVCHI/lljxjBBWNTne/fUmN8gayTKTztA4UbO/heJECAwEAATANBgkqhkiG\n" +
            "9w0BAQQFAAOCAQEAEUDRup8nlBrK6z2114ReO2gt+k+ZwtqbSIGBMM6kCKvnUV7f\n" +
            "Bmi9XnvglM/ekmKBNIqMXuCjbOcRqgU5eiuKpvctHRzUKTHT9CKUQfR7ow2+Kkq8\n" +
            "0vD7JCcsbIqDyWD7tsf/RGNLNZIcOGuBFDrJx1+lNo8R/FlXnestXGVIRCLyH+Y2\n" +
            "w8GvvmUdKMymq0Adpr14v4B6/+xikxWJoUVTwnBLCNWoAqizCjla9lm4wOtKqsS1\n" +
            "8TyDvB+rL9Gz+K5SRUxpWt0ADRWUJRdmF29H8GcDUcaAK7Ka6BjyrOhE9t6emB7e\n" +
            "cX/Yl+RgwYa4F314O0xBGP6baqtVy/5BObtucA==\n" +
            "-----END CERTIFICATE-----";


    public void checkLicense(Connection connection, String currentVersion, String productName, String productVersionMajor, String productVersionMinor) throws InvalidLicenseException {
        License lic = getLicense(connection);
        String licMessage = null;
        try {
            if (lic == null)
                licMessage = "No license found. The gateway will not be fully featured without a license.";
            else {
                lic.checkValidity();
                if (!lic.isProductEnabled(productName, productVersionMajor, productVersionMinor)) {
                    licMessage = "Your current license is not valid for this product (" + BuildInfo.getProductName() + " version " + currentVersion + ").";
                }
            }
        } catch ( InvalidLicenseException e) {
                licMessage = "The License is not valid. The gateway will not be fully featured without a valid license.";
        }

        if (StringUtils.isNotEmpty(licMessage)) {
            throw new InvalidLicenseException(licMessage);
        }
    }

    private License getLicense(Connection conn) {
        Statement stmt = null;
        License lic = null;
        try {
            stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("Select propvalue from cluster_properties where propkey='license'");
            if (rs != null) {
                if (rs.next()) {
                    String licString = rs.getString(1);
                    lic = new License(
                            licString,
                            new X509Certificate[] {
                                    CertUtils.decodeCert(licensePem.getBytes())
                            },
                            null);
                }
            }
        } catch (Exception e) {
            logger.severe("An error occurred while getting the license from the database: " + e.getMessage());
        } finally {
            ResourceUtils.closeQuietly( stmt );
            ResourceUtils.closeQuietly( conn );
        }
        return lic;
    }
}
