#!/usr/bin/perl

use DBI;
use DBD::mysql;
use Switch;

use strict;
use warnings;


sub usage() {
	print "UpdateSymbolsTable -db databasei [-u database user] [-pw database password] \n\n";
	exit;
}

my $db_name = "dayTrader_test";
my $db_user = "root";
my $db_passwd = "r00t";

my @exchanges = ('nasdaq', 'amex', 'nyse');

	if (@ARGV == 0) {
		usage();
	} else {
		for(my $i = 0; $i < @ARGV; $i++) {
			if ($ARGV[$i] eq "-db") {
				$db_name = $ARGV[$i+1];
				$i++;
			} elsif ($ARGV[$i] eq "-u") {
				$db_user = $ARGV[$i+1];
				$i++;
			} elsif ($ARGV[$i] eq "-pw") {
				$db_passwd = $ARGV[$i+1];
				$i++;
			} else {
				usage();
			}	
		}
	}

	my $connection = DBI->connect( "DBI:mysql:database=$db_name;host=localhost", $db_user, $db_passwd, {'RaiseError' => 1} );

	foreach my $exchange  (@exchanges) {

		my $csv_file = "/tmp/${exchange}_symbols.csv";
		`wget "www.nasdaq.com/screening/companies-by-industry.aspx?industry=ALL&exchange=${exchange}&sortname=symbol&sorttype=0&render=download" -O $csv_file`;	

		open (my $file_handle, '<', $csv_file) or die $!;
  	my @symbols;
	  my $i=0;
	
		my $line = <$file_handle>;
		$line = substr($line, 1, length($line) - 2);
		my @headers  = split("\",\"", $line);
		my ($symbolIndex, $nameIndex, $mcIndex, $secIndex, $indIndex) = 0;
		for(my $i = 0; $i < @headers; $i++) {
			switch ($headers[$i]) {
				case "Symbol" {
					$symbolIndex = $i;
				}
				case "Name" {
					$nameIndex = $i;
				}
				case "MarketCap" {
					$mcIndex = $i;
				}
				case "Sector" {
					$secIndex = $i;
				}
				case "Industry" {
					$indIndex = $i;
				}
			}
		}
	
		#get the symbols from our database
	  my $db = $connection->prepare( "SELECT * FROM symbols where exchange = \"$exchange\"" );
	  $db->execute();
		my $db_symbols = $db->fetchall_hashref('symbol');

		#print "db_symbols size = " . keys( %$db_symbols ) . "\n";
	
		my $insert_count = 0;
		my %new_symbols = ();
	  while ($line = <$file_handle>) {
		
			$line = substr($line, 1, length($line) - 2);
	
			my @csv_row  = split("\",\"", $line);
			$csv_row[$symbolIndex] =~ s/\s//g;
			$new_symbols{$csv_row[$symbolIndex]}{'name'} = $csv_row[$nameIndex];
			$new_symbols{$csv_row[$symbolIndex]}{'market_cap'} = $csv_row[$mcIndex];
			$new_symbols{$csv_row[$symbolIndex]}{'sector'} = $csv_row[$secIndex];
			$new_symbols{$csv_row[$symbolIndex]}{'industry'} = $csv_row[$indIndex];
	
			my $type = "common";
			if(length($csv_row[$symbolIndex]) > 4) {
				$type = "other";
			}
			
	
			#if our database contains this symbol then make sure the expired field is set to FALSE
			#if the database doesn't contain our row add it to the database		
			if (exists $db_symbols->{$csv_row[$symbolIndex]} ) {
        my $update_statement = "UPDATE symbols SET expired = 0, type = \"${type}\" WHERE id = " . $db_symbols->{$csv_row[$symbolIndex]}{'id'};
				#print $update_statement . "\n";
				$connection->do( $update_statement );
				delete $db_symbols->{$csv_row[$symbolIndex]};
			} else {
				my $insert_statement = "INSERT INTO symbols (symbol, name, market_cap, sector, industry, exchange, expired, type) VALUES " .
					"(\"$csv_row[$symbolIndex]\", " . 
					"\"$new_symbols{$csv_row[$symbolIndex]}{'name'}\", " . 
					"\"$new_symbols{$csv_row[$symbolIndex]}{'market_cap'}\", " . 
					"\"$new_symbols{$csv_row[$symbolIndex]}{'sector'}\", " .
					"\"$new_symbols{$csv_row[$symbolIndex]}{'industry'}\", " . 
					"\"${exchange}\", 0, \"${type}\")";
					#print $insert_statement . "\n";
					$connection->do( $insert_statement );
					$insert_count++;
			}
	
	  }
			
		#print "db_symbols size = " . keys( %$db_symbols ) . "\n";
		#print "new_symbols size = " . keys( %new_symbols ) . "\n";


		my $update_count = 0;
		#for every object that still exists in the db_symbols hash, it wasn't in the spread sheet so its
		#no longer a valid symbol
		while( my ($symbol, $hash) = each %$db_symbols ) {
				my $type = "common";
    	  if(length($symbol) > 4) {
  	      $type = "other";
	      }

        my $update_statement = "UPDATE symbols SET expired = 1, type = \"${type}\" WHERE id = " . $db_symbols->{$symbol}{'id'};
			print $update_statement . " symbol = " . $symbol . "\n";
				$connection->do( $update_statement );
				$update_count++;
    }
	
		print "ADDED: " . $insert_count . " MARKED AS EXPIRED: " . $update_count . "\n";

		close($file_handle);
	
	
	} #close @exchanges loop



$connection->disconnect();

