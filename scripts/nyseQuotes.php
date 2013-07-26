<?php

//make a connection to the database and select the stockdata database
$connection = mysql_connect("localhost", "root", "r00t");
mysql_select_db("stockdata", $connection);


//construct the url to query the data
//this is the base url that's needed for all queries, regardless of which table is being queried
$yql_base="http://query.yahooapis.com/v1/public/yql";

//query the database to get a complete list of all the companies traded on the market
$symbols = mysql_query("SELECT symbol, id FROM symbols WHERE exchange=\"nyse\"");


while ($row = mysql_fetch_array($symbols)) {

  //build a SQL like query string. I can test my queries at http://developer.yahoo.com/yql/console/?
  $data_query="SELECT * FROM yahoo.finance.quotes where symbol=\"" . $row['symbol'] . "\"";

  //encode the query string into url format
  //the "env" parameter in the url needed to include community tables that includes the finance.quotes table
  //the "Format" parameter is used to return the results in json format
  $data_query_url=$yql_base . "?q=" .  urlencode($data_query) . 
                                       "&env=http://datatables.org/alltables.env&format=json";  

  //execute the url and get back the results in json format
  $session=curl_init($data_query_url);  
  curl_setopt($session, CURLOPT_RETURNTRANSFER,true);   
  $today_json=curl_exec($session);

  //convert the json formatted string to a nice PHP object for easy access of each field
  $todays_data=json_decode($today_json); 


  if(!is_null($todays_data->query->results)){  

    //get today's quote from the returned json
    $quote = $todays_data->query->results->quote;  

    //create the mysql INSERT statement to store the relevant data and the exectue the query
    $insert_query="INSERT INTO nyse_quotes (symbolId, date, open, close, high, low, volume, previousClose) " .  
			"VALUES (" . $row['id'] . ", CURRENT_DATE(), " . $quote->Open . ", " . $quote->LastTradePriceOnly .
			", " . $quote->DaysHigh . ", " . $quote->DaysLow . ", " . $quote->Volume . ", " . $quote->PreviousClose . ")";
    mysql_query($insert_query); 


    print $insert_query . "\n";
    print $row['symbol'] . ", open: " . $quote->Open . "\n";
    
  } else {
    print "Query failed. Symbol: " . $row['symbol']; 
  }


} //close while looping through our sql query results


//close our connection to the database
mysql_close($connection);


//write to the log file that this script was executed
$logFile="/home/nathan/dayTrader/executionLog.log";
$fh=fopen($logFile, 'a');
fwrite($fh, "NYSE quotes executed at: " . date("D, m d Y H:i:s") . "\n");
fclose($fh);

?>

