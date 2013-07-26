<?php


//connect to the mysql database
$connection  = mysql_connect("localhost", "root", "r00t");
mysql_select_db("stockdata", $connection);


//construct the url to query the data
//this is the base url that's needed for all queries, regardless of which table is being queried
$yql_base="http://query.yahooapis.com/v1/public/yql";


$symbols = mysql_query("SELECT symbol, id FROM symbols");

while ($row = mysql_fetch_array($symbols)) {

  print "Executing for symbol: " . $row["symbol"] . "\n";

  //build a SQL like query string. I can test my queries at http://developer.yahoo.com/yql/console/?. For historical data...
  //I need to query one symbol at a time since the returned data doesn't differentiate between symbols for me to parse
  $hist_data_query="select * FROM yahoo.finance.historicaldata where symbol=\"" . $row["symbol"] . "\" AND startDate=\"2002-01-01\" AND endDate=\"2012-10-17\"";

  //print $hist_data_query . "\n";

  //encode the query string into url format, the "env" parameter is needed to include the community tables which
  //includes the finance.quotes and finance.historicaldata tables
  $hist_data_query_url=$yql_base . "?q=" .  urlencode($hist_data_query) . "&diagnostics=true&env=http://datatables.org/alltables.env";
  $hist_data_query_url .= "&format=json";  

  //execute the url and get back the results in json format
  $session=curl_init($hist_data_query_url);  
  curl_setopt($session, CURLOPT_RETURNTRANSFER,true);   
  $hist_json=curl_exec($session);

  //convert the json formatted string to a nice PHP object for easy access of each field
  $hist_data=json_decode($hist_json); 


  if(!is_null($hist_data->query->results)){  

    // Parse results and extract data to display. Use the YQL console (link above) to execute the query and see
    //which fields will be returned and can be accessed  
    foreach($hist_data->query->results->quote as $quote){  

      $percent_change=round((floatval($quote->Close) - floatval($quote->Open)) / floatval($quote->Open) * 100, 2);
      $max_percent_change = round((floatval($quote->High) - floatval($quote->Open)) / floatval($quote->Open) * 100, 2);

      //print $quote->date . ": open: " . $quote->Open . "\t\tclose: " . $quote->Close . "\t\thigh: " . $quote->High . "\t\tchange: " . $percent_change . "\t\tmax change: " . $max_percent_change . "%\n";

      //insert the data into the db
      $insert_statement = "INSERT INTO quotes (symbolId, date, open, close, high, low, volume) VALUES (" . $row["id"] . ", \"" . $quote->date . "\", " . $quote->Open . ", " . $quote->Close . ", " . $quote->High . ", " . $quote->Low . ", " . $quote->Volume . ")";
      mysql_query($insert_statement);
      print $insert_statement . "\n";

    }  
  }
  else {
    print "Query failed\nError message: " . $hist_data->query->diagnostics->warning . "\n" . $hist_data_query . "\n";
    var_dump($hist_data);
  }

} //close while looping through symbols

//close the connection to the database
mysql_close($connection);

?>

