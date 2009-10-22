package com.l7tech.server.processcontroller.patching.client;

import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.IOUtils;
import com.l7tech.server.processcontroller.patching.PatchServiceApi;
import com.l7tech.server.processcontroller.patching.PatchStatus;
import com.l7tech.server.processcontroller.patching.PatchException;
import com.l7tech.server.processcontroller.CxfUtils;

import java.net.URL;
import java.net.MalformedURLException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * @author jbufu
 */
public class PatchCli {

    // - PUBLIC

    public static final int SYNTAX_ERROR = 1;
    public static final int PATCH_API_ERROR = 2;

    public static void main(String[] args) {

        PatchAction patchAction = null;
        try {
            patchAction = PatchAction.fromArgs(args);
        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + ExceptionUtils.getMessage(e) + "\n");
            printUsage();
            System.exit(SYNTAX_ERROR);
        }

        PatchServiceApi api = new CxfUtils.ApiBuilder(patchAction.getEndpoint().toString()).build(PatchServiceApi.class);
        try {
            Collection<PatchStatus> result = patchAction.call(api);
            for(PatchStatus status : result) {
                System.out.println(status.toString());
            }
        } catch (Exception e) {
            System.out.println(ExceptionUtils.getMessage(e));
            System.exit(PATCH_API_ERROR);
        }
    }

    // - PRIVATE

    private static enum PatchAction {

        // - PUBLIC

        UPLOAD(" <filename>", "Uploads the patch represented by the <filename> to the gateway.") {
            @Override
            void extractActionArguments(String[] args) {
                this.argument = extractOneStringActionArgument(args);
            }
            @Override
            public Collection<PatchStatus> call(PatchServiceApi api) throws PatchException, IOException {
                final PatchStatus status = api.uploadPatch(IOUtils.slurpFile(new File(getArgument())));
                return new ArrayList<PatchStatus>() {{ add(status); }};
            }},
        INSTALL("<patch_id>", "Installs the (previously uploaded) patch represented by the provided ID on the gateway.") {
            @Override
            void extractActionArguments(String[] args) {
                this.argument = extractOneStringActionArgument(args);
            }
            @Override
            public Collection<PatchStatus> call(PatchServiceApi api) throws PatchException, IOException {
                final PatchStatus status = api.installPatch(getArgument(), null);
                return new ArrayList<PatchStatus>() {{ add(status); }};
            }},
        DELETE("<patch_id>", "Deletes the package archive of patch represented by the provided ID from the gateway patch manager's repository.") {
            @Override
            void extractActionArguments(String[] args) {
                this.argument = extractOneStringActionArgument(args);
            }
            @Override
            public Collection<PatchStatus> call(PatchServiceApi api) throws PatchException, IOException {
                final PatchStatus status = api.deletePackageArchive(getArgument());
                return new ArrayList<PatchStatus>() {{ add(status); }};
            }},
        STATUS("<patch_id>", "Returns the status of the patch represented by the provided ID on the gateway.") {
            @Override
            void extractActionArguments(String[] args) {
                this.argument = extractOneStringActionArgument(args);
            }
            @Override
            public Collection<PatchStatus> call(PatchServiceApi api) throws PatchException, IOException {
                final PatchStatus status = api.getStatus(getArgument());
                return new ArrayList<PatchStatus>() {{ add(status); }};
            }},
        LIST("", "Returns a list of the patches and their statuses on the gateway.") {
            @Override
            void extractActionArguments(String[] args) {
                /*no extra arguments for list*/
            }
            @Override
            public Collection<PatchStatus> call(PatchServiceApi api) throws PatchException, IOException {
                return api.listPatches();
            }};

        public String getSyntax() {
            return syntax;
        }

        public String getDescription() {
            return description;
        }

        public URL getEndpoint() {
            return endpoint;
        }

        public String getArgument() {
            return argument;
        }

        public static PatchAction fromArgs(String[] args) {
            if (args == null || args.length == 0)
                throw new IllegalArgumentException("No arguments specified.");

            URL endpoint;
            try {
                endpoint = new URL(args[0]);
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException("Invalid patch API endpoint URL specified: " + ExceptionUtils.getMessage(e));
            }

            if (args.length < 2)
                throw new IllegalArgumentException("No action specified.");

            PatchAction patchAction;
            try {
                patchAction = PatchAction.valueOf(args[1].toUpperCase());
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid action: " + ExceptionUtils.getMessage(e));
            }

            patchAction.endpoint = endpoint;
            patchAction.extractActionArguments(args);
            return patchAction;
        }

        public abstract Collection<PatchStatus> call(PatchServiceApi api) throws PatchException, IOException;

        // - PACKAGE

        abstract void extractActionArguments(String[] args);

        URL endpoint;
        String argument;

        // - PRIVATE

        private final String syntax;
        private final String description;

        private PatchAction(String syntax, String description) {
            this.syntax = syntax;
            this.description = description;
        }

        private static String extractOneStringActionArgument(String[] args) {
            // action argument is #3
            if (args == null || args.length < 3)
                throw new IllegalArgumentException("Argument for the patch action not specified.");

            return args[2];
        }
    }

    private static void printUsage() {
        System.out.println("Usage: " + PatchCli.class.getSimpleName() + " <patch API endpoint URL> <action>");
        System.out.println("\t<action>:");
        for (PatchAction o : PatchAction.values()) {
            System.out.println("\t" + o.name().toLowerCase() + " " + o.getSyntax() + "\n\t\t" + o.getDescription());
        }
    }
}
