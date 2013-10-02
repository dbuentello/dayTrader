#==========================================================
#TODO: add volume total
# ?? use symbolId or symbol?
# note: this assumes that all other sell orders for this symbol have already been executed,
# eg there are only unique symbols with a null sell_date
# it wont update if already sold  TODO: keep track if it keeps rising?

sub updateSellPositions()
{
  my ($symbol, $price, $date) = @_;

# $stmt = "UPDATE Holdings SET sell_price = $price, buy_volume = $buy_volume, buy_total = $buy_total WHERE symbolid = $sid AND buy_date = \"$date\"";
  $stmt = "UPDATE Holdings SET sell_price = $price, sell_date= \"$date\" WHERE symbol = \"$symbol\" AND sell_date is NULL";

if ($_dbg) { print DBGFILE "\nupdateSellPositions: $stmt"; }

  my $rows_changed = $connection->do( $stmt );

if ($_dbg) {
if ($rows_changed == 0) { print DBGFILE "\nnot updating $symbol because its already sold"; }
}

  return $rows_changed;
}

# return for require
1


