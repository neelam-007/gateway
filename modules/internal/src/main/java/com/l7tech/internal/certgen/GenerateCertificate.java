package com.l7tech.internal.certgen;

import com.l7tech.common.io.CertGenParams;
import com.l7tech.common.io.CertUtils;
import com.l7tech.common.io.X509GeneralName;
import com.l7tech.security.cert.TestCertificateGenerator;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.JdkLoggerConfigurator;
import com.l7tech.util.Pair;
import com.l7tech.util.TextUtils;
import org.springframework.beans.support.ArgumentConvertingMethodInvoker;
import org.springframework.util.MethodInvoker;

import java.io.IOException;
import java.io.PrintStream;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.*;

/**
 * Command line utility for generating certificates.  Uses TestCertificateGenerator.
 */
public class GenerateCertificate {

    private static abstract class Option {
        final String desc;

        protected Option(String desc) {
            this.desc = desc;
        }

        /**
         * Configure this option on the specified target, consuming and removing zero or more
         * strings from the specified iterator, leaving it positioned just after this Option's
         * configuration info.
         *
         * @param target the target object to configure.  Required.
         * @param remainingArgs an Iterator containing remaining arguments.  Some of these may be consumed
         *                      by this option and removed.
         */
        abstract void configure(GenerateCertificate target, Iterator<String> remainingArgs);
    }

    private static class MethodOption extends Option {
        private final String methodName;
        private final int numArgs;

        private MethodOption(String desc, String methodName, int numArgs) {
            super(desc);
            this.methodName = methodName;
            this.numArgs = numArgs;
        }

        @Override
        public void configure(GenerateCertificate target, Iterator<String> remainingArgs) {
            invoke(target, remainingArgs);
        }

        protected void invoke(Object targetObject, Iterator<String> remainingArgs) {
            MethodInvoker mi = new ArgumentConvertingMethodInvoker();
            mi.setTargetObject(targetObject);
            mi.setTargetMethod(methodName);
            mi.setArguments(extractArgs(remainingArgs, numArgs));

            try {
                mi.prepare();
                mi.invoke();
            } catch (Exception e) {
                throw new IllegalArgumentException(e);
            }
        }

        protected String[] extractArgs(Iterator<String> remainingArgs, int num) {
            String[] args = new String[num];
            for (int i = 0; i < num; ++i) {
                args[i] = remainingArgs.next();
                remainingArgs.remove();
            }
            return args;
        }
    }

    private static class GeneratorOption extends MethodOption {
        private GeneratorOption(String desc, String methodName, int numArgs) {
            super(desc, methodName, numArgs);
        }

        @Override
        public void configure(GenerateCertificate target, Iterator<String> remainingArgs) {
            invoke(target.generator, remainingArgs);
        }
    }

    static final Map<String, Integer> KEY_USAGE_BITS_BY_NAME = new TreeMap<String, Integer>(String.CASE_INSENSITIVE_ORDER) {{
        putAll(CertUtils.KEY_USAGE_BITS_BY_NAME);
        put("None", 0);
    }};

    static final Map<String, String> KEY_PURPOSE_IDS_BY_NAME = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER) {{
        putAll(CertUtils.KEY_PURPOSE_IDS_BY_NAME);
    }};

    private static final Map<String, Option> OPTIONS = new TreeMap<String, Option>(String.CASE_INSENSITIVE_ORDER) {{
        put("help",                 new MethodOption("\tDisplay this help message and exit", "help", 0));
        put("outfile",              new MethodOption("kspath kspass\tName of PKCS#12 file in which to save resulting cert and private key", "outfile", 2));
        put("text",                 new MethodOption("\tDisplay detailed information about the generated certificate", "text", 0));
        put("noBase64",             new MethodOption("\tDo not output PEM certificate", "noBase64", 0));
        put("issuer",               new MethodOption("kspath kspass\tSign using cert chain and key from the specified keystore", "issuer", 2));
        put("keySize",              new GeneratorOption("rsabits\tSet the RSA key size in bits (default=512)", "keySize", 1));
        put("subject",              new GeneratorOption("dn\tSet the subject DN (default='cn=test')", "subject", 1));
        put("daysUntilExpiry",      new GeneratorOption("days\tNumber of days until expiry (default=7300)", "daysUntilExpiry", 1));
        put("notBefore",            new GeneratorOption("date\tStart date of cert validity (default=now)", "notBefore", 1));
        put("signatureAlgorithm",   new GeneratorOption("string\tName of signature algorithm to use when signing (default=SHA1withRSA)", "signatureAlgorithm", 1));
        put("noExtensions",         new GeneratorOption("\tDo not include default extensions", "noExtensions", 0));
        put("noBasicConstraints",   new GeneratorOption("\tDo not include basic constraints extension", "noBasicConstraints", 0));
        put("basicConstraintsNoCa", new GeneratorOption("\tInclude basic constraints with the cA bit set to false", "basicConstraintsNoCa", 0));
        put("basicConstraintsCa",   new GeneratorOption("pathlen\tInclude basic constraints with the cA bit set to true, with the specified maximum path length", "basicConstraintsCa", 1));
        put("noKeyUsage",           new GeneratorOption("\tDo not include a key usage extension", "noKeyUsage", 0));
        put("noExtKeyUsage",        new GeneratorOption("\tDo not include an extended key usage extension", "noExtKeyUsage", 0));
        put("keyUsageCritical",     new GeneratorOption("false\tWhether the key usage extension should be marked as Critical (default=true)", "keyUsageCriticality", 1));
        put("extKeyUsageCritical",  new GeneratorOption("false\tWhether the extended key usage should be marked as Critical (default=true)", "extendedKeyUsageCriticality", 1));
        put("extKeyUsage",          new ExtKeyUsageOption("kpNameOrOid\tInclude an extended key usage including the specified key purpose (may be repeated)"));
        put("keyUsage",             new KeyUsageOption("keyUsage\tInclude a key usage including the specified key usage bit (may be repeated)"));
        put("countriesOfCitizenship", new CountriesOfCitizenshipOption("countryCode\tInclude SubjectDirectoryAttributes ext including country code (may be repeated)"));
        put("certificatePolicies",  new CertificatePolicyOption("certificatePolicy\tInclude certificate policy (may be repeated)"));
        put("subjectAltName",       new SubjectAlternativeNamePolicyOption("hostnameOrIp\tSubject Alternative Name hostname pattern or IP address (may be repeated)"));
        put("ocspUrl",              new OcspUrlOption("url\tIncludes an AuthorityInfoAccess extension containing the specified OCSP URL (may be repeated)"));
        put("ocspUrlCritical",      new MethodOption("false\tWhether an AuthorityInfoAccess extension should be marked as Critical (default=false)", "ocspUrlCritical", 1));
    }};

    //
    // Locals
    //

    TestCertificateGenerator generator;
    final PrintStream textout;
    String outputPath = null;
    String outputPassword = "password";
    boolean helpMode = false;
    boolean printAsText = false;
    boolean printAsBase64 = true;
    boolean sawExplicitKeyUsage = false;
    boolean sawExplicitExtKeyUsage = false;
    X509Certificate[] issuerChain = null;

    GenerateCertificate(PrintStream textout, List<String> args) {
        this.textout = textout;
        generator = new TestCertificateGenerator();
        Iterator<String> it = new ArrayList<String>(args).iterator();
        while (it.hasNext()) {
            String arg = it.next();
            if ("--".equals(arg)) // allow double dash by itself to terminate option processing
                break;
            if (arg.startsWith("--"))  // allow double-dash to introduce options
                arg = arg.substring(1);
            if (arg.startsWith("-") && arg.length() > 1) {
                arg = arg.substring(1);
                Option option = OPTIONS.get(arg);
                if (option != null) {
                    it.remove();
                    option.configure(this, it);
                } else
                    throw new IllegalArgumentException("Unrecognized option: " + arg);
            } else
                throw new IllegalArgumentException("Invalid option format; should be -option val: " + arg);
        }
    }

    void generate() throws GeneralSecurityException, IOException {
        if (helpMode) {
            doHelp();
            return;
        }

        X509Certificate cert = generator.generate();
        final X509Certificate[] chain;
        if (issuerChain == null) {
            chain = new X509Certificate[] { cert };
        } else {
            chain = new X509Certificate[issuerChain.length + 1];
            chain[0] = cert;
            System.arraycopy(issuerChain, 0, chain, 1, chain.length - 1);
        }
        PrivateKey privateKey = generator.getPrivateKey();

        if (printAsText)
            textout.println(cert);

        if (printAsBase64)
            textout.println(CertUtils.encodeAsPEM(cert));

        if (outputPath != null) {
            TestCertificateGenerator.saveAsPkcs12(chain, privateKey, outputPath, outputPassword);
            textout.println("Cert chain with private key saved to " + outputPath);
        }
    }

    private void doHelp() {
        textout.println("Usage: GenerateCertificate [options]\n");
        for (Map.Entry<String, Option> entry : OPTIONS.entrySet()) {
            textout.print("  -");
            textout.print(entry.getKey());
            textout.print(' ');
            textout.println(entry.getValue().desc);
        }
    }

    public void ocspUrlCritical(String crit) {
        this.generator.getCertGenParams().setAuthorityInfoAccessCritical(Boolean.valueOf(crit));
    }

    public void outfile(String outputPath, String kspass) {
        this.outputPath = outputPath;
        this.outputPassword = kspass;
    }

    public void text() {
        this.printAsText = true;
    }

    public void noBase64() {
        this.printAsBase64 = false;
    }

    public void help() {
        this.helpMode = true;
    }

    public void issuer(String path, String password) throws IOException, GeneralSecurityException {
        Pair<X509Certificate[],PrivateKey> issuer = TestCertificateGenerator.loadFromPkcs12(path, password);
        issuerChain = issuer.left;
        generator.issuer(issuerChain[0], issuer.right);
    }

    public static void main(String[] rawArgs) {
        JdkLoggerConfigurator.configure(GenerateCertificate.class.getName(), null);
        System.setProperty("com.l7tech.common.security.jceProviderEngineName", "BC");

        try {
            new GenerateCertificate(System.out, Arrays.asList(rawArgs)).generate();
        } catch (Exception e) {
            System.err.println("Fatal: " + ExceptionUtils.getMessage(e));
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }

    private static String list(Set<String> tojoin) {
        return ("\n   \t    " + TextUtils.join("\n   \t    ", tojoin));
    }

    private static class OcspUrlOption extends Option {
        protected OcspUrlOption(String desc) {
            super(desc);
        }

        @Override
        void configure(GenerateCertificate target, Iterator<String> remainingArgs) {
            String url = remainingArgs.next();
            remainingArgs.remove();

            CertGenParams params = target.generator.getCertGenParams();
            List<String> urls = params.getAuthorityInfoAccessOcspUrls();
            if (urls == null)
                urls = new ArrayList<String>();
            urls.add(url);
            params.setIncludeAuthorityInfoAccess(true);
            params.setAuthorityInfoAccessOcspUrls(urls);
        }
    }

    private static class ExtKeyUsageOption extends Option {
        protected ExtKeyUsageOption(String desc) {
            super(desc + list(KEY_PURPOSE_IDS_BY_NAME.keySet()));
        }

        @Override
        public void configure(GenerateCertificate target, Iterator<String> remainingArgs) {
            if (!target.sawExplicitExtKeyUsage) {
                target.generator.noExtKeyUsage();
                target.generator.extKeyUsage(true, Collections.<String>emptyList());
            }
            target.sawExplicitExtKeyUsage = true;

            String keyPurposeNameOrOid = remainingArgs.next();
            remainingArgs.remove();

            final String oid;
            if (keyPurposeNameOrOid.matches("^\\d+(?:\\.\\d+)+$")) {
                oid = keyPurposeNameOrOid;
            } else {
                oid = KEY_PURPOSE_IDS_BY_NAME.get(keyPurposeNameOrOid);
                if (oid == null)
                    throw new IllegalArgumentException("Extended key purpose ID is neither a dotted decimal OID nor a recognized key purpose name: " + keyPurposeNameOrOid);
            }

            List<String> oids = new ArrayList<String>(target.generator.getCertGenParams().getExtendedKeyUsageKeyPurposeOids());
            oids.add(oid);
            target.generator.getCertGenParams().setExtendedKeyUsageKeyPurposeOids(oids);
        }
    }

    private static class CountriesOfCitizenshipOption extends Option {
        protected CountriesOfCitizenshipOption(String desc) {
            super(desc);
        }

        @Override
        public void configure(GenerateCertificate target, Iterator<String> remainingArgs) {
            String countryCode = remainingArgs.next().toUpperCase();
            remainingArgs.remove();
            List<String> codes = new ArrayList<String>(target.generator.getCertGenParams().getCountryOfCitizenshipCountryCodes());
            codes.add(countryCode);
            target.generator.countriesOfCitizenship(true, codes.toArray(new String[codes.size()]));
        }
    }

    private static class SubjectAlternativeNamePolicyOption extends Option {
        protected SubjectAlternativeNamePolicyOption(String desc) {
            super(desc);
        }

        @Override
        void configure(GenerateCertificate target, Iterator<String> remainingArgs) {
            String name = remainingArgs.next();
            remainingArgs.remove();
            List<X509GeneralName> names = new ArrayList<X509GeneralName>(target.generator.getCertGenParams().getSubjectAlternativeNames());
            names.add(X509GeneralName.fromHostNameOrIp(name));
            target.generator.subjectAlternativeNames(false, names.toArray(new X509GeneralName[names.size()]));
        }
    }

    private static class CertificatePolicyOption extends Option {
        protected CertificatePolicyOption(String desc) {
            super(desc);
        }

        @Override
        public void configure(GenerateCertificate target, Iterator<String> remainingArgs) {
            String certPolicy = remainingArgs.next().toUpperCase();
            remainingArgs.remove();
            List<String> policies = new ArrayList<String>(target.generator.getCertGenParams().getCertificatePolicies());
            policies.add(certPolicy);
            target.generator.certificatePolicies(true, policies.toArray(new String[policies.size()]));
        }
    }

    private static class KeyUsageOption extends Option {
        protected KeyUsageOption(String desc) {
            super(desc + list(KEY_USAGE_BITS_BY_NAME.keySet()));
        }

        @Override
        public void configure(GenerateCertificate target, Iterator<String> remainingArgs) {
            if (!target.sawExplicitKeyUsage) {
                target.generator.noKeyUsage();
                target.generator.keyUsage(true, 0);
            }
            target.sawExplicitKeyUsage = true;

            String keyUsageName = remainingArgs.next();
            remainingArgs.remove();

            Integer bit = KEY_USAGE_BITS_BY_NAME.get(keyUsageName);
            if (bit == null)
                throw new IllegalArgumentException("Unrecognized key usage bit name: " + keyUsageName);
            CertGenParams cgp = target.generator.getCertGenParams();
            cgp.setKeyUsageBits(cgp.getKeyUsageBits() | bit);
        }
    }
}
