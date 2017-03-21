package filter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
/** Java program to implement Position-Aware Cuckoo Filter and compare its result with standard Cuckoo Filter*/

public class ScaleUpPAFilter {

	ScaleUpFilterObjects filter[];
	int insertedCount;
	int successCount;
	int failureCount;
	Map<Integer, ArrayList<Long>> routeMap;
	ArrayList<Long> routeIp;
	private int lookupNum;
	private int checkAllPos2;
	private int checkAllPos1;

	private final Integer Max_Kicks = ConfigPAFilter.MAX_KICK_OFF;
	private int filterSize = ConfigPAFilter.BUCKET_SIZE;
	String hashAlgorithm = ConfigPAFilter.HASH_ALGORITHM;

	public ScaleUpPAFilter() {
		//creating 2000 filter buckets [ 0 ...1999]
		filter = new ScaleUpFilterObjects[filterSize + 1];

		routeMap = new HashMap<Integer, ArrayList<Long>>();
		
		//creating objects for each bucket.
		for (int i = 0; i <= filterSize; i++)
			filter[i] = new ScaleUpFilterObjects();

		insertedCount = 0;
		successCount = 0;
		failureCount = 0;
		lookupNum = 0;
		checkAllPos2 = 0;
		checkAllPos1 = 0;
	}

	/* Method to initialize all the cells of each bucket with  value "128".
	 * The value "128" is chosen because in java, MD5 Hash value can range from 0 to 127. 
	 * So, inorder to detect empty cell or not, value 128 is chosen.
	 * 
	 * Each bucket contains 5 cells range from 0 to 4. 
	 * cells 0 to 3 store fingerprints and cell 4 store position values corresponding to elements inserted using position 2. 
	 * 
	 */
	private void initializeFilter() {
		for (int i = 0; i <= filterSize; i++) {
			for (int j = 0; j < 4; j++) {
				filter[i].fingerprint[j] = (128) & 255; // initializing with val
														// 128.
			}

		}

	}

	/* Method to convert IP address to decimal numbers*/
/*	public long convertIptoDecimal(String ip) {
		long prefix = 0;

		// 256 is the base of ip addr. So, 256^3, 256^2,..256^0 is performed on
		// ip addr.

		String[] ipArr = ip.split("\\.");
		for (int i = 0; i < ipArr.length; i++) {
			int pow = 3 - i;

			int pre = Integer.parseInt(ipArr[i]);

			prefix += (pre * Math.pow(256, pow));

		}

		return prefix;
	}*/

	/* Method to invoke MD5 hash algorithm and calculate the hash value*/
	private String calculateHash(String prefix, String algorithm) {
		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance(algorithm);

			byte[] hashedBytes = digest.digest(prefix.getBytes("UTF-8"));
			return convertByteArrtoString(hashedBytes);

		} catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
    /*Method to convert byte array of the hash value  into string value */
	private String convertByteArrtoString(byte[] hashedVal) {

		StringBuilder build = new StringBuilder();
		for (int i = 0; i < hashedVal.length; i++) {
			build.append(Integer.toString((hashedVal[i] & 0xff) + 0x100, 16).substring(1));
		}
		return build.toString();

	}

	/* Method which takes input number and convert it into fingerprint of char type.*/
	private char generateFingerprint(long prefix) {
		String hashStr = calculateHash(String.valueOf(prefix), hashAlgorithm);
		BigInteger val = new BigInteger(hashStr, 16);
		int mappedValue = val.intValue();
		mappedValue = (mappedValue & 0xFF) % 127;
		char fp = (char) mappedValue;
		return fp;

	}

	/*Method to calculate the position  of the fingerprint in the filter
	 input : prefix : Input number
	         algorithm : Hash Algorithm;
	output  : position value for the input number.*/

	private int calculatePosition(String prefix, String algorithm) {
		String hash = calculateHash((prefix), algorithm);
		BigInteger val = new BigInteger(hash, 16);
		int mappedValue = val.intValue();

		mappedValue = mappedValue % filterSize;
		if (mappedValue < 0) {
			mappedValue = mappedValue * -1;
		}
		return mappedValue;

	}

	/* Method to check whether element is successfully inserted in the filter in either of the two positions
	 * input : fp - fingerprint
	 *         pos - position value in the filter
	 *         hashPosition - position 1 or position 2.
	 *         
	 *  output: true - insert success.
	 *          false - insert fail.         
	 * 
	 * Note: In each bucket,
	 *       Elements for position 1 are inserted in the following order indices: 0,1,2,3.
	 *       Elements for position 2 are inserted in the following order indices : 3,2,1,0. 
	 *        So, position cell( cell 4) is initialized with value 3 
	 *      and it decreases to 2,1,0 when the elements are inserted.  
	 */
	private boolean isInsertSucess(char fp, int pos, int hashPosition) {
         
		//Ascii value of number 3 is 51 and so to get number 3, 51 mod 48 = 3 ;
		int secondPos = (int) (filter[pos].fingerprint[4]) % 48;
		//Ascii value for minus "-" is 45. when all cells are occupied by position 2 elements,
		//then position cell value is decreased to -1 to indicate bucket is fully occupied. 

		if (secondPos == 45)
			return false;

		else if (hashPosition == 1) {

			for (int i = 0; i <= secondPos; i++) {
				int posVal = (int) filter[pos].fingerprint[i];
				//if cell is empty, insert the fingerprint.
				if (posVal == 128) {
					filter[pos].fingerprint[i] = fp;
					return true;
				}

			}
			return false;

		} else if (hashPosition == 2) {
			int insertPos = secondPos;
			int posVal = (int) (filter[pos].fingerprint[insertPos]);
			//if the current position is not empty, then all the cells in the bucket are occupied.
			if (posVal != 128)
				return false;

			filter[pos].fingerprint[insertPos] = fp;
			insertPos = insertPos - 1;

			filter[pos].fingerprint[4] = convertIntToChar(insertPos); //updating position cell by decreasing one value.

		}

		return true;

	}

	private char convertIntToChar(int val) {
		return Integer.toString(val).charAt(0);
	}

     /*Method to check whether  element is inserted into the filter.
      * If not, it will do kickOff and try to insert the element
      */
	private boolean insertItem(char fp, int pos1, int pos2) {

		
		//Try to insert element in either of the two positions.
		if (isInsertSucess(fp, pos1, 1))
			return true;
		if (isInsertSucess(fp, pos2, 2))
			return true;
		
	/*Since element insertion is full, it will KickOFF exising element. 
	 * It will be done as,
	 * Element from position 1 is removed ,say element "a" and current element is inserted in position 1.
	 * Position 2 of element "a" is calculated and try to insert it in the bucket. 
	 * If that fails, then position 1 element from the same bucket as element "a"'s second position is removed 
	 * and element "a" is inserted in second position and its position cell is updated. 
	 * Then this cycle follows till it reaches KickOFF limit.
	 */
	 
		int pos = pos1;

		int toInsertPos = (int) (filter[pos].fingerprint[4]) % 48;
		if ((toInsertPos != 45)) {
			char tmpFp = filter[pos].fingerprint[toInsertPos];

			filter[pos].fingerprint[toInsertPos] = fp;

			for (int i = 0; i < Max_Kicks; i++) {

				int nextPos = (calculatePosition(tmpFp + "", hashAlgorithm) ^ pos);

				nextPos = nextPos % filterSize;
				if (isInsertSucess(tmpFp, nextPos, 2))
					return true;
				else {
					int secondSize = (int) (filter[nextPos].fingerprint[4]) % 48;
					if ((secondSize != 45)) {
						char firstFp = filter[nextPos].fingerprint[secondSize];

						filter[nextPos].fingerprint[secondSize] = tmpFp;
						int insertPos = secondSize;
						insertPos = insertPos - 1;
						filter[nextPos].fingerprint[4] = convertIntToChar(insertPos);
						pos = nextPos;
						tmpFp = firstFp;

					} else
						return false;

				}

			}
		}
		System.out.println("Max Kicks reached");
		return false;

	}

	private boolean insertToLacf(char fp, int pos1, int pos2) {
		if (insertItem(fp, pos1, pos2)) {
			insertedCount++;
			return true;
		}

		return false;

	}

	private void insertIntoFilter(long prefix) {
		char fingerprint = generateFingerprint(prefix);


		int pos1 = -1, pos2 = -1;

		pos1 = calculatePosition(String.valueOf(prefix), hashAlgorithm);
		pos2 = (calculatePosition(fingerprint + "", hashAlgorithm)) ^ pos1; // calculating
																			// pos2
																			// by
																			// XOR.

		pos2 = pos2 % filterSize;


		if (!insertToLacf(fingerprint, pos1, pos2))
			System.out.println(prefix);

	}

	private void insertIntoMap(long prefix) {

		/*
		 * if (!(routeMap.containsKey(preLen))) { routeIp = new
		 * ArrayList<Long>(); routeIp.add(prefix); routeMap.put(preLen,
		 * routeIp); } else routeMap.get(preLen).add(prefix);
		 */

		insertIntoFilter(prefix);

	}
/*Method to read input random number file and convert it into long value*/
	public void insertInputFile(String name) throws IOException {
		BufferedReader buf = new BufferedReader(new FileReader(new File(name)));
		String line = null;
		while ((line = buf.readLine()) != null) {

			String ip = line.replaceAll("\n", "");
			
			long prefix = Integer.parseInt(ip);
			
			insertIntoMap(prefix);

		}

	}
	
/*Method to check whether particular fingerprint is found in the bucket
 * 
 * input: fp - finperprint
 *        pos - position value in int
 *        hashPosition - position 1 or position 2
 * */
	
	private boolean isIpFound(char fp, int pos, int hashPosition) {

		int start = -1;
		int end = 0;
		
		/* isPAModeOn : variable to denote Position Aware Mode ON.
		 * if it is True, then for position 1,
		 *  it searches from 0(inclusive) till first occurence of position 2 element (exclusive) 
		 *  
		 *  if it is False, it can search as standard cuckoo filter,
		 *  for position 1 and position 2, it searches from 0 till 3 (both inclusive).		  
		 */
		if (ConfigPAFilter.isPAModeOn) {
			start = (int) (filter[pos].fingerprint[4]) % 48;

			if (start == 45) {
				start = -1;

			}
			end = start;

		} else {
			end = 3;
			start = -1;

		}

		//checking the bucket corresponding to position 1 of the element..
		if (hashPosition == 1) {

			for (int i = 0; i <= end; i++) {
				int posVal = (int) (filter[pos].fingerprint[i]);
				//checking whether it is empty cell .
				if (posVal != 128) {
					if (filter[pos].fingerprint[i] == fp)
						return true;
				}else
					continue;
			}
		} 
		//checking the bucket corresponding to position 2 of the element..
		//Position 2 cannot be empty cell, because of the position cell containing starting position of element .
		else {

			for (int i = start + 1; i < 4; i++) {
				if (filter[pos].fingerprint[i] == fp)
					return true;
			}
		}
		return false;

	}

	private boolean searchInFilter(long prefix) {
		int pos1 = -1;
		int pos2 = -1;

		char fp = generateFingerprint(prefix);
		//calculate position 1 of the element.
		pos1 = calculatePosition(String.valueOf(prefix), hashAlgorithm);

		//calculate position 2 of the element.
		pos2 = (calculatePosition(fp + "", hashAlgorithm)) ^ pos1;

		pos2 = pos2 % filterSize;
		

		if (isIpFound(fp, pos1, 1))
			return true;
		if (isIpFound(fp, pos2, 2))
			return true;
		return false;
	}

	private boolean search(long prefix) {

		long ip = prefix;
    //if the element is found in the filter, then return true
		if (searchInFilter(ip)) {
			

			return true;
		}
		
		return false;

	}

	List<String> failureList = new ArrayList<String>();

	private void lookup(String file) throws IOException {

		BufferedReader buf = new BufferedReader(new FileReader(new File(file)));

		ArrayList<String> lookupList = new ArrayList<String>();
		String line = null;
		while ((line = buf.readLine()) != null) {
			line.replaceAll("\n", "");
			lookupList.add(line);
		}

		for (String i : lookupList) {
			long prefix = Integer.parseInt(i);
			if (search(prefix))
				successCount++;
			else {
				failureList.add(i);

				failureCount++;
			}

		}


	}

 //main method starts here
	public static void main(String[] args) throws IOException {
		ScaleUpPAFilter filterObj = new ScaleUpPAFilter();

		String FileName = ConfigPAFilter.Input_FileName;
		filterObj.initializeFilter();
		filterObj.insertInputFile(FileName);
		System.out.println("Total inserted entries " + filterObj.insertedCount);
		String lookupFile = ConfigPAFilter.LookUp_FileName;
		filterObj.lookup(lookupFile);
		System.out.println("False positive count is: " + filterObj.successCount);
		System.out.println("Failure Count is: " + filterObj.failureCount);
	
	}
}
