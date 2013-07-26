<?

//create a connection to mysql and select the "stockdata" database
$con = mysql_connect("localhost", "root", "r00t");
mysql_select_db("stockdata", $con);

//get all the symbols for which we have data
$symbols_query = "SELECT id, symbol, exchange FROM symbols";
$symbols = mysql_query($symbols_query);

//create empty arrays to store our calculated results
$high_aves = array();
$low_aves = array();
$night_aves = array();


while ($symbol = mysql_fetch_array($symbols)) {

  //for each symbol, get all the quotes we have for that symbol
  $quotes_query = "SELECT * FROM " . $symbol['exchange'] . "_quotes WHERE symbolId = " . $symbol['id'];
  $quotes = mysql_query($quotes_query);


  $high = 0;
  $low = 0;
  $night = 0;

  while ($quote = mysql_fetch_array($quotes)) {
    //total up the daily high, low, and after market trading for each symbol
    $high += (floatval($quote['high']) - floatval($quote['open'])) / floatval($quote['open']);
    $low += (floatval($quote['low']) - floatval($quote['open'])) / floatval($quote['open']);
    $night += (floatval($quote['previousClose']) - floatval($quote['open'])) / floatval($quote['open']);

  }

  $num_rows = mysql_num_rows($quotes);

  if ($num_rows != 0) {
    
    //calculate the daily high average, low average, and after-market trading average
    $high_ave = $high / $num_rows * 100;
    $low_ave = $low / $num_rows * 100;
    $night_ave = $night / $num_rows * 100;

//  print "Symbol: " . $symbol['symbol'] . "\n";
//  print "High Average: " . $high_ave . "\tLow Average: " . $low_ave . "\tNight Average: " . $night_ave . "\n";

    //store our calculated averages in arrays for each average type
    $high_aves[$symbol['symbol']] = $high_ave;
    $low_aves[$symbol['symbol']] = $low_ave;
    $night_aves[$symbol['symbol']] = $night_ave;
  
  }

}

mysql_close($con);

//sort our calulate averages
arsort($high_aves);
asort($low_aves);
arsort($night_aves);


//loop through and print out the highest/lowest 10 symbols for each type of average
$i = 0;
print "\nDaily % gainers:\n";
foreach($high_aves as $symbol => $ave) {  
  print $symbol . ":\t" . $ave . "\n";
  $i++;
  if ($i > 10)
    break;
}


$i = 0;
print "\nDaily % losers:\n";
foreach($low_aves as $symbol => $ave) {  
  print $symbol . ":\t" . $ave . "\n";
  $i++;
  if ($i > 10)
    break;
}


$i = 0;
print "\nDaily % night gainers:\n";
foreach($night_aves as $symbol => $ave) {  
  print $symbol . ":\t" . $ave . "\n";
  $i++;
  if ($i > 10)
    break;
}





?>
