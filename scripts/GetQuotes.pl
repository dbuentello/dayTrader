#!/usr/bin/perl

# Get End of Day Stock exchange quotes from TD ameritrade

do 'dtCommon.pl';


#============================================================
# get end of day quotes and determine biggest losers


#==================main=======================================
print "\n** Get End of Day Quotes from TD Ameritrade **\n";

  #global log file
  open(LOGFILE, ">>".$logfileLocation."dayTrader.log") || die "Error opening logfile!";

  #connect to our database
  $connection = DBI->connect( "DBI:mysql:database=stockdata;host=localhost", "root", "sal", {'RaiseError' => 1} );

#for debuging
#$connection->trace(1);

  $sessionId = login();
print "\nreturned sid = $sessionId";
  if (length($sessionId) == 0)
  {
     warn "\nFATAL: Invalid login\n";
     exit;
  }

  getQuotes();
  my @biggest_losers = biggestLosers();
print "\nBiggest losers are: @biggest_losers";

  #update our holdings table with stocks to buy (just symbol and date)
  updateHoldings(@biggest_losers);

  # calcualate buy positions and update DB
  updateBuyPositions(@biggest_losers);

  # execute orders (and update buy price, if different than calculated)

  logout();

  $connection->disconnect;

  close LOGFILE;

print "\n**end**\n";


#=================end main=====================================



#=========================================================
# get all the quotes from TD and load into end of day quote DB
#
# right now, this is done one quote at a time! and could take a while
# TODO: fetch all at once, or in batches - can update our DB one at a time
# this table could aslo get very big (7000+ entries per day)

sub getQuotes
{
#debug - and only use AMEX !!!!
my $count = 0;
my $MAX_QUOTES = 10;

  # clear the temp table first so we only have one entry per date
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

  my @data;
  while ( @data  = $db->fetchrow_array())
  {
     my $symbol_id = $data[0];
     my $symbol    = $data[1];
     my $exchange  = $data[6];
#print "\n $symbol, $symbol_id, $exchange";

     my $quote_data = getQuote($symbol);
print LOGFILE "\n$quote_data";

     #sanity check
     my $q_symbol = simpleParse($quote_data, "symbol");

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

# this is interesting!!!  TD is returning 0.00 and 0.00% for change and change-percent!!
# is this because te markets are closed?  eg, price = close?
# change = (prev)close - (current)price  %chg = change/(prev)close * 100
# so calculate it ourselves
     if ($open == 0 or $close == 0)
     {
       $change = 0;
       $chgper = 0;
     }
     else
     {
       $change = $open-$close;
       $chgper = ($change/$open *100)."%";
     }

print LOGFILE "\n$symbol: $open, $close, $low, $high, $price, $volume, $change, $chgper";

     #insert into our end of day quote DB
     updateQuotes($symbol, $symbol_id, $open, $close, $low, $high, $price, $volume, $change, $chgper);

#debug
last if ($count++ == $MAX_QUOTES);

  }

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
  # BIG TODO: use a different date!  lastTradeDate & format
  my $date = getDate();
  
  #ensure all our values are correctly defined - TODO: check this
  if ( !defined $open   || $open eq "")    { $open = "NULL"; }
  if ( !defined $close  || $close eq "")   { $close = "NULL"; }
  if ( !defined $low    || $low eq "")     { $low = "NULL"; }
  if ( !defined $high   || $high eq "")    { $high = "NULL"; }
  if ( !defined $price  || $price eq "")   { $price = "NULL"; }
  if ( !defined $volume || $volume eq "" ) { $volume = "NULL"; }
  if ( !defined $change || $change eq "")  { $change = 0; }
  if ( !defined $chgper || $chgper eq "")  { $chgper = NULL; }


  # insert the data into the db  - use prev close for %change
  my $insert_statement = "INSERT INTO EndOfDayQuotes (symbolid, symbol, date, open, close, high, low, price, volume, chng, chgper) VALUES ($sid, \"$symbol\", \"$date\", $open, $close, $high, $low, $price, $volume, $change, \"$chgper\")";
print LOGFILE "\nupdateQuotes: $insert_statement";

  #TODO: error check
  $connection->do( $insert_statement );

}

#======================================================
# return the top n biggest losers by percent change
# note: limited to $nlosers=10 right now, without any criteria

sub biggestLosers
{
my $n_losers = 10;

  my @losers;


  # BIG TODO: use a different date!
  my $date = getDate();

  #get the symbols from our database (date based to retrieve most current)
  my $select = "SELECT symbolId FROM EndOfDayQuotes WHERE date = \"$date\" ORDER BY chgper";
print "\nbiggestLosers: ".$select;

  my $db = $connection->prepare( $select );
  $db->execute();

  my $rows = $db->rows;
#print "\n", $rows, " fetched";

  if ($rows==0)
  {
    warn "\nFatal DB error - no rows in quotes table\n";
    return 0;
  }

  for (my $i=0; $i<$n_losers; $i++)
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
#          use the previous query when all stocks were queried
sub getCurrentPrice
{
  my ($sid) = @_;

  my $symbol = symbolLookup($sid);
  my $results = getQuote($symbol);
  my $price = simpleParse($results, "last");

  return $price;
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
    warn "\nFatal DB error - no rows in symbol table\n";
    return "";
  }

  my @data = $db->fetchrow_array();
  my $symbol    = $data[0];
#print "\nlookup $symbolid: $symbol";

  $db->finish();
  
  return $symbol;
}
