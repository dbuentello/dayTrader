<?
//
// Calculate the next day gains of the previous day's biggest losing stocks
//
//we're going to get all the distinct dates for each table, then for each date find the 
//biggest losers of the day. Then, for each of the day's biggest losers, we're going to see
//how much they gained (or lost) the next day	and calculate the average next day gain for the 
//symbols that were the biggest losers the day before
//


$debug = 0;

$con = mysqli_connect("localhost", "root", "r00t");
mysqli_select_db($con, "stockdata");

//the tables that contain our quotes, we'll tally up each table separately
$tables = array(1 => "amex_quotes", 2 => "nasdaq_quotes", 3 => "nyse_quotes");

foreach ($tables as $table) {

	//use these variables to tally the overall average change of the stocks
	$cumHighChange = 0;
	$cumDayChange = 0;
	$stockCount = 0;

  //get all the distinct dates in the table. We'll then query the table again on these dates
  //to get all the quotes from a single day
  $dates_qry = "SELECT DISTINCT(date) FROM $table ORDER BY date ASC";
	$dates = mysqli_query($con, $dates_qry);

if ($debug) {
	print "\ndates_qry = " . $dates_qry;
}

	//the currentDateRow variable will keep track on the current row index as we loop through our dates
  //we'll use this index to adjust our iterator when we need to query the next day's data
	$currentDateRow = -1;

	while ($date = mysqli_fetch_array($dates)) {
		$currentDateRow += 1;		
		
		print "\n\n\n###########################################################################\n";
		print "\t" . $table . "\t" . $date['date'] . "\n";
		print "###########################################################################\n\n\n";

    //get all the quotes we have for a single day
		$quotes_qry = "SELECT * FROM $table WHERE date=\"" . $date['date'] . "\" ";
		$quotes = mysqli_query($con, $quotes_qry);

if ($debug) {
	print "\nquotes_qry " . $quotes_qry;
}

		//for each of the day's quotes, calculate the loss/gain for the given symbol
		$daysChanges = array();
		while ($quote = mysqli_fetch_array($quotes)) {

			//make sure we don't divide by zero, this could happen if we didn't get complete data for the quote
			if ($quote['previousClose'] != 0 ) {
				$diff = (floatval($quote['close']) - floatval($quote['previousClose'])) / floatval($quote['previousClose']) * 100;
				$daysChanges[$quote['symbolId']] = $diff;
			}

		} // close foreach($quotes)

    //sort our day's change array low->high, so the biggest losers will be the first elements in the array
		asort($daysChanges);

		//
		// now we have every symbol's gain/loss sorted in our array $daysChanges. Let's take the 10 biggest losers
   	// and find out how they faired the next day. We'll calculate the next day's high gain and the day's close 
		// lose/gain
		//
		
		// get the date of the next day we have data for
		$nextDay = mysqli_fetch_array($dates);
   	//adjust our array pointer back on so we don't skip a date on the next loop iteration
		mysqli_data_seek($dates, $currentDateRow);


		//use i as a counter to break out of our loop after the 10 biggest losers
   	$i=0;
		// declare an array for our calculated values
		$calcValues = array();

   	foreach ($daysChanges as $symbol => $change) {

			$i += 1;
			$nextDayQuote_qry = "SELECT $table.*, symbols.symbol FROM $table JOIN symbols ON symbols.id = $symbol WHERE symbolId = " . $symbol . " AND date = \"" . $nextDay['date'] . "\" LIMIT 1";
			$nextDayQuote = mysqli_fetch_array(mysqli_query($con, $nextDayQuote_qry));

if ($debug) {
	print "\nnextDayQuote_qry = " . $nextDayQuote_qry;
}

			if ($nextDayQuote['previousClose'] != 0) {
				$highGain = (floatval($nextDayQuote['high']) - floatval($nextDayQuote['previousClose'])) / floatval($nextDayQuote['previousClose']) * 100;
				$dayGainLoss = (floatval($nextDayQuote['close']) - floatval($nextDayQuote['previousClose'])) / floatval($nextDayQuote['previousClose']) * 100;
				$calcValues[$symbol] = array('highGain' => $highGain, 'dayGain' => $dayGainLoss);

				echo sprintf ("\nSymbol: %s CHANGE: %0.2f%%\tNEXT DAY HIGH: %0.2f%%\tNEXT DAY CHANGE: %0.2f%%", $nextDayQuote['symbol'], $change, $highGain, $dayGainLoss);

				$cumHighChange += $highGain;
				$cumDayChange += $dayGainLoss;
				$stockCount += 1;
			}


			if ($i > 10) {
				break;
			}

		}	// close foreach($daysChanges)


	
	} // close foreach($dates)


	print "*******************************************************************\n";
	echo sprintf("AVERAGE HIGH CHANGE: %0.2f\tAVERAGE DAY CHANGE: %0.2f\n", $cumHighChange/$stockCount, $cumDayChange/$stockCount);
	print "*******************************************************************\n";

} // close foreach($tables)





mysqli_close($con);

?>
