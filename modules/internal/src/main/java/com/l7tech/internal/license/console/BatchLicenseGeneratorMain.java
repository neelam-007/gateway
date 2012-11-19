package com.l7tech.internal.license.console;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.util.SyspropUtil;
import org.apache.commons.cli.*;
import org.w3c.dom.Document;

import java.io.*;
import java.security.*;
import java.security.cert.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.l7tech.internal.license.LicenseGenerator.*;
import static com.l7tech.internal.license.LicenseGeneratorKeystoreUtils.*;

/**
 * @author jwilliams@layer7tech.com
 */
public class BatchLicenseGeneratorMain {

    // resource bundles
    private static final String BUNDLE_LICENSE_PRODUCTS =
            "com/l7tech/internal/license/console/resources/licenseProducts";
    private static final String BUNDLE_LICENSE_FEATURE_SETS =
            "com/l7tech/internal/license/console/resources/licenseFeatureSets";

    // external properties files
    private static final String EULA_PROPERTIES_FILE_PATH = "./eula.properties";

    private static final String OPTION_HELP_OPT = "h";
    private static final String OPTION_HELP_DESCRIPTION = "print this message";
    private static final String OPTION_FILE_OPT = "f";
    private static final String OPTION_FILE_ARG_NAME = "file";
    private static final String OPTION_FILE_DESCRIPTION = "Specify CSV file path from which to read license details";
    private static final String OPTION_EULA_OPT = "e";
    private static final String OPTION_EULA_ARG_NAME = "eula";
    private static final String OPTION_EULA_DESCRIPTION =
            "Specify DEFAULT, a custom EULA code, or custom EULA file path";
    private static final String OPTION_SAVE_ONLY_UNSIGNED_LICENSES_OPT = "u";
    private static final String OPTION_SAVE_ONLY_UNSIGNED_LICENSES_DESCRIPTION = "Save unsigned licenses only";
    private static final String OPTION_SAVE_BOTH_SIGNED_AND_UNSIGNED_LICENSES_OPT = "b";
    private static final String OPTION_SAVE_BOTH_SIGNED_AND_UNSIGNED_LICENSES_DESCRIPTION =
            "Save both signed and unsigned licenses";

    private static final char UNDERSCORE_SEPARATOR = '_';
    private static final String UNSIGNED_PREFIX = "Unsigned_";
    private static final String ZIP_FILE_EXTENSION = ".zip";

    private static boolean saveSignedLicenses = true;
    private static boolean saveUnsignedLicenses = false;

    private static String eulaFilePath;
    private static String licenseDetailsFile;

    private static Map<String, String> eulaFilePaths;

    public static void main(String[] args) {
        //set crypto jar path
        SyspropUtil.setProperty("com.l7tech.security.prov.rsa.libpath.nonfips", "USECLASSPATH");

        //disable logging set up for other modules - unimportant to the batch license generation
        LogManager.getLogManager().reset();

        if(System.currentTimeMillis() < 0) {
            throw new NullPointerException();
        }

        try {
            eulaFilePaths = loadEULADefinitions();

            processCommandLineArguments(args);

            Calendar generationTime = Calendar.getInstance();

            String eulaText = getEulaText(eulaFilePath);

            String generationTimeFormatted =
                    new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-SSS").format(generationTime.getTime());

            Map<String, String> products = createUnmodifiableMapFromBundle(BUNDLE_LICENSE_PRODUCTS);
            Map<String, String> featureSets = createUnmodifiableMapFromBundle(BUNDLE_LICENSE_FEATURE_SETS);

            BatchLicenseGenerator batchGenerator;

            if(saveSignedLicenses) {
                KeyStore keyStore = loadKeyStore();
                X509Certificate signerCert = getSignerCert(keyStore);
                PrivateKey signerKey = getSignerKey(keyStore);

                batchGenerator =
                        new BatchLicenseGenerator(generationTime, products, featureSets, signerCert, signerKey);
            } else {
                batchGenerator = new BatchLicenseGenerator(generationTime, products, featureSets);
            }

            List<BatchLicenseGenerator.LicenseDetailsRecord> licenceDetailsRecords =
                    batchGenerator.readLicenseDetailsRecords(licenseDetailsFile);

            for (BatchLicenseGenerator.LicenseDetailsRecord record : licenceDetailsRecords) {
                Map<String, Document> unsignedLicenses = batchGenerator.generateLicenses(record, eulaText);

                String fileNameSuffix = UNDERSCORE_SEPARATOR + generationTimeFormatted + ZIP_FILE_EXTENSION;

                if(saveUnsignedLicenses) {
                    String unsignedArchiveFileName = UNSIGNED_PREFIX + record.getLicensee() +
                            UNDERSCORE_SEPARATOR + record.getProductCode() + fileNameSuffix;

                    File archiveUnsigned = createLicenseArchive(unsignedLicenses, unsignedArchiveFileName);
                    System.out.println(archiveUnsigned.getAbsolutePath());
                }

                if(saveSignedLicenses) {
                    Map<String,Document> signedLicenses = batchGenerator.signLicenses(unsignedLicenses);

                    String signedArchiveFileName = record.getLicensee() +
                            UNDERSCORE_SEPARATOR + record.getProductCode() + fileNameSuffix;

                    File archiveSigned = createLicenseArchive(signedLicenses, signedArchiveFileName);
                    System.out.println(archiveSigned.getAbsolutePath());
                }
            }
        } catch (LicenseGeneratorException e) {
            System.err.println(e.getMessage());

            if(null != e.getCause()) {
                System.err.println(e.getCause().getMessage());
            }

            System.exit(1);
        } catch (GeneralSecurityException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    private static void processCommandLineArguments(String[] commandLineArguments) {
        Option helpOpt = new Option(OPTION_HELP_OPT, OPTION_HELP_DESCRIPTION);

        Option fileOpt = OptionBuilder.withArgName(OPTION_FILE_ARG_NAME)
                .hasArg()
                .isRequired()
                .withDescription(OPTION_FILE_DESCRIPTION)
                .create(OPTION_FILE_OPT);

        Option eulaOpt = OptionBuilder.withArgName(OPTION_EULA_ARG_NAME)
                .hasArg()
                .isRequired()
                .withDescription(OPTION_EULA_DESCRIPTION)
                .create(OPTION_EULA_OPT);

        Option saveUnsignedOnlyOpt = OptionBuilder.withDescription(OPTION_SAVE_ONLY_UNSIGNED_LICENSES_DESCRIPTION)
                .create(OPTION_SAVE_ONLY_UNSIGNED_LICENSES_OPT);

        Option saveBothOpt = OptionBuilder.withDescription(OPTION_SAVE_BOTH_SIGNED_AND_UNSIGNED_LICENSES_DESCRIPTION)
                .create(OPTION_SAVE_BOTH_SIGNED_AND_UNSIGNED_LICENSES_OPT);

        if(0 == commandLineArguments.length) {
            printHelp(fileOpt, eulaOpt, saveUnsignedOnlyOpt, saveBothOpt, helpOpt);
            System.exit(1);
        }

        OptionGroup licenseSaveOptGroup = new OptionGroup();
        licenseSaveOptGroup.addOption(saveUnsignedOnlyOpt);
        licenseSaveOptGroup.addOption(saveBothOpt);

        Options helpOptions = new Options();
        helpOptions.addOption(helpOpt);

        Options runOptions = new Options();
        runOptions.addOption(fileOpt);
        runOptions.addOption(eulaOpt);
        runOptions.addOptionGroup(licenseSaveOptGroup);

        CommandLineParser parser = new PosixParser();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(helpOptions, commandLineArguments, true);

            if(cmd.hasOption(OPTION_HELP_OPT)) {
                printHelp(fileOpt, eulaOpt, saveUnsignedOnlyOpt, saveBothOpt, helpOpt);
                System.exit(0);
            }

            cmd = parser.parse(runOptions, commandLineArguments);
        } catch (ParseException e) {
            printHelp(e.getMessage(), fileOpt, eulaOpt, saveUnsignedOnlyOpt, saveBothOpt, helpOpt);
            System.exit(1);
        }

        licenseDetailsFile = cmd.getOptionValue(OPTION_FILE_OPT);

        String eulaValue = cmd.getOptionValue(OPTION_EULA_OPT);
        eulaFilePath = eulaFilePaths.containsKey(eulaValue)
                ? eulaFilePaths.get(eulaValue)
                : eulaValue;

        if(cmd.hasOption(saveUnsignedOnlyOpt.getOpt())) {
            saveSignedLicenses = false;
            saveUnsignedLicenses = true;
        } else if(cmd.hasOption(saveBothOpt.getOpt())) {
            saveSignedLicenses = true;
            saveUnsignedLicenses = true;
        }
    }

    private static void printHelp(Option ... options) {
        final List<Option> orderedList = Arrays.asList(options);

        Options allOptions = new Options();

        for(Option option : orderedList) {
            allOptions.addOption(option);
        }

        Comparator<Option> comparator = new Comparator<Option>(){
            @Override
            public int compare(final Option o1, final Option o2){
                // sort options in order specified by parameters of createOptionsComparator()
                return Integer.valueOf(orderedList.indexOf(o1))
                        .compareTo(orderedList.indexOf(o2));
            }
        };

        HelpFormatter formatter = new HelpFormatter();
        formatter.setOptionComparator(comparator);
        formatter.printHelp("BatchLicenseGeneratorMain", allOptions);
    }

    private static void printHelp(String message, Option ... options) {
        System.out.println(message);
        printHelp(options);
    }

    private static File createLicenseArchive(Map<String,Document> licenses, String archiveFileName) throws IOException, LicenseGeneratorException {

        File archive = new File(archiveFileName);

        if(archive.exists()) {
            throw new LicenseGeneratorException("License archive file '" + archiveFileName + "' already exists! " +
                    "Only one record for each Licensee/Product combination can be present in the input file.");
        }

        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(archive));

        try {
            for (String name : licenses.keySet()) {
                Document license = licenses.get(name);
                String xmlString = XmlUtil.nodeToFormattedString(license);

                zos.putNextEntry(new ZipEntry(name));
                zos.write(xmlString.getBytes(), 0, xmlString.length());
                zos.closeEntry();
            }
        } finally {
            zos.close();
        }

        return new File(archiveFileName);
    }

    /**
     * Read entire EULA file contents into String and return.
     */
    private static String getEulaText(String eulaFilePath) throws IOException {
        File file = new File(eulaFilePath);

        return new Scanner(file).useDelimiter("\\Z").next();
    }

    /**
     * Load the EULA file definitions from the properties file into a map of properties/values.
     */
    private static Map<String, String> loadEULADefinitions() throws LicenseGeneratorException {
        Properties eulaProperties = new Properties();

        try {
            FileInputStream file = new FileInputStream(EULA_PROPERTIES_FILE_PATH);

            eulaProperties.load(file);

            file.close();
        } catch (FileNotFoundException e) {
            throw new LicenseGeneratorException("Could not find 'eula.properties' file!", e);
        } catch (IOException e) {
            throw new LicenseGeneratorException("Error reading EULA properties file!", e);
        }

        Map<String, String> map = new HashMap<String, String>();

        for(String property : eulaProperties.stringPropertyNames()) {
            map.put(property, eulaProperties.getProperty(property));
        }

        return Collections.unmodifiableMap(map);
    }

    private static Map<String, String> createUnmodifiableMapFromBundle(String bundleLocation) {
        ResourceBundle bundle = ResourceBundle.getBundle(bundleLocation);

        Map<String, String> map = new HashMap<String, String>();

        for(String code : bundle.keySet()) {
            map.put(code, bundle.getString(code));
        }

        return Collections.unmodifiableMap(map);
    }
}
