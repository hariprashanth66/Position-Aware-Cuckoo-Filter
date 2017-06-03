package filter;

/**
 * Java program which contains variables  to configure filter as well as 
 * to run as standard Cuckoo Filter and also as  PA Cuckoo Filter.
 * 
 * 
 * @author Vijay
 *
 */
public class ConfigPAFilter {
   //Kick Off limit variable
	public static Integer MAX_KICK_OFF = 1000;
	//total number of buckets. 0 ... 1999 = >total = 2000.
	public static Integer BUCKET_SIZE = 16383;
	//Hash algorithm to choose. "MD5" or "SHA1".
	public static String HASH_ALGORITHM = "SHA1";
	
	//input data file.
	public static String Input_FileName = "data/OneMillionInput.txt";
	
	
	
	//lookup file
	public static String LookUp_FileName = "data/OneMillionLookup.txt";
	
	
	//variable to test both standard cuckoo filter and Position Aware Cuckoo Filter.
	//if it is TRUE, then PA filter else, standard filter.
	public static boolean isPAModeOn  = true ;
	
	
}

