#!/usr/bin/perl

# Get Real Time Stock exchange quotes from TD ameritrade

# right now, they are queried one at a time - could query in a batch for efficiency, or stream in binary

do 'dtCommon.pl';


#==================main=======================================
print "\n** Get Real Time Quotes from TD Ameritrade **\n";

  #debugging
  if ($_dbg) { 
    print "\nDebugging Level $_dbg ON (to $dbgfileLocation"."$dbgfileName)\n";
    if ($_marketClosed) {print "\n*** market is closed:  using calculated \%Chg\n"; print DBGFILE "\n*** market is closed:  using calculated \%Chg\n"; }
  }
  open(DBGFILE, ">>".$dbgfileLocation.$dbgfileName) || die "Error opening dbgfile!";

  #global log file
  open(LOGFILE, ">>".$logfileLocation."dayTrader.log") || die "Error opening logfile!";

  #connect to our database
  $connection = DBI->connect( "DBI:mysql:database=stockdata;host=localhost", "root", "sal", {'RaiseError' => 1} );

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
    warn print "\nFatal DB error - no rows in Holdings table\n";
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
# input is last trade data for a symbol

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
  # 0: buy price
  # 1: percent increase
  # 2: percent decrease
  # 3: ...
  my @crit = getSellCriteria($symbol);

  my $buy_price = shift(@crit);
  my $per_incr  = shift(@crit);

  my $sell_price = $buy_price + ($buy_price * $per_incr);
#print "\nBuy/Sell $symbol: current: $price, sell at: $sell_price; we bought at: $buy_price, $per_incr% incr";

  #TODO: should we round to 2 decimal places?  val = int(val*100 + 0.5)/100;
  if ($price >= $sell_price)
  {
#if ($_dbg) { print DBGFILE "\n".timeStamp().": **SELL $symbol at $date"; }
#print "\n**SELL $symbol at $date\n";
  }
  else
  {
#print "\n**HOLD $symbol at $date\n";
  }

}

#==================================================
# get the sell criteria from the Holdings DB.  This is an
# XML string defining the criteria.  It can be different for
# each stock and as complex as necessary.
# to start it will be sell when price > buy_price * 2%
# (for testing its 0% - eg sell now)

# input is symbol
# output is an array of criteria
  # 0: buy price
  # 1: percent increase
  # 2: percent decrease
  # 3: ..."\nFatal DB error - incorrect nrows in Holdings table\n";

sub getSellCriteria
{
  my ($symbol) = @_;

  my @crit;


#get the buy price and criteria from the Holdings table from the day before
  # BIG TODO: use a different date!
  my $date = getDate();

  my $select = "SELECT buy_price, criteria FROM Holdings WHERE symbol = \"$symbol\" AND buy_date = \"$date\"";
#print "\ngetSellCriteria: ".$select;

  my $db = $connection->prepare( $select );
  $db->execute();

  my $rows = $db->rows;
#print "\n", $rows, " fetched";

  if ($rows!=1)
  {
    warn "\nFatal DB error - incorrect nrows in Holdings table\n";
    print LOGFILE "\nFatal DB error - incorrect nrows in Holdings table\n";
    return 0;
  }

  my @data = $db->fetchrow_array();
  $db->finish();

  push(@crit, $data[0]);
  #push(@crit, $data[1]);
  push(@crit, .00);

#print "\n criteria for $symbol is @crit";

  return @crit;

}

