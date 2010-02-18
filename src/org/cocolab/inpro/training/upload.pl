#!/usr/bin/perl -w

use strict;
use CGI; # Modul fuer CGI-Programme

# place this little program on your server.

my $cgi = new CGI; # neues Objekt erstellen

# Content-type fuer die Ausgabe
print $cgi->header(-type => 'text/html');

# die datei-daten holen
my $file = $cgi->param('zipfile');
# we use as little as possible from the filename
# in order to reduce risk from weird filenames
# remember: the internet is evil
$file = ($file =~ m/\.zip$/) ? '.zip' : '.unknown';

# dateinamen erstellen und die datei auf dem server speichern
my $fname = 'test/'.$ENV{REMOTE_ADDR}.'_'.time.'_'.$file;
open DAT,'>'.$fname or die 'Error processing file: ',$!;

# Dateien in den Binaer-Modus schalten
binmode $file;
binmode DAT;

my $data;
while(read $file,$data,1024) {
  print DAT $data;
}
close DAT;

print <<"HTML";
<html>
<head>
<title>Fileupload</title>
</head>
<h1>Status</h1>
<p>Die Datei $file wurde erfolgreich auf dem Server gespeichert.</p>
</body>
</html>
HTML

