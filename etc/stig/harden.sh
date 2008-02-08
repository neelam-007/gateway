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

  # GEN002720
  chkconfig --level 3 auditd on
  sed -i -e '
/-a exit,always -S unlink -S rmdir/ i\
-a exit,always -S open -F success=0' /etc/audit.rules
  service auditd start

  # GEN002860
  cat > /etc/cron.daily/auditd-rotate <<'EOF'
#!/bin/bash

kill -USR1 `cat /var/run/auditd.pid`
EOF
  chmod u+x /etc/cron.daily/auditd-rotate

  # GEN005540
  sed -i -e '/sshd: ALL/d' /etc/hosts.allow
}

soften() {
  # GEN000400, GEN000420
  mv -f /etc/issue.orig /etc/issue
  mv -f /etc/issue.net.orig /etc/issue.net
  sed -i -e 's/Banner \/etc\/issue.net/#Banner \/some\/path/' /etc/ssh/sshd_config
  service sshd restart

  # GEN000460
  sed -i -e 's/\(auth  *required  *.*\/pam_tally.so deny\)=[0-9]\( no_magic_root reset\)/\1=5\2/' /etc/pam.d/system-auth

  # GEN002720
  sed -i -e '/-a exit,always -S open -F success=0/d' /etc/audit.rules
  chkconfig --level 3 auditd off
  service auditd stop
  rm -f /etc/cron.daily/auditd-rotate
}

if [ "$1" = "-r" ]; then
  soften
else
  harden
fi
