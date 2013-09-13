#!/usr/bin/perl

# Get Real Time Stock exchange quotes from TD ameritrade

# right now, they are queried one at a time - could query in a batch for efficiency, or stream in binary

do '/home/dayTrader/bin/dtCommon.pl';
#do 'dtCommon.pl';

#==================main=======================================
print "\n\n*** Get RealTime Quotes ".strftime('%a %b %e %Y %H:%M',localtime)." ***\n\n";

  # caution!
  if ($_ignoreMarketClosed==1) { print "\nWARNING: you are overriding check for open market!\n";  }

  #debugging
  if ($_dbg) { 
    print "\nDebugging Level $_dbg ON (to $dbgfileLocation"."$dbgfileName)\n";
  }
  open(DBGFILE, ">>".$dbgfileLocation.$dbgfileName) || die "Error opening dbgfile ($dbgfileLocation.$dbgfileName) $!";
  print DBGFILE "\n\n*** Get RealTime Quotes ".strftime('%a %b %e %Y %H:%M',localtime)." ***\n\n";

  #global log file
  open(LOGFILE, ">>".$logfileLocation."dayTrader.log") || die "Error opening logfile ($logfileLocation.dayTrader.log) $!";
  print LOGFILE "\n\n*** Get RealTime Quotes ".strftime('%a %b %e %Y %H:%M',localtime)." ***\n\n";

  #connect to our database
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

  $sessionId = login();
#print "\nreturned sid = $sessionId";
  if (length($sessionId) == 0)
  {
     warn "\nRealTime: Invalid login\n";
     exit;
  }

  #these are the ones we're interested in...
  my @losers = &getLosers();				#(& needed to avoid forward prototype error)
if ($_dbg) { print DBGFILE "\nRealTime: losers are  @losers"; }

  print "\nRetrieving Real Time data for todays biggest losers (@losers)";

  foreach my $loser (@losers)
  {
    my $data = getQuote($loser);
    realTimeUpdate($data);

    shouldWeSell($data);
  }


  logout();

  $connection->disconnect;

  close LOGFILE;

  close DBGFILE;

print "\n**end**\n";

#=================end main=====================================



#==============================================================
# get todays losers from our Holdings DB

sub getLosers()
{
  my @losers;

  my $date = getDate();
  my $stmt = "SELECT symbol from Holdings WHERE buy_date = \"$date\"";
if ($_dbg) { print DBGFILE "\nRealTime::getLosers(): $stmt"; }

  my $db = $connection->prepare($stmt);
  $db->execute();

  my $rows = $db->rows;
#print "\n", $rows, " fetched";

  if ($rows==0)
  {
    warn "\nRealTime::getLosers() Fatal DB error - no rows in Holdings table\n";
    print LOGFILE "\nRealTime::getLosers() Fatal DB error - no rows in Holdings table\n";
    return @losers;
  }

  my @data;
  while ( @data  = $db->fetchrow_array())
  {
     push(@losers,$data[0]);
  }

  $db->finish();

  return @losers;
}

#==============================================================
#update the realtime table

sub realTimeUpdate
{
  my ($data) = @_;

  #TODO use symbolId?
  my $symbol = simpleParse($data, "symbol");
  if ($symbol eq "")
  {
     print LOGFILE "\n!!RealTime::realTimeUpdate() NULL symbol !!\n";
     return;
  }

  my $price  = simpleParse($data, "last");
  my $volume = simpleParse($data, "last-trade-size");
  my $date   = simpleParse($data, "last-trade-date");

 #ensure all our values are correctly defined
  if ( $price eq "")   { $price = "NULL"; }
  if ( $volume eq "" ) { $volume = "NULL"; }
  if ( $date eq "")    { $date = "NULL"; }


  # insert the data into the db
  my $insert_statement = "INSERT INTO RealTimeQuotes (symbol, date, price, volume) VALUES (\"$symbol\", \"$date\", $price, $volume)";
#if ($_dbg) { "\nRealTime::realTimeUpdate(): $insert_statement"; }

  #TODO: error check
  $connection->do( $insert_statement );
}


#=======================================================
# determine if we should sell the stock now
#
# input is the latest real time trade data for a symbol
#
# TODO: +/- increment now... can have separate increase/decrease

sub shouldWeSell
{
  my ($data) = @_;

  #TODO use symbolId?
  my $symbol = simpleParse($data, "symbol");
  if ($symbol eq "")
  {
     print LOGFILE "\n!!RealTime::shouldWeSell() NULL symbol !!\n";
     return;
  }

  my $price  = simpleParse($data, "last");
  my $volume = simpleParse($data, "last-trade-size");
  my $date   = simpleParse($data, "last-trade-date");

  # an array of criteria
  # 1: percent increase
  # 2: percent decrease
  # 3: ...
  my @crit = getSellCriteria();
  my $per_incr  = shift(@crit);
  my $per_decr  = shift(@crit);

  # get the buy price from Holdings table
  my $buy_price = getBuyPrice($symbol);

  my $sell_price_diff = $buy_price * $per_incr;
#print "\nBuy/Sell $symbol: current: $price, sell at +/-: $sell_price_diff; we bought at: $buy_price ($per_incr% incr)";

  # round real time price to 3 digits
  $price = int($price*1000 + 0.5)/1000;

  # sell if it goes up or down       #per share
  if (($price >= $buy_price + $sell_price_diff) || ($price < $buy_price - $sell_price_diff))
  {
      #update the Holdings Table with this info
      if (updateSellPositions($symbol, $price, $date))
      {
        my $net = "EVEN";
        if ($price > $buy_price + $sell_price_diff) { $net = "GAIN"; }
        if ($price < $buy_price - $sell_price_diff) { $net = "LOSS"; }

        print "\n".timeStamp().": *** SELL $symbol at a $net of \$$sell_price_diff ($price : $buy_price) at $date ***"; 
        print LOGFILE "\n".timeStamp().": *** SELL $symbol at a $net of \$$sell_price_diff ($price : $buy_price)  at $date ***";
if ($_dbg) { print DBGFILE "\n".timeStamp().": *** SELL $symbol at a $net of \$$sell_price_diff ($price : $buy_price) at $date ***"; }

      } #only log if updated

  } # we should sell

}

#================================================================
# get the sell criteria from the criteria file (TODO). This is an
# XML string defining the criteria.  It is the same for all stocks
# to start it will be sell when price increases or decreases by 2%
# over/under the buy_price

# output is an array of criteria
  # 1: percent increase
  # 2: percent decrease
  # 3: ...

sub getSellCriteria
{
  my @crit;

  push(@crit, .02);   # % increase
  push(@crit, .02);   # % decrease

#print "\nsell criteria is @crit";

  return @crit;

}

#============================================================================
# get the buy price for this symbol from the Holdings Table.  Get the last
# holdings by previous date BIG TODO.  Now, return the most recent based on date
# (which may be good enough)

sub getBuyPrice
{
  my ($symbol) = @_;


#get the buy price and criteria from the Holdings table from the day before
  # BIG TODO: use a different date!
  #my $date = getPreviousDate();
  #my $date = getDate();
  #my $select = "SELECT buy_price FROM Holdings WHERE symbol = \"$symbol\" AND buy_date = \"$date\"";

  my $select = "SELECT buy_price FROM Holdings WHERE symbol = \"$symbol\" ORDER BY buy_date DESC LIMIT 1";
#print "\ngetBuyPrice: ".$select;

  my $db = $connection->prepare( $select );
  $db->execute();

  my $rows = $db->rows;

  if ($rows!=1)
  {
    warn "\ngetBuyPrice() cant retrieve buy price from Holdings for $symbol";
    print LOGFILE "\getBuyPrice() cant retrieve buy price from Holdings for $symbol";
    return 0;
  }

  my @data = $db->fetchrow_array();
  $db->finish();

  $buy_price = $data[0];

#print "\ngetBuyPrice for $symbol = $buy_price";

  return $buy_price;

}

#==========================================================
#TODO: add volume total
# ?? use symbolId or symbol?
# note: this assumes that all other sell orders for this symbol have already been executed,
# eg there are only unique symbols with a null sell_date
# it wont update if already sold  TODO: keep track if it keeps rising?

sub updateSellPositions()
{
  my ($symbol, $price, $date) = @_;

#     $stmt = "UPDATE Holdings SET sell_price = $price, buy_volume = $buy_volume, buy_total = $buy_total WHERE symbolid = $sid AND buy_date = \"$date\"";
     $stmt = "UPDATE Holdings SET sell_price = $price, sell_date= \"$date\" WHERE symbol = \"$symbol\" AND sell_date is NULL";

if ($_dbg) { print DBGFILE "\nupdateSellPositions: $stmt"; }

     my $rows_changed = $connection->do( $stmt );

if ($_dbg) {
if ($rows_changed == 0) { print DBGFILE "\nnot updating $symbol because its already sold"; }
}

  return $rows_changed;
}


