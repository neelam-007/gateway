package com.l7tech.internal.license.console;

import com.l7tech.common.io.csv.CSVPreference;
import com.l7tech.common.io.csv.CSVReader;
import com.l7tech.internal.license.LicenseGenerator;
import com.l7tech.internal.license.LicenseSpec;
import com.l7tech.server.GatewayFeatureSets;
import org.w3c.dom.Document;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.l7tech.internal.license.LicenseGenerator.*;

/**
 * @author jwilliams@layer7tech.com
 */
public class BatchLicenseGenerator {

    // license details record fields
    private static final int NUMBER_OF_FIELDS = 13;
    private static final int LICENSEE_NAME_FIELD = 0;       //required
    private static final int LICENSEE_EMAIL_FIELD = 1;
    private static final int HOST_FIELD = 2;
    private static final int IP_ADDRESS_FIELD = 3;
    private static final int PRODUCT_CODE_FIELD = 4;        //required
    private static final int MAJOR_VERSION_FIELD = 5;       //required
    private static final int MINOR_VERSION_FIELD = 6;
    private static final int DURATION_FIELD = 7;            //required
    private static final int DESCRIPTION_FIELD = 8;         //required
    private static final int ATTRIBUTES_FIELD = 9;
    private static final int FEATURE_LABEL_FIELD = 10;
    private static final int FEATURE_SET_CODES_FIELD = 11;  //required
    private static final int NUMBER_FIELD = 12;             //required

    // license name segments
    private static final char DASH_SEPARATOR = '_';
    private static final String XML_FILE_EXTENSION = ".xml";

    private static final Pattern DURATION_PATTERN = Pattern.compile("^(\\d+)(d|y)(\\s*\\z)", Pattern.CASE_INSENSITIVE);

    private static final Random RANDOM = new SecureRandom();

    private final Calendar generationTime;
    private final Date licenseStartDate;

    private final Map<String, String> PRODUCTS;
    private final Map<String, String> FEATURE_SETS;

    private X509Certificate signerCert;
    private PrivateKey signerKey;

    public BatchLicenseGenerator(Calendar generationTime, Map<String, String> products,
                                 Map<String, String> featureSets) {
        PRODUCTS = products;
        FEATURE_SETS = featureSets;

        this.generationTime = generationTime;

        Calendar yesterday = (Calendar) generationTime.clone();
        yesterday.add(Calendar.DATE, -1);

        licenseStartDate = yesterday.getTime();
    }

    public BatchLicenseGenerator(Calendar generationTime, Map<String, String> products,
                                 Map<String, String> featureSets, X509Certificate signerCert, PrivateKey signerKey) {
        this(generationTime, products, featureSets);
        this.signerCert = signerCert;
        this.signerKey = signerKey;
    }

    public List<LicenseDetailsRecord> readLicenseDetailsRecords(String fileName) throws LicenseGeneratorException, IOException {
        List<LicenseDetailsRecord> licenseDetailsRecords = new LinkedList<LicenseDetailsRecord>();

        FileReader fReader = new FileReader(fileName);
        try {
            BufferedReader bReader = new BufferedReader(fReader);
            try {
                final CSVPreference csvPreference = new CSVPreference('"', ',', "\n");
                final CSVReader csvReader = new CSVReader(bReader, csvPreference);

                int i = 0;
                List<String> line = csvReader.readRecord(true);

                while (null != line) {
                    i++;

                    LicenseDetailsRecord licenseDetailsRecord = parseRecord(line);
                    licenseDetailsRecords.add(licenseDetailsRecord);

                    line = csvReader.readRecord(true);
                }
            } finally {
                bReader.close();
            }
        } finally {
            fReader.close();
        }

        if(licenseDetailsRecords.size() == 0) {
            throw new LicenseGeneratorException("No records in specified file.");
        }

        return licenseDetailsRecords;
    }

    private LicenseDetailsRecord parseRecord(List<String> record) throws LicenseGeneratorException {
        LicenseDetailsRecord licenseDetailsRecord = new LicenseDetailsRecord();

        if (NUMBER_OF_FIELDS != record.size()) {
            throw new LicenseGeneratorException("Invalid number of fields in row: " + record.toString());
        }

        String licensee = record.get(LICENSEE_NAME_FIELD);

        if(licensee.isEmpty()) {
            throw new LicenseGeneratorException("Licensee Name is required: " + record.toString());
        }

        licenseDetailsRecord.setLicensee(licensee);
        licenseDetailsRecord.setEmail(record.get(LICENSEE_EMAIL_FIELD));
        licenseDetailsRecord.setHost(record.get(HOST_FIELD));
        licenseDetailsRecord.setIpAddress(record.get(IP_ADDRESS_FIELD));

        String productCode = record.get(PRODUCT_CODE_FIELD);

        if(productCode.isEmpty()) {
            throw new LicenseGeneratorException("Product is required: " + record.toString());
        }

        licenseDetailsRecord.setProductCode(productCode);
        licenseDetailsRecord.setProduct(getProductName(productCode));

        String majorVersion = record.get(MAJOR_VERSION_FIELD);

        if(majorVersion.isEmpty()) {
            throw new LicenseGeneratorException("Major Version is required: " + record.toString());
        }

        licenseDetailsRecord.setMajorVersion(majorVersion);
        licenseDetailsRecord.setMinorVersion(record.get(MINOR_VERSION_FIELD));

        String duration = record.get(DURATION_FIELD);

        if(duration.isEmpty()) {
            throw new LicenseGeneratorException("Duration is required: " + record.toString());
        }

        Calendar expiryDateCalendar = getExpirationDate(duration);

        if(null == expiryDateCalendar) {
            throw new LicenseGeneratorException("Invalid license duration format for licensee " +
                    licenseDetailsRecord.getLicensee() + ".");
        }

        licenseDetailsRecord.setExpiryDate(expiryDateCalendar.getTime());
        licenseDetailsRecord.setFeatureLabel(record.get(FEATURE_LABEL_FIELD));

        String description = record.get(DESCRIPTION_FIELD);

        if(description.isEmpty()) {
            throw new LicenseGeneratorException("Description is required: " + record.toString());
        }

        licenseDetailsRecord.setDescription(description);

        String featureSetCodes = record.get(FEATURE_SET_CODES_FIELD).trim();

        if(featureSetCodes.isEmpty()) {
            throw new LicenseGeneratorException("Feature Sets are required: " + record.toString());
        }

        ArrayList<String> featureSets = new ArrayList<String>();

        if(!featureSetCodes.isEmpty()) {
            for(String featureSetCode : featureSetCodes.split(",")) {
                featureSets.add(getFeatureSet(featureSetCode.trim()));
            }
        }

        licenseDetailsRecord.setFeatureSets(featureSets);
        licenseDetailsRecord.setAttributes(new HashSet<String>(Arrays.asList(record.get(ATTRIBUTES_FIELD).split(","))));

        int number = 0;

        try {
            number = Integer.parseInt(record.get(NUMBER_FIELD));
        } catch (NumberFormatException e) {
            throw new LicenseGeneratorException("Invalid number of licenses for licensee " +
                    licenseDetailsRecord.getLicensee() + ".");
        }

        if(number < 1) {
            throw new LicenseGeneratorException("Number of licenses must be at least 1: " + record.toString());
        }

        licenseDetailsRecord.setNumber(number);

        return licenseDetailsRecord;
    }

    public Map<String,Document> generateLicenses(LicenseDetailsRecord licenseDetails, String eula)
            throws LicenseGeneratorException {

        int licenseCount = licenseDetails.getNumber();

        Map<String,Document> licenses = new HashMap<String,Document>(licenseCount);

        LicenseSpec spec = new LicenseSpec();
        spec.setLicenseeName(licenseDetails.getLicensee());
        spec.setLicenseeContactEmail(licenseDetails.getEmail());
        spec.setHostname(licenseDetails.getHost());
        spec.setIp(licenseDetails.getIpAddress());
        spec.setProduct(licenseDetails.getProduct());
        spec.setVersionMajor(licenseDetails.getMajorVersion());
        spec.setVersionMinor(licenseDetails.getMinorVersion());
        spec.setStartDate(licenseStartDate);
        spec.setExpiryDate(licenseDetails.getExpiryDate());
        spec.setDescription(licenseDetails.getDescription());
        spec.setAttributes(licenseDetails.getAttributes());
        spec.setFeatureLabel(licenseDetails.getFeatureLabel());
        spec.setEulaText(eula);

        for(int j = 0; j < licenseCount; j++) {
            spec.setLicenseId(LicenseGenerator.generateRandomId(RANDOM));
            Document license = LicenseGenerator.generateUnsignedLicense(spec, true);
            licenses.put(generateLicenseName(spec, licenseDetails.getProductCode()), license);
        }

        return licenses;
    }

    public Map<String,Document> signLicenses(Map<String,Document> unsignedLicenses)
            throws LicenseGeneratorException, IOException, GeneralSecurityException {
        Map<String,Document> signedLicenses = new HashMap<String,Document>(unsignedLicenses.size());

        for(String licenseName : unsignedLicenses.keySet()) {
            Document unsignedLicense = unsignedLicenses.get(licenseName);
            Document signedLicense = LicenseGenerator.signLicenseDocument(unsignedLicense, signerCert, signerKey);
            signedLicenses.put(licenseName, signedLicense);
        }

        return signedLicenses;
    }

    private static String generateLicenseName(LicenseSpec spec, String productCode) {
        StringBuilder nameBuilder = new StringBuilder();

        nameBuilder.append(spec.getLicenseeName());
        nameBuilder.append(DASH_SEPARATOR);
        nameBuilder.append(spec.getLicenseId());
        nameBuilder.append(DASH_SEPARATOR);
        nameBuilder.append(productCode);

        if(!spec.getVersionMajor().isEmpty()) {
            nameBuilder.append(DASH_SEPARATOR);
            nameBuilder.append(spec.getVersionMajor());

            if(!spec.getVersionMinor().isEmpty()) {
                nameBuilder.append(DASH_SEPARATOR);
                nameBuilder.append(spec.getVersionMinor());
            }
        }

        nameBuilder.append(XML_FILE_EXTENSION);

        return nameBuilder.toString();
    }

    private String getProductName(String productCode) throws LicenseGeneratorException {
        String productName = PRODUCTS.get(productCode);

        if(null == productName) {
            throw new LicenseGeneratorException("Unrecognized product code \"" + productCode + "\".");
        }

        return productName;
    }

    private String getFeatureSet(String featureSetCode) throws LicenseGeneratorException {
        String featureSetName = FEATURE_SETS.get(featureSetCode);

        if(null == featureSetName || !GatewayFeatureSets.getRootFeatureSets().containsKey(featureSetName)) {
            throw new LicenseGeneratorException("Unrecognized feature set \"" + featureSetCode + "\".");
        }

        return featureSetName;
    }

    private Calendar getExpirationDate(String duration) {
        Calendar expiry = (Calendar) generationTime.clone();

        Matcher m = DURATION_PATTERN.matcher(duration);

        if (m.find()) {
            int value = Integer.parseInt(m.group(1));

            switch (m.group(2).charAt(0)) {
                case 'd': expiry.add(Calendar.DATE, value);
                case 'y': expiry.add(Calendar.YEAR, value);
            }

            return expiry;
        }

        return null;
    }

    /**
     * Hold the details of a license.
     */
    public class LicenseDetailsRecord {
        private int number;

        private String licensee;
        private String email;
        private String host;
        private String ipAddress;
        private String product;
        private String productCode;
        private String majorVersion;
        private String minorVersion;
        private String featureLabel;
        private String description;

        private Date expiryDate;
        private ArrayList<String> featureSets;
        private HashSet<String> attributes;

        public int getNumber() {
            return number;
        }

        public void setNumber(int number) {
            this.number = number;
        }

        public String getLicensee() {
            return licensee;
        }

        public void setLicensee(String licensee) {
            this.licensee = licensee;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public String getIpAddress() {
            return ipAddress;
        }

        public void setIpAddress(String ipAddress) {
            this.ipAddress = ipAddress;
        }

        public String getProduct() {
            return product;
        }

        public void setProduct(String product) {
            this.product = product;
        }

        public String getProductCode() {
            return productCode;
        }

        public void setProductCode(String productCode) {
            this.productCode = productCode;
        }

        public String getMajorVersion() {
            return majorVersion;
        }

        public void setMajorVersion(String majorVersion) {
            this.majorVersion = majorVersion;
        }

        public String getMinorVersion() {
            return minorVersion;
        }

        public void setMinorVersion(String minorVersion) {
            this.minorVersion = minorVersion;
        }

        public String getFeatureLabel() {
            return featureLabel;
        }

        public void setFeatureLabel(String featureLabel) {
            this.featureLabel = featureLabel;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Date getExpiryDate() {
            return expiryDate;
        }

        public void setExpiryDate(Date expiryDate) {
            this.expiryDate = expiryDate;
        }

        public ArrayList<String> getFeatureSets() {
            return featureSets;
        }

        public void setFeatureSets(ArrayList<String> featureSets) {
            this.featureSets = featureSets;
        }

        public HashSet<String> getAttributes() {
            return attributes;
        }

        public void setAttributes(HashSet<String> attributes) {
            this.attributes = attributes;
        }
    }
}
