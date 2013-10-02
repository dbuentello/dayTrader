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
  our $logfileLocation = "./logs/";

# global variables
  our $sessionId;
  our $connection;

# global parameters
  our $MIN_VOLUME = 10000;
  our $MIN_PRICE  = .50;
  our $MAX_LOSERS = 10;

# debugging
  # 1 = standard debugging; 2 include DT return data (lots!)
  our $_dbg = 1;
  our $dbgfileLocation = "./logs/";
  our $dbgfileName = "dt.dbg";

# for testing

  # set this if the market is closed (for testing)
  our $_ignoreMarketClosed = 0;

  # use with extreme care
  our $_simulate = 0;
  our $_simulate_date;

#=================================================
# -t[est]
# -d[ebug]{2}
# -ic [ignore market close]
# -o<directory>  [output directory for log and debug files]
# -s<date>  simulation for <date YYY-MM-DD>  do not get Quotes from TD; assumes its in the EOD and RT databases
#    use with extreme care - tables will have to be manipulated

sub getCommandLineArgs
{
  my $n = @ARGV;
  for (my $i=0; $i<$n; $i++)
  {
    if ($ARGV[$i] eq "-t")
    {
      $_dbg = 1;
      $dbname .= "_test";
    }
    if ($ARGV[$i] eq "-d")
    {
      $_dbg = 1;
    }
    if ($ARGV[$i] eq "-d2")
    {
      $_dbg = 2;
    }
    if ($ARGV[$i] eq "-ic")
    {
      $_ignoreMarketClosed = 1;
    }
    #TODO: be sure it has trailing /
    if (substr($ARGV[$i], 0, 2) eq "-o")
    {
       $dbgfileLocation = substr($ARGV[$i], 2);
       $logfileLocation = substr($ARGV[$i], 2);
    }
    if (substr($ARGV[$i], 0, 2) eq "-s")
    {
       $_simulate = 1;
       $_simulate_date = substr($ARGV[$i], 2);
    }

  } # next arg

if ($_dbg) { print "\nCommand Line Args: DBName= $dbname  dbg= $_dbg  output dir= $logfileLocation"; }

}

#================================================
sub Initialize
{
  my ($title) = @_;

  # caution!
  if ($_ignoreMarketClosed==1) { print "\nWARNING: you are overriding check for open market!\n";  }

  #debugging
  if ($_dbg) { print "\nDebugging Level $_dbg ON (to $dbgfileLocation"."$dbgfileName)\n"; }
  open(DBGFILE, ">>".$dbgfileLocation.$dbgfileName) || die "Error opening dbgfile ($dbgfileLocation.$dbgfileName) $!";
  print DBGFILE "\n\n*** $title ".strftime('%a %b %e %Y %H:%M',localtime)." ***\n\n";

  #global log file
  open(LOGFILE, ">>".$logfileLocation."dayTrader.log") || die "Error opening logfile ($logfileLocation.dayTrader.log) $!";
  print LOGFILE "\n\n*** $title ".strftime('%a %b %e %Y %H:%M',localtime)." ***\n\n";

  #connect to our database
  print "\nconnecting to $dbname database...";
  $connection = DBI->connect( "DBI:mysql:database=$dbname;host=localhost", "root", "sal", {'RaiseError' => 1} );


  #before we do anything, lets make sure the market is open today...
  # (for developing, we can set the ignore flag...)
  my $isOpen = isMarketOpen();
  if (!$_ignoreMarketClosed) {
    if ($isOpen != 1) {
      print "\nMarket is not open today.  Bye.\n";
      exit;
    }
  }

  return ;
}

#=================================================
# return a valid session Id if login is successful
sub TDlogin
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
sub TDlogout
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
     print LOGFILE "\nParse error for tag $tag";
     if ($_dbg==2) { print DBGFILE "\nParse error for tag $tag"; }
     return "";
  }

  my $val = substr($xml,$start+$tagsize, $end-$start-$tagsize);

  return $val;

}

#==================================================
# the date format is compatible with our quote tables
# this not used now 09.22.13
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

#alternate:
# my $dt = strftime("%F %T", localtime);
# return $dt
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


#=============================================================================
# Current Market Open Date
# return date of the most current open market (accounts for weekends and holidays)
sub getCurrentTradeDate
{

# TESTING
if ($_simulate)
{
  return $_simulate_date;
}
# TESTING

  #today
  my $date = strftime("%Y-%m-%d", localtime);

  my $stmt = "SELECT date, market_is_open FROM calendar WHERE date BETWEEN DATE_ADD(\"$date\", INTERVAL -7 DAY) AND \"$date\" ORDER BY date DESC";
#print $stmt;

  my $db = $connection->prepare($stmt);
  $db->execute();

  my @data;
  while (@data = $db->fetchrow_array())
  {
     if ($data[1] == 1) 
     {
          $db->finish();
          return $data[0];
     }
  }
  $db->finish();
  return "";   # error
}

#=============================================================================
# Previous Trade Date (previous date market was open)
# return date of the previous open market date (accounts for weekends and holidays)
# input is the 'current' date; return is the date previous
# default input is today
sub getPreviousTradeDate
{
  #default is current date
  my $date = "";
  if (@_) { $date = $_[0]; }
#  if ($date eq "") { $date = strftime("%Y-%m-%d", localtime); }
  if ($date eq "") { $date = getCurrentTradeDate(); }

  my $stmt = "SELECT date, market_is_open FROM calendar WHERE date BETWEEN DATE_ADD(\"$date\", INTERVAL -8 DAY) AND DATE_ADD(\"$date\", INTERVAL -1 DAY) ORDER BY date DESC";
#print $stmt;

  my $db = $connection->prepare($stmt);
  $db->execute();

  my @data;
  while (@data = $db->fetchrow_array())
  {
     if ($data[1] == 1) 
     {
          $db->finish();
          return $data[0];
     }
  }
  $db->finish();
  return "";   # error
}


#===================================================
# takes weekends and holidays into account

sub getNextTradeDate
{
  #default is current date
  my $date = "";
  if (@_) { $date = $_[0]; }
#  if ($date eq "") { $date = strftime("%Y-%m-%d", localtime); }
  if ($date eq "") { $date = getCurrentTradeDate(); }

  my $stmt = "SELECT date, market_is_open FROM calendar WHERE date BETWEEN DATE_ADD(\"$date\", INTERVAL 1 DAY) AND DATE_ADD(\"$date\", INTERVAL 8 DAY) AND market_is_open = 1 ORDER BY date LIMIT 1";
#print $stmt;

  my $db = $connection->prepare($stmt);
  $db->execute();
  my @data = $db->fetchrow_array();
  my $date = $data[0];
  $db->finish();

  return $date;
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



