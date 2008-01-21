#!/bin/bash

harden() {
  # GEN005020
  userdel ftp

  # LNX00260
  #rpm -e yum
  #rm -f /etc/yum.conf

  # LNX00320
  userdel sync
  userdel shutdown
  userdel halt

  # LNX00580
  sed -i -e 's/^\(ca::ctrlaltdel:\)/#\1/' /etc/inittab

  # GEN000500
  echo 'export TMOUT=900' >> /etc/profile

  # GEN002860
  cat > /etc/cron.daily/auditd-rotate <<'EOF'
#!/bin/bash

kill -USR1 `cat /var/run/auditd.pid`
EOF
  chmod u+x /etc/cron.daily/auditd-rotate

  # GEN000020
  echo "~~:S:wait:/sbin/sulogin" >> /etc/inittab

  # GEN000440
  touch /var/log/btmp

  # GEN000460
  if ! grep -Eq 'auth +required +.*/pam_tally.so deny=[0-9] no_magic_root reset' /etc/pam.d/system-auth; then
    sed -i -e '
/auth\s*required\s*.*\/pam_env\.so/ i\
auth        required      /lib/security/$ISA/pam_tally.so deny=5 no_magic_root reset' /etc/pam.d/system-auth
  fi

  # GEN000480
  echo 'FAIL_DELAY 4' >> /etc/login.defs

  # GEN000540, GEN000700
  # Give root and ssgconfig 60 days from today before their passwords must be changed
  local DAYS
  DAYS=$((`date +'%s'`/60/60/24))
  sed -i -e "s/^\(root:[^:]*\):[^:]*:[^:]*:[^:]*:/\1:$DAYS:1:60:/' /etc/shadow
  sed -i -e 's/^\(ssgconfig:[^:]*\):[^:]*:[^:]*:[^:]*:/\1:$DAYS:1:60:/' /etc/shadow

  # GEN000580, GEN000600, GEN000620 GEN000640
  sed -i -e 's/PASS_MIN_LEN\t5/PASS_MIN_LEN\t9/' /etc/login.defs
  sed -i -e 's/\(password\s*requisite\s*.*\/pam_cracklib.so\s.*\)/\1 minlen=9 ucredit=2 lcredit=2 dcredit=2 ocredit=2/' /etc/pam.d/system-auth

  # GEN000800
  sed -i -e 's/\(password\s*sufficient\s*.*\/pam_unix.so\s.*\)/\1 remember=5/' /etc/pam.d/system-auth

  # GEN000820
  sed -i -e 's/PASS_MAX_DAYS\t99999/PASS_MAX_DAYS\t60/' -e 's/PASS_MIN_DAYS\t0/PASS_MIN_DAYS\t1/' /etc/login.defs

  # GEN000920
  chmod 700 /root

  # GEN000980
  cp /etc/securetty /etc/securetty.orig
  echo 'console' > /etc/securetty

  # GEN001260
  chmod 644 /var/log/wtmp*
  sed -i -e 's/create 0664 root utmp/create 0644 root utmp/' /etc/logrotate.conf

  # GEN001880
  if [ -e /home/ssgconfig/.bash_logout ]; then
    chmod 740 /home/ssgconfig/.bash_logout
  fi
  if [ -e /home/ssgconfig/.bash_profile ]; then
    chmod 740 /home/ssgconfig/.bash_profile
  fi
  if [ -e /home/ssgconfig/.bashrc ]; then
    chmod 740 /home/ssgconfig/.bashrc
  fi

  # GEN002480
  find / -type f -perm -002 -printf '%p %m\n' | grep -v '^/tmp/' | grep -v '^/var/tmp/' > /root/original_permissions
  find / -type d -perm -002 -printf '%p %m\n' | grep -v '^/tmp ' | grep -v '^/tmp/' | grep -v '^/var/tmp ' | grep -v '^/var/tmp/' | grep -v '^/dev/' >> /root/original_permissions
  if [ -s /root/original_permissions ]; then
    sed -e 's/\(.*\) [0-9]\{1,\}$/"\1"/' /root/original_permissions | xargs chmod o-w
  fi

  # GEN002720
  chkconfig --level 3 auditd on
  sed -i -e 's/max_log_file = 5/max_log_file = 125/' /etc/auditd.conf
  echo '-a exit,always -S open -F success=0' >> /etc/audit.rules

  # GEN002740
  echo '-a exit,always -S unlink -S rmdir' >> /etc/audit.rules

  # GEN002760
  echo '-w /var/log/audit/' >> /etc/audit.rules
  echo '-w /etc/auditd.conf' >> /etc/audit.rules
  echo '-w /etc/audit.rules' >> /etc/audit.rules
  echo '-a exit,always -F arch=b32 -S stime -S acct -S reboot -S swapon' >> /etc/audit.rules
  echo '-a exit,always -S settimeofday -S setrlimit -S setdomainname' >> /etc/audit.rules
  echo '# The mysqld program is expected to call sched_setscheduler' >> /etc/audit.rules
  echo '-a exit,always -S sched_setparam -S sched_setscheduler -F euid!=27' >> /etc/audit.rules
  service auditd start

  # GEN002960
  touch /etc/cron.allow
  chmod 600 /etc/cron.allow

  # GEN003080
  chmod 600 /etc/crontab
  chmod 700 /etc/cron.daily/*
  chmod 700 /etc/cron.monthly/*
  chmod 700 /etc/cron.weekly/*

  # GEN003320
  touch /etc/at.allow
  chmod 600 /etc/at.allow

  # GEN003540
  chmod 700 /var/crash

  # GEN003740
  chmod 440 /etc/xinetd.conf

  # GEN003865
  #rpm -e tcpdump

  # GEN004000
  chmod 4700 /bin/traceroute*

  # GEN005360
  chgrp sys /etc/snmp/snmpd.conf

  # GEN005400
  chmod 640 /etc/syslog.conf

  # GEN005540
  echo 'sshd1: ALL' >> /etc/hosts.deny
  echo 'sshd2: ALL' >> /etc/hosts.deny
  echo 'sshdfwd-x11: ALL' >> /etc/hosts.deny

  # LNX00340
  userdel news
  userdel games
  userdel gopher

  # LNX00440
  chmod 640 /etc/security/access.conf

  # LNX00520
  chmod 600 /etc/sysctl.conf
}

soften() {
  # GEN005020
  sed -i -e '
/^lock:/ i\
ftp:x:50:' /etc/group
  sed -i -e '
/^lock:/ i\
ftp:::' /etc/gshadow
  sed -i -e '
/^nobody:/ i\
ftp:x:14:50:FTP User:/var/ftp:/sbin/nologin' /etc/passwd
  sed -i -e '
/^nobody:/ i\
ftp:*:13637:0:99999:7:::' /etc/shadow

  # LNX00320
  sed -i -e '
/^mail:/ i\
sync:x:5:0:sync:/sbin:/bin/sync' /etc/passwd
  sed -i -e '
/^mail:/ i\
sync:*:13637:0:99999:7:::' /etc/shadow
  sed -i -e '
/^mail:/ i\
shutdown:x:6:0:shutdown:/sbin:/sbin/shutdown' /etc/passwd
  sed -i -e '
/^mail:/ i\
shutdown:*:13637:0:99999:7:::' /etc/shadow
sed -i -e '
/^mail:/ i\
halt:x:7:0:halt:/sbin:/sbin/halt' /etc/passwd
  sed -i -e '
/^mail:/ i\
halt:*:13637:0:99999:7:::' /etc/shadow

  # LNX00580
  sed -i -e 's/^#\(ca::ctrlaltdel:\)/\1/' /etc/inittab

  # GEN000500
  sed -i -e '/export TMOUT=900/d' /etc/profile

  # GEN002860
  rm -f /etc/cron.daily/auditd-rotate

  # GEN000020
  sed -i -e '/\~\~:S:wait:\/sbin\/sulogin.*/d' /etc/inittab

  # GEN000440
  rm -f /var/log/btmp

  # GEN000460
  sed -i -e '/auth        required      \/lib\/security\/$ISA\/pam_tally.so deny=5 no_magic_root reset/d' /etc/pam.d/system-auth

  # GEN000480
  sed -i -e '/FAIL_DELAY 4/d' /etc/login.defs

  # GEN000540, GEN000700
  sed -i -e 's/^\(root:[^:]*:[^:]*\):[^:]*:[^:]*:/\1:0:99999:/' /etc/shadow
  sed -i -e 's/^\(ssgconfig:[^:]*:[^:]*\):[^:]*:[^:]*:/\1:0:99999:/' /etc/shadow

  # GEN000580, GEN000600, GEN000620, GEN000640
  sed -i -e 's/PASS_MIN_LEN\t9/PASS_MIN_LEN\t5/' /etc/login.defs
  sed -i -e 's/\(password\s*requisite\s*.*\/pam_cracklib.so\s.*\) minlen=9 ucredit=2 lcredit=2 dcredit=2 ocredit=2/\1/' /etc/pam.d/system-auth

  # GEN000800
  sed -i -e 's/\(password\s*sufficient\s*.*\/pam_unix.so\s.*\) remember=5/\1/' /etc/pam.d/system-auth

  # GEN000820
  sed -i -e 's/PASS_MAX_DAYS\t60/PASS_MAX_DAYS\t99999/' -e 's/PASS_MIN_DAYS\t1/PASS_MIN_DAYS\t0/' /etc/login.defs

  # GEN000920
  chmod 750 /root

  # GEN000980
  if [ -e /etc/securetty.orig ]; then
    mv -f /etc/securetty.orig /etc/securetty
  fi

  # GEN001260
  chmod 664 /var/log/wtmp*
  sed -i -e 's/create 0644 root utmp/create 0664 root utmp/' /etc/logrotate.conf

  # GEN001880
  if [ -e /home/ssgconfig/.bash_logout ]; then
    chmod 744 /home/ssgconfig/.bash_logout
  fi
  if [ -e /home/ssgconfig/.bash_profile ]; then
    chmod 744 /home/ssgconfig/.bash_profile
  fi
  if [ -e /home/ssgconfig/.bashrc ]; then
    chmod 764 /home/ssgconfig/.bashrc
  fi

  # GEN002480
  if [ -s /root/original_permissions ]; then
    sed -e 's/\(.*\) [0-9]\{1,\}$/"\1"/' /root/original_permissions | xargs chmod -f o+w
  fi
  rm -f /root/original_permissions

  # GEN002720
  chkconfig --level 3 auditd off
  service auditd stop
  sed -i -e 's/max_log_file = 125/max_log_file = 5/' /etc/auditd.conf
  sed -i -e '/-a exit,always -S open -F success=0/d' /etc/audit.rules

  # GEN002740
  sed -i -e '/-a exit,always -S unlink -S rmdir/d' /etc/audit.rules

  # GEN002760
  sed -i -e '/-w \/var\/log\/audit\//d' \
         -e '/-w \/etc\/auditd.conf/d' \
         -e '/-w \/etc\/audit.rules/d' \
         -e '/-a exit,always -F arch=b32 -S stime -S acct -S reboot -S swapon/d' \
         -e '/-a exit,always -S settimeofday -S setrlimit -S setdomainname/d' \
         -e '/# The mysqld program is expected to call sched_setscheduler/d' \
         -e '/-a exit,always -S sched_setparam -S sched_setscheduler -F euid!=27/d' \
      /etc/audit.rules

  # GEN002960
  rm -f /etc/cron.allow

  # GEN003080
  chmod 644 /etc/crontab
  chmod 755 /etc/cron.daily/*
  chmod 755 /etc/cron.monthly/*
  chmod 755 /etc/cron.weekly/*

  # GEN003320
  rm -f /etc/at.allow

  # GEN003540
  chmod 755 /var/crash

  # GEN003740
  chmod 644 /etc/xinetd.conf

  # GEN004000
  chmod 4755 /bin/traceroute*

  # GEN005360
  chgrp root /etc/snmp/snmpd.conf

  # GEN005400
  chmod 644 /etc/syslog.conf

  # GEN005540
  sed -i -e '/sshd1: ALL/d' -e 'sshd2: ALL' -e '/sshdfwd-x11: ALL/d' /etc/hosts.deny

  # LNX00340
  sed -i -e 's/news:x:13:/news:x:13:news/' /etc/group
  sed -i -e 's/news:::/news:::news/' /etc/gshadow
  sed -i -e '
/^uucp:/ i\
news:x:9:13:news:/etc/news:' /etc/passwd
  sed -i -e '
/^uucp:/ i\
news:*:13637:0:99999:7:::' /etc/shadow
  sed -i -e '
/^ftp:/ i\
games:x:12:100:games:/usr/games:/sbin/nologin' /etc/passwd
  sed -i -e '
/^ftp:/ i\
games:*:13637:0:99999:7:::' /etc/shadow
  sed -i -e '
/^dip:/ i\
gopher:x:30:' /etc/group
  sed -i -e '
/^dip:/ i\
gopher:::' /etc/gshadow
  sed -i -e '
/^ftp:/ i\
gopher:x:13:30:gopher:/var/gopher:/sbin/nologin' /etc/passwd
  sed -i -e '
/^ftp:/ i\
gopher:*:13637:0:99999:7:::' /etc/shadow

  # LNX00440
  chmod 644 /etc/security/access.conf

  # LNX00520
  chmod 644 /etc/sysctl.conf
}

if [ "$1" = "-r" ]; then
  soften
else
  harden
fi

