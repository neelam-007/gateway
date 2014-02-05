package com.l7tech.policy;

import com.l7tech.util.Pair;
import com.l7tech.util.Triple;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.*;
import java.util.*;

public class ApiSerialVersionUidTest {

    public static final String API_ROOT_DIR_PATH = "modules/gateway/api/src/main/java/com/l7tech/policy";
    public static final String API_FILES_EXTENSION = ".java";
    public static final String API_ROOT_PACKAGE = "com.l7tech.policy";
    public static final String FILE_SERIAL_VERSION_UID_STORE = "modules/gateway/server/src/test/java/com/l7tech/policy/apiSerialVersionUids.csv";
    private static final String INFO_URL = "https://wiki.l7tech.com/mediawiki/index.php/Layer_7_api_module";
    private static final String NEW_LINE_APPENDIX = "\r\n";

    /**
     * Validates that none of the api module classes have changed their <code>serialVersionUID</code> value.<br/>
     * Test will also report errors if files were added to the api module.
     */
    @Test
    public void validateSerialVersionUid() throws Exception {
        System.out.println();
        System.out.println("!!!!DO NOT MODIFY (apiSerialVersionUids.csv), UNLESS INSTRUCTED BY THIS UNIT-TEST!!!!");
        System.out.println("Find more info here " + INFO_URL);
        System.out.println();

        String errorMassage = "";

        // key: class-name, value: serialVersionUID
        final List<Pair<String, String>> newClasses = new ArrayList<>();
        // left: class-name, middle: new serialVersionUID, right: original serialVersionUID
        final List<Triple<String, String, String>> diffClasses = new ArrayList<>();
        // left: serialVersionUID, middle: stored class-name, right: new class-name
        final List<Triple<String, String, String>> movedOrRenamedClasses = new ArrayList<>();

        //load cashed the source file serial-version-uid values
        final Map<String, String> storedUidMap = readStore(FILE_SERIAL_VERSION_UID_STORE);

        processSerialVersionUID(storedUidMap, newClasses, diffClasses, movedOrRenamedClasses);

        boolean shouldFail = false;

        // loop through newly added classes
        if (!newClasses.isEmpty()) {
            shouldFail = true;
            errorMassage += "----------------------------------------------------------------------------------------------" + NEW_LINE_APPENDIX;
            errorMassage += "There are classes which are not listed in the source api Serial-Version-UID." + NEW_LINE_APPENDIX;
            errorMassage += "Either manually APPEND the following entries in the store (apiSerialVersionUids.csv)," + NEW_LINE_APPENDIX;
            errorMassage += "Or execute serialVersionUidSource test, which will printout the whole content of (apiSerialVersionUids.csv):" + NEW_LINE_APPENDIX;
            errorMassage += "----------------------------------------------------------------------------------------------" + NEW_LINE_APPENDIX;
            for (final Pair<String, String> newEntry : newClasses) {
                errorMassage += newEntry.getKey() + ", " + newEntry.getValue() + NEW_LINE_APPENDIX;
            }
            errorMassage += "----------------------------------------------------------------------------------------------" + NEW_LINE_APPENDIX;
            errorMassage += NEW_LINE_APPENDIX;
        }

        // loop through moved or renamed classes
        if (!movedOrRenamedClasses.isEmpty()) {
            shouldFail = true;
            errorMassage += "----------------------------------------------------------------------------------------------" + NEW_LINE_APPENDIX;
            errorMassage += "There are classes which are assumed to be renamed, moved to a different package or both." + NEW_LINE_APPENDIX;
            errorMassage += "If the above assumption is WRONG, then please use unique serialVersionUID's, it will help this unit test detect changes correctly." + NEW_LINE_APPENDIX;
            errorMassage += "If the above assumption is indeed CORRECT then, DO NOT REMOVE OR MODIFY the values in the store (apiSerialVersionUids.csv)." + NEW_LINE_APPENDIX;
            errorMassage += "Instead UNDO your changes, since renaming or moving classes will BREAK backward compatibility:" + NEW_LINE_APPENDIX;
            errorMassage += "----------------------------------------------------------------------------------------------" + NEW_LINE_APPENDIX;
            for (Triple<String, String, String> movedOrRenamedClass : movedOrRenamedClasses) {
                errorMassage += "Serial-Version-UID \"" + movedOrRenamedClass.left + "\" found in different api class:" + NEW_LINE_APPENDIX;
                errorMassage += "Stored class name: " + movedOrRenamedClass.middle + NEW_LINE_APPENDIX;
                errorMassage += "New class name: " + movedOrRenamedClass.right + NEW_LINE_APPENDIX;
                errorMassage += "Please UNDO your changes, renaming or moving classes will BREAK backward compatibility." + NEW_LINE_APPENDIX;
                errorMassage += NEW_LINE_APPENDIX;
            }
            errorMassage += "----------------------------------------------------------------------------------------------" + NEW_LINE_APPENDIX;
            errorMassage += NEW_LINE_APPENDIX;
        }

        // loop through removed classes
        if (!storedUidMap.isEmpty()) {
            shouldFail = true;
            errorMassage += "----------------------------------------------------------------------------------------------" + NEW_LINE_APPENDIX;
            errorMassage += "There are classes which are either removed from the api module or no longer serializable." + NEW_LINE_APPENDIX;
            errorMassage += "DO NOT REMOVE the values in the store (apiSerialVersionUids.csv)." + NEW_LINE_APPENDIX;
            errorMassage += "Instead Please UNDO your changes, since this will BREAK backward compatibility:" + NEW_LINE_APPENDIX;
            errorMassage += "----------------------------------------------------------------------------------------------" + NEW_LINE_APPENDIX;
            for (final Map.Entry<String, String> deletedEntry : storedUidMap.entrySet()) {
                errorMassage += "Class name: \"" + deletedEntry.getKey() + "\", Serial-Version-UID \"" + deletedEntry.getValue() + "\"" + NEW_LINE_APPENDIX;
            }
            errorMassage += "----------------------------------------------------------------------------------------------" + NEW_LINE_APPENDIX;
            errorMassage += NEW_LINE_APPENDIX;
        }

        // Inform the developer of classes which serialVersionUID was changed since last time
        if (!diffClasses.isEmpty()) {
            shouldFail = true;
            errorMassage += "----------------------------------------------------------------------------------------------" + NEW_LINE_APPENDIX;
            errorMassage += "There are BROKEN classes that have been modified without preserving serialVersionUID." + NEW_LINE_APPENDIX;
            errorMassage += "This means that you need to modify the class source code i.e. make sure the class source code contain correct serialVersionUID." + NEW_LINE_APPENDIX;
            errorMassage += "The following lists each erroneous class individually:" + NEW_LINE_APPENDIX;
            errorMassage += "----------------------------------------------------------------------------------------------" + NEW_LINE_APPENDIX;
            for (final Triple<String, String, String> diffEntry : diffClasses) {
                shouldFail = true;
                errorMassage += "Class serialVersionUID does not match, class have been changed without preserving serialVersionUID." + NEW_LINE_APPENDIX;
                errorMassage += "Class-name: \"" + diffEntry.left + "\""  + NEW_LINE_APPENDIX;
                errorMassage += "Stored serialVersionUID: \"" + diffEntry.right + "\""  + NEW_LINE_APPENDIX;
                errorMassage += "New serialVersionUID: \"" + diffEntry.middle + "\""  + NEW_LINE_APPENDIX;
                errorMassage += "DO NOT MODIFY the value in the store (apiSerialVersionUids.csv)." + NEW_LINE_APPENDIX;
                errorMassage += "Instead make sure the class source code contain the following line:" + NEW_LINE_APPENDIX;
                errorMassage += "private static final long serialVersionUID = " + diffEntry.right + "L;" + NEW_LINE_APPENDIX;
                errorMassage += NEW_LINE_APPENDIX;
            }
            errorMassage += "----------------------------------------------------------------------------------------------" + NEW_LINE_APPENDIX;
        }

        Assert.assertFalse(NEW_LINE_APPENDIX + errorMassage + NEW_LINE_APPENDIX, shouldFail);
    }

    /**
     * Run this unit-test to create/update source serialVersionUID file.<br/>
     * The unit test will output the entire content that needs to be copy and pasted into apiSerialVersionUids.csv file.<br/>
     * In addition, if the test finds broken classes it will fail with assertion message containing the list of
     * broken classes along with instruction on how to fix them.
     */
    @Ignore
    @Test
    public void serialVersionUidSource() throws Exception {
        // key: class-name, value: serialVersionUID
        final List<Pair<String, String>> newClasses = new ArrayList<>();
        // left: class-name, middle: new serialVersionUID, right: original serialVersionUID
        final List<Triple<String, String, String>> diffClasses = new ArrayList<>();
        // left: serialVersionUID, middle: stored class-name, right: new class-name
        final List<Triple<String, String, String>> movedOrRenamedClasses = new ArrayList<>();

        //load cashed the source file serial-version-uid values
        final Map<String, String> originalStoredUidMap = readStore(FILE_SERIAL_VERSION_UID_STORE);
        final Map<String, String> storedUidMap = new HashMap<>(originalStoredUidMap); // shallow-copy

        processSerialVersionUID(storedUidMap, newClasses, diffClasses, movedOrRenamedClasses);

        // remove all keys mapped for deletion
        for (String keyToRemove : storedUidMap.keySet()) {
            originalStoredUidMap.remove(keyToRemove);
        }

        // add all new classes
        for (Pair<String, String> keyToAdd : newClasses) {
            originalStoredUidMap.put(keyToAdd.getKey(), keyToAdd.getValue());
        }

        // print the content of all classes, copy and paste this inside apiSerialVersionUids.csv file.
        for (Map.Entry<String, String> entry : originalStoredUidMap.entrySet()) {
            System.out.println(entry.getKey() + ", " + entry.getValue());
        }

        boolean shouldFail = false;
        String errorMassage = "";

        // loop through moved or renamed classes
        if (!movedOrRenamedClasses.isEmpty()) {
            shouldFail = true;
            errorMassage += "----------------------------------------------------------------------------------------------" + NEW_LINE_APPENDIX;
            errorMassage += "There are classes which are assumed to be renamed, moved to a different package or both." + NEW_LINE_APPENDIX;
            errorMassage += "If the above assumption is WRONG, then please use unique serialVersionUID's, it will help this unit test detect changes correctly." + NEW_LINE_APPENDIX;
            errorMassage += "If the above assumption is indeed CORRECT then, DO NOT REMOVE OR MODIFY the values in the store (apiSerialVersionUids.csv)." + NEW_LINE_APPENDIX;
            errorMassage += "Instead UNDO your changes, since renaming or moving classes will BREAK backward compatibility:" + NEW_LINE_APPENDIX;
            errorMassage += "----------------------------------------------------------------------------------------------" + NEW_LINE_APPENDIX;
            for (Triple<String, String, String> movedOrRenamedClass : movedOrRenamedClasses) {
                errorMassage += "Serial-Version-UID \"" + movedOrRenamedClass.left + "\" found in different api class:" + NEW_LINE_APPENDIX;
                errorMassage += "Stored class name: " + movedOrRenamedClass.middle + NEW_LINE_APPENDIX;
                errorMassage += "New class name: " + movedOrRenamedClass.right + NEW_LINE_APPENDIX;
                errorMassage += "Please UNDO your changes, renaming or moving classes will BREAK backward compatibility." + NEW_LINE_APPENDIX;
                errorMassage += NEW_LINE_APPENDIX;
            }
            errorMassage += "----------------------------------------------------------------------------------------------" + NEW_LINE_APPENDIX;
            errorMassage += NEW_LINE_APPENDIX;
        }

        // loop through removed classes
        if (!storedUidMap.isEmpty()) {
            shouldFail = true;
            errorMassage += "----------------------------------------------------------------------------------------------" + NEW_LINE_APPENDIX;
            errorMassage += "There are classes which are either removed from the api module or no longer serializable." + NEW_LINE_APPENDIX;
            errorMassage += "DO NOT REMOVE the values in the store (apiSerialVersionUids.csv)." + NEW_LINE_APPENDIX;
            errorMassage += "Instead Please UNDO your changes, since this will BREAK backward compatibility:" + NEW_LINE_APPENDIX;
            errorMassage += "----------------------------------------------------------------------------------------------" + NEW_LINE_APPENDIX;
            for (final Map.Entry<String, String> deletedEntry : storedUidMap.entrySet()) {
                errorMassage += "Class name: \"" + deletedEntry.getKey() + "\", Serial-Version-UID \"" + deletedEntry.getValue() + "\"" + NEW_LINE_APPENDIX;
            }
            errorMassage += "----------------------------------------------------------------------------------------------" + NEW_LINE_APPENDIX;
            errorMassage += NEW_LINE_APPENDIX;
        }

        // Inform the developer of classes which serialVersionUID was changed since last time
        if (!diffClasses.isEmpty()) {
            shouldFail = true;
            errorMassage += "----------------------------------------------------------------------------------------------" + NEW_LINE_APPENDIX;
            errorMassage += "There are BROKEN classes that have been modified without preserving serialVersionUID." + NEW_LINE_APPENDIX;
            errorMassage += "This means that you need to modify the class source code i.e. make sure the class source code contain correct serialVersionUID." + NEW_LINE_APPENDIX;
            errorMassage += "The following lists each erroneous class individually:" + NEW_LINE_APPENDIX;
            errorMassage += "----------------------------------------------------------------------------------------------" + NEW_LINE_APPENDIX;
            for (final Triple<String, String, String> diffEntry : diffClasses) {
                shouldFail = true;
                errorMassage += "Class serialVersionUID does not match, class have been changed without preserving serialVersionUID." + NEW_LINE_APPENDIX;
                errorMassage += "Class-name: \"" + diffEntry.left + "\""  + NEW_LINE_APPENDIX;
                errorMassage += "Stored serialVersionUID: \"" + diffEntry.right + "\""  + NEW_LINE_APPENDIX;
                errorMassage += "New serialVersionUID: \"" + diffEntry.middle + "\""  + NEW_LINE_APPENDIX;
                errorMassage += "DO NOT MODIFY the value in the store (apiSerialVersionUids.csv)." + NEW_LINE_APPENDIX;
                errorMassage += "Instead make sure the class source code contain the following line:" + NEW_LINE_APPENDIX;
                errorMassage += "private static final long serialVersionUID = " + diffEntry.right + "L;" + NEW_LINE_APPENDIX;
                errorMassage += NEW_LINE_APPENDIX;
            }
            errorMassage += "----------------------------------------------------------------------------------------------" + NEW_LINE_APPENDIX;
        }

        Assert.assertFalse(NEW_LINE_APPENDIX + errorMassage + NEW_LINE_APPENDIX, shouldFail);
    }

    /**
     * Goes through all serializable classes inside api module source dir ({@link #API_ROOT_DIR_PATH}), looking for
     *
     * @param storedUidMap             Input stored serialVersionUID's.
     * @param newClasses               Output a list of (class-name, serialVersionUID) pair containing newly found classes
     *                                 in api module source dir.
     * @param diffClasses              Output a list of (class-name, new serialVersionUID, original serialVersionUID) triple,
     *                                 containing classes having different serialVersionUID, compared with the reference value
     *                                 in <tt>storedUidMap</tt>
     * @param movedOrRenamedClasses    Output a list of (serialVersionUID, stored class-name, new class-name) triple,
     *                                 containing classes assuming being renamed, moved to a different package or both.
     */
    private void processSerialVersionUID (
            final Map<String, String> storedUidMap,
            final List<Pair<String, String>> newClasses,
            final List<Triple<String, String, String>> diffClasses,
            final List<Triple<String, String, String>> movedOrRenamedClasses
    ) throws Exception {
        // final List<Pair<String, String>> newClasses
        // key: class-name, value: serialVersionUID

        // final List<Triple<String, String, String>> diffClasses
        // left: class-name, middle: new serialVersionUID, right: original serialVersionUID

        // final List<Triple<String, String, String>> movedOrRenamedClasses
        // left: serialVersionUID, middle: stored class-name, right: new class-name

        Assert.assertNotNull("storedUidMap cannot ne null", storedUidMap);
        Assert.assertNotNull("newClasses cannot ne null", newClasses);
        Assert.assertNotNull("diffClasses cannot ne null", diffClasses);
        Assert.assertNotNull("movedOrRenamedClasses cannot ne null", movedOrRenamedClasses);

        final File rootSourceDir = new File(API_ROOT_DIR_PATH);
        Assert.assertTrue("api module source dir exists", rootSourceDir.exists());

        // load all api classes
        final Map<Class<?>, String> apiClasses = findApiClasses(rootSourceDir, API_FILES_EXTENSION, API_ROOT_PACKAGE);
        for (Map.Entry<Class<?>, String> newEntry : apiClasses.entrySet()) {
            final String clsName = newEntry.getKey().getName();
            if (!storedUidMap.containsKey(clsName)) {
                newClasses.add(new Pair<>(clsName, newEntry.getValue()));
            } else if (!storedUidMap.get(clsName).equals(newEntry.getValue())) {
                diffClasses.add(new Triple<>(clsName, newEntry.getValue(), storedUidMap.get(clsName)));
            }
            storedUidMap.remove(clsName);
        }

        // in case some of the api classes were renamed, moved to different packages or both
        // loop through the newly found entries and try to find their serialVersionUIDs into stored entries
        if (!storedUidMap.isEmpty() && !newClasses.isEmpty()) {
            for (final Iterator<Pair<String, String>> it = newClasses.iterator(); it.hasNext(); ) {
                final Pair<String, String> newEntry = it.next();
                for (final Map.Entry<String, String> storedEntry : storedUidMap.entrySet()) {
                    // first try to find same serialVersionUIDs
                    if (newEntry.getValue().equals(storedEntry.getValue())) {
                        // most likely the class was either renamed or moved to a different package, or both
                        movedOrRenamedClasses.add(new Triple<>(newEntry.getValue(), storedEntry.getKey(), newEntry.getKey()));
                        storedUidMap.remove(storedEntry.getKey());
                        it.remove();
                        break;
                    }
                }
            }
        }
    }

    /**
     * Load the serialVersionUID's (class-name, serialVersionUID) pair from the specified <tt>fileStore</tt>.
     *
     * @param fileStore    stored serialVersionUID's filename.
     * @return a map of (class-name, serialVersionUID) pairs.
     * @throws IOException if an IO error happen during file reading.
     */
    private static Map<String, String> readStore(final String fileStore) throws IOException {
        final File storeFile = new File(fileStore);
        Assert.assertTrue("stored serialVersionUID's file must exist", storeFile.exists());

        final BufferedReader reader = new BufferedReader(new FileReader(storeFile));
        final HashMap<String, String> fileHashes = new HashMap<>();

        String line;
        while ((line = reader.readLine()) != null) {
            final String[] lineSplit = line.split(",", 2);
            Assert.assertEquals("expected number of items per line", lineSplit.length, 2);
            fileHashes.put(lineSplit[0].trim(), lineSplit[1].trim());
        }
        return fileHashes;
    }

    /**
     * Retrieve all serializable classes from the specified <tt>directory</tt> belonging to the specified parent package-name.<br/>
     *
     * @param directory      the directory where to start searching for applicable classes.
     * @param extension      the extension to look for (typically .java)
     * @param packageName    the root package name classes belongs to.
     * @return a map containing a pair of the class object and <code>serialVersionUID</code>.  Never null.
     * @throws ClassNotFoundException when {@link Class#forName(String) forName} cannot extract the specified class string.
     */
    private static Map<Class<?>, String> findApiClasses(final File directory, final String extension, final String packageName) throws ClassNotFoundException {
        final Map<Class<?>, String> classes = new HashMap<>();
        if (!directory.exists()) {
            return classes;
        }

        final File[] files = directory.listFiles();
        assert files != null;
        for (final File file : files) {
            if (file.isDirectory()) {
                assert !file.getName().contains(".");
                classes.putAll(findApiClasses(file, extension, packageName + "." + file.getName()));
            } else if (file.isFile() && file.getName().toLowerCase().endsWith(extension.toLowerCase())) {
                final Class<?> aClass = Class.forName(packageName + '.' + file.getName().substring(0, file.getName().length() - extension.length()));
                if (aClass != null) {
                    Pair<Class<?>, String> ret = isCandidate(aClass);
                    if (ret != null) {
                        classes.put(ret.getKey(), ret.getValue());
                    }
                    // loop through all inner classes
                    for (Class<?> innerClass : aClass.getDeclaredClasses()) {
                        ret = isCandidate(innerClass);
                        if (ret != null) {
                            classes.put(ret.getKey(), ret.getValue());
                        }
                    }
                }
            }
        }

        return classes;
    }

    /**
     * Determine whether the specified class is serializable, if so extract the class <code>serialVersionUID</code>.
     *
     * @param aClass    specified class object.
     * @return null if the specified class is not serializable, otherwise return a pair of the class object with <code>serialVersionUID</code>.
     */
    private static Pair<Class<?>, String> isCandidate(final Class<?> aClass) {
        if (!aClass.isInterface() && !aClass.isEnum()) {
            final ObjectStreamClass sClass = ObjectStreamClass.lookup(aClass);
            if (sClass != null) {
                return new Pair<Class<?>, String>(aClass, String.valueOf(sClass.getSerialVersionUID()));
            }
        }

        return null;
    }

}
