#!/usr/bin/perl

use Data::Dumper;
use DBI;
use DBD::mysql;
use JSON qw( decode_json );
use LWP::Simple;
use URI::Escape;
use DateTime;
use DateTime::Format::Strptime;
use Switch;
use Scalar::Util qw(looks_like_number);

use strict;
use warnings;


yesterdays_quotes();

sub get_exchanges;
sub get_date;

my $date = get_date();
my @exchanges = get_exchanges(); 

#connect to our database
my $connection = DBI->connect( "DBI:mysql:database=stockdata;host=localhost", "root", "r00t", {'RaiseError' => 1} );

foreach  my $exchange (@exchanges) {

	my $html_file = retrieve_html($exchange);
	if ( !defined $html_file ) {
		next;
	}

	my @symbols = get_symbols($html_file);
  
  
  foreach my $symbol (@symbols) {
  	 
		my $quote = get_quote($symbol);

		if ( defined $quote ) {
  		#get the symbolId for the symbol we retreived a quote for
  		my $sth = $connection->prepare( "SELECT * FROM symbols WHERE symbol=\"$symbol\" AND exchange=\"$exchange\"" );
  		$sth->execute();
  		my $sql_result = $sth->fetchrow_hashref();
  		$sth->finish();
  
  		if ( defined $sql_result )  {
  			
  			my $symbolId = $sql_result->{'id'};
  
  			#format certain fields so they insert in the database nicely
  			my $lastTradeDate = "NULL";
				if ( defined $quote->{'LastTradeDate'} ) {
	  			my $parser = DateTime::Format::Strptime->new( pattern => '%m/%d/%Y' );
  				my $datetime = $parser->parse_datetime( $quote->{'LastTradeDate'} );
  			  $lastTradeDate = $datetime->strftime('%Y-%m-%d');
				} 
  
  			my $percentChange = "NULL";
				if ( defined $quote->{'PercentChange'} && $quote->{'PercentChange'} =~ m/\-+[0-9]*.+[0-9]*/ ) {	
  				$percentChange = $quote->{'PercentChange'};
  				$percentChange =~ /(\-+[0-9]*\.+[0-9]*)/;
  				$percentChange = $1;
				}
  
  			#ensure all our values are defined
  			my ($open, $lastTradePriceOnly, $daysHigh, $daysLow, $volume, $averageDailyVolume);
  			if ( defined $quote->{'Open'} ) { $open = $quote->{'Open'}; } else { $open = "NULL"; }
  			if ( defined $quote->{'LastTradePriceOnly'} ) {  $lastTradePriceOnly = $quote->{'LastTradePriceOnly'}; } else {  $lastTradePriceOnly = "NULL"; }
  			if ( defined $quote->{'DaysHigh'} ) {  $daysHigh = $quote->{'DaysHigh'} } else {  $daysHigh = "NULL"; }
  			if ( defined $quote->{'DaysLow'} ) {  $daysLow = $quote->{'DaysLow'} } else {  $daysLow = "NULL"; }
  			if ( defined $quote->{'Volume'} ) {  $volume = $quote->{'Volume'} } else {  $volume = "NULL"; }
  			if ( defined $quote->{'AverageDailyVolume'} ) {  $averageDailyVolume = $quote->{'AverageDailyVolume'} } else {  $averageDailyVolume = "NULL"; }
 
  	    # insert the data into the db
    	  my $insert_statement = "INSERT INTO ${exchange}_max_losers (symbolId, symbol, date, open, close, high, low, volume, percentChange, averageDailyVolume) VALUES ($symbolId, \"$symbol\", \"$lastTradeDate\", $open, $lastTradePriceOnly, $daysHigh, $daysLow, $volume, $percentChange, $averageDailyVolume)";
        print "$insert_statement\n";
    		$connection->do( $insert_statement );
  		}	
    }
		else {
 			print "Symbol $symbol doesn't exist in the database.\n";
  	}
  
  } # close foreach ($symbol)

} #close foreach @echanges  
$connection->disconnect();

sub get_symbols {

	my ($html_file) = @_;
  open (my $file_handle, '<', $html_file) or die $!;
  my $search_string = 'td class="first"><b><a href="\/q\?s=';
  my @symbols;
	my $i=0;
  while (my $line = <$file_handle>) {
  	
  
  	while ($line =~ /$search_string/g) {
  		my $result = substr($line, $+[0], 7);
  		$symbols[$i] = substr($result, 0, index($result, '"', 0));
  		print "Symbol = $symbols[$i]\n";
  		$i++;
  	}
  			
  }
  
  close($file_handle);

	return @symbols;

}

sub retrieve_html {

	my ($exchange) = @_;
 	my $html_file = "/home/nathan/dayTrader/html_source/${exchange}_losers_$date.html";
	switch ($exchange) {
		case "nasdaq" {
		  # nasdaq
			`curl http://finance.yahoo.com/losers?e=o > $html_file`;
    } 
		case "amex" {
			# amex
			`curl http://finance.yahoo.com/losers?e=aq > $html_file`;
		}
		case "nyse" {
			# nyse
			`curl http://finance.yahoo.com/losers?e=nq > $html_file`;
		}
		else {
			print "Could not retrieve Yahoo Losers page for exchange $exchange.\n";
			$html_file = undef;
			# all
			#`curl http://finance.yahoo.com/losers?e=us > $html_file`;
		}
	} # close switch ($exchange)

	return $html_file;

}


sub get_quote {

	my ($symbol) = @_;

  # construct the url to query the data
  # this is the base url that's needed for all queries, regardless of which table is being queried
  my $yql_base="http://query.yahooapis.com/v1/public/yql";
  my $yql_suffix="&env=store://datatables.org/alltableswithkeys"; 

  # build a SQL like query string. I can test my queries at http://developer.yahoo.com/yql/console/?. For historical data...
  # I need to query one symbol at a time since the returned data doesn't differentiate between symbols for me to parse
  my $mysql_query = "select * FROM yahoo.finance.quotes where symbol=\"${symbol}\"";
  #encode the query string into url format, the "env" parameter is needed to include the community tables which
  #includes the finance.quotes and finance.historicaldata tables
  my $query_url = "$yql_base?q=" . uri_escape($mysql_query) . "&diagnostics=true&format=json$yql_suffix";
 
	#print $query_url . "\n"; 
	#execute the url and get back the results in json format
  my $json_quote = LWP::Simple::get( $query_url );
  my $dj_quote;

	if( defined $json_quote) {
		$dj_quote = decode_json( $json_quote );
		#print Dumper $dj_quote;
  } else {
		print "**********************************************************************\n";
		print "Failed to get quote for ${symbol}. Query = ${query_url}";
		print "**********************************************************************\n";
		next;
	}

	
	if ( defined $dj_quote ) {
  	return $dj_quote->{'query'}->{'results'}->{'quote'};
 	} 
  else {
		print "########################### ERROR!!!!! #############################################\n";
		if ( defined $dj_quote->{'query'}->{'diagnostics'}->{'warning'} ) {
 	    print "Failed to get a quote for symbol: $symbol. Error: " . $dj_quote->{'query'}->{'diagnostics'}->{'warning'} . "\n $mysql_query\n";
 		}
 		else {
 			print "Failed to connect to Yahoo. Could not get a quote at all for symbol: $symbol\n";
 		}
		print "########################### ERROR!!!!! #############################################\n";
  }

	return 0;
}


sub get_date {

	(my $second, my $minute, my $hour, my $dayOfMonth, my $month, my $yearOffset, my $dayOfWeek, my $dayOfYear, my $daylightSavings) = localtime();
	$month++;
	my $year = 1900 + $yearOffset;
	return $month . "_" . $dayOfMonth . "_" . $year;

}

sub get_exchanges() {

	return ( "nasdaq", "amex", "nyse" );

}

sub yesterdays_quotes {

	print "*************************************************\n";
	print "\t\t Getting yesterday's quotes\n";
	print "*************************************************\n";

	my $connection = DBI->connect( "DBI:mysql:database=stockdata;host=localhost", "root", "r00t", {'RaiseError' => 1} );

	my @exchanges = get_exchanges();
	foreach my $exchange (@exchanges) {

		my $averageHighChange = 0;
		my $averageLowChange = 0;
		
		#get the date for yesterday's quotes
  	my $sth = $connection->prepare( "SELECT date FROM ${exchange}_max_losers ORDER BY date DESC LIMIT 1" );
  	$sth->execute();
  	my $sql_result = $sth->fetchrow_hashref();
  	$sth->finish();
		
		# get all the symbols for which we got quotes for yesterday
		$sth = $connection->prepare( "SELECT symbol, symbolId, close FROM ${exchange}_max_losers WHERE date = \"$sql_result->{'date'}\"" );
		$sth->execute();
		
		my $highCounter = 0;
		my $lowCounter = 0;
		while ($sql_result = $sth->fetchrow_hashref() ) {

			my $symbol = $sql_result->{'symbol'};
			my $symbolId = $sql_result->{'symbolId'};
			my $prevClose = $sql_result->{'close'};			

			#get today's quote for yesterday's symbol
			my $quote = get_quote($symbol);

			if (defined $quote) {
				
								
  			#format certain fields so they insert in the database nicely
  			my $lastTradeDate = "NULL";
				if ( defined $quote->{'LastTradeDate'} ) {
	  			my $parser = DateTime::Format::Strptime->new( pattern => '%m/%d/%Y' );
  				my $datetime = $parser->parse_datetime( $quote->{'LastTradeDate'} );
  			  $lastTradeDate = $datetime->strftime('%Y-%m-%d');
				} 
  
  			my $percentChange = "NULL";
				if ( defined $quote->{'PercentChange'} && $quote->{'PercentChange'} =~ m/\-+[0-9]*.+[0-9]*/ ) {	
  				$percentChange = $quote->{'PercentChange'};
  				$percentChange =~ /(\-+[0-9]*\.+[0-9]*)/;
  				$percentChange = $1;
				}
  
  			#ensure all our values are defined
  			my ($open, $lastTradePriceOnly, $daysHigh, $daysLow, $volume, $highPercentChange, $averageDailyVolume, $lowPercentChange);
  			if ( defined $quote->{'Open'} ) { $open = $quote->{'Open'}; } else { $open = "NULL"; }
  			if ( defined $quote->{'LastTradePriceOnly'} ) {  $lastTradePriceOnly = $quote->{'LastTradePriceOnly'}; } else {  $lastTradePriceOnly = "NULL"; }
  			if ( defined $quote->{'DaysHigh'} ) {  $daysHigh = $quote->{'DaysHigh'} } else {  $daysHigh = "NULL"; }
  			if ( defined $quote->{'DaysLow'} ) {  $daysLow = $quote->{'DaysLow'} } else {  $daysLow = "NULL"; }
  			if ( defined $quote->{'Volume'} ) {  $volume = $quote->{'Volume'} } else {  $volume = "NULL"; }
  			if ( defined $quote->{'AverageDailyVolume'} ) {  $averageDailyVolume = $quote->{'AverageDailyVolume'} } else {  $averageDailyVolume = "NULL"; }

				if ( looks_like_number($daysHigh) && looks_like_number($prevClose) ) {
					$highPercentChange = ( $daysHigh - $prevClose ) / $prevClose * 100;
					$highPercentChange = sprintf( "%.2f", $highPercentChange );
					
					$highCounter++;
					$averageHighChange += $highPercentChange;
				}
				else {
					$highPercentChange = "NULL";
				}
	
				if ( looks_like_number($daysLow) && looks_like_number($prevClose) ) {
					$lowPercentChange = ( $daysLow - $prevClose ) / $prevClose * 100;
					$lowPercentChange = sprintf( "%.2f", $lowPercentChange );
					
					$lowCounter++;
					$averageLowChange += $lowPercentChange;
				}
				else {
					$lowPercentChange = "NULL";
				}

				# insert today's quote into the database
    	  my $insert_statement = "INSERT INTO ${exchange}_gains_losses (symbolId, symbol, date, open, close, high, low, volume, percentChange, averageDailyVolume, lowPercentChange, highPercentChange) VALUES ($symbolId, \"$symbol\", \"$lastTradeDate\", $open, $lastTradePriceOnly, $daysHigh, $daysLow, $volume, $percentChange, $averageDailyVolume, $lowPercentChange, $highPercentChange)";
				print $insert_statement . "\n";
				$connection->do( $insert_statement );


			} # close if (defined $quote)


		} #close while sth->fetch_hashref()

		$sth->finish();

		if ($highCounter != 0) {
			$averageHighChange /= $highCounter;
			$averageHighChange = sprintf( "%.2f", $averageHighChange );
		} 
		else {
			$averageHighChange = "NULL";
		}
		if ($lowCounter != 0) {
			$averageLowChange /= $lowCounter;
			$averageLowChange = sprintf( "%.2f", $averageLowChange );
		}
		else {
			$averageLowChange = "NULL";
		}
		my $insert_averages = "INSERT INTO averages (averageHighChange, averageLowChange, date, exchange) VALUES (${averageHighChange}, ${averageLowChange}, CURRENT_DATE(), \"${exchange}\")";
		print $insert_averages . "\n";
		$connection->do( $insert_averages );

	} #close foreach @exchanges


# SELECT SUM(highPercentChange) / COUNT(*) AS average FROM nyse_gains_losses;

	$connection->disconnect();

}


