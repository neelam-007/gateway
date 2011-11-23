package com.l7tech.external.assertions.ssh.keyprovider;

import com.l7tech.policy.variable.Syntax;
import com.l7tech.util.Option;
import static com.l7tech.util.Option.none;
import static com.l7tech.util.Option.optional;
import static com.l7tech.util.Option.some;
import static com.l7tech.util.TextUtils.matches;

import java.util.regex.Pattern;

/**
 * SSH key utility class.
 */
public class SshKeyUtil {

    private static final Pattern FINGERPRINT_PATTERN = Pattern.compile(
            "^[0-9a-f][0-9a-f]\\:[0-9a-f][0-9a-f]\\:[0-9a-f][0-9a-f]\\:" +
            "[0-9a-f][0-9a-f]\\:[0-9a-f][0-9a-f]\\:[0-9a-f][0-9a-f]\\:[0-9a-f][0-9a-f]\\:[0-9a-f][0-9a-f]\\:" +
            "[0-9a-f][0-9a-f]\\:[0-9a-f][0-9a-f]\\:[0-9a-f][0-9a-f]\\:[0-9a-f][0-9a-f]\\:[0-9a-f][0-9a-f]\\:" +
            "[0-9a-f][0-9a-f]\\:[0-9a-f][0-9a-f]\\:[0-9a-f][0-9a-f]$");

    /**
     * Validate the format of an SSH public key fingerprint
     *
     * @param fingerPrint SSH public key fingerprint format string
     * @return Returns an optional validation error message
     */
    public static Option<String> validateSshPublicKeyFingerprint( final String fingerPrint ) {
        final Option<String> message;

        // fingerprint must match or contain context variables
        if ( !optional(fingerPrint).exists(matches(FINGERPRINT_PATTERN)) &&
                Syntax.getReferencedNames(fingerPrint).length == 0 ) {
            message = some( "The SSH public key fingerprint entered is not valid." );
        } else {
            message = none();
        }

        return message;
    }

}
