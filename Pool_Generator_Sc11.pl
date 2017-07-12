	use strict;
	use	DBI;
	use Unicode::Map;
	use Config::Simple;
	
#read cofig file
	my %Config;
	Config::Simple->import_from('connection.ini', \%Config);
		my $host=$Config{"DB.host"};
		my $sid=$Config{"DB.host"};
		my $port=$Config{"DB.port"};
		my $user=$Config{"AUTH.user"};
		my $pass=$Config{"AUTH.password"};
		
#DEFINE SQL REQUEST
my $req_sql = "select
TUuser.user_id as \"p_login",
TExec.user_id as \"executor\"
from
(select u.id_ as user_id, u.req_bank_id_ as Bank_ID
...."

$ENV{NLS_LANG}="AMERICAN_AMERICA.CL8MSWIN1251";
$ENV{PATH}="C:\\oracle\\product\\11g\\Client\\bin";

my $dbh = DBI->connect('dbi:Oracle:host='.$host.';sid='.$sid.';port='.$port.';',''.$user.'',''.$pass.'') or die "CONNECT ERROR! :: $DBI::err
$DBI::errstr $DBI::state $!\n";

sub proc_req
	{
		open OUT_FILE, ">> $_[1]";
		
		my $sth = $dbh->prepare($_[0]);
		$sth->execute();
		if($sth){
			my $fields = $sth->{NUM_OF_FIELDS};
			my $name = $sth->{NAME};
			my $string = $sth->[0];
			#заполняем шапку таблицы
			for (my $i = 1; $i < $fields; $i++){
				$string=$string.','.$name->[$i];
			}
			print OUT_FILE "$out_string\n";
			#получаем результат запроса в массив и построчно заполняем
			while (my @rows = $sth->fetchrow_array)
			{
				my $out_string = $rows[0];
				#заполняем строки в excel
				for (my $i = 1; $i < $fields; $i++){
					$out_string=$out_string.','.$rows[$i];
				}
			print OUT_FILE "$out_string\n";
			}
		}
		$sth->finish();
		close OUT_FILE;
	}

proc_req($req_sql,"C:\\output_file.dat");
	
print "POOL complete\n";

$dbh->disconnect;	