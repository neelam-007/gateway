#!/bin/bash

harden() {
  # GEN000400, GEN000420
  mv -f /etc/issue /etc/issue.orig
  cp -f /ssg/etc/issue /etc/issue
  mv -f /etc/issue.net /etc/issue.net.orig
  cp -f /ssg/etc/issue /etc/issue.net
  sed -i -e 's/#Banner \/some\/path/Banner \/etc\/issue.net/' /etc/ssh/sshd_config
  service sshd restart

  # GEN000460
  sed -i -e 's/\(auth  *required  *.*\/pam_tally.so deny\)=[0-9]\( no_magic_root reset\)/\1=3\2/' /etc/pam.d/system-auth
}

soften() {
  # GEN000400, GEN000420
  mv -f /etc/issue.orig /etc/issue
  mv -f /etc/issue.net.orig /etc/issue.net
  sed -i -e 's/Banner \/etc\/issue.net/#Banner \/some\/path/' /etc/ssh/sshd_config
  service sshd restart

  # GEN000460
  sed -i -e 's/\(auth  *required  *.*\/pam_tally.so deny\)=[0-9]\( no_magic_root reset\)/\1=5\2/' /etc/pam.d/system-auth
}

if [ "$1" = "-r" ]; then
  soften
else
  harden
fi

