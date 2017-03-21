package filter;

public class ConfigPAFilter {
   //Kick Off limit variable
	public static Integer MAX_KICK_OFF = 20;
	//total number of buckets. 0 ... 1999 = >total = 2000.
	public static Integer BUCKET_SIZE = 1999;
	//Hash algorithm to choose. "MD5" or "SHA1".
	public static String HASH_ALGORITHM = "MD5";
	
	//input data file.
	public static String Input_FileName = "data/Insert6500Num.txt";
	
	//lookup file
	public static String LookUp_FileName = "data/OneMillionLookup.txt";
	
	//variable to test both standard cuckoo filter and Position Aware Cuckoo Filter.
	//if it is TRUE, then PA filter else, standard filter.
	public static boolean isPAModeOn  = false ;
	
	
}
