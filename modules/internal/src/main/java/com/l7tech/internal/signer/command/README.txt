Much of the command line parsing functionality have been "borrowed" and slightly modified from the GMU project.
As GMU is in a different repository then UneasyRooster and the SkarSigner needs access to common layer7 libraries,
specifically the SignerUtils.java, for now the classes are going to be kept in this package.
Currently there are no other classes that use the command line parsing, so once that changes consider moving these
files into one of the common libs.


Command.java                        -- a base command containing a help option and hide progress option.
OptionBuilder.java                  -- a utility class for creating Option objects.
CommandException.java               -- an error thrown while executing a command.  Optionally holding a exit-code.
skar_signer.bat & skar_signer.sh    -- batch and shell scripts for running the SkarSigner.jar (optionally setting a JDK folder).
EncodePasswordCommand.java          -- a command which can encodes a given password from args or user input.
                                       Optionally move this command to common package is other apps need password encoder.
PasswordEncoder.java                -- from com.l7tech.common.password package, same as PasswordEncoder from GMU, but
                                       refactored to use HexUtils instead of sun base64 encoder/decoder.