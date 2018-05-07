#!/bin/bash

##########################################################################
# This script will install the text below into the /etc/issue and        #
# /etc/issue.net files.                                                  #
#                                                                        #
# The provided example text is specific to US Government clients         #
# Most commercial users with specific requirements around sign on        #
# banners will want to replace this text with their require text and     #
# then run this on all nodes in their cluster.                           #
# To change the text in the banner files, replace the text in between    #
# "cat ..." line and the "EOF" line.
##########################################################################

cat > /etc/issue <<'EOF'
You are accessing a U.S. Government (USG) information system (IS) that
is provided for USG-authorized use only.

By using this IS, you consent to the following conditions:

-The USG routinely monitors communications occurring on this IS, and any
device attached to this IS, for purposes including, but not limited to,
penetration testing, COMSEC monitoring, network defense, quality control,
and employee misconduct, law enforcement, and counterintelligence
investigations.
-At any time, the USG may inspect and/or seize data stored on this IS
and any device attached to this IS.
-Communications occurring on or data stored on this IS, or any device
attached to this IS, are not private. They are subject to routine monitoring
and search.
-Any communications occurring on or data stored on this IS, or any device
attached to this IS, may be disclosed or used for any USG-authorized purpose.
-Security protections may be utilized on this IS to protect certain interests
that are important to the USG. For example, passwords, access cards,
encryption or biometric access controls provide security for the benefit of
the USG. These protections are not provided for your benefit or privacy and
maybe modified or eliminated at the USG's discretion.
EOF

cp /etc/issue /etc/issue.net

# If SSH is not using a banner, then set it up to use the new banner
sed -i -e 's/#Banner \/some\/path/Banner \/etc\/issue.net/' /etc/ssh/sshd_config
service sshd restart
