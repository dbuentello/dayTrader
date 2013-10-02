#!/usr/bin/perl

# Get Real Time Stock exchange quotes from TD ameritrade

# right now, they are queried one at a time - could query in a batch for efficiency, or stream in binary

require './dtCommon.pl';
require './UpdateSellPositions.pl';


#==================main=======================================
print "\n\n*** Get RealTime Quotes ".strftime('%a %b %e %Y %H:%M',localtime)." ***\n\n";

  getCommandLineArgs();

  # open DB and log files
  Initialize("Get Real Time Quotes");

  # connect to TD Ameritrade
  $sessionId = TDlogin();
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


  TDlogout();

  $connection->disconnect;

  close LOGFILE;

  close DBGFILE;

print "\n**end**\n";

#=================end main=====================================



#==============================================================
# get yesterdays (eg most recent) losers from our Holdings DB

sub getLosers()
{
  my @losers;

  my $date = getPreviousTradeDate();
  my $stmt = "SELECT symbol from Holdings WHERE DATE(buy_date) = \"$date\"";

  # this will select the most recent entries in our Holding table - TODO: be sure this is correct!
  #my $stmt = "SELECT symbol from Holdings ORDER by buy_DATE DESC LIMIT $MAX_LOSERS";
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
  # TODO: check the IGNORE - its based on symbol and Last-Trade-date -- what happens if volume, ask or some other param changes?
  my $insert_statement = "INSERT IGNORE INTO RealTimeQuotes (symbol, date, price, volume) VALUES (\"$symbol\", \"$date\", $price, $volume)";
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
  # 3: ...(implement trailing sell limit)
  # 4: ...
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
      if (updateSellPositions($symbol, $price, $date) != 0)
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
  my $date = getPreviousTradeDate();
  my $select = "SELECT buy_price FROM Holdings WHERE symbol = \"$symbol\" AND DATE(buy_date) = \"$date\"";

  #my $select = "SELECT buy_price FROM Holdings WHERE symbol = \"$symbol\" ORDER BY buy_date DESC LIMIT 1";
if ($_dbg) { print DBGFILE "\ngetBuyPrice: ".$select; }

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



