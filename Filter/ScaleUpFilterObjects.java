package filter;

/**Java program class for each bucket in the filter
 * 
 * 
 * @author Vijay
 *
 */
public class ScaleUpFilterObjects {
	char[] fingerprint;
	 int size;
	 int posSize =3; // index where position 2 element is inserted at first. Then it is inserted in 2 ,1 ,0.
	 //position 1 element is inserted in 0,1,2,3 order.
	
	public  ScaleUpFilterObjects()
	{
		fingerprint = new char[5];
		size =0;
		
		fingerprint[4] = Integer.toString(posSize).charAt(0);
	}
}
