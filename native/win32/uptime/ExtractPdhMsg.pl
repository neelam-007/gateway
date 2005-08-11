#!perl

# $Id$
#
# Purpose: Extracts PDH error code names and description from pdhmsg.h.
# Author: rmak

use strict;			# It's a good thing.

my $path='C:\Program Files\Microsoft Visual Studio .NET 2003\Vc7\PlatformSDK\Include\pdhmsg.h';
open( FH, "<$path" ) or die( "Cannot open file." );
local $/;
undef $/;
my $buf = <FH>;       # Whole file read in for multiline pattern matching.
while ( $buf =~ m|// MessageId:\s*(\S+).*?// MessageText:\s*(.*?)#define|s )
{
    $buf = $';      # Keeps post-match string for re-match in next loop.
    my $name = $1;
    my $description = "";
    foreach ( split( /[\r\n]+/, $2 ) )
    {
        s|^//\s*(.*?)\s*$|\1|;      # Remove comment characters, leading and trailing spaces.
        $description .= " " . $_;   # Concatenate into one line.
    }
    $description =~ s|^\s*||;       # Trims leading spaces.
    $description =~ s|\s*$||;       # Trims trailing spaces.
    $description =~ s|\\|\\\\|g;    # Escape backslashes.
    $description =~ s|"|\\"|g;      # Escape double quotes.
    print "    { $name, \"$name\", \"$description\" },\n";
}
close FH;
