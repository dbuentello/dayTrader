<?php

//construct the url to query the data
//this is the base url that's needed for all queries, regardless of which table is being queried
$yql_base="http://query.yahooapis.com/v1/public/yql";

//build a SQL like query string. I can test my queries at http://developer.yahoo.com/yql/console/?. For historical data...
//I need to query one symbol at a time since the returned data doesn't differentiate between symbols for me to parse
$hist_data_query="select * FROM yahoo.finance.historicaldata where symbol=\"NFLX\" AND startDate=\"2008-01-01\" AND endDate=\"2012-10-17\"";

print $hist_data_query . "\n";

//encode the query string into url format, the "env" parameter is needed to include the community tables which
//includes the finance.quotes and finance.historicaldata tables
$hist_data_query_url=$yql_base . "?q=" .  urlencode($hist_data_query) . "&env=http://datatables.org/alltables.env";
$hist_data_query_url .= "&format=json";  

$hist_data_query_url = "http://finance.yahoo.com/d/quotes.csv?s=NFLX&f=sopghk2";

//execute the url and get back the results in json format
$session=curl_init($hist_data_query_url);  
curl_setopt($session, CURLOPT_RETURNTRANSFER,true);   
$hist_json=curl_exec($session);

print $hist_json . "\n";
exit;



//convert the json formatted string to a nice PHP object for easy access of each field
$hist_data=json_decode($hist_json); 


if(!is_null($hist_data->query->results)){  

  // Parse results and extract data to display. Use the YQL console (link above) to execute the query and see
  //which fields will be returned and can be accessed  
  foreach($hist_data->query->results->quote as $quote){  

    $percent_change=round((floatval($quote->Close) - floatval($quote->Open)) / floatval($quote->Open) * 100, 2);
    $max_percent_change = round((floatval($quote->High) - floatval($quote->Open)) / floatval($quote->Open) * 100, 2);

    print $quote->date . ": open: " . $quote->Open . "\t\tclose: " . $quote->Close . "\t\thigh: " . $quote->High . "\t\tchange: " . $percent_change . "\t\tmax change: " . $max_percent_change . "%\n";

  }  
}  


?>

