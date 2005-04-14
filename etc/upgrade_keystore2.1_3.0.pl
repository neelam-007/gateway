#!/usr/bin/perl
use strict;
#
# Upgrade Keystores 
# Take the existing keys, hide the originals, convert to the other format
# 

my $keyproperties="/ssg/etc/conf/keystore.properties";

# No user serviceable parts after this

my %props=();
open ( PROPS, $keyproperties);
while (<PROPS>) {
	if ( m/(.*)=(.*)/) {
		$props{$1}=$2;
	}
}

if ( ! -e "/ssg/tomcat/webapps/ROOT/WEB-INF" ) {
	print STDERR "run fix_config_keystores.sh first to expand the WAR\n";
	exit;
}

fix_keystore("$props{keystoredir}/$props{sslkstorename}",$props{sslkspasswd});
fix_keystore("$props{keystoredir}/$props{rootcakstorename}",$props{rootcakspasswd});


sub fix_keystore($$) {
	my $keystore = shift;
	my $keypass = shift;
	if ( $keystore && -e $keystore) {
		print "Keystore File: $keystore\n";
	} else { 
		print "Keystore $keystore not found\n";
		return 0;
	}
	unlink "/tmp/working.p12";
	unlink "/tmp/working.ks";
	`cp $keystore /tmp/working.ks`;
	`JAVA_HOME=/ssg/j2sdk1.4.2_05 \$JAVA_HOME/bin/java -classpath /ssg/tomcat/common/classpath/bcprov-jdk14-124.jar:/ssg/tomcat/webapps/ROOT/WEB-INF/classes com.l7tech.common.security.CopyKeystore /tmp/working.ks JKS /tmp/working.p12 BCPKCS12 $keypass`;

	`mv $keystore $keystore.old_format`;
	`cp /tmp/working.p12 $keystore`;
}
