#!/usr/bin/perl

# Get End of Day Stock exchange quotes from TD ameritrade

do 'dtCommon.pl';


#============================================================
# get end of day quotes and determine biggest losers


#==================main=======================================
print "\n** Get End of Day Quotes from TD Ameritrade **\n";

  #debugging select * from RealTimeQuotes
  if ($_dbg) { 
    print "\nDebugging Level $_dbg ON (to $dbgfileLocation"."$dbgfileName)\n";
    if ($_marketClosed) {print "\n*** market is closed:  using calculated \%Chg\n"; print DBGFILE "\n*** market is closed:  using calculated \%Chg\n"; }
  }
  open(DBGFILE, ">>".$dbgfileLocation.$dbgfileName) || die "Error opening dbgfile!";

  #global log file
  open(LOGFILE, ">>".$logfileLocation."dayTrader.log") || die "Error opening logfile!";

  #connect to our database
  $connection = DBI->co select * from RealTimeQuotesnnect( "DBI:mysql:database=stockdata;host=localhost", "root", "sal", {'RaiseError' => 1} );

#for debuging
#$connection->trace(1);

  $sessionId = login();
#print "\nreturned sid = $sessionId";
  if (length($sessionId) == 0)
  {
     warn "\nFATAL: Invalid login\n";
     exit;
  }

  print "\nGetting todays quotes...\n";
  getQuotes();

  my @biggest_losers = biggestLosers();
  print "\nTodays biggest losers are: "; for (my $i=0; $i<$MAX_LOSERS; $i++) { print " ".symbolLookup($biggest_losers[$i]); } print "\n";

if ($_dbg) {
print "\nBiggest losers are: @biggest_losers\n";
for (my $i=0; $i<$MAX_LOSERS; $i++) { print " ".symbolLookup($biggest_losers[$i]); } print "\n"; }

  #update our holdings table with stocks to buy (just symbol and date)
  print "\nUpdating holdings...\n";
  updateHoldings(@biggest_losers);

  # calcualate buy positions and update DB
  updateBuyPositions(@biggest_losers);

  # execute orders (anLOGFILEd update buy price, if different than calculated)

  #update our Analysis/History DB (still need TD)
  print "\nUpdating History...";
  updateAnalysis();

  #logout from TD Ameritrade
  logout();

  $connection->disconnect;

  close LOGFILE;
  close DBGFILE;

print "\n**end**\n";


#=================end main=====================================




#================================================================
# get 100 at a time
sub getQuotes
{
my $MAX_QUOTES = 100;

  # clear the table first so we only have one entry per date
  my $date = getDate();
  my $stmt = "DELETE FROM EndOfDayQuotes WHERE date = \"$date\"";
  my $db = $connection->prepare( $stmt );
  $db->execute();


  #get the symbols from our database  (DEBUG                             \/ \/
  my $db = $connection->prepare( "SELECT * FROM symbols where exchange = \"AMEX\"" );
  $db->execute();

  my $rows = $db->rows;
#print "\n", $rows, " fetched";

  if ($rows==0)
  {
    warn "\nFatal DB error - no rows in symbol table\n";
    return 0;
  }

  my $niters = int($rows/$MAX_QUOTES)+1;
  my $modcnt = $rows % $MAX_QUOTES;	# last partial batch
#print "\nniters=$niters, modcnt=$modcnt";

#debug
#$niters=3;

  my $count;

  my $symbols = '';
  my %symbol_ids = ();	#hash table

  for (my $it=0; $it<$niters; $it++)
  {
     # get at least 100 symbols at a time
     if ($it<$niters-1) { $count = $MAX_QUOTES; }
     else               { $count = $modcnt;     }
     
     for (my $i=0; $i<$count; $i++)
     {
       my @data  = $db->fetchrow_array();
 
       my $symbol_id = $data[0];
       my $symbol    = $data[1];
       my $exchange  = $data[6];
#print "\n $symbol, $symbol_id, $exchange";

       $symbols .= $symbol;
       $symbol_ids{$symbol} = $symbol_id;

       if ($i!=$count-1) { $symbols .= ',';  }
     } # next symbol

if ($_dbg) {
print DBGFILE "\nsymbol list= $symbols";
print DBGFILE "\nsymbol ids = "; 
while (($k, $v) = each (%symbol_ids)) { print DBGFILE "$k: $v  "; }
}
     
     # process
     my $quote_data = getQuote($symbols);
if ($_dbg == 2) { print DBGFILE "\n$quote_data"; }

     # look for the start of the next quote
     my $qlstart = index($quote_data, "<quote-list>");
     # sanity check
     if ($qlstart == -1)
     {
        warn "\nGetQuotes: cant find start of quote-list";
	return;
     }

     for (my $i=0; $i<$count; $i++)
     {
       my $qstart = index($quote_data, "<quote>");
       if ($qstart == -1)
       {
         warn "\nGetQuotes: cant find start of quote";
	 return;
       }

       # strip off beginning
       $quote_data = substr($quote_data, $qstart);


       #parse the return data

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

       if ($price == 0 or $close == 0)
       {
         $change = '';
         $chgper = '';
       }

# this is interesting!!!  TD is returning 0.00 and 0.00% for change and change-percent!!
# is this because te markets are closed?  eg, price = close?
# also price = close, which must mean that the last price is the previous close when the markets are closed!!!
# change = (current)price - (prev)close -   %chg = change/(prev)close * 100
# so calculate it ourselves- when the market is closed, used open instead of close
if ($_marketClosed) {
     if ($price == 0 or $open == 0)
     {
       $change = 0;
       $chgper = 0;
     }
     else
     {
       $change = $price-$open;
       $chgper = ($change/$open *100);
     }
}

       
       #look for an error - this could be done first, bit little harm here
       # note: they'll still be inserted, but with empty values and 0 % change
       my $err = simpleParse($quote_data, "error");
       if ($err ne "")
       {
          print LOGFILE "\nError in quote data for $symbol from AmeriTrade: $err";
       }

print DBGFILE "\n$symbol ($symbol_ids{$symbol}): $open, $close, $low, $high, $price, $volume, $change, $chgper";

       # however, this is fatal because we wont have an id to insert!
       if (!defined($symbol_ids{$symbol}))
       {
          warn "\nCant find symbol id for $symbol! Not updating quote table!";
          print LOGFILE "\nCant find symbol id for $symbol! Not updating quote table!";
       }
       else
       {
         #insert into our end of day quote DB
         updateQuotes($symbol, $symbol_ids{$symbol}, $open, $close, $low, $high, $price, $volume, $change, $chgper);
       }

       # get ready to parse next quote
       my $qend = index($quote_data, "</quote>");
       if ($qend == -1)
       {
         warn "\nGetQuotes: cant find end of quote";
	 return;
       }

       # strip off beginning
       $quote_data = substr($quote_data, $qend);

     }  # next quote

     # start again
     $symbols = '';
     %symbol_ids = ();		# this isnt absolutely necessary, but it will save memory
  
  } # next batch

  $db->finish();
  return $rows;
}

#================================================================     
#update our end of day quote DB with all pertinent stock info
sub updateQuotes
{
  my($symbol, $sid, $open, $close, $low, $high, $price, $volume, $change, $chgper) = @_;
  
#TODO: use last trade date!!
  my $date = getDate();

  #format certain fields so they insert in the database nicely
  #ensure all our values are correctly defined - TODO: check this
  if ( !defined $open   || $open eq "")    { $open = "NULL"; }
  if ( !defined $close  || $close eq "")   { $close = "NULL"; }
  if ( !defined $low    || $low eq "")     { $low = "NULL"; }
  if ( !defined $high   || $high eq "")    { $high = "NULL"; }
  if ( !defined $price  || $price eq "")   { $price = "NULL"; }
  if ( !defined $volume || $volume eq "" ) { $volume = "NULL"; }
  if ( !defined $change || $change eq "")  { $change = 0; }
  if ( !defined $chgper || $chgper eq "")  { $chgper = NULL; }


  # insert the data into the EOD db
  my $insert_statement = "INSERT INTO EndOfDayQuotes (symbolid, symbol, date, open, close, high, low, price, volume, chng, chgper) VALUES ($sid, \"$symbol\", \"$date\", $open, $close, $high, $low, $price, $volume, $change, \"$chgper\")";
print DBGFILE "\nupdateQuotes: $insert_statement";

  #TODO: error check
  $connection->do( $insert_statement );

}

#======================================================
# return the top n biggest losers by percent change
# note: limited to $MAX_LOSERS=10 right now, with only minVolume criteria

sub biggestLosers
{
  my @losers;


  # BIG TODO: use a different date!
  my $date = getDate();

  #get the symbols from our database (date based to retrieve most current)
  my $select = "SELECT symbolId FROM EndOfDayQuotes WHERE date = \"$date\" AND volume > $MIN_VOLUME ORDER BY chgper";
#print "\nbiggestLosers: ".$select;

  my $db = $connection->prepare( $select );
  $db->execute();

  my $rows = $db->rows;
#print "\n", $rows, " fetched";

  if ($rows==0)
  {
    warn "\nFatal DB error - no rows in quotes table\n";
    return 0;
  }

  for (my $i=0; $i<$MAX_LOSERS; $i++)
  {
    my @data = $db->fetchrow_array();
    push(@losers, $data[0]);
  }

  $db->finish();
  return @losers;
}

#========================================================
# update our Holdings db with the stocks we want to buy today
sub updateHoldings
{
  my (@sid) = @_;

  # BIG TODO: use a different date!
  my $date = getDate();

  #remove previous entires (really just needed for testing)
  my $stmt = "DELETE FROM Holdings WHERE buy_date = \"$date\"";
  #my $db = $connection->prepare( $stmt );
  #$db->execute();
  $connection->do( $stmt );   # this does the 2 statements above

  # now, add initial holdings data (symbol and date)
  foreach my $sid (@sid)
  {
     #for debugging - insert symbol too
     my $symbol = symbolLookup($sid);

     my $insert_statement = "INSERT INTO Holdings (symbolId, symbol, buy_date) VALUES ($sid, \"$symbol\", \"$date\")";
#print "\nupdateHoldings: $insert_statement";

     #TODO: error check
     $connection->do( $insert_statement );
  }

}

#========================================================
# update our Holdings db with buy position of the the stocks we want to buy today
# buy price, #of share, total $ amt
# TODO: calcualate how much of each stock to buy
# TOD0: also create the sell criteria (sell price, %incr/decr, etc).  This will be an XML field.
sub updateBuyPositions
{
  my (@sid) = @_;

# positions need to be calcauted based on
# total assets = $25000 or calculate actual
# number of stocks

#also, the sell criteria is to be added at this stage

  my $date = getDate();
  my $buy_price, $buy_volume=200, $buy_total=400;

  foreach my $sid (@sid)
  {
     $buy_price = getCurrentPrice($sid);

     my $stmt = "UPDATE Holdings SET buy_price = $buy_price, buy_volume = $buy_volume, buy_total = $buy_total WHERE symbolid = $sid AND buy_date = \"$date\"";
#print "\nupdateBuyPositions: $stmt";

     #TODO: error check
     $connection->do( $stmt );
  }

}

#=======================================================
#get the most current price from ????
# options: query TD right now
#          use the previous query when all stocks were queried  <<<--- this should be fine
sub getCurrentPrice
{
  my ($sid) = @_;

  my $symbol = symbolLookup($sid);
  my $results = getQuote($symbol);
  my $price = simpleParse($results, "last");

  if ($price == '')
  {
    warn "\ngetCurrentPrice for $symbol ($sid) is not valid";
    return 0;
  }

  return $price;
}



#=============================================================================
# this is a copy of whats in Analyze.pl - should we do it here or separate module?
# we already have our TD login here

#=============================================================================
# Read from the holdings table to get todays holdings, then
# get complete history from TDAmeritrade

sub updateAnalysis
{
  #TODO
  my $date = getDate();

  #only one entry per date allowed
  my $stmt = "DELETE FROM Analysis WHERE date = \"$date\"";
  $connection->do( $stmt );


  #get the symbols from our database (date based to retrieve most current)
# TODO: check this!!!! buy date may be tomorrow!!!
  my $select = "SELECT symbol FROM Holdings WHERE buy_date = \"$date\"";
  my $db = $connection->prepare( $select );
  $db->execute();

  my $rows = $db->rows;
  if ($rows==0)
  {
    warn "\nupdate Analysis: Fatal DB error - no rows in Holdings table\n";
    return 0;
  }

  
  for (my $i=0; $i<$rows; $i++)
  {
    my @data = $db->fetchrow_array();
#print "\nupdateAnaylsis data is @data";

    my $symbolf = $data[0];

    my $quote_data = getQuote($symbolf);
if ($_dbg == 2) { print DBGFILE "\n$quote_data"; }


    #parse the return data

    my $symbol = simpleParse($quote_data, "symbol");

    #sanity check
    if ($symbol ne $symbolf)
    {
       warn "\nupdateAnalysis: discrpancy between symbols $symbolf:$symbol";
    }

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


    #ensure all our values are correctly defined - TODO: check this
    if ( !defined $close  || $close eq "")   { $close = 0; }
    if ( !defined $low    || $low eq "")     { $low = 0; }
    if ( !defined $high   || $high eq "")    { $high = 0; }
    if ( !defined $price  || $price eq "")   { $price = 0; }
    if ( !defined $volume || $volume eq "" ) { $volume = 0; }
    if ( !defined $chgper || $chgper eq "")  { $chgper = 0; }

    my $stmt = "INSERT INTO Analysis (symbol, date, close, high, low, price, volume, chgper ) VALUES (\"$symbol\", \"$date\",$close, $high, $low, $price, $volume, $chgper)"; 
if ($_dbg) { print DBGFILE "\nupdateAnalysis: $stmt"; }

    #TODO: error check
    $connection->do( $stmt );

  } #next quote

  $db->finish();
}

