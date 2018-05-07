#!/bin/bash
# root's public keys
# also ssgconfig pub keys

PUB_KEY_URI=http://169.254.169.254/1.0/meta-data/public-keys/0/openssh-key
PUB_KEY_FROM_HTTP=/tmp/openssh_id.pub
PUB_KEY_FROM_EPHEMERAL=/mnt/openssh_id.pub
ROOT_AUTHORIZED_KEYS=/root/.ssh/authorized_keys
SSGCONFIG_AUTHORIZED_KEYS=/home/ssgconfig/.ssh/authorized_keys



# We need somewhere to put the keys.
if [ ! -d /root/.ssh ] ; then
        mkdir -p /root/.ssh
        chmod 700 /root/.ssh
fi
# for ssgconfig too
if [ ! -d /home/ssgconfig/.ssh ] ; then
        mkdir -p /home/ssgconfig/.ssh
        chmod 700 /home/ssgconfig/.ssh
	chown ssgconfig /home/ssgconfig/.ssh
fi

# Fetch credentials...

# First try http
curl --silent --fail -o $PUB_KEY_FROM_HTTP $PUB_KEY_URI
if [ $? -eq 0 -a -e $PUB_KEY_FROM_HTTP ] ; then
    if ! grep -q -f $PUB_KEY_FROM_HTTP $ROOT_AUTHORIZED_KEYS
    then
            cat $PUB_KEY_FROM_HTTP >> $ROOT_AUTHORIZED_KEYS
            echo "New key added to authorized keys file from ephemeral store"|logger -t "ec2"
            chmod 600 $SSGCONIFG_AUTHORIZED_KEYS
            chown ssgconfig $SSGCONIFG_AUTHORIZED_KEYS

    fi

    if ! grep -q -f $PUB_KEY_FROM_HTTP $SSGCONFIG_AUTHORIZED_KEYS
    then
            cat $PUB_KEY_FROM_HTTP >> $SSGCONFIG_AUTHORIZED_KEYS
            echo "New key added to authorized keys file from ephemeral store"|logger -t "ec2"
    	    chmod 600 $ROOT_AUTHORIZED_KEYS

    fi

    rm -f $PUB_KEY_FROM_HTTP

elif [ -e $PUB_KEY_FROM_EPHEMERAL ] ; then
    # Try back to ephemeral store if http failed.
    # NOTE: This usage is deprecated and will be removed in the future
    if ! grep -q -f $PUB_KEY_FROM_EPHEMERAL $ROOT_AUTHORIZED_KEYS
    then
            cat $PUB_KEY_FROM_EPHEMERAL >> $ROOT_AUTHORIZED_KEYS
            echo "New key added to authorized keys file from ephemeral store"|logger -t "ec2"
	    chmod 600 $SSGCONIFG_AUTHORIZED_KEYS
	    chown ssgconfig $SSGCONIFG_AUTHORIZED_KEYS

    fi

    if ! grep -q -f $PUB_KEY_FROM_EPHEMERAL $SSGCONFIG_AUTHORIZED_KEYS
    then
            cat $PUB_KEY_FROM_EPHEMERAL >> $SSGCONFIG_AUTHORIZED_KEYS
            echo "New key added to authorized keys file from ephemeral store"|logger -t "ec2"
	    chmod 600 $ROOT_AUTHORIZED_KEYS

    fi
    chmod 600 $PUB_KEY_FROM_EPHEMERAL

fi

