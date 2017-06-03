package filter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PrintInsertCount implements Runnable{
	
	static ArrayList<Long> inputList = new ArrayList<Long>();
	static ArrayList<String> lookupList = new ArrayList<String>();


/** Method to read input file and store it in arraylist
 * 
 * @throws NumberFormatException
 * @throws IOException
 */
	public void readinputFile() throws NumberFormatException, IOException
	{
		
	
			BufferedReader buf = new BufferedReader(new FileReader(new File(ConfigPAFilter.Input_FileName)));
			String line = null;
			
			while ((line = buf.readLine()) != null) {
				
				String ip = line.replaceAll("\n", "");

				long prefix = Integer.parseInt(ip);

				inputList.add(prefix);
			   
			}
			
			buf.close();
			
		}
	
	/** Method to read lookup file and store it in arraylist
	 * 
	 * @throws NumberFormatException
	 * @throws IOException
	 */
	public void readLookupFile() throws NumberFormatException, IOException
	{
	
	BufferedReader buf = new BufferedReader(new FileReader(new File(ConfigPAFilter.LookUp_FileName)));

	String line = null;

	while ((line = buf.readLine()) != null) {

		line.replaceAll("\n", "");
		lookupList.add(line);

	}
	buf.close();
	
	}

		
	
	/**
	 * Method to execute the filter and store the result into arraylist.
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static void executeFilter() throws IOException, InterruptedException
	{
	
		// number of times filter has to be executed.
	int loopCount = 10;
	
	List<Integer> countList = new ArrayList<Integer>();
	List<Integer> fprList = new ArrayList<Integer>();

	
	for(int i=0;i<loopCount;i++)
	{
		//change the file name accordingly for std filter and PA filter.
		ScaleUpFilter obj = new ScaleUpFilter();

		//System.out.println(lookupList.size());
		int[] result = obj.executeFilter(obj, inputList,lookupList);
		countList.add(result[0]);
		fprList.add(result[1]);
		
		obj = null;
		
		if(i%10==0)
			System.out.println(i);
		
	}
	synchronized(inputList)
	{
	BufferedWriter bw = new BufferedWriter(new FileWriter("STDfilter_16k_8bits.txt",true ));
	
	for(int i=0;i<countList.size();i++)
	{
		bw.write(countList.get(i)+" ("+fprList.get(i)+")                ");
		
		if(i%5==0)
			bw.newLine();
	}
	
	bw.flush();
	bw.close();
	}}
	
	public void run()
	{
		try {
			executeFilter();
		} catch (NumberFormatException | IOException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	/**
	 * Main method starts here
	 * @param args
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static void main(String[] args) throws IOException, InterruptedException {
		// TODO Auto-generated method stub
		
		PrintInsertCount p1 = new PrintInsertCount();
		p1.readinputFile();
		p1.readLookupFile();

		for(int i=0;i<2;i++)
		{
		Thread t1 = new Thread(p1);
			
		t1.start();
		}
		
		

	}

}
