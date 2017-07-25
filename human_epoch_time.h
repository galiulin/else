//преобразование даты в epoch
//@parameters human_time i.e. 2017-07-17
//Pname - наименование параметра, куда сохранится выходная строка
//@Return Pname i.e. 1500238800

void human_epoch_time(char* human_time, char* Pname){
	struct tm
	{
		int tm_sec;
		int tm_min;
		int tm_hour;
		int tm_mday;
		int tm_mon;
		int tm_year;
		int tm_wday;
		int tm_yday;
		int tm_isdst;
	};
	
	typedef long time_t;
	struct tm tmVar;
	char epochString[10];
	
	time_t timeVar=0;
	
	if(sscanf(human_time, "%d-%d-%d %d:%d:%d", &tmVar.tm_year, &tmVar.tm_mon, &tmVar.tm_mday, &tmVar.tm_hour, &tmVar.tm_min, &tmVar.tm_sec)==6){
		tmVar.tm_year=tmVar.tm_year-1900;
		tmVar.tm_mon=tmVar.tm_mon-1;
		timeVar=mktime32(&tmVar);
		sprintf(epochString,"%lu",timeVar);
		lr_save_string(epochString, Pname);
	} else {
		lr_error_message("Error in format\n YYYY-MM-DD HH:MM:SS \n");
	}
}