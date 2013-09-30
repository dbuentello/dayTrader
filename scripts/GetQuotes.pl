#!/usr/bin/perl

# Get End of Day Stock exchange quotes from TD ameritrade

#do '/home/dayTrader/bin/dtCommon.pl';
do './dtCommon.pl';
do './UpdateSellPositions.pl';

#============================================================
# get end of day quotes and determine biggest losers


#==================main=======================================
print "\n\n*** Get End of Day Quotes ".strftime('%a %b %e %Y %H:%M',localtime)." ***\n\n";

  getCommandLineArgs();

  # open DB and log files
  Initialize("Get End of Day Quotes");

  # connect to TD Ameritrade
  $sessionId = TDlogin();
#print "\nreturned sid = $sessionId";
  if (length($sessionId) == 0)
  {
     warn "\nFATAL: Invalid login\n";
     exit;
  }

  # get EndOfDay Quotes and update EODQuotes table (using last-trade-date and last trade price)
if ($_simulate == 0) {
  print "\nGetting todays quotes...\n";
  getEODQuotes();
}


  # before we do any new transations,
  # sell all of our holdings now (if they didnt sell during the day)
  # TODO: this will be an IB routine - just include it here for analysis
  print "\nPreparing to sell remaining holdings...\n";
  sellHoldings();

  # for all the stocks we just sold, calculate net standing
  calculateNet();


  # now we can calculate todays biggest losers
  my @biggest_losers = biggestLosers();
  my $nlosers = @biggest_losers;
  if ($nlosers < $MAX_LOSERS)
  {
     print "\nGetQuotes() FATAL- cant retrieve biggest losers (only $nlosers found)";
     print LOGFILE "\nGetQuotes() FATAL- cant retrieve biggest losers (only $nlosers found)";
     goto QUIT;
  }

  #update our holdings table with stocks to buy today (just symbol and date)
  print "\nUpdating holdings...\n";
  updateHoldings(@biggest_losers);

  # calcualate todays buy positions and update DB
  updateBuyPositions(@biggest_losers);

  # execute orders (and update buy price, if different than calculated)



QUIT:
  #logout from TD Ameritrade
  TDlogout();

  $connection->disconnect;

  close LOGFILE;
  close DBGFILE;

print "\n**end**\n";


#=================end main=====================================




#================================================================
# get todays quotes and update EOD database
# returns number of rows inserted, or 0 on error

# get 100 at a time (300 max)

sub getEODQuotes
{
my $MAX_QUOTES = 100;

  # clear the table first so we only have one entry per date
  my $date = getCurrentTradeDate();
  my $stmt = "DELETE FROM EndOfDayQuotes WHERE DATE(date) = \"$date\"";
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
        warn "\nFATAL GetQuotes: cant find start of quote-list";
	return 0;
     }

     for (my $i=0; $i<$count; $i++)
     {
       my $qstart = index($quote_data, "<quote>");
       if ($qstart == -1)
       {
         warn "\nFATAL GetQuotes: cant find start of quote";
	 return 0;
       }

       # strip off beginning
       $quote_data = substr($quote_data, $qstart);


       #parse the return data

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
          print LOGFILE "\nError in quote data for $symbol from AmeriTrade: $err";
          $symerr = 1;
       }

if ($_dbg) { print DBGFILE "\n$symbol ($symbol_ids{$symbol}): $date, $open, $close, $low, $high, $price, $volume, $change, $chgper"; }
if ($_dbg==2) { print DBGFILE ", $bid, $ask, $bidsize, $asksize, $yearlo, $yearhi"; }

       # this is realbad because we wont have an id to insert!
       if (!defined($symbol_ids{$symbol}))
       {
          warn "\nCant find symbol id for $symbol! Not updating quote table!";
          print LOGFILE "\nCant find symbol id for $symbol! Not updating quote table!";
          $symerr = 1;
       }

#TODO - check for a legit date - real important!

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
         #insert into our end of day quote DB using last-trade-date
         updateQuotes($symbol, $symbol_ids{$symbol}, $date, $open, $close, $low, $high, $price, $volume, $change, $chgper, $bid, $ask, $bidsize, $asksize, $yearlo, $yearhi);
       }

       # get ready to parse next quote
       my $qend = index($quote_data, "</quote>");
       if ($qend == -1)
       {
         warn "\nGetQuotes: cant find end of quote";
	 return 0;
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
  my($symbol, $sid, $date, $open, $close, $low, $high, $price, $volume, $change, $chgper) = @_;

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
  if ( !defined $bid    || $bid eq "")     { $bid = NULL; }
  if ( !defined $ask    || $ask eq "")     { $ask = NULL; }
  if ( !defined $bidsize || $bidsize eq "") { $bidsize = NULL; }
  if ( !defined $asksize || $asksize eq "") { $asksize = NULL; }
  if ( !defined $yearlo || $yearlo eq "")  { $yearlo = NULL; }
  if ( !defined $yearhi || $yearhi eq "")  { $yearhi = NULL; }

  # insert the data into the EOD db
  my $insert_statement = "INSERT INTO EndOfDayQuotes (symbolid, symbol, date, open, close, high, low, price, volume, chng, chgper, bid, ask, bidsize, asksize, yearlo, yearhi) VALUES ($sid, \"$symbol\", \"$date\", $open, $close, $high, $low, $price, $volume, $change, \"$chgper\", $bid, $ask, $bidsize, $asksize, $yearlo, $yearhi)";
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

  my $date = getCurrentTradeDate();
  if ($date eq "")
  {
    warn "\nGetQuotes::biggestLosers() FATAL- cant get Current Trade Date";
    print LOGFILE "\nGetQuotes::biggestLosers() FATAL- cant get Current Trade Date";
    return 0;
  }

  #get the symbols from our database (date based to retrieve most current)
  my $select = "SELECT symbolId FROM EndOfDayQuotes WHERE DATE(date) = \"$date\" AND volume > $MIN_VOLUME AND price > $MIN_PRICE ORDER BY chgper LIMIT $MAX_LOSERS";
if ($_dbg==2) { "\nbiggestLosers: ".$select; }

  #TODO error check
  my $db = $connection->prepare( $select );
  $db->execute();

  my $rows = $db->rows;

  if ($rows<$MAX_LOSERS)
  {
    warn "\nGetQuotes::biggestLosers() FATAL- $rows rows returned from EndOfDayQuotes table";
    print LOGFILE "\nGetQuotes::biggestLosers() FATAL- $rows rows returned from EndOfDayQuotes table";
    return @losers;   # empty
  }

  for (my $i=0; $i<$MAX_LOSERS; $i++)
  {
    my @data = $db->fetchrow_array();
    push(@losers, $data[0]);
  }

  $db->finish();

  # logging
  print "\nTodays biggest losers are: "; 
  for (my $i=0; $i<$MAX_LOSERS; $i++) { print " ".symbolLookup($losers[$i]); } print "\n";

  print LOGFILE "\nTodays biggest losers are:";
  for (my $i=0; $i<$MAX_LOSERS; $i++) { print LOGFILE " ".symbolLookup($losers[$i]); } print "\n";

if ($_dbg) {
print DBGFILE "\nTodays biggest losers are: @biggest_losers\n";
for (my $i=0; $i<$MAX_LOSERS; $i++) { print DBGFILE " ".symbolLookup($losers[$i]); } print "\n"; }


  return @losers;
}

#========================================================
# update our Holdings db with the stocks we want to buy today
# TODO: include current price (which could be updated when the stock is actually bought) here?
# its done in updateBuyPositions now
# input is symbol list of biggest losers
sub updateHoldings
{
  my (@sid) = @_;

  my $date = getCurrentTradeDate();
#print "\nupdateHoldings for $date";

  #remove previous entires
  my $stmt = "DELETE FROM Holdings WHERE DATE(buy_date) = \"$date\"";
  $connection->do( $stmt );
#print $stmt;

  # now, add initial holdings data (symbol and date)
  foreach my $sid (@sid)
  {
     #for debugging - insert symbol too
     my $symbol = symbolLookup($sid);

     my $stmt = "INSERT INTO Holdings (symbolId, symbol, buy_date) VALUES ($sid, \"$symbol\", \"$date\")";
#print "\nupdateHoldings: $stmt";

     #TODO: error check
     if ($connection->do($stmt) != 1)
     {
         print LOGFILE "\nFATAL ERROR: $stmt FAILED";
     }
  }

}

#========================================================
# update our Holdings db with buy position of the the stocks we want to buy today
# buy price, #of share, total $ amt
# calcualate how much of each stock to buy
# volume is declared as INT so only full shares are bought
# TOD0: also create the sell criteria (sell price, %incr/decr, etc).  This will be an XML field.
# input is symbol list of biggest losers
sub updateBuyPositions
{

  # get latest capital from DB
  my $stmt = "SELECT price from DailyNet ORDER BY date DESC LIMIT 1";
  my $db = $connection->prepare( $stmt );
  $db->execute();
  if ($db->rows != 1)
  {
     warn "\nCant get starting capital value from DailyNet!\n";
     return;
  }
  my @data = $db->fetchrow_array();
  $db->finish();
  my $capital = $data[0];
#print "\nStarting capital for today is \$$capital";


  my (@sid) = @_;

# positions need to be calcauted based on
# total assets and calculate actual number of stocks

# also, the sell criteria is to be added at this stage

  my $date = getCurrentTradeDate();


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
# get the End of the Day price for this symbol from our DB
# TODO: could use symbol instead of symbolid
sub getEODPrice
{
  my ($sid) = @_;

  my $date = getCurrentTradeDate();

  my $symbol = symbolLookup($sid);
 
  my $stmt = "SELECT price from EndOfDayQuotes where symbol = \"$symbol\" AND DATE(date) = \"$date\"";
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



#=============================================================
# this is only included now for Analysis - it will be an IB execute module

#sell everything that hasnt been sold today
sub sellHoldings
{

if ($_dbg) { print DBGFILE "\nSelling all remaining holdings..."; }

  my $stmt = "SELECT symbol, symbolId, buy_price FROM Holdings where sell_date is NULL";
  my $db = $connection->prepare( $stmt );
  $db->execute();

  my $rows = $db->rows;
  if ($rows == 0)
  {
    print LOGFILE "\nThere are no remaining Holdings to sell at the end of the day";
print "\nThere are no remaining Holdings to sell at the end of the day";
    return;
  }

  for (my $i=0; $i<$rows; $i++)
  {
    my @data = $db->fetchrow_array();

    my $symbol = $data[0];
    my $symbolid = $data[1];
    my $buy_price = $data[2];

    # get current price and date
    my $price = getEODPrice($symbolid);
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

  $db->finish();

}


#=============================================================================
# for the most recent updates to Holdings, the net profit/loss field (now criteria)
# will be empty.  Update the sell total, and calculate the net. Add this to our daily net table
# must be done before todays losers are determined

sub calculateNet
{

  my $cum_net = 0;
  my $cum_vol = 0;

  #	note: change the criteria field to net					\/ \/
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
      $cum_vol += $buy_volume;
    }
    else
    {
       warn "\ncalculateNet - sell price for Holding id $id is NULL";
    }

  } # next Holding

  $db->finish();

  # update daily table with net for today

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

  # update our net for today
  $stmt = "UPDATE DailyNet SET net=$cum_net, totalVolume=$cum_vol WHERE id=$id;";
#print "\n$stmt";
  $connection->do($stmt);

  my $date = getNextTradeDate();
  # add new record for starting point for tomorrow

# we can do it two ways - cumulative or start fresh every day
# TEST--- always start at 10000
  $start_price = 10000; $cum_net=0;
# TEST---

  $stmt = "INSERT INTO DailyNet (date, price) VALUES (\"$date\", $start_price+$cum_net)";
#print "\n$stmt";

  $connection->do($stmt);

}


