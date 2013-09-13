#!/usr/bin/perl

# Get End of Day Stock exchange quotes from TD ameritrade

do '/home/dayTrader/bin/dtCommon.pl';
#do 'dtCommon.pl';

#============================================================
# get end of day quotes and determine biggest losers


#==================main=======================================
print "\n\n*** Get End of Day Quotes ".strftime('%a %b %e %Y %H:%M',localtime)." ***\n\n";

  # caution!
  if ($_ignoreMarketClosed==1) { print "\nWARNING: you are overriding check for open market!\n";  }

  #debugging select * from RealTimeQuotes
  if ($_dbg) { 
    print "\nDebugging Level $_dbg ON (to $dbgfileLocation"."$dbgfileName)\n";
  }
  open(DBGFILE, ">>".$dbgfileLocation.$dbgfileName) || die "Error opening dbgfile ($dbgfileLocation.$dbgfileName) $!";
  print DBGFILE "\n\n*** Get End of Day Quotes ".strftime('%a %b %e %Y %H:%M',localtime)." ***\n\n";

  #global log file
  open(LOGFILE, ">>".$logfileLocation."dayTrader.log") || die "Error opening logfile ($logfileLocation.dayTrader.log) $!";
  print LOGFILE "\n\n*** Get End of Day Quotes ".strftime('%a %b %e %Y %H:%M',localtime)." ***\n\n";

  #connect to our database
  $connection = DBI->connect( "DBI:mysql:database=$dbname;host=localhost", "root", "sal", {'RaiseError' => 1} );

#for debuging
#$connection->trace(1);

  #before we do anything, lets make sure the market is open today...
  # (for developing, we can set the ignore flag...)
  my $isOpen = isMarketOpen();
  if (!$_ignoreMarketClosed) {
    if ($isOpen != 1) {
      print "\nMarket is not open today.  Bye.\n";
      exit;
    }
  }

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
  print "\nTodays biggest losers are: "; 
  for (my $i=0; $i<$MAX_LOSERS; $i++) { print " ".symbolLookup($biggest_losers[$i]); } print "\n";

  print LOGFILE "\nTodays biggest losers are:";
  for (my $i=0; $i<$MAX_LOSERS; $i++) { print LOGFILE " ".symbolLookup($biggest_losers[$i]); } print "\n";

if ($_dbg) {
print DBGFILE "\nTodays biggest losers are: @biggest_losers\n";
for (my $i=0; $i<$MAX_LOSERS; $i++) { print DBGFILE " ".symbolLookup($biggest_losers[$i]); } print "\n"; }

  # before we do any new transations,
  # sell all of our holdings now (if they didnt sell during the day)
  # TODO: this will be an IB routine - just include it here for analysis
  print "\nPreparing to sell remaining holdings...\n";
  sellHoldings();

  # for all the stocks we just sold, calculate net standing
  calculateNet();

  #update our holdings table with stocks to buy today (just symbol and date)
  print "\nUpdating holdings...\n";
  updateHoldings(@biggest_losers);

  # calcualate buy positions and update DB
  updateBuyPositions(@biggest_losers);

  # execute orders (and update buy price, if different than calculated)

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
# get 100 at a time (300 max)
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

       
       #look for errors - this could be done first, bit little harm here
       # dont write to DB
       my $symerr = 0;

       #TODO: also check for result (before the list!!!
       my $err = simpleParse($quote_data, "error");
       if ($err ne "")
       {
          print LOGFILE "\nError in quote data for $symbol from AmeriTrade: $err";
          $symerr = 1;
       }

if ($_dbg) { print DBGFILE "\n$symbol ($symbol_ids{$symbol}): $open, $close, $low, $high, $price, $volume, $change, $chgper"; }

       # this is realbad because we wont have an id to insert!
       if (!defined($symbol_ids{$symbol}))
       {
          warn "\nCant find symbol id for $symbol! Not updating quote table!";
          print LOGFILE "\nCant find symbol id for $symbol! Not updating quote table!";
          $symerr = 1;
       }

       # now check the contents of the return data TODO: why is this happening???
       if ($price eq "")
       {
          print LOGFILE "\nNULL price for $symbol! Not updating quote table!";
          $symerr = 1;
       }

       if ($open eq "")
       {
          print LOGFILE "\nNULL open for $symbol! Not updating quote table!";
          $symerr = 1;
       }

       # OK to write
       if ($symerr==0)
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
if ($_dbg==2) { print DBGFILE "\nupdateQuotes: $insert_statement"; }

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
# TODO: include current price (which could be updated when the stock is actually bought) here?
# its done in updateBuyPositions now
sub updateHoldings
{
  my (@sid) = @_;

  # BIG TODO: use a different date!
  my $date = getDate();

  #remove previous entires (really just needed for testing)
  my $stmt = "DELETE FROM Holdings WHERE buy_date = \"$date\"";
  $connection->do( $stmt );

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
# TODO: volume is declared as INT so only full shares are bought now...
sub updateBuyPositions
{
# get latest capital from DB
  my $stmt = "SELECT price from DailyNet ORDER BY date DESC LIMIT 1";
  my $db = $connection->prepare( $stmt );
  $db->execute();
  if ($db->rows != 1)
  {
     warn "\nCant get starting capital value from DailyNet!\n";
     exit;
  }
  my @data = $db->fetchrow_array();
  $db->finish();
  my $capital = $data[0];
print "\nStarting capital for today is \$$capital";

  my (@sid) = @_;

# positions need to be calcauted based on
# total assets and calculate actual number of stocks

# also, the sell criteria is to be added at this stage

  # BIG TODO:  timestamp or last_date
  my $date = getDate();


  my $buy_price, $buy_volume;

  # how much we can buy of each stock
  my $buy_total = $capital/$MAX_LOSERS;

  foreach my $sid (@sid)
  {
     $buy_price = getEODPrice($sid);

     # TODO: adjusted for full shares
     $buy_volume = int ($buy_total/$buy_price);
     $buy_total = $buy_volume * $buy_price;

     my $stmt = "UPDATE Holdings SET buy_price = $buy_price, buy_volume = $buy_volume, buy_total = $buy_total WHERE symbolid = $sid AND buy_date = \"$date\"";
#print "\nupdateBuyPositions: $stmt";

     #TODO: error check
     $connection->do( $stmt );
  }

}


#=======================================================
#get the most current price from TD now
sub getCurrentPrice_fromTD
{
  my ($sid) = @_;

  my $symbol = symbolLookup($sid);
  my $results = getQuote($symbol);
  my $price = simpleParse($results, "last");

  if ($price == '')
  {
    print LOGFILE "\ngetCurrentPrice_fromTD for $symbol ($sid) is not valid";
if ($_dbg) { print DBGFILE "\ngetCurrentPrice_fromTD for $symbol ($sid) is not valid"; }

    return 0;
  }

  # round to 3 digits
  $price = int($price*1000 + 0.5)/1000;

  return $price;
}

#=======================================================
#get the most current price from the last RealTime Update
# could just use symbol - id is one more step of indirection

sub getCurrentPrice
{
  my ($sid) = @_;

  my $symbol = symbolLookup($sid);

  my $date = getDate();

  my $stmt = "SELECT price from RealTimeQuotes where symbol = \"$symbol\" and date LIKE \"$date\%\" ORDER BY date DESC LIMIT 1";
#print "\n$stmt";

  my $db = $connection->prepare($stmt);
  $db->execute();

  if ($db->rows==0)
  {
    warn "\nGetQuotes::getCurrentPrice(): cant find $symbol in RealTime table - fallback to TD";
    print LOGFILE"\nGetQuotes::getCurrentPrice(): cant find $symbol in RealTime table - fallback to TD";
if ($_dbg) { print DBGFILE"\nGetQuotes::getCurrentPrice(): cant find $symbol in RealTime table - fallback to TD"; }

    # fallback to TD - this shouldnt be necessary!!!  TODO
    return getCurrentPrice_fromTD($sid);
  }

  my @data = $db->fetchrow_array();
  my $price = $data[0];

#print "\ngetCurrentPrice for $symbol = $price";

  return $price;
}

#=======================================================
#get the End of the Day price for this symbol from our DB
sub getEODPrice
{
  my ($sid) = @_;

  my $date = getDate();

  my $symbol = symbolLookup($sid);
 
  my $stmt = "SELECT price from EndOfDayQuotes where symbol = \"$symbol\" AND date = \"$date\"";
  my $db = $connection->prepare( $stmt );
  $db->execute();

  if ($db->rows==0)
  {
    warn "\nGetQuotes::getEODPrice(): cant find $symbol in EOD table";
    return 0;
  }

  my @data = $db->fetchrow_array();
  my $price = $data[0];
  
  $db->finish();
  
#print "\nGetEODPrice for $symbol ($sid): $price";

  return $price;
}

#=============================================================================
# this is a copy of whats in Analyze.pl - should we do it here or separate module?
# we already have our TD login here

#=============================================================================
# Read from the holdings table to get todays holdings, then
# get complete history from TDAmeritrade
# do this for yesterdays holdings first, then todays holdings
# we could have dups if the same stock loses both days
#
# aslo calculate and update net standing

sub updateAnalysis
{
  my $date = getDate();

  #only one set of entries per date allowed
  my $stmt = "DELETE FROM Analysis WHERE date = \"$date\"";
  $connection->do( $stmt );

  # get yesterdays Holdings symbols w/todays data
  my @symbols = getYesterdaysLosers();
  updateAnalysisTable(@symbols);

  #get todays (current) losers
  @symbols = getTodaysLosers();
  updateAnalysisTable(@symbols);

}

#====================================================================
sub getTodaysLosers()
{
  my @symbols;

  my $date = getDate();

  #get the symbols from our database (date based to retrieve most current)
  my $select = "SELECT symbol FROM Holdings WHERE buy_date = \"$date\"";
  my $db = $connection->prepare( $select );
  $db->execute();

  my $rows = $db->rows;
  if ($rows==0)
  {
    warn "\nupdate Analysis: Fatal DB error - no rows in Holdings table\n";
    return @symbols;
  }
  
  for (my $i=0; $i<$rows; $i++)
  {
    my @data = $db->fetchrow_array();
#print "\nupdateAnaylsis data is @data";

    push(@symbols, $data[0]);
  }

  $db->finish();

  return @symbols;
}

#=======================================================================
# get the last 2 days of holdings - this depends on the fact that exaxtly MAX_LOSERS are
# added to the DB each day!!!!
# TODO:a better way would be to calculate 'yesterday' which would be the previous market day,
# accounting for weekends and holidays.
#
# this will return an empty set the very first time it is run
sub getYesterdaysLosers()
{
  my @symbols;

  #get the symbols from our database (date based to retrieve most current)
  my $select = "SELECT symbol FROM Holdings order by buy_date DESC LIMIT $MAX_LOSERS,$MAX_LOSERS";
  my $db = $connection->prepare( $select );
  $db->execute();

  my $rows = $db->rows;
  if ($rows==0)
  {
    warn "\nGetQuotes::getYesterdaysLosers() no rows in Holdings table\n";
    return @symbols;
  }
  
  for (my $i=0; $i<$rows; $i++)
  {
    my @data = $db->fetchrow_array();
#print "\nupdateAnaylsis data is @data";

    push(@symbols, $data[0]);
  }

  $db->finish();

  return @symbols;
}

#=======================================================================
# for each symbol (in the Holdings table for yesterday and today)
# retrieve the most current info from TD and store
# TODO - add more fields
 
sub updateAnalysisTable
{
  my (@symbols) = @_;

  my $date = getDate();

  foreach my $symbol (@symbols)
  {
    my $quote_data = getQuote($symbol);
if ($_dbg==2) { print DBGFILE "\n$quote_data"; }

    #parse the return data

    my $psymbol = simpleParse($quote_data, "symbol");

    #sanity check
    if ($psymbol ne $symbol)
    {
       warn "\nupdateAnalysis: discrepancy between symbols $psymbolf:$symbol";
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

  } #next symbol
}

#=============================================================
# this is only included now for Analysis

#sell everything that hasnt been sold today
sub sellHoldings
{

if ($_dbg) { print DBGFILE "\nSelling all remaining holdings..."; }

  my $stmt = "SELECT symbol, symbolId, buy_price FROM Holdings where sell_date is NULL";
  my $db = $connection->prepare( $stmt );
  $db->execute();

  my $rows = $db->rows;

  for (my $i=0; $i<$rows; $i++)
  {
    my @data = $db->fetchrow_array();

    my $symbol = $data[0];
    my $symbolid = $data[1];
    my $buy_price = $data[2];

    # get current price and date (TODO: from last RealTime update) - wont need id
    my $price = getCurrentPrice($symbolid);
    my $date = timeStamp();

#print "\nsell Holdings $symbol at $price, bought at $buy_price";

    #update the Holdings Table with this info
    if (updateSellPositions($symbol, $price, $date))
    {
      my $diff = $price-$buy_price;

      my $net = "EVEN";
      if ($diff>0) { $net = "GAIN"; }
      if ($diff<0) { $net = "LOSS"; }

if ($_dbg) { print DBGFILE "\n".timeStamp().": *** SELL $symbol at a $net of \$$diff ($price:$buy_price) per share at $date ***"; }
print LOGFILE "\n".timeStamp().": *** SELL $symbol at a $net of \$$diff ($price:$buy_price) per share at $date ***";
    } #only log if updated

  }  # next unsold

  $db->finish();2498.300

}


#==========================================================
#TODOadd to common - used by RealTime, too

sub updateSellPositions()
{
  my ($symbol, $price, $date) = @_;

     $stmt = "UPDATE Holdings SET sell_price = $price, sell_date= \"$date\" WHERE symbol = \"$symbol\" AND sell_date is NULL";

if ($_dbg) { print DBGFILE "\nupdateSellPositions: $stmt"; }

     my $rows_changed = $connection->do( $stmt );

if ($_dbg) {
if ($rows_changed == 0) { print DBGFILE "\nnot updating $symbol because its already sold"; }
}

  return $rows_changed;
}

#=============================================================================
#for the most recent updates to Holdings, the net profit/loss field (now criteria)
# will be empty.  Update the sell total, and calculate the net. Add this to our daily net table (TODO)

sub calculateNet
{

  my $cum_net = 0;

  my $stmt = "SELECT id, buy_volume, buy_total, sell_price FROM Holdings WHERE criteria IS NULL";
  my $db = $connection->prepare( $stmt );
  $db->execute();

  my $rows = $db->rows;
if ($rows != $MAX_LOSERS) { warn "\nsomething is fishy in Holdings... $rows retrieved"; }

  for (my $i=0; $i<$rows; $i++)
  {
    my @data = $db->fetchrow_array();

    my $id = $data[0];
    my $buy_volume = $data[1];
    my $buy_total = $data[2];
    my $sell_price = $data[3];

    if ($sell_price != 0)
    {
      my $sell_total = $buy_volume * $sell_price;
      my $net =  $sell_total - $buy_total;

      #                                                       \/   temp text field!!!
      my $stmt = "UPDATE Holdings SET sell_total=$sell_total, criteria=\"$net\" WHERE id=$id";
#print "\n$stmt";
      $connection->do($stmt);

      $cum_net += $net;
    }
    else
    {
       warn "\ncalculateNet - sell price for Holding id $id is NULL";
    }

  } # next Holding

  $db->finish();

  #update daily table

  # get latest capital record from DB
  my $stmt = "SELECT id, price from DailyNet ORDER BY date DESC LIMIT 1";
  my $db = $connection->prepare( $stmt );
  $db->execute();
  if ($db->rows != 1)
  {
     warn "\nCant get starting capital value from DailyNet!\n";
     exit;
  }
  my @data = $db->fetchrow_array();
  $db->finish();
  my $id          = $data[0];
  my $start_price = $data[1];
#print "\nStarting capital for today is \$$capital";

  #$stmt = "INSERT INTO DailyNet (date, price) VALUES (\"$date\", $cum_net)";
  $stmt = "UPDATE DailyNet SET net=$cum_net WHERE id=$id;";
#print "\n$stmt";
  $connection->do($stmt);

  my $date = getDate();
  # add new record
  $stmt = "INSERT INTO DailyNet (date, price) VALUES (\"$date\", $start_price+$cum_net)";
#print "\n$stmt";
  $connection->do($stmt);

}

