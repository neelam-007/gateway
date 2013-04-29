package com.l7tech.policy;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import sun.misc.BASE64Encoder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * This is here to ensure that the api sources remain serialization compatible.
 * See [SSG-6899] and information on the wiki here:
 * <p/>
 * This test validates that the hashes of each source file in the api module don't change.
 * Also will fail if new files are encountered or files are removed.
 */
public class ApiSourceHashTest {

    public static final String SOURCE_ROOT_DIR_PATH = "modules/gateway/api/src/main/java/com/l7tech/policy";
    public static final String FILE_HASH_STORE = "modules/gateway/server/src/test/java/com/l7tech/policy/apiSourceHashes.csv";
    private static final String INFO_URL = "https://wiki.l7tech.com/mediawiki/index.php/Layer_7_api_module";

    /**
     * Validates that none of the source files changed in the api module.
     * This will also report errors if files were removed or added the the api module.
     *
     * @throws IOException
     * @throws NoSuchAlgorithmException
     */
    @Test
    public void validateHashes() throws IOException, NoSuchAlgorithmException {
        File rootSourceDir = new File(SOURCE_ROOT_DIR_PATH);

        //loads the source file hashes
        Map<String, String> hashesMap = readHashStore(FILE_HASH_STORE);

        Set<File> files = listFilesInDirectory(rootSourceDir, ".java");
        BASE64Encoder encoder = new BASE64Encoder();
        for (File file : files) {
            String filePath = file.getPath().replaceAll("\\\\", "/");
            Assert.assertTrue("The file is not listed in the source hashes. Find more info here " + INFO_URL + " \nFile: " + filePath, hashesMap.containsKey(filePath));
            String source = readFileAsString(file);

            Assert.assertEquals("File hash does not match, the file has been changed. Find more info here " + INFO_URL + " \nFile: " + filePath, hashesMap.get(filePath), encoder.encode(toSHA1(source)));
            hashesMap.remove(filePath);
        }

        if (!hashesMap.isEmpty()) {
            Assert.fail("File removed from api module. Find more info here " + INFO_URL + " \nFile: " + hashesMap.keySet().iterator().next());
        }
    }

    /**
     * Use this to create the source hashes file.
     *
     * @throws IOException
     * @throws NoSuchAlgorithmException
     */
    @Test
    @Ignore
    public void hashSource() throws IOException, NoSuchAlgorithmException {
        File rootSourceDir = new File(SOURCE_ROOT_DIR_PATH);

        Set<File> files = listFilesInDirectory(rootSourceDir, ".java");
        BASE64Encoder encoder = new BASE64Encoder();
        for (File file : files) {
            String source = readFileAsString(file);
            System.out.println(file.getPath().replaceAll("\\\\", "/") + "," + encoder.encode(toSHA1(source)));
        }
    }

    private Map<String, String> readHashStore(String fileHashStore) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(new File(fileHashStore)));

        HashMap<String, String> fileHashes = new HashMap<>();
        String line;
        while ((line = reader.readLine()) != null) {
            String[] lineSplit = line.split(",", 2);
            Assert.assertEquals(lineSplit.length, 2);
            fileHashes.put(lineSplit[0], lineSplit[1]);
        }
        return fileHashes;
    }

    private byte[] toSHA1(String source) throws NoSuchAlgorithmException {
        MessageDigest crypt = MessageDigest.getInstance("SHA-1");
        crypt.reset();
        crypt.update(source.replaceAll("\\s+", " ").getBytes());
        return crypt.digest();
    }

    private String readFileAsString(File file) throws IOException {
        StringBuilder fileData = new StringBuilder();
        BufferedReader reader = new BufferedReader(
                new FileReader(file));
        char[] buf = new char[1024];
        int numRead;
        while ((numRead = reader.read(buf)) != -1) {
            String readData = String.valueOf(buf, 0, numRead);
            fileData.append(readData);
        }
        reader.close();
        return fileData.toString();
    }

    public Set<File> listFilesInDirectory(File directory, String extension) {
        extension = extension.toLowerCase();
        Set<File> files = new HashSet<>();

        File[] filesInDirectory = directory.listFiles();
        if (filesInDirectory != null) {
            for (File file : filesInDirectory) {
                if (file.isFile() && extension.equals(file.getName().substring(file.getName().length() - extension.length()).toLowerCase())) {
                    files.add(file);
                } else if (file.isDirectory()) {
                    files.addAll(listFilesInDirectory(file, extension));
                }
            }
        }
        return files;
    }

}