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
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * This program implements Position-Aware Cuckoo Filter as well as standard
 * Cuckoo Filter and compare their results with one another in terms of False
 * Positive Rate, storage, Number of elements inserted into the filter.
 */

public class ScaleUpPAFilter {

	ScaleUpFilterObjects filter[];
	int insertedCount;
	int elementsInsertedCount;
	int elementsInsertFailCount;

	private final Integer Max_Kicks = ConfigPAFilter.MAX_KICK_OFF;
	private int filterSize = ConfigPAFilter.BUCKET_SIZE;
	String hashAlgorithm = ConfigPAFilter.HASH_ALGORITHM;

	/**
	 * constructor instantiating array of objects to store buckets of the filter
	 * 
	 */
	public ScaleUpPAFilter() {

		// creating 2000 filter buckets [ 0 ...1999]
		filter = new ScaleUpFilterObjects[filterSize + 1];

		// creating objects for each bucket.
		for (int i = 0; i <= filterSize; i++)
			filter[i] = new ScaleUpFilterObjects();

		insertedCount = 0;
		elementsInsertedCount = 0;
		elementsInsertFailCount = 0;

	}

	/**
	 * Each bucket contains 5 cells range from 0 to 4. cells 0 to 3 store
	 * fingerprints of elements and cell 4 called position cell, tracks number
	 * of elements inserted in their second position.
	 * 
	 * 
	 * <P>
	 * Method to initialize first 4 cells (0,1,2,3) of each bucket with value
	 * "128". The value "128" is chosen because in java, MD5 Hash value can
	 * range from 0 to 127. So, inorder to detect empty cell or not, value 128
	 * is chosen.
	 * 
	 */
	private void initializeFilter() {
		for (int i = 0; i <= filterSize; i++) {
			for (int j = 0; j < 4; j++) {
				filter[i].fingerprint[j] = (char)4095;
				//initializing with 4095 to mark empty cell for a fingerprint size of 12 bits.		
														

			}

		}

	}

	/**
	 * Method to invoke hash algorithm and calculate the hash value
	 * 
	 * @param prefix
	 *            - number for which fingerprint is to be calculated.
	 * @param algorithm
	 *            - MD5 or SHA1 algorithm to calculate hash value.
	 * 
	 * @return - string hash value.
	 * 
	 */
	private String calculateHash(String prefix, String algorithm) {
		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance(algorithm);

			byte[] hashedBytes = digest.digest(prefix.getBytes("UTF-8"));  
			return convertByteArrtoString(hashedBytes);

		} catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Method to convert byte array of the hash value into string value
	 * 
	 * @param hashedVal
	 *            - byte array of hash value calculated from Hash algorithm
	 * @return - string hash value.
	 * 
	 */
	private String convertByteArrtoString(byte[] hashedVal) {

		StringBuilder build = new StringBuilder();
		for (int i = 0; i < hashedVal.length; i++) {
			build.append(Integer.toString((hashedVal[i] & 0xFF) + 0x100, 16).substring(1));
		}
		return build.toString();

	}

	/**
	 * Method which takes input number and convert it into fingerprint of char
	 * type.
	 * 
	 * @param prefix
	 *            - number to be inserted into the filter.
	 * @return - fingerprint character value.
	 */
	private char generateFingerprint(long prefix) {
		String hashStr = calculateHash(String.valueOf(prefix), hashAlgorithm);
		BigInteger val = new BigInteger(hashStr, 16);
		int mappedValue = val.intValue();
		mappedValue = (mappedValue & 0xFFF ) %4094;      
		
		if (mappedValue < 0) {
			mappedValue = mappedValue * -1;
		}
		char fp = (char) mappedValue;
		return fp;

	}

	/**
	 * Method to calculate the position of the fingerprint where it can be
	 * inserted in the filter
	 * 
	 * @param prefix
	 *            - String value for which fingerprint is to be calculated.
	 * @param algorithm
	 *            - MD5 or SHA1 algorithm to calculate hash value.
	 * 
	 * @return - bucker number in the filter.
	 */

	private int calculatePosition(String prefix, String algorithm) {
		String hash = calculateHash((prefix), algorithm);
		BigInteger val = new BigInteger(hash, 16);
		int mappedValue = val.intValue();

		mappedValue =mappedValue  % filterSize;  
		if (mappedValue < 0) {
			mappedValue = mappedValue * -1;
		}
		return mappedValue;

	}

	/**
	 * Method to check whether element is successfully inserted in the filter in
	 * either of the two positions .
	 * 
	 * <P>
	 * In each bucket, elements for position 1 are inserted in the following
	 * order indices: 0,1,2,3. Elements for position 2 are inserted in the
	 * following order indices : 3,2,1,0. So, position cell( cell 4) is
	 * initialized with value 3 and it decreases to 2,1,0 when the elements are
	 * inserted in position 2.
	 * 
	 * @param fp
	 *            - fingerprint of the number
	 * @param position
	 *            - position value in the filter
	 * @param hashPosition
	 *            - position 1 or position 2 of the fingerprint.
	 * 
	 * @return true - insert success. false - insert fail.
	 */
	private boolean isInsertSucess(char fp, int position, int hashPosition) {

		// Ascii value of number 3 is 51 and so to get number 3, 51 mod 48 = 3 ;
		int secondPos = (int) (filter[position].fingerprint[4]) % 48;
		// Ascii value for minus "-" is 45. when all cells are occupied by
		// position 2 elements,
		// then position cell value is decreased to -1 to indicate bucket is
		// fully occupied.

		if (hashPosition == 1) {
			for (int i = 0; i <= secondPos; i++) {
				int posVal = (int) filter[position].fingerprint[i];
				// if cell is empty, insert the fingerprint.
				if (posVal == 4095) { 
					filter[position].fingerprint[i] = fp;
					return true;
				}

			}
			return false;

		}

		else if (secondPos == 2) // already 3 elements are occupied in 2nd
									// posiition
			return false;

		else if (hashPosition == 2) {

			
			
			int insertPos = secondPos;

			int posVal = (int) (filter[position].fingerprint[insertPos]);

			// if the current position is not empty, then all the cells in the
			// bucket are occupied.

			if (posVal != 4095)
				return false;

			filter[position].fingerprint[insertPos] = fp;
			// updating position cell once the element has been inserted in that
			// location.
			insertPos = insertPos - 1;

			filter[position].fingerprint[4] = convertIntToChar(insertPos);
			

		}

		return true;

	}

	/**
	 * method to convert the int value to ASCII value
	 * 
	 * @param val
	 *            - int value
	 * @return ASCII value for the corresponding input int value.
	 */
	private char convertIntToChar(int val) {
		return Integer.toString(val).charAt(0);
	}

	/**
	 * Method to check whether element is inserted into the filter. If not, it
	 * will do kickOff and try to insert the element
	 * 
	 * @param fp
	 *            - fingerprint of the element
	 * @param pos1
	 *            - position 1 of the fingerprint in the filter
	 * @param pos2
	 *            - position 2 of the fingerprint in the filter
	 * 
	 * @return boolean - Insert success or failure.
	 */
	private boolean insertItem(char fp, int pos1, int pos2) {

		// Try to insert element in either of the two positions.
		if (isInsertSucess(fp, pos1, 1))
			return true;
		if (isInsertSucess(fp, pos2, 2)) 
			return true;

		/*
		 * Since element insertion is full, it will start KickingOFF elements.
		 * It will be done as, element from position 1 is removed ,say element
		 * "a" and current element is inserted in position 1. Position 2 of
		 * element "a" is calculated and try to insert it in the bucket. If that
		 * fails, then position 1 element from the same bucket as element "a"'s
		 * second position is removed and element "a" is inserted and its
		 * position cell is updated. Then this cycle follows till it reaches
		 * KickOFF limit.
		 */

		int elementKickedOutPos = 0;
		int hashPosofElement = 0;
		int secHashPos = (int) (filter[elementKickedOutPos].fingerprint[4]) % 48;
		char tempFp = '\0';
		int nextInsertPos = 0;
		// choosing random position to start kick off.
		Random rand = new Random();
		int pickPos = rand.nextInt(2);
		if (pickPos == 0) {
			elementKickedOutPos = pos1;
			hashPosofElement = 1;

		} else {
			elementKickedOutPos = pos2;
			hashPosofElement = 2;

		}

		for (int i = 0; i < Max_Kicks; i++) {

			if (hashPosofElement == 1) {

				nextInsertPos = 2;
				// choosing random position to insert the fingerprint
				int randPos = rand.nextInt(4);
				// check to see if random pos is occupied by sec hash element
				if (randPos > (secHashPos + 1)) {
					tempFp = filter[elementKickedOutPos].fingerprint[randPos];
					char toBeSwapedFP = filter[elementKickedOutPos].fingerprint[secHashPos + 1];

					// swapping the elements so that the 1 pos element can be
					// inserted
					filter[elementKickedOutPos].fingerprint[randPos] = toBeSwapedFP;
					filter[elementKickedOutPos].fingerprint[secHashPos + 1] = fp;
					// incrementing the value of position cell
					int insertPos = secHashPos;
					insertPos = insertPos + 1;
					filter[elementKickedOutPos].fingerprint[4] = convertIntToChar(insertPos);

				} else {// if random position is occupied by 1 hash element
					tempFp = filter[elementKickedOutPos].fingerprint[randPos];
					filter[elementKickedOutPos].fingerprint[randPos] = fp;

				}

			} else if (hashPosofElement == 2) {
				nextInsertPos = 1;
				int randPos = 0;
				// choosing between 3 elements .
				if (secHashPos == 2) {
					int max = 3;
					int min = 1;
					//randPos = rand.nextInt((max - min) + 1) + min;
					 randPos = 3;

				} else if (secHashPos > 2) {

					randPos = rand.nextInt(4);
				}

				// random is occupied by 2 hash pos element.
				if (randPos > secHashPos) {// swapping current 2 hash pos
											// element with new one.
					tempFp = filter[elementKickedOutPos].fingerprint[randPos];
					filter[elementKickedOutPos].fingerprint[randPos] = fp;
				} else {

					tempFp = filter[elementKickedOutPos].fingerprint[randPos];

					if (randPos != secHashPos) {
						char toBeSwapedFP = filter[elementKickedOutPos].fingerprint[secHashPos];

						// swapping the elements so that the 2 pos element can
						// be inserted
						filter[elementKickedOutPos].fingerprint[randPos] = toBeSwapedFP;
					}
					filter[elementKickedOutPos].fingerprint[secHashPos] = fp;
					// decrementing the value of position cell
					int insertPos = secHashPos;
					insertPos = insertPos - 1;
					filter[elementKickedOutPos].fingerprint[4] = convertIntToChar(insertPos);

				}

			}

			int nextPosOfKickedoutElement = (calculatePosition(tempFp + "", hashAlgorithm) ^ elementKickedOutPos);

			nextPosOfKickedoutElement = nextPosOfKickedoutElement % filterSize;

			if (isInsertSucess(tempFp, nextPosOfKickedoutElement, nextInsertPos))
				return true;
			else {
				hashPosofElement = nextInsertPos;
				fp = tempFp;
				elementKickedOutPos = nextPosOfKickedoutElement;

			}

		}

		// System.out.println("Max Kicks reached");
		return false;

	}

	private boolean insertToLacf(char fp, int pos1, int pos2) {
		if (insertItem(fp, pos1, pos2)) {
			insertedCount++;
			return true;
		}

		return false;

	}

	/**
	 * Method to calculate two positions in the filter for the number .
	 * 
	 * @param prefix
	 *            - number to be inserted into the filter.
	 * 
	 */
	private boolean insertIntoFilter(long prefix) {
		char fingerprint = generateFingerprint(prefix);

		int pos1 = -1, pos2 = -1;

		pos1 = calculatePosition(String.valueOf(prefix), hashAlgorithm);
		pos2 = (calculatePosition(fingerprint + "", hashAlgorithm)) ^ pos1; // calculating
																			// pos2
																			// by
																			// XOR.

		pos2 = pos2 % filterSize;

		if (!insertToLacf(fingerprint, pos1, pos2)) {
			// System.out.println(prefix);
			return false;

		}
		return true;
	}

	/**
	 * Method to read input file containing random numbers and insert it into the filter
	 * 
	 * @param ipList - List containing random numbers
	 *          
	 * 
	 */
	

	public void insertInputFile(List<Long> ipList) throws IOException {

		Random r = new Random();
		int countToInsert = 0;
		int start = r.nextInt(98000);
		for (int i = start; i < ipList.size(); i++) {
			boolean status = insertIntoFilter(ipList.get(i));
			countToInsert++;
			if (!status)
				break;
		}

	}

	/**
	 * Method to check whether particular fingerprint is found in the bucket
	 * 
	 * @param fp
	 *            - fingerprint of the number
	 * @param position
	 *            - position value in the filter
	 * @param hashPosition
	 *            - position 1 or position 2 of the fingerprint.
	 * 
	 * @return true - insert success. false - insert fail.
	 */

	private boolean isIpFound(char fp, int position, int hashPosition) {

		int start = -1;
		int end = 0;

		/*
		 * isPAModeOn : variable to denote Position Aware Mode ON. if it is
		 * True, then for position 1, it searches from 0(inclusive) till first
		 * occurence of position 2 element (exclusive)
		 * 
		 * if it is False, it can search as standard cuckoo filter, for position
		 * 1 and position 2, it searches from 0 till 3 (both inclusive).
		 */
		if (ConfigPAFilter.isPAModeOn) {
			start = (int) (filter[position].fingerprint[4]) % 48;

			if (start == 45) {
				start = -1;

			}
			end = start;

		} else {
			end = 3;
			start = -1;

		}

		// checking the bucket corresponding to position 1 of the element..
		if (hashPosition == 1) {

			for (int i = 0; i <= end; i++) {
				int posVal = (int) (filter[position].fingerprint[i]);
				//System.out.println("check empty cell "+posVal);
				// checking whether it is empty cell .
				if (posVal != 4095) { //changed for 12 bits
					if (filter[position].fingerprint[i] == fp)
						return true;
				} else
					continue;
			}
		}
		// checking the bucket corresponding to position 2 of the element..
		// Position 2 cannot be empty cell, because of the position cell
		// containing starting position of element .
		else {

			for (int i = start + 1; i < 4; i++) {
				if (filter[position].fingerprint[i] == fp)
					return true;
			}
		}
		return false;

	}

	/**
	 * Method to search whether the number is present in the filter
	 * 
	 * 
	 * @param prefix
	 *            - number to be searched
	 * @return boolean - True- number present, False - number not present
	 */
	private boolean searchInFilter(long prefix) {
		int pos1 = -1;
		int pos2 = -1;

		char fp = generateFingerprint(prefix);
		// calculate position 1 of the element.
		pos1 = calculatePosition(String.valueOf(prefix), hashAlgorithm);

		// calculate position 2 of the element.
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
		// if the element is found in the filter, then return true
		if (searchInFilter(ip)) {

			return true;
		}

		return false;

	}

	private void lookup(List<String> lookupList) throws IOException {

	

		for (String i : lookupList) {
			long prefix = Integer.parseInt(i);
			if (search(prefix))
				elementsInsertedCount++;
			else {
				elementsInsertFailCount++;
			}

		}

	}

	/**
	 * Main method execution begins here. Note : Use PrintInsertCount.java file
	 * to run either PAFilter or standard filter. Avoid running from this main
	 * method.
	 *
	 * @throws IOException
	 *             - if the input file is not found.
	 */
	public static void main(String[] args) throws IOException {
		ScaleUpPAFilter filterObj = new ScaleUpPAFilter();

		String FileName = ConfigPAFilter.Input_FileName;
		filterObj.initializeFilter();
		System.out.println("Total inserted entries " + filterObj.insertedCount);
		String lookupFile = ConfigPAFilter.LookUp_FileName;
		System.out.println("False positive count is: " + filterObj.elementsInsertedCount);
		System.out.println("Failure Count is: " + filterObj.elementsInsertFailCount);

	}

	public int[] executeFilter(ScaleUpPAFilter filterObj, List<Long> ipList, List<String> lookupList) throws IOException {
		filterObj.initializeFilter();
		filterObj.insertInputFile(ipList);
		filterObj.lookup(lookupList);
		int[] result = new int[2];
		result[0] = filterObj.insertedCount;
		result[1] = filterObj.elementsInsertedCount;
		return result;

	}
}
