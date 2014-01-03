#!/usr/bin/perl

use Data::Dumper;
use DBI;
use DBD::mysql;

use LWP;
use LWP::Simple;

use DateTime;
use DateTime::Format::Strptime;
use POSIX qw/strftime/;

use strict;
use warnings;


#==================main=======================================

sub usage() {
  print "GatherData.pl -db databasei [-u database user] [-pw database password] \n\n";
  exit;
}

my $db_name = "MarketData";
my $db_user = "root";
my $db_passwd = "r00t";



	if (@ARGV == 0) {
    usage();
  } else {
    for(my $i = 0; $i < @ARGV; $i++) {
      if ($ARGV[$i] eq "-db") {
        $db_name = $ARGV[$i+1];
        $i++;
      } elsif ($ARGV[$i] eq "-u") {
        $db_user = $ARGV[$i+1];
        $i++;
      } elsif ($ARGV[$i] eq "-pw") {
        $db_passwd = $ARGV[$i+1];
        $i++;
      } else {
				usage();
      }
    }
  }


	#connect to our database
  print "\nconnecting to $db_name database...";
  my $connection = DBI->connect( "DBI:mysql:database=$db_name;host=localhost", $db_user, $db_passwd, {'RaiseError' => 1} );

	#before we do anything, lets make sure the market is open today...
  my $isOpen = isMarketOpen();
  if ($isOpen != 1) {
    print "\nMarket is not open today.  Bye.\n";
    exit;
  }

	###################################
  # connect to TD Ameritrade
	###################################
	#TD connect parameters
  our $tdapi_base = "https://apis.tdameritrade.com/apps/100/";
  our $sourceId = "source=NALA";
  our $sourceInfo = $sourceId."&version=1.0";
  our $loginInfo  = "userid=Ladd3113&password=139WhitakerRd";

	my $url = $tdapi_base.'LogIn?'.$sourceInfo;
  my $data = $loginInfo.'&'.$sourceInfo."\n\n";    #two newlines (asii 10) are needed for post data

  my $ua = LWP::UserAgent->new;
  my $resp = $ua->post( $url,  'content-type' => "application/x-www-form-urlencoded", Content => $data);

  die "HTTP request error ", $resp->status_line
    unless $resp->is_success;

  print "\nLogged in to TD Ameritrade...\n";

  my $sessionId = simpleParse( $resp->content, "session-id" );

	#print "\nreturned sid = $sessionId";
  if (length($sessionId) == 0)
  {
     warn "\nFATAL: Invalid login\n";
     exit;
  }

	my $currentTime = DateTime->now(time_zone => "-0500");
	my $timeString = $currentTime->strftime("%Y-%m-%d %H:%M:%S");

	my $parser = DateTime::Format::Strptime->new(
  	pattern => '%Y-%m-%d %H:%M:%S',
	  on_error => 'croak',
	);
	my $marketOpen = $parser->parse_datetime(getMarketOpen());
	my $marketClose = $parser->parse_datetime(getMarketClose());

	while ($currentTime < $marketOpen) {
		sleep(30);
		$currentTime = DateTime->now(time_zone => "-0500");
	}

	my $select = "SELECT S.id, S.symbol FROM BiggestLosers B INNER JOIN symbols S ON S.id = B.symbolId WHERE DATE(B.sell_date) = DATE(\"$timeString\")";
	my $db = $connection->prepare($select);
	$db->execute();

	my $symbols = '';
  my %symbol_ids = ();  #hash table

	my $quote_list = "";
  while(my @loser = $db->fetchrow_array()) {
    
		my $symbol_id = $loser[0];
    my $symbol    = $loser[1];

    $symbols .= $symbol . ",";
    $symbol_ids{$symbol} = $symbol_id;
		
		$quote_list .= $symbol . ",";
	}  
	$db->finish();	

	while ($currentTime > $marketOpen && $currentTime < $marketClose) {
print "Retrieving RealTimeQuotes...\n";
		my $quote_data = getQuote($quote_list);

		my $found_quote = 1;
    while($found_quote)
    {
      my $qstart = index($quote_data, "<quote>");
      if ($qstart == -1)
      {
        warn "\nFATAL GetQuotes: cant find start of quote";
        $found_quote = 0;;
      }
      else {

        # strip off beginning
        $quote_data = substr($quote_data, $qstart);

				updateQuotes($quote_data, "RealTimeQuotes", %symbol_ids);

        # get ready to parse next quote
        my $qend = index($quote_data, "</quote>");
        if ($qend == -1)
        {
          warn "\nGetQuotes: cant find end of quote";
         return 0;
        }

        # strip off beginning
        $quote_data = substr($quote_data, $qend);
      }
		}
	
		sleep(5*60);		
		$currentTime = DateTime->now(time_zone => "-0500");
	}


  # get EndOfDay Quotes and update EODQuotes table (using last-trade-date and last trade price)
  print "\nGetting todays quotes...\n";
#  getEODQuotes();


  # now we can calculate todays biggest losers
 # biggestLosers();

  #logout from TD Ameritrade
	$url = $tdapi_base.'LogOut;jsessionId='.$sessionId.'?'.$sourceId;
  my $getContent = LWP::Simple::get($url);

  die "logout error ", $getContent
    unless defined $getContent;

#print "\nLogout results: ", $getContent, "\n";
  print "\nLogged out from TD Ameritrade...\n";

  $connection->disconnect;


print "\n**end**\n";


#=================end main=====================================



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
     print "\nParse error for tag $tag";
     return "";
  }

  my $val = substr($xml,$start+$tagsize, $end-$start-$tagsize);

  return $val;

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





#================================================================
# get todays quotes and update EOD database
# returns number of rows inserted, or 0 on error

# get 100 at a time (300 max)

sub getEODQuotes
{
my $MAX_QUOTES = 100;
my $EXCHANGE = "NASDAQ";


  #get the symbols from our database
  my $db = $connection->prepare( "SELECT * FROM symbols where exchange = \"$EXCHANGE\" AND market_cap > 0 AND type = \"common\"" );
  $db->execute();

  if ($db->rows==0)
  {
    warn "\nFatal DB error - no rows in symbol table\n";
    return 0;
  }

  my $symbols = '';
  my %symbol_ids = ();	#hash table

	my @quote_lists = ();
	my $count = 1;
  while (my @data  = $db->fetchrow_array())
  {
    my $symbol_id = $data[0];
    my $symbol    = $data[1];

    $symbols .= $symbol . ",";
    $symbol_ids{$symbol} = $symbol_id;
			
		if ($count % 100 == 0) {
			$quote_lists[$count/100 - 1] = $symbols;						
			$symbols = '';
		}
		$count++;

	}
	
	if($symbols ne '') {
		$quote_lists[$count/100] = $symbols;						
	}
 
	foreach my $quote_list (@quote_lists) {
		# process
    my $quote_data = getQuote($quote_list);

    # look for the start of the next quote
    my $qlstart = index($quote_data, "<quote-list>");
    # sanity check
    if ($qlstart == -1)
    {
      warn "\nFATAL GetQuotes: cant find start of quote-list";
    }

		my $found_quote = 1;
  	while($found_quote)
   	{
     	my $qstart = index($quote_data, "<quote>");
      if ($qstart == -1)
 	    {
   	    warn "\nFATAL GetQuotes: cant find start of quote";
				$found_quote = 0;;
     	}
			else {

	     	# strip off beginning
  	   	$quote_data = substr($quote_data, $qstart);
    	    
				updateQuotes($quote_data, "EndOfDayQuotes", %symbol_ids);

	      # get ready to parse next quote
  	    my $qend = index($quote_data, "</quote>");
    	  if ($qend == -1)
      	{
        	warn "\nGetQuotes: cant find end of quote";
				 return 0;
  	    }

    	  # strip off beginning
      	$quote_data = substr($quote_data, $qend);
			}

     }  # next quote

  
  } # next batch

  $db->finish();

}

#================================================================     
#update our end of day quote DB with all pertinent stock info
sub updateQuotes
{
  my ($quote_data, $table, %symbol_ids) = @_;

  #parse the return data

	my $sid    = "";
  my $date   = simpleParse($quote_data, "last-trade-date");
  my $symbol = simpleParse($quote_data, "symbol");
 	my $open   = simpleParse($quote_data, "open");
  my $close  = simpleParse($quote_data, "close");
  my $low    = simpleParse($quote_data, "low");
  my $high   = simpleParse($quote_data, "high");
 	my $price  = simpleParse($quote_data, "last");
  my $volume = simpleParse($quote_data, "volume");
  my $change = simpleParse($quote_data, "change");
  my $chgper = simpleParse($quote_data, "change-percent");  
 	# TD returns a string with a trailing % we dont want that
  $chgper =~ /(\-+[0-9]*\.+[0-9]*)/;
  $chgper =~ s/[%]//g;

  my $bid   = simpleParse($quote_data, "bid");
 	my $ask   = simpleParse($quote_data, "ask");
  my $bas   = simpleParse($quote_data, "bid-ask-size");
  # retured as bidXask, eg 200X400
  my $x = index($bas, "X");
  my $bidsize = substr($bas, 0, $x);
  my $asksize = substr($bas, $x+1);
  my $yearlo  = simpleParse($quote_data, "year-low");
 	my $yearhi  = simpleParse($quote_data, "year-high");

  # look for errors - this could be done first, bit little harm here
  # note that we could have parse errors previous to this error
  # dont write to DB if there is a symbol error
 	my $symerr = 0;

  #TODO: also check for result (before the list!!!)
  my $err = simpleParse($quote_data, "error");
  if ($err ne "")
 	{
   	print "\nError in quote data for $symbol from AmeriTrade: $err";
    $symerr = 1;
  }

  # this is realbad because we wont have an id to insert!
  if (!defined($symbol_ids{$symbol}))
  {
 	  warn "\nCant find symbol id for $symbol! Not updating quote table!";
    $symerr = 1;
 	} else {
		$sid = $symbol_ids{$symbol};
	}

  # now check the contents of the return data TODO: why is this happening???
  if ($price eq "")
  {
    print "\nNULL price for $symbol! Not updating quote table!";
    $symerr = 1;
  }

  if ($open eq "")
  {
    print "\nNULL open for $symbol! Not updating quote table!";
    $symerr = 1;
  }

  # OK to write
  if ($symerr==1)
  {
   	return;
	}

  #format certain fields so they insert in the database nicely
  #ensure all our values are correctly defined - TODO: check this
  if ( !defined $open   || $open eq "")    { $open = "NULL"; }
  if ( !defined $close  || $close eq "")   { $close = "NULL"; }
  if ( !defined $low    || $low eq "")     { $low = "NULL"; }
  if ( !defined $high   || $high eq "")    { $high = "NULL"; }
  if ( !defined $price  || $price eq "")   { $price = "NULL"; }
  if ( !defined $volume || $volume eq "" ) { $volume = "NULL"; }
  if ( !defined $change || $change eq "")  { $change = 0; }
  if ( !defined $chgper || $chgper eq "")  { $chgper = "NULL"; }
  if ( !defined $bid    || $bid eq "")     { $bid = "NULL"; }
  if ( !defined $ask    || $ask eq "")     { $ask = "NULL"; }
  if ( !defined $bidsize || $bidsize eq "") { $bidsize = "NULL"; }
  if ( !defined $asksize || $asksize eq "") { $asksize = "NULL"; }
  if ( !defined $yearlo || $yearlo eq "")  { $yearlo = "NULL"; }
  if ( !defined $yearhi || $yearhi eq "")  { $yearhi = "NULL"; }

	my $insert_statement = "";
  # insert the data into the EOD db
	if ($table eq "EndOfDayQuotes") {
	  $insert_statement = "INSERT INTO EndOfDayQuotes (symbolid, symbol, date, open, close, high, low, price, volume, chng, chgper, bid, ask, bidsize, asksize, yearlo, yearhi) VALUES ($sid, \"$symbol\", \"$date\", $open, $close, $high, $low, $price, $volume, $change, \"$chgper\", $bid, $ask, $bidsize, $asksize, $yearlo, $yearhi)";
	} elsif ($table eq "RealTimeQuotes") {
		my $currentTime = DateTime->now(time_zone => "-0500");
	  my $timeString = $currentTime->strftime("%Y-%m-%d %H:%M:00");
	  $insert_statement = "INSERT INTO RealTimeQuotes (symbolId, symbol, date, open, prevClose, high, low, price, volume, chg, chgper, bid, ask) VALUES ($sid, \"$symbol\", \"$timeString\", $open, $close, $high, $low, $price, $volume, $change, \"$chgper\", $bid, $ask)";
	}
  
	$connection->do( $insert_statement );

}

#======================================================
# return the top n biggest losers by percent change (by symbolId)
# note: limited to $MAX_LOSERS=10 right now, with only minVolume criteria

sub biggestLosers
{
  my @losers;

  my $currDate = getCurrentTradeDate();
  if ($currDate eq "")
  {
    warn "\nGetQuotes::biggestLosers() FATAL- cant get Current Trade Date";
    return 0;
  }
	my $nextDate = getNextTradeDate();

  #get the symbols from our database (date based to retrieve most current)
  my $select = "SELECT symbolId FROM EndOfDayQuotes WHERE DATE(date) = \"$currDate\" ORDER BY chgper LIMIT 50";
  my $db = $connection->prepare( $select );
  $db->execute();

	while (my @data = $db->fetchrow_array()) {
    push(@losers, $data[0]);

		my $insert = "INSERT INTO BiggestLosers (symbolId, buy_date, sell_date) VALUES ($data[0], \"$currDate\", \"$nextDate\")";
		$connection->do( $insert );

	}

	$db->finish();
}


#=============================================================================
# Current Market Open Date
# return date of the most current open market (accounts for weekends and holidays)
#
sub getCurrentTradeDate
{

  #today
  my $date = strftime("%Y-%m-%d", localtime);

  my $stmt = "SELECT date, is_market_open FROM calendar WHERE date BETWEEN DATE_ADD(\"$date\", INTERVAL -7 DAY) AND \"$date\" AND is_market_open = 1 ORDER BY date DESC LIMIT 1";
#print $stmt . "\n";

  my $db = $connection->prepare($stmt);
  $db->execute();
  my @data = $db->fetchrow_array();
  $db->finish();
  
	return $data[0];
  
}

#==============================================================================
# Get the next trade date that the market is open
#
sub getNextTradeDate
{
  #default is current date
  my $date = strftime("%Y-%m-%d", localtime);

  my $stmt = "SELECT date, is_market_open FROM calendar WHERE date BETWEEN DATE_ADD(\"$date\", INTERVAL 1 DAY) AND DATE_ADD(\"$date\", INTERVAL 8 DAY) AND is_market_open = 1 ORDER BY date LIMIT 1";
#print $stmt . "\n";

  my $db = $connection->prepare($stmt);
  $db->execute();
  my @data = $db->fetchrow_array();
  my $nextdate = $data[0];
  $db->finish();

  return $nextdate;
}

#================================================
# Get the market open time
#
sub getMarketOpen
{

  #today
  my $date = strftime("%Y-%m-%d", localtime);

  my $stmt = "SELECT date, open_time, is_market_open FROM calendar WHERE date BETWEEN DATE_ADD(\"$date\", INTERVAL -7 DAY) AND \"$date\" AND is_market_open = 1 ORDER BY date DESC LIMIT 1";
#print $stmt . "\n";

  my $db = $connection->prepare($stmt);
  $db->execute();
  my @data = $db->fetchrow_array();
  $db->finish();

  return $data[1];

}


#================================================
# Get the market close time
#
sub getMarketClose
{

  #today
  my $date = strftime("%Y-%m-%d", localtime);

  my $stmt = "SELECT date, close_time, is_market_open FROM calendar WHERE date BETWEEN DATE_ADD(\"$date\", INTERVAL -7 DAY) AND \"$date\" AND is_market_open = 1 ORDER BY date DESC LIMIT 1";
#print $stmt . "\n";

  my $db = $connection->prepare($stmt);
  $db->execute();
  my @data = $db->fetchrow_array();
  $db->finish();

  return $data[1];

}


sub isMarketOpen
{

  my $stmt = "SELECT date, is_market_open FROM calendar WHERE date=CURRENT_DATE()";

  my $db = $connection->prepare($stmt);
  $db->execute();
  my $sql_result = $db->fetchrow_hashref();
  $db->finish();

  return ($sql_result->{'is_market_open'});
}
