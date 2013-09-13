#!/usr/bin/perl

# Get Stock exchange quotes from TD ameritrade

use Data::Dumper;
use DBI;
use DBD::mysql;
use XML::Parser;

use LWP;
use LWP::Simple;

use DateTime;
use DateTime::Format::Strptime;
use POSIX qw/strftime/;

use strict;
use warnings;
#use diagnostics;


# global defines

#TD connect parameters
  our $tdapi_base = "https://apis.tdameritrade.com/apps/100/";
  our $sourceId = "source=NALA";
  our $sourceInfo = $sourceId."&version=1.0";
  our $loginInfo  = "userid=Ladd3113&password=139WhitakerRd";

#database connect parameters
  our $dbname = "stockdata";

# global logfile
#  our $logfileLocation = "./";
  our $logfileLocation = "/home/dayTrader/logs/";

# global variables
  our $sessionId;
  our $connection;

# global parameters
  our $MIN_VOLUME = 0;
  our $MAX_LOSERS = 10;

# debugging
  # 1 = standard debugging; 2 include DT return data (lots!)
  our $_dbg = 1;
#  our $dbgfileLocation = "./";
  our $dbgfileLocation = "/home/dayTrader/logs/";
  our $dbgfileName = "dt.dbg";

  # set this if the market is closed (for testing)
  our $_ignoreMarketClosed = 0;

#=================================================
# return a valid session Id if login is successful
sub login
{
  my $url = $tdapi_base.'LogIn?'.$sourceInfo;
  my $data = $loginInfo.'&'.$sourceInfo."\n\n";		 #two newlines (asii 10) are needed for post data
#print "\nurl=", $url,"\ndata=",$data;

  my $ua = LWP::UserAgent->new;
  my $resp = $ua->post( $url,  'content-type' => "application/x-www-form-urlencoded", Content => $data);

  die "HTTP request error ", $resp->status_line
    unless $resp->is_success;

#print "\nLogin results: ", $resp->content, "\n";
  print "\nLogged in to TD Ameritrade...\n";

  my $sid = simpleParse( $resp->content, "session-id" );
  return $sid;

}

#==========================================================
sub logout
{
  my $url = $tdapi_base.'LogOut;jsessionId='.$sessionId.'?'.$sourceId;
  my $getContent = LWP::Simple::get($url);

  die "logout error ", $getContent
    unless defined $getContent;

#print "\nLogout results: ", $getContent, "\n";
  print "\nLogged out from TD Ameritrade...\n";
}


#==============================================================
#takes a stock symbol, or multiple symbols separated by commas
#returns XML data
sub getQuote
{
  my ($symbol) = @_;

  my $url = $tdapi_base.'Quote;jsessionid='.$sessionId.'?'.$sourceId.'&symbol='.$symbol;
#print "\ngetQuote= ", $url;

  my $getContent = LWP::Simple::get($url);

# TODO: check for error
#  die "getQuote error ", $getContent
#    unless defined $getContent;

#print "\ngetQuote results: ", $getContent, "\n";

  return $getContent;
}

#=====================================
# pass in the full xml string and tag
sub simpleParse
{
  my($xml, $tag) = @_;

  my $taga = "<".$tag.">";
  my $tagb = "</".$tag.">";
  my $tagsize = length($tag)+2;

  #find <tag>
  my $start = index($xml, $taga);
  my $end   = index($xml, $tagb);
  if ($start==-1 || $end==-1)
  {
warn "\nParse error for tag $tag\n";
     return "";
  }

  my $val = substr($xml,$start+$tagsize, $end-$start-$tagsize);

  return $val;

}

#==================================================
# the date format is compatible with our quote tables
sub getDate
{
  my $dt = DateTime->now;
  my $date = $dt->ymd;
  return $date;
}


#================================================================
sub timeStamp
{
  my $dt = DateTime->now;
  my $date = $dt->ymd;
  my $time = $dt->hms;

  return "$date $time";
}

#===========================================================
# return 1 if open, 0 if closed TODO -1 on query error
sub isMarketOpen
{
  #default is current date
  my $date = "";
  if (@_) { $date = $_[0]; }

  my $stmt = "SELECT date, market_is_open FROM calendar WHERE date=CURRENT_DATE()";
  if ($date ne "") { $stmt = "SELECT date, market_is_open FROM calendar WHERE date=\"$date\""; }
#print "\n$stmt";

  my $db = $connection->prepare($stmt);
  $db->execute();
  my $sql_result = $db->fetchrow_hashref();
  $db->finish();

  return ($sql_result->{'market_is_open'});
}

#========================================================
sub symbolLookup
{
  my ($symbolid) = @_;

  my $db = $connection->prepare( "SELECT symbol FROM symbols where id = $symbolid" );
  $db->execute();

  my $rows = $db->rows;

  if ($rows!=1)
  {
    warn "\nsymbolLookup: Fatal DB error - cant find id $symbolid in symbol table\n";
    return "";
  }

  my @data = $db->fetchrow_array();
  my $symbol    = $data[0];
#print "\nlookup $symbolid: $symbol";

  $db->finish();
  
  return $symbol;
}


