package filter;

public class ScaleUpFilterObjects {
	char[] fingerprint;
	 int size;
	 int posSize =3;
	
	public  ScaleUpFilterObjects()
	{
		fingerprint = new char[5];
		size =0;
		
		fingerprint[4] = Integer.toString(posSize).charAt(0);
	}
}
