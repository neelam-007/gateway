package com.l7tech.gateway.config.backuprestore;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

/**
 * Buzzcut separates migrate and restore. A migrate in restore is simply a restore with 'migrate' capabilities
 * See FUNCSPEC //todo [Donal]
 * for details.
 * This class accecpts 'classic' migrate parameters and converts them into the equivilant parameters for a
 * selective restore, producing exactly the same behaviour as the old migrate parameters.
 * New clients can just use ssgrestore.sh directly, specifying the desired behaviour on the command line, old
 * clients will need to call ssgmigrate.sh instead of ssgrestore.sh, but nothing else needs to change
 */
public class MigrateToRestoreConvertor {

    /**
     * Convert the supplied args intended for the 'classic' migrate behaviour of ssgrestore.sh and convert them
     * into their equivilant for ssgrestore.sh
     * @param args cannot be null or empty
     * @return String [] args with the new args for Importer
     */
    public static String [] getConvertedArguments(String [] args)
            throws BackupRestoreLauncher.InvalidProgramArgumentException {

        final List<CommandLineOption> validArgList = new ArrayList<CommandLineOption>();
        validArgList.addAll(Arrays.asList(Importer.ALL_MIGRATE_OPTIONS));

        //validate that we only get allowed options, and that any options requiring a value recieve one
        final Map<String, String> initialValidArgs =
                ImportExportUtilities.getAndValidateCommandLineOptions(args,
                        validArgList, Arrays.asList(Importer.ALL_IGNORED_OPTIONS));

        //Now going to build up the translation between what was supplied to migrate and what
        //was is required to achieve the same results with a selective restore
        //we don't need to worry about actually valdating the arguments, this code, it's logic and tests
        //are all located in Importer, so we don't need to worry about that here

        List<String> restoreArgs = new ArrayList<String>();
        restoreArgs.add("migrate");//this index is ignored by importer
        //image
        restoreArgs.add(Importer.IMAGE_PATH.getName());
        restoreArgs.add(initialValidArgs.get(Importer.IMAGE_PATH.getName()));
        //migrate
        restoreArgs.add(Importer.MIGRATE.getName());

        //verbose and halt always, this mimics the default behaviour of migrate
        restoreArgs.add(ImportExportUtilities.VERBOSE.getName());
        restoreArgs.add(ImportExportUtilities.HALT_ON_FIRST_FAILURE.getName());

        //all other database options, currently migrate ALWAYS requires these
        //again, not validating here, invalid arguments will be caught in Importer
        restoreArgs.add(Importer.DB_ROOT_USER.getName());
        restoreArgs.add(initialValidArgs.get(Importer.DB_ROOT_USER.getName()));
        restoreArgs.add(Importer.DB_ROOT_PASSWD.getName());
        restoreArgs.add(initialValidArgs.get(Importer.DB_ROOT_PASSWD.getName()));
        restoreArgs.add(Importer.DB_HOST_NAME.getName());
        restoreArgs.add(initialValidArgs.get(Importer.DB_HOST_NAME.getName()));
        restoreArgs.add(Importer.GATEWAY_DB_USERNAME.getName());
        restoreArgs.add(initialValidArgs.get(Importer.GATEWAY_DB_USERNAME.getName()));
        restoreArgs.add(Importer.GATEWAY_DB_PASSWORD.getName());
        restoreArgs.add(initialValidArgs.get(Importer.GATEWAY_DB_PASSWORD.getName()));
        restoreArgs.add(Importer.CLUSTER_PASSPHRASE.getName());
        restoreArgs.add(initialValidArgs.get(Importer.CLUSTER_PASSPHRASE.getName()));

        //if config - don't add db and audits
        final boolean configOnly = initialValidArgs.containsKey(Importer.CONFIG_ONLY.getName());

        //main db if not config
        if(!configOnly){
            restoreArgs.add(ImportExportUtilities.MAINDB_OPTION.getName());
            restoreArgs.add(ImportExportUtilities.AUDITS_OPTION.getName());
        }else{
            restoreArgs.add(Importer.CONFIG_ONLY.getName());
        }

        //os?
        if(initialValidArgs.containsKey(ImportExportUtilities.OS_OPTION.getName())){
            restoreArgs.add(ImportExportUtilities.OS_OPTION.getName());
        }

        //mapping file?
        if(initialValidArgs.containsKey(Importer.MAPPING_PATH.getName())){
            restoreArgs.add(Importer.MAPPING_PATH.getName());
        }

        //new db or dbname
        final boolean newDb = initialValidArgs.containsKey(Importer.CREATE_NEW_DB.getName());
        if(newDb){
            restoreArgs.add(Importer.CREATE_NEW_DB.getName());
            restoreArgs.add(initialValidArgs.get(Importer.CREATE_NEW_DB.getName()));
        }else{
            restoreArgs.add(Importer.DB_NAME.getName());
            restoreArgs.add(initialValidArgs.get(Importer.DB_NAME.getName()));
        }
        
        return restoreArgs.toArray(new String[restoreArgs.size()]);
    }
}
