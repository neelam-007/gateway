#!/bin/bash

#HAZ_ISVM="`dmesg | grep -i vmware`"
#HAZ_ISHARDWARE="`uname -a | grep x86_64`"
#if [ ! -z "$HAZ_ISHARDWARE" ] ; then
#	ISHARDWARE=1
#	ISVM=
#	if [ ! -z "$HAZ_ISVM" ] ; then
#		ISHARDWARE=
#		ISVM=1
#	fi
#fi

# set this manually as there's no way to tell by script the difference between normal hardware and NCES
ISNCES=""

# Notes: a correct hardened pam.d/system-auth file looks like:
## #%PAM-1.0
## # This file is auto-generated.
## # User changes will be destroyed the next time authconfig is run.
## auth        required      pam_tally2.so deny=5 even_deny_root_account onerr=fail unlock_time=1200 root_unlock_time=1200
## auth        required      pam_env.so
## auth        sufficient    pam_unix.so likeauth nullok
## auth        required      pam_deny.so
## 
## account     required      pam_unix.so
## account     required      pam_tally2.so
## account     sufficient    pam_succeed_if.so uid < 100 quiet
## account     required      pam_permit.so
## 
## password    requisite     pam_cracklib.so retry=3 minlen=9 ucredit=-2 lcredit=-2 dcredit=-2 ocredit=-2
## password    sufficient    pam_unix.so nullok use_authtok md5 shadow remember=5
## password    required      pam_deny.so
## 
## session     required      pam_limits.so
## session     required      pam_unix.so


harden() {
  if [ -z "$1" ] ; then
      ISVM=0
      ISHARDWARE=1
  else
     case "$1" in
        "vmware"  ) ISVM=1;
            ISHARDWARE=0;;
        "*"  ) ISVM=0;
            ISHARDWARE=1;;
     esac
  fi

  # GEN005020
  userdel ftp

  # LNX00260
  rpm -e yum
  rm -f /etc/yum.conf
  rm -rf /etc/yum.repos.d

  # LNX00320
  userdel sync
  userdel shutdown
  userdel halt

  # LNX00580
  sed -i -e 's/^\(ca::ctrlaltdel:\)/#\1/' /etc/inittab

  # GEN000500
  sed -i -e '/^TMOUT=900/d' /etc/profile
  echo 'export TMOUT=900' >> /etc/profile

  # GEN000020
  sed -i -e '/s0.*ttyS0/d' /etc/inittab
  sed -i -e '/s1.*ttyS1/d' /etc/inittab
  sed -i -e '/~~:S:wait:/sbin/sulogin/d' /etc/inittab
  echo "s0:2345:respawn:/sbin/agetty -L 9600 ttyS0 vt100" >> /etc/inittab
  echo "s1:2345:respawn:/sbin/agetty -L 9600 ttyS1 vt100" >> /etc/inittab
  echo "~~:S:wait:/sbin/sulogin" >> /etc/inittab

  # GEN000440
  touch /var/log/btmp

  # GEN000460
  sed -i -e '/pam_tally\.so/d' /etc/pam.d/system-auth

  if ! grep -Eq 'auth +required +.*pam_tally2.so deny=[0-9].*unlock_time=1200 root_unlock_time=1200' /etc/pam.d/system-auth; then

	 # delete any auth required pam_tally2 lines to be sure
	 sed -i -e '/auth\s*required\s*.*pam_tally2.so/d' /etc/pam.d/system-auth

	 # add in the new line before the pam_env line, use \2 as the substitution to ensure we have the same lib or lib64 line
	 sed -i -r -e 's/^(.*(auth\s*required\s*.*)pam_env\.so.*)$/\2pam_tally2\.so deny=5 even_deny_root_account onerr=fail unlock_time=1200 root_unlock_time=1200\n\1/' /etc/pam.d/system-auth

	 # delete any account sufficient pam_tally2 lines to be sure
	 sed -i -e '/account\s*required\s*.*pam_tally2.so/d' /etc/pam.d/system-auth

	 sed -i -r -e 's/^(.*(account\s*)sufficient(\s*.*)pam_succeed_if\.so.*)$/\2required  \3pam_tally2.so\n\1/' /etc/pam.d/system-auth

  fi
  # Use a listfile with SSH to prevent DOS attacks on system accounts
  if [ ! -e /etc/ssh/ssh_allowed_users ]; then
    echo 'ssgconfig' > /etc/ssh/ssh_allowed_users
    sed -i -e '
/#%PAM-1.0/ a\
auth        requisite     pam_listfile.so item=user sense=allow file=/etc/ssh/ssh_allowed_users onerr=succeed' /etc/pam.d/sshd
  fi
  # Use a listfile with console login
  if [ ! -e /etc/tty_users ]; then
    echo 'root' > /etc/tty_users
    echo 'ssgconfig' >> /etc/tty_users
    sed -i -e '
/auth       required\tpam_securetty.so/ a\
auth       requisite    pam_listfile.so item=user sense=allow file=/etc/tty_users onerr=succeed' /etc/pam.d/login
  fi

  # SSH set to protocol 2 only (Bug #6371)
  sed -i -e '/Protocol/d' /etc/ssh/sshd_config
  echo "# Only allow Protocol 2 as per bug #6371" >> /etc/ssh/sshd_config
  echo "Protocol 2" >> /etc/ssh/sshd_config


  # GEN000480
  echo 'FAIL_DELAY 4' >> /etc/login.defs

  # GEN000540, GEN000700
  # Give root and ssgconfig 60 days from today before their passwords must be changed, this is only for
  # image preparation, as sealsys will still expire the password
  local DAYS
  DAYS=$((`date +'%s'`/60/60/24))
  chage -m 1 -M 60 -d $DAYS root
  chage -m 1 -M 60 ssgconfig

  # GEN000580, GEN000600, GEN000620 GEN000640
  sed -i -e 's/PASS_MIN_LEN\t5/PASS_MIN_LEN\t9/' /etc/login.defs
  sed -i -e 's/\(password\s*requisite\s*.*pam_cracklib.so\)\(\s.*\)/\1 retry=3 minlen=9 ucredit=-2 lcredit=-2 dcredit=-2 ocredit=-2/' /etc/pam.d/system-auth

  # GEN000800
  # remove the pam_unix line with potentially the remember=5 already at the end
  sed -i -e '/password\s*sufficient\s*.*pam_unix.so\s*.*/d' /etc/pam.d/system-auth
  # and now add it back in properly with all arguments
  sed -i -r -e 's/^(password\s*required\s*(.*)pam_deny.so\s*)$/password    sufficient    pam_unix.so nullok use_authtok md5 shadow remember=5\n\1/' /etc/pam.d/system-auth

  # GEN000820
  sed -i -e 's/PASS_MAX_DAYS\t99999/PASS_MAX_DAYS\t60/' -e 's/PASS_MIN_DAYS\t0/PASS_MIN_DAYS\t1/' /etc/login.defs

  # GEN000920
  chmod 700 /root

  # GEN000980
  echo 'console' > /etc/securetty
  echo 'tty1'   >> /etc/securetty
  echo 'ttyS0'  >> /etc/securetty
  echo 'ttyS1'  >> /etc/securetty

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
  find / -xdev -type f -perm -002 -printf '%p %m\n' 2>&1 | grep -v '^/tmp/' | grep -v '^/proc/' | grep -v '^/var/tmp/' > /root/original_permissions
  find / -xdev -type d -perm -002 -printf '%p %m\n' 2>&1 | grep -v '^/tmp ' | grep -v '^/proc/' | grep -v '^/tmp/' | grep -v '^/var/tmp ' | grep -v '^/var/tmp/' | grep -v '^/dev/' >> /root/original_permissions
  if [ -s /root/original_permissions ]; then
    sed -e 's/\(.*\) [0-9]\{1,\}$/"\1"/' /root/original_permissions | xargs chmod o-w
  fi
  rm -f /root/original_permissions

  # GEN002740
  # VM has max_log_file set to 5, hardware is 125
  if [ "$ISVM" ] ; then
	  sed -i -e 's/\(max_log_file = .*\)/max_log_file = 5/' /etc/audit/auditd.conf
  fi
  if [ "$ISHARDWARE" ] ; then
	  sed -i -e 's/\(max_log_file = .*\)/max_log_file = 125/' /etc/audit/auditd.conf
  fi
  if [ "$ISNCES" ] ; then
	  sed -i -e 's/\(max_log_file = .*\)/max_log_file = 125/' /etc/audit/auditd.conf
  fi

  # Remove audit.rules added contents first to ensure not duplication
  sed -i -e '/^\-a exit,always \-S open \-F success=0$/d' /etc/audit/audit.rules
  sed -i -e '/^\-a exit,always \-S unlink \-S rmdir/, /sched_setscheduler \-F euid\!=27$/d' /etc/audit/audit.rules

  # GEN002760
  if [ "$ISNCES" ] ; then 
	  echo '-a exit,always -S open -F success=0' >> /etc/audit/audit.rules
  fi
  echo '-a exit,always -S unlink -S rmdir' >> /etc/audit/audit.rules
  echo '-w /var/log/audit/' >> /etc/audit/audit.rules
  echo '-w /etc/auditd.conf' >> /etc/audit/audit.rules
  echo '-w /etc/audit.rules' >> /etc/audit/audit.rules
  echo '-a exit,always -F arch=b32 -S stime -S acct -S reboot -S swapon' >> /etc/audit/audit.rules
  echo '-a exit,always -S settimeofday -S setrlimit -S setdomainname' >> /etc/audit/audit.rules
  echo '# The mysqld program is expected to call sched_setscheduler' >> /etc/audit/audit.rules
  echo '-a exit,always -S sched_setparam -S sched_setscheduler -F euid!=27' >> /etc/audit/audit.rules

  # GEN002960
  touch /etc/cron.allow
  chmod 600 /etc/cron.allow
  sed -i -e '/^ssgconfig/d' /etc/cron.deny
  echo 'ssgconfig' >> /etc/cron.deny

  # GEN003080
  chmod 600 /etc/crontab
  chmod 700 /etc/cron.daily/*
  chmod 700 /etc/cron.monthly/*
  chmod 700 /etc/cron.weekly/*

  # GEN003320
  touch /etc/at.allow
  chmod 600 /etc/at.allow
  sed -i -e '/^ssgconfig/d' /etc/at.deny
  echo 'ssgconfig' >> /etc/at.deny

  # GEN003540
  chmod 700 /var/crash

  # GEN003740
  chmod 440 /etc/xinetd.conf

  # GEN003865
  #rpm -e tcpdump

  # GEN004000
  chmod 4700 /bin/traceroute*

  # GEN004540
  mv -f /etc/mail/helpfile /etc/mail/helpfile.old

  # GEN005360
  chgrp sys /etc/snmp/snmpd.conf

  # GEN005400
  chmod 640 /etc/syslog.conf

  # GEN005540, GEN006620
  sed -i -e '/ALL:\s*ALL/d' /etc/hosts.deny
  echo 'ALL: ALL' >> /etc/hosts.deny
  sed -i -e '/sshd:\s*ALL/d' /etc/hosts.allow
  echo 'sshd: ALL' >> /etc/hosts.allow

  # LNX00340
  userdel news
  userdel games
  userdel gopher

  # LNX00440
  chmod 640 /etc/security/access.conf

  # LNX00520
  chmod 600 /etc/sysctl.conf

  # GEN001280
  find /usr/share/man -type f -a -perm +133 | xargs chmod 644

  # GEN004560
  sed -i -e 's/O SmtpGreetingMessage=\$j Sendmail \$v\/\$Z; \$b/O SmtpGreetingMessage= Mail Server Ready ; $b/' /etc/mail/sendmail.cf

	# change root and ssgconfig password to 'password'
#	usermod -p $1$c.vxuRpE$uNvTtWmyjg/UzmVXGOJaT. root
#	usermod -p $1$c.vxuRpE$uNvTtWmyjg/UzmVXGOJaT. ssgconfig

	# remove hwaddr in eth configs
	for i in 0 1 2 3 ; do 
		if [ -e "/etc/sysconfig/network-scripts/ifcfg-eth${i}" ] ; then
			sed -i -e '/HWADDR/d' /etc/sysconfig/network-scripts/ifcfg-eth${i}
		fi
	done
}

soften() {
  # GEN005020
  if [ ! "`grep 'ftp:x:50:' /etc/group`" ] ; then
	  sed -i -e '
/^lock:/ i\
ftp:x:50:' /etc/group
  fi
  if [ ! "`grep 'ftp:::' /etc/gshadow`" ] ; then
	  sed -i -e '
/^lock:/ i\
ftp:::' /etc/gshadow
  fi
  if [ ! "`grep 'ftp:x:14:50:FTP' /etc/passwd`" ] ; then
	  sed -i -e '
/^nobody:/ i\
ftp:x:14:50:FTP User:/var/ftp:/sbin/nologin' /etc/passwd
  fi
  if [ ! "`grep 'ftp:\*:13637:0:9999:7:::' /etc/shadow`" ] ; then
	  sed -i -e '
/^nobody:/ i\
ftp:*:13637:0:99999:7:::' /etc/shadow
  fi

  # LNX00320
  if [ ! "`grep 'sync:x:5:0:sync:/sbin:/bin/sync' /etc/passwd`" ] ; then 
	  sed -i -e '
/^mail:/ i\
sync:x:5:0:sync:/sbin:/bin/sync' /etc/passwd
  fi
  if [ ! "`grep 'sync:\*:13637:0:99999:7:::' /etc/shadow`" ] ; then 
	  sed -i -e '
/^mail:/ i\
sync:*:13637:0:99999:7:::' /etc/shadow
  fi
  if [ ! "`grep 'shutdown:x:6:0:shutdown:/sbin:/sbin/shutdown' /etc/passwd`" ] ; then
	  sed -i -e '
/^mail:/ i\
shutdown:x:6:0:shutdown:/sbin:/sbin/shutdown' /etc/passwd
  fi
  if [ ! "`grep 'shutdown:\*:13637:0:99999:7:::' /etc/shadow`" ] ; then
	  sed -i -e '
/^mail:/ i\
shutdown:*:13637:0:99999:7:::' /etc/shadow
  fi
  if [ ! "`grep 'halt:x:7:0:halt:/sbin:/sbin/halt' /etc/passwd`" ] ; then
		sed -i -e '
/^mail:/ i\
halt:x:7:0:halt:/sbin:/sbin/halt' /etc/passwd
  fi
  if [ ! "`grep 'halt:\*:13637:0:99999:7:::' /etc/shadow`" ] ; then
	  sed -i -e '
/^mail:/ i\
halt:*:13637:0:99999:7:::' /etc/shadow
  fi
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
  sed -i -e '/auth\s*required\s*pam_tally2.so deny=5.* onerr=fail no_magic_root unlock_time=1200 root_unlock_time=1200/d' /etc/pam.d/system-auth
  sed -i -e '/account\s*required\s*pam_tally2.so no_magic_root.* reset/d' /etc/pam.d/system-auth
  rm -f /etc/ssh/ssh_allowed_users
  sed -i -e '/auth       requisite    pam_listfile.so item=user sense=allow file=\/etc\/ssh\/ssh_allowed_users onerr=succeed/d' /etc/pam.d/sshd
  rm -f /etc/tty_users
  sed -i -e '/auth       requisite    pam_listfile.so item=user sense=allow file=\/etc\/tty_users onerr=succeed/d' /etc/pam.d/login

  # SSH set either protocol (Reverse Bug #6371)
  sed -i -e '/Protocol/d' /etc/ssh/sshd_config
  echo "# Protocol 2,1" >> /etc/ssh/sshd_config

  # GEN000480
  sed -i -e '/FAIL_DELAY 4/d' /etc/login.defs

  # GEN000540, GEN000700
  chage -m 0 -M 99999 root
  chage -m 0 -M 99999 ssgconfig

  # GEN000580, GEN000600, GEN000620, GEN000640
  sed -i -e 's/PASS_MIN_LEN\t9/PASS_MIN_LEN\t5/' /etc/login.defs
#  sed -i -e 's/(password\s*requisite\s*.*pam_cracklib.so\s.*) minlen=9 ucredit=2 lcredit=2 dcredit=2 ocredit=2.*$/\1/' /etc/pam.d/system-auth
  sed -i -e 's/^\(password\s*requisite\s*.*pam_cracklib\.so\s\)\(.*\)\(minlen.*\)/\1\2/' /etc/pam.d/system-auth

  # GEN000800
  sed -i -e 's/\(password\s*sufficient\s*.*pam_unix.so\s.*\) remember=5/\1/' /etc/pam.d/system-auth

  # GEN000820
  sed -i -e 's/PASS_MAX_DAYS\t60/PASS_MAX_DAYS\t99999/' -e 's/PASS_MIN_DAYS\t1/PASS_MIN_DAYS\t0/' /etc/login.defs

  # GEN000920
  chmod 750 /root

  # GEN000980
  echo 'console' > /etc/securetty
  echo 'vc/1' >> /etc/securetty
  echo 'vc/2' >> /etc/securetty
  echo 'vc/3' >> /etc/securetty
  echo 'vc/4' >> /etc/securetty
  echo 'vc/5' >> /etc/securetty
  echo 'vc/6' >> /etc/securetty
  echo 'vc/7' >> /etc/securetty
  echo 'vc/8' >> /etc/securetty
  echo 'vc/9' >> /etc/securetty
  echo 'vc/10' >> /etc/securetty
  echo 'vc/11' >> /etc/securetty
  echo 'tty1' >> /etc/securetty
  echo 'tty2' >> /etc/securetty
  echo 'tty3' >> /etc/securetty
  echo 'tty4' >> /etc/securetty
  echo 'tty5' >> /etc/securetty
  echo 'tty6' >> /etc/securetty
  echo 'tty7' >> /etc/securetty
  echo 'tty8' >> /etc/securetty
  echo 'tty9' >> /etc/securetty
  echo 'tty10' >> /etc/securetty
  echo 'tty11' >> /etc/securetty
  echo >> /etc/securetty

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

  # GEN002740
  # I don't think we need to change this on soften, it'll be set correctly by the harden() regardless
  #sed -i -e 's/max_log_file = 125/max_log_file = 5/' /etc/audit/auditd.conf

  # GEN002760
  sed -i -e '/-a exit,always -S open -F success=0/d' \
  			-e '/-a exit,always -S unlink -S rmdir/d' \
  			-e '/-w \/var\/log\/audit\//d' \
         -e '/-w \/etc\/audit\/auditd.conf/d' \
         -e '/-w \/etc\/audit\/audit.rules/d' \
         -e '/-a exit,always -F arch=b32 -S stime -S acct -S reboot -S swapon/d' \
         -e '/-a exit,always -S settimeofday -S setrlimit -S setdomainname/d' \
         -e '/# The mysqld program is expected to call sched_setscheduler/d' \
         -e '/-a exit,always -S sched_setparam -S sched_setscheduler -F euid!=27/d' \
      /etc/audit/audit.rules
  # GEN002960
  rm -f /etc/cron.allow
  sed -i -e '/ssgconfig/d' /etc/cron.deny

  # GEN003080
  chmod 644 /etc/crontab
  chmod 755 /etc/cron.daily/*
  chmod 755 /etc/cron.monthly/*
  chmod 755 /etc/cron.weekly/*

  # GEN003320
  rm -f /etc/at.allow
  sed -i -e '/ssgconfig/d' /etc/at.deny

  # GEN003540
  chown root:root /var/crash
  chmod -R 700 /var/crash

  # GEN003740
  chmod 644 /etc/xinetd.conf

  # GEN004000
  chmod 4755 /bin/traceroute*

  # GEN004540
  mv /etc/mail/helpfile.old /etc/mail/helpfile

  # GEN005360
  chgrp root /etc/snmp/snmpd.conf

  # GEN005400
  chmod 644 /etc/syslog.conf

  # GEN005540
  sed -i -e '/sshd1: ALL/d' -e 'sshd2: ALL' -e '/sshdfwd-x11: ALL/d' /etc/hosts.deny
  sed -i -e '/sshd2: LOCAL/d' /etc/hosts.allow

  # GEN005540
  sed -i -e '/sshd1: ALL/d' -e '/sshd2: ALL/d' -e '/sshdfwd-x11: ALL/d' /etc/hosts.deny
  sed -i -e '/sshd2: LOCAL/d' /etc/hosts.allow

  # GEN006620
  sed -i -e '/ALL: ALL/d' /etc/hosts.deny

  # LNX00340
  sed -i -e 's/news:x:13:.*/news:x:13:news/' /etc/group
  sed -i -e 's/news:::.*/news:::news/' /etc/gshadow
  if [ ! "`grep 'news:x:9:13:news:/etc/news:' /etc/passwd`" ] ; then
	  sed -i -e '
/^uucp:/ i\
news:x:9:13:news:/etc/news:' /etc/passwd
  fi
  if [ ! "`grep 'news:\*:13637:0:99999:7:::' /etc/shadow`" ] ; then 
	  sed -i -e '
/^uucp:/ i\
news:*:13637:0:99999:7:::' /etc/shadow
  fi
  if [ ! "`grep 'games:x:12:100:games:/usr/games:/sbin/nologin' /etc/passwd`" ] ; then
	  sed -i -e '
/^ftp:/ i\
games:x:12:100:games:/usr/games:/sbin/nologin' /etc/passwd
  fi
  if [ ! "`grep 'games:\*:13637:0:99999:7:::' /etc/shadow`" ] ; then 
	  sed -i -e '
/^ftp:/ i\
games:*:13637:0:99999:7:::' /etc/shadow
  fi
  if [ ! "`grep 'gopher:x:30:' /etc/group`" ] ; then
	  sed -i -e '
/^dip:/ i\
gopher:x:30:' /etc/group
  fi
  if [ ! "`grep 'gopher:::' /etc/gshadow`" ] ; then
	  sed -i -e '
/^dip:/ i\
gopher:::' /etc/gshadow
  fi
  if [ ! "`grep 'gopher:x:13:30:gopher:/var/gopher:/sbin/nologin' /etc/passwd`" ] ; then
	  sed -i -e '
/^ftp:/ i\
gopher:x:13:30:gopher:/var/gopher:/sbin/nologin' /etc/passwd
  fi
  if [ "`grep 'gopher:*:13637:0:99999:7:::' /etc/shadow`" ] ; then
	  sed -i -e '
/^ftp:/ i\
gopher:*:13637:0:99999:7:::' /etc/shadow
  fi

  # LNX00440
  chmod 644 /etc/security/access.conf

  # LNX00520
  chmod 644 /etc/sysctl.conf

  # GEN004560
  sed -i -e 's/O SmtpGreetingMessage= Mail Server Ready ; \$b/O SmtpGreetingMessage=$j Sendmail $v\/$Z; $b/' /etc/mail/sendmail.cf
}

stigtest() {

# Run through the STIG Resolutions at 
# http://sarek.l7tech.com/mediawiki/index.php?title=4.3_STIG_Resolutions 
# and confirm each note on a system post-harden

# GEN005020
if [ "`getent passwd ftp`" ]; then
	echo "Error - user FTP exists"
fi

# LNX00260
if [ -e /etc/yum.conf ] ; then
	echo "Error - yum.conf exists"
fi
if [ "`rpm -qa | grep yum`" ] ; then
	echo "Error - yum package installed"
fi

# LNX00320
if [ "`getent passwd sync shutdown halt`" ] ; then
	echo "Error - user sync/shutdown/halt still may exist"
fi

# LNX00580
if [ -z "`grep ctrlaltdel /etc/inittab`" ] ; then
	echo "Error - ctrlaltdel is in /etc/inittab"
fi

# GEN000500
if [ ! "`grep TMOUT /etc/profile | grep 900 | grep -i export`" ] ; then  
	echo "Error - TMOUT is not 900 in /etc/profile"
fi

# GEN002860
if [ "$ISNCES" ] ; then
	if [ ! -x /etc/cron.daily/auditd-rotate ]; then
		echo "Error - /etc/cron.daily/auditd-rotate isn't there or isn't executable (only needed for NCES images)"
	fi
fi
if [ -x /etc/cron.daily/auditd-rotate ] ; then
	if [ ! "`grep USR1 /etc/cron.daily/auditd-rotate | grep auditd`" ] ; then
		echo "Error - No USR1 sent to auditd"
	fi
fi

# GEN000020
if [ ! "`grep '~~:S:wait:/sbin/sulogin' /etc/inittab`" ] ; then
	echo "Error - sulogin not in /etc/inittab"
fi

# bug 5534
if [ ! "`grep 's0.*ttyS0' /etc/inittab`" ] ; then
	echo "Error - ttyS0 not in /etc/inittab"
fi
if [ ! "`grep 's.*ttyS1' /etc/inittab`" ] ; then
	echo "Error - ttyS1 not in /etc/inittab"
fi

# GEN000440
if [ ! -e /var/log/btmp ] ; then 
	echo "Error /var/log/btmp doesn't exist"
fi

# GEN000460
if [ ! "$(cat /etc/pam.d/system-auth | grep ^auth | head -n 1 | egrep 'auth(.*)required(.*)pam_tally2.so deny=5 even_deny_root_account onerr=fail unlock_time=1200 root_unlock_time=1200')" ] ; then
	echo "Error - pam_tally2 not set up properly - auth required"
fi

# GEN000480
if [ ! "`grep "FAIL_DELAY 4" /etc/login.defs`" ] ; then
	echo "Error - FAIL_DELAY 4 not in /etc/login.defs"
fi

# GEN000540 GEN000700 GEN000820
if [ ! "`getent shadow root ssgconfig | awk -F : '{print $4, $5}' | grep "1 60" | wc -l`" = "2" ] ; then
	echo "Error - Invalid password change rules"
fi

# GEN000580 GEN000600 GEN000620 GEN000640
if [ ! "`cat /etc/login.defs | grep PASS_MIN_LEN | grep 9`" ] ; then 
	echo "Error - PASS_MIN_LEN setting invalid in /etc/login.defs"
fi

if [ ! "$(cat /etc/pam.d/system-auth | grep ^password | head -n 1 | egrep 'password(.*)requisite(.*)pam_cracklib.so retry=3 minlen=9 ucredit=-2 lcredit=-2 dcredit=-2 ocredit=-2(\s*)$')" ] ; then
	echo "Error - Invalid password complexity requirements"
fi

# GEN000800
if [ ! "$(egrep 'password(.*)sufficient(.*)pam_unix.so nullok use_authtok md5 shadow remember=5' /etc/pam.d/system-auth)" ] ; then
	echo "Error - Invalid password reuse requirements"
fi

# GEN000920
if [ ! "`stat --format=%a /root`" = "700" ] ; then
	echo "Error - Invalid permissions on /root"
fi

# GEN000980
if [ "`cat /etc/securetty | grep -v console | grep -v tty1 | grep -v ttyS0 | grep -v ttyS1`" ] ; then
	echo "Error - extra lines in /etc/securetty"
fi

# GEN001880
if [ "`stat --format=%a /home/ssgconfig/.bash_logout /home/ssgconfig/.bash_profile /home/ssgconfig/.bashrc 2>/dev/null | grep -v 740`" ] ; then
	echo "Error - Invalid permissions on ssh initialization files"
fi

# GEN002480
F1="`find / -xdev -type f -perm -002 -printf '%p %m\n' 2>&1 | grep -v '^/proc/' | grep -v '^/tmp/' | grep -v '^/var/tmp/'`"
F2="`find / -xdev -type d -perm -002 -printf '%p %m\n' 2>&1 | grep -v '^/proc/' | grep -v '^/tmp ' | grep -v '^/tmp/' | grep -v '^/var/tmp ' | grep -v '^/var/tmp/' | grep -v '^/dev/'`"
if [ "$F1" -o "$F2" ] ; then
	echo "Error - invalid world write access for files or dirs:
$F1
$F2"
fi

# GEN002740 GEN002760
F="/etc/audit/audit.rules"

if [ "$ISNCES" ] ; then
	if [ ! "`/sbin/chkconfig --list auditd | grep "3:on"`" ] ; then 
		echo "Error - auditd missing from runlevel 3"
	fi
	if [ ! "`grep -- "-a exit,always -S open -F success=0" $F`" ] ; then
	  echo "Error - Missing settings in /etc/audit/audit.rules"
	fi
fi

if [ "$ISVM" ] ; then 
	if [ ! "`egrep 'max_log_file = 5' /etc/auditd.conf`" ] ; then
		echo "Error - bad max_log_file setting for /etc/auditd.conf"
	fi
fi

# For NCES only 
#if [ "$ISNCES" ] ; then
#	  ! "`grep -- "-a exit,always -S unlink -S rmdir" $F`" -o \
#	  ! "`grep -- "-w /var/log/audit/" $F`" -o \
#	  ! "`grep -- "-w /etc/audit/auditd.conf" $F`" -o \
#	  ! "`grep -- "-w /etc/audit/audit.rules" $F`" -o \
#	  ! "`grep -- "-a exit,always -F arch=b32 -S stime -S acct -S reboot -S swapon" $F`" -o \
#	  ! "`grep -- "-a exit,always -S settimeofday -S setrlimit -S setdomainname" $F`" -o \
#	  ! "`grep -- "# The mysqld program is expected to call sched_setscheduler" $F`" -o \
#	  ! "`grep -- "-a exit,always -S sched_setparam -S sched_setscheduler -F euid!=27" $F`" ] ; then
#	  echo "Error - Missing settings in /etc/audit/audit.rules"
#	fi
#fi

# Non NCES, non VM
if [ "$ISHARDWARE" ] ; then 
	if [ ! "`egrep 'max_log_file = 125' /etc/audit/auditd.conf`" ] ; then
		echo "Error - bad max_log_file setting for /etc/audit/auditd.conf"
	fi
fi

# for all
if [ ! "`grep -- "-a exit,always -S unlink -S rmdir" $F`" -o \
	  ! "`grep -- "-w /var/log/audit/" $F`" -o \
	  ! "`grep -- "-w /etc/audit/auditd.conf" $F`" -o \
	  ! "`grep -- "-w /etc/audit/audit.rules" $F`" -o \
	  ! "`grep -- "-a exit,always -F arch=b32 -S stime -S acct -S reboot -S swapon" $F`" -o \
	  ! "`grep -- "-a exit,always -S settimeofday -S setrlimit -S setdomainname" $F`" -o \
	  ! "`grep -- "# The mysqld program is expected to call sched_setscheduler" $F`" -o \
	  ! "`grep -- "-a exit,always -S sched_setparam -S sched_setscheduler -F euid!=27" $F`" ] ; then
	  echo "Error - Missing settings in /etc/audit/audit.rules"
fi

# Settings for virtual machines
if [ "$ISVM" ] ; then
	if [ ! "`grep max_log_file /etc/audit/auditd.conf | grep 5`" ] ; then
		echo "Error - bad max_log_file setting for /etc/audit/auditd.conf"
	fi
fi

# GEN002740, GEN002760 
# FIXME - duplication?
#F="/etc/audit/audit.rules"
#if [ "$ISNCES" ] ; then
#	if [ ! "`grep -- '-a exit,always -S open -F success=0' $F`" ] ; then
#		echo "NCES: Missing rule in /etc/audit/audit.rules"
#	fi
#fi
#if [ ! "`grep -- "-a exit,always -S unlink -S rmdir" $F`" -o \
#	  ! "`grep -- "-w /var/log/audit/" $F`" -o \
#	  ! "`grep -- "-w /etc/audit/auditd.conf" $F`" -o \
#	  ! "`grep -- "-w /etc/audit/audit.rules" $F`" -o \
#	  ! "`grep -- "-a exit,always -F arch=b32 -S stime -S acct -S reboot -S swapon" $F`" -o \
#	  ! "`grep -- "-a exit,always -S settimeofday -S setrlimit -S setdomainname" $F`" -o \
#	  ! "`grep -- "# The mysqld program is expected to call sched_setscheduler" $F`" -o \
#	  ! "`grep -- "-a exit,always -S sched_setparam -S sched_setscheduler -F euid!=27" $F`" ] ; then
#	  echo "Error - Missing settings in /etc/audit/audit.rules"
#fi

# GEN002960
if [ ! -e /etc/cron.allow ] ; then
	echo "Error - /etc/cron.allow missing"
fi
if [ ! "`grep ssgconfig /etc/cron.deny`" ] ; then
	echo "Error - ssgconfig missing from /etc/cron.deny"
fi

# GEN003080
if [ ! "`stat --format=%a /etc/crontab | grep 600`" -o \
	    "`stat --format=%a /etc/cron.daily/* /etc/cron.monthly/* /etc/cron.weekly/* | grep -v 700`" ] ; then
		 echo "Error - bad permissions on /etc/crontab or /etc/cron.*/*"
fi

# GEN003320
if [ ! -e /etc/at.allow ] ; then
	echo "Error - /etc/at.allow missing"
fi
if [ ! "`stat --format=%a /etc/at.allow | grep 600`" ] ; then
	echo "Error - bad permissions on /etc/at.allow"
fi

# GEN003540
if [ ! "`stat --format=%a /var/crash | grep 700`" ] ; then 
	echo "Error - bad permissions on /var/crash"
fi

# GEN003740
if [ ! "`stat --format=%a /etc/xinetd.conf | grep 440`" ] ; then
	echo "Error - bad permissions on /etc/xinetd.conf"
fi

# GEN003865
if [ "`find / -xdev -name tcpdump 2>&1 | grep -v '^/proc/' `" ] ; then
	echo "Error - tcpdump is installed"
fi

# GEN004000
if [ ! "`stat --format=%a /bin/traceroute | grep 4700`" ] ; then
	echo "Error - bad permissions on traceroute"
fi

# GEN004540
if [ -e /etc/mail/helpfile ] ; then
	echo "Error - sendmail help exists"
fi

# GEN005360
if [ -e /etc/snmp/snmpd.conf ] ; then 
	if [ ! "`stat --format=%G /etc/snmp/snmpd.conf | grep sys`" ] ; then
		echo "Error - bad group ownership of /etc/snmp/snmpd.conf"
	fi
fi
if [ -e /etc/snmp/snmpd.conf_example ] ; then 
	if [ ! "`stat --format=%G /etc/snmp/snmpd.conf_example | grep sys`" ] ; then
		echo "Error - bad group ownership of /etc/snmp/snmpd.conf_example"
	fi
fi

# GEN005400
if [ ! "`stat --format=%a /etc/syslog.conf | grep 640`" ] ; then
	echo "Error - bad permissions on /etc/syslog.conf"
fi

# GEN005540
if [ ! "$ISNCES" ] ; then
	if [ ! "`grep "ALL: ALL" /etc/hosts.deny`" ] ; then
		echo "Error - missing ALL: ALL in /etc/hosts.deny"
	fi
	if [ ! "`grep "sshd: ALL" /etc/hosts.allow`" ] ; then
		echo "Error - missing sshd: ALL in /etc/hosts.allow"
	fi
fi
if [ "$ISNCES" ] ; then
	if [ ! "`grep "ALL: ALL" /etc/hosts.deny`" ] ; then
		echo "Error - missing ALL: ALL in /etc/hosts.deny"
	fi
	if [ "`cat /etc/hosts.allow`" ] ; then
		echo "Error - hosts.allow is not empty"
	fi
fi

# SSH set either protocol (Reverse Bug #6371)
# Only for version 5.0+
if [ "`rpm -qa | grep ssg-5`" ] ; then
	if [ ! "`grep '^Protocol 2$' /etc/ssh/sshd_config`" ] ; then
		echo "Error - SSH protocol 2 only is not enabled"
	fi
fi

# GEN006620
if [ ! "`grep "ALL: ALL" /etc/hosts.deny`" ] ; then
	echo "Error - missing ALL: ALL in /etc/hosts.deny"
fi

# LNX00340
if [ "`getent passwd news games gopher`" ] ; then
	echo "Error - news gopher or games account exists"
fi

# LNX00440
if [ ! "`stat --format=%a /etc/security/access.conf | grep 640`" ] ; then
	echo "Error - bad permissions on /etc/security/access.conf"
fi

# LNX00520
if [ ! "`stat --format=%a /etc/sysctl.conf | grep 600`" ] ; then
	echo "Error - bad permissions on /etc/sysctl.conf"
fi

# GEN001280
if [ "`find /usr/share/man -type f -a -perm +133`" ] ; then
	echo "Error - extra permissions over 644 on manpages"
fi
}

case "$1" in
  "-r"  ) shift;
	      soften;;
  "-t"  ) shift;
	      stigtest;;
  *     ) shift;
	      harden;;
esac
