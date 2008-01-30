#!/bin/bash

#we're in /SSG_ROOT/bin right now
cd `dirname $0`
pushd .. > /dev/null
SSG_ROOT=`pwd`
popd > /dev/null

. ${SSG_ROOT}/etc/profile

expected_java_version="1.6"
MY_JAVA_HOME=""
result=""
cryptoOk=0
javaclassname=CryptoStrengthProbe

#Uses an inline java class to check that the JDK at the path passed in as an argument meets the unlimited crypto strengh requirement.
#	The argument must be the path to a valid java installation (ex. /usr/j2se/jdk1.6.0_02),
#   this function will attempt to find java and java c in the bin/ sudirectory
#
#	SIDE EFFECT:
#		sets cryptoOk = 0 if the crypto is ok, 1 if it is not
check_crypto() {
    temp_java="${1}"
    currentdir=`pwd`
    cd /tmp
#START OF INLINE JAVA
    cat >CryptoStrengthProbe.java <<-EOF
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.security.MessageDigest;
import java.security.SecureRandom;

public class CryptoStrengthProbe {
    public static void main(String[] args) {
        try {
            ensureStrongCryptoEnabled();
        } catch (Exception e) {
            System.err.println("Cryptography subsystem not configured correctly: " + e.getMessage());
            System.exit(1);
        }
        System.exit(0);
    }

    /**
     * Test if strong crypto policy files are installed on the current JDK.
     * This test works by attempting to to use SHA-512 to generate an AES 256 key,
     * and then encrypting some sample data with AES 256.
     * @return true if strong crypto works; false if it appears to be disabled
     * @throws RuntimeException if an unexpected exception (one other than InvalidKeyException) was thrown
     */
    public static void ensureStrongCryptoEnabled() throws Exception {
        testCrypto();
    }

    private static void testCrypto() throws Exception {
        Cipher aes = Cipher.getInstance("AES/CBC/PKCS5Padding");
        int blocksize = aes.getBlockSize();

        byte[] mpBytes = "my very extremely long pass phrase".getBytes("UTF-8");

        byte[] saltBytes = new byte[blocksize];
        new SecureRandom().nextBytes(saltBytes);

        MessageDigest sha = MessageDigest.getInstance("SHA-512");
        sha.reset();
        sha.update(mpBytes);
        sha.update(saltBytes);
        byte[] keybytes = sha.digest();

        AesKey key = new AesKey(keybytes, 256);
        byte[] plaintextBytes = new String("a bunch of plaintext data".toCharArray()).getBytes("UTF-8");
        aes.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(saltBytes));
        aes.doFinal(plaintextBytes);
    }

    private static class AesKey implements SecretKey {
        private byte[] bytes;

        public AesKey(byte[] bytes, int keyBits) {
            if (keyBits != 128 && keyBits != 192 && keyBits != 256)
                throw new IllegalArgumentException("Unsupported AES key length " + keyBits + ".  Supported lengths are 128, 192 or 256");
            if (bytes.length * 8 < keyBits)
                throw new IllegalArgumentException("Byte array is too short for AES with " + keyBits + " bit key");
            this.bytes = new byte[keyBits / 8];
            System.arraycopy(bytes, 0, this.bytes, 0, this.bytes.length);
        }

        public String getAlgorithm() {
            return "AES";
        }

        public String getFormat() {
            return "RAW";
        }

        public byte[] getEncoded() {
            return bytes;
        }
    }
}

	EOF
#END OF INLINE JAVA CODE
	`${temp_java}/bin/javac ${javaclassname}.java`
    `${temp_java}/bin/java ${javaclassname}`
    cryptoOk=$?
    cd ${currentdir}
}

#	If the argument is a directory, this function will attempt to find java in the bin/ sudirectory
#	If the argument is not a directory, then it will be used as is to check the version
#
#	SIDE EFFECT:
#		sets result = the location where java was found (suitable for use in JAVA_HOME, i.e. minus the bin/java part)
# 		or empty if a compatible version of java was not found
check_java_version() {
	basepath=${1}
	result=""

	echo "Trying to find java at ${basepath} ..."

	if [ ! -d "${basepath}" ] ; then
	    echo ""
		echo "No Java found at ${basepath}"
		echo ""
		return
	else
		binpath="${basepath}/bin/java"
		if [ ! -x "${binpath}" ]; then
			echo ""
			echo "No Java found at ${basepath} (or ${binpath})"
			echo ""
			return
		fi
	fi

	javaver=`${binpath} -version 2>&1 | awk -F\" '/version/ {print $2}' | awk -F\. '{print $1"."$2}'`;
	echo "Found Java ${javaver} at ${binpath}"

	if [ "${javaver}" != ${expected_java_version} ]; then
		echo "Java ${expected_java_version} is required"
	else
		if [ "${basepath}" = "${binpath}" ] ; then
			temp=`dirname ${basepath}`
			result="`dirname ${temp}`"
		else
			result="${basepath}"
			echo ""
			echo "Validating the JDK ..."
			if [ ! -d ${result}/jre/lib/ext ] ; then
                echo ""
                echo "*** ${result} does not point to valid JDK. (no ${result}/jre/lib/ext was found) ***"
                echo "Please specify a location for a full JDK."
                echo "See the SecureSpan Installation and Maintenance Manual:"
	            echo "'Chapter 3 - Installing the SecureSpan Gateway' for details"
	            echo ""
	            result=""
			else
			    echo "The JDK is valid"
                echo ""
                echo "Now checking for appropriate cryptography configuration ..."
                check_crypto "${result}"
                if [ ${cryptoOk} != 0 ] ; then
                    echo "*** Cryptography configuration test failed. ***"
                    echo "Please ensure that the JDK has been updated with the unlimited strength crypto"
                    echo "policy files."
                    echo "See the SecureSpan Installation and Maintenance Manual:"
                    echo "'Chapter 3 - Installing the SecureSpan Gateway' for details"
                    result=""
                else
                    echo "Cryptography configuration test passed"
                fi
	            rm /tmp/${javaclassname}*
            fi
		fi
	fi
}

#Gets the location of a compatible (1.6) java installation.
#	If the expected value of ${expected_java_home} is found, and is actually the right version, that will be used
# 	If ${expected_java_home} is not found then the user is prompted to enter a path, which is checked with check_java_version
#	SIDE EFFECT:
#		Sets MY_JAVA_HOME to the appropriate location
get_java_location() {
	result=""
	while [ "${result}" = "" ]
	do
		echo -n "Please enter the path to a Java ${expected_java_version} installation:"
		read response
		check_java_version "${response}"
	done

	echo ""
	echo "A compatible JDK was found at ${result}"
	MY_JAVA_HOME=${result}
}

getYesNoResponse() {
    prompt=$1
    default=$2
    isValid=0

    while [ $isValid -eq 0 ]
    do
        echo -n ${prompt}
        read RESPONSE
        [ -z "$RESPONSE" ]  && RESPONSE="$default"

        isyes=`echo $RESPONSE | egrep -i "^\s*(y|yes)$"`
        if [ -n "$isyes" ] ; then
            RESPONSE="y"
            isValid=1
        else
            isno=`echo $RESPONSE | egrep -i "^\s*(n|no)$"`
            if [ -n "$isno" ] ; then
                isValid=1
            else
                echo "That is not a valid response. Please enter yes or no"
                isValid=0
            fi
        fi
    done
    eval "$3=$RESPONSE"
}

getValidMemoryValue() {
    prompt=$1

    isValid=0
    while [ $isValid -eq 0 ]
    do
        echo -n ${prompt}
        read RESPONSE
        onlyNumbers=`echo $RESPONSE | egrep -i "^[1-9][0-9]*$"`
        if [ -n "$onlyNumbers" ] ; then
            if [ $RESPONSE -lt 256 ] ; then
                echo "The minimum recommended amount for an SSG is 256 (megabytes)"
                isValid=0
            else
                isValid=1
            fi
        else
            echo "That is not a valid number."
            isValid=0
        fi
    done
    eval "$2=$RESPONSE"
}

store_java_location() {
    MY_JAVAHOME_COMMAND="sed -e s@SSG_JAVA_HOME=.*@SSG_JAVA_HOME=${MY_JAVA_HOME}@ ${SSG_ROOT}/etc/profile.d/java.sh > ${SSG_ROOT}/etc/profile.d/java.sh.temp && mv ${SSG_ROOT}/etc/profile.d/java.sh.temp ${SSG_ROOT}/etc/profile.d/java.sh"
}

configure_java_home() {
    echo "----------------------------------------------------"
    echo "           Configuration of Java Home               "
    echo "----------------------------------------------------"
    DOIT="n"
    if [ -z "${SSG_JAVA_HOME}" ] || [ ! -e ${SSG_JAVA_HOME} ] ; then
        echo "The SecureSpan Gateway requires Java ${expected_java_version} to be installed."
        DOIT="y"
    else
        echo "The gateway is currently configured to use \"${SSG_JAVA_HOME}\" for Java."
        getYesNoResponse 'Would you like to configure a new Java location? [y] ' 'y' DOIT
    fi

    if [ "$DOIT" == "y" ] ; then
        get_java_location
        store_java_location
    fi
}

configure_memory() {
    echo "----------------------------------------------------"
    echo "           Configuration of Java Options            "
    echo "----------------------------------------------------"
    getYesNoResponse 'Would you like to configure the Java options (such as memory usage) for the gateway? [y] ' 'y' DOIT

    MY_MEM_COMMAND=""
    if [ "${DOIT}" == "y" ] ; then
        HOWMUCHMEM=""
        while [ -z "${HOWMUCHMEM}" ] ; do
            echo "How much memory should be allocated to each gateway partition?"
            echo "Note: a minimum of 256 is required"
            getValidMemoryValue 'Please answer in megabytes: ' HOWMUCHMEM
        done
        FOUND=`grep "\-Xmx" ${SSG_ROOT}/etc/profile.d/jvmoptions`
	    if [ -n "${FOUND}" ] ; then
		    SEDCMD="sed -e 's/-Xmx.*/-Xmx${HOWMUCHMEM}m/g' ${SSG_ROOT}/etc/profile.d/jvmoptions > ${SSG_ROOT}/etc/profile.d/jvmoptions.memtemp && mv ${SSG_ROOT}/etc/profile.d/jvmoptions.memtemp ${SSG_ROOT}/etc/profile.d/jvmoptions"
	    else
		    SEDCMD="echo '-Xmx${HOWMUCHMEM}m' >> ${SSG_ROOT}/etc/profile.d/jvmoptions"
	    fi
        MY_MEM_COMMAND="${SEDCMD}"
    fi
}

save_all() {
    MYMSG=""
    MYCOMMAND=""
    if [ -n "${MY_JAVAHOME_COMMAND}" ] ; then
        MYCOMMAND="${MY_JAVAHOME_COMMAND}"
        MYMSG="    adding ${MY_JAVA_HOME} to ${SSG_ROOT}/etc/profile.d/java.sh\n"
    fi

    if [ -n "${MY_MEM_COMMAND}" ] ; then
        if [ -n "${MYCOMMAND}" ] ; then
            MYCOMMAND="${MYCOMMAND} && ${MY_MEM_COMMAND}"
        else
            MYCOMMAND="${MY_MEM_COMMAND}"
        fi
        MYMSG="${MYMSG}    adding -Xmx${HOWMUCHMEM}m to ${SSG_ROOT}/etc/profile.d/jvmoptions\n"
    fi

    if [ -n "${MYMSG}" ] && [ -n "${MYCOMMAND}" ]; then
        echo -e "This script is about to perform the following: \n${MYMSG}"
        getYesNoResponse 'Proceed?: [n] ' 'n' DOIT

        if [ ! "${DOIT}" == "n" ] ; then
            check_user
            do_command_as_user "ssgconfig" "${MYCOMMAND}"
            if [ "${?}" == "0" ]; then
                echo "The configuration was successful."
            else
                echo "The Configuration was unsuccessful. Please re-run this script"
            fi
        else
            echo "The configuration will not be changed."
        fi
    else
        echo "No configuration changes will be made to the system."
    fi
}

echo ""
configure_java_home
echo ""
if [ ! -e ${SSG_ROOT}/appliance ] ; then
    configure_memory
fi
echo ""
save_all
exit 0
