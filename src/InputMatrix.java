import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class InputMatrix {
	public static ArrayList<String> bidders=new ArrayList<String>();
	public static float[][] matrix=null;
	@SuppressWarnings("serial")
	private static class FileCorruptedException extends Exception {
		private String corruptedLine;
		FileCorruptedException(String thisLine){
			corruptedLine=thisLine;
		}
		public void pritCorruptedLine(){
			System.out.println("Corrupted Line: ==>" + corruptedLine);
		}
	}

	private static class BidderRecord{
		public String bidderName;
		public Hashtable<String,Float> slots= new Hashtable<String,Float>();
		public boolean valid=true;
	}
	private static String thisLine;
	public static boolean fillInputMatrix(){
		try {
			BufferedReader fin= new BufferedReader(new FileReader("input.txt"));
			thisLine=fin.readLine();
			if(thisLine==null)
				throw new FileCorruptedException(thisLine);
			ArrayList<Hashtable<String,Float>> temp=new ArrayList<Hashtable<String,Float>>();
			while(thisLine != null){
				if(!thisLine.trim().toUpperCase().startsWith("M/S")){
					thisLine=fin.readLine();
					continue;
				}
				BidderRecord br= new BidderRecord();
				br.bidderName=thisLine.trim();
				thisLine=fin.readLine();

				while(thisLine != null && !thisLine.trim().toUpperCase().startsWith("M/S")){
					String pattern="^\\s*([a-dA-D])\\s*,?\\s*([a-dA-D])?\\s*[:]\\s*(-?\\s*\\d*\\.?\\d*)\\s*$";
					Pattern p = Pattern.compile(pattern);
					Matcher m = p.matcher(thisLine); 
					boolean isValid = m.matches(); 
					if(isValid){
						String slotString=m.group(1);

						if(m.group(2)!=null)
							slotString=m.group(1).concat(m.group(2));
						float amount;

						try{
							amount=Float.parseFloat(m.group(3));
							//System.out.println("\nRivet: "+amount);
						}
						catch (NumberFormatException e) {
							System.out.print("Bidder:" + br.bidderName +" is Invalid.\r\n");
							System.out.print("Invalid amount provided!\r\n");
							br.valid=false;
							break;
						}
						if(slotString.length()==1){
							br.slots.put(slotString, amount);
						}
						else if(slotString.length()==2){
							char[] c1={slotString.charAt(0)};
							char[] c2={slotString.charAt(1)};
							try{
								float a= br.slots.get(new String(c1)); //may throw NPE
								float b= br.slots.get(new String(c2)); //may throw NPE

								float ab=(a+b)*((100-amount)/100);
								if(a==0.0f || b==0.0f || ab< a || ab < b ){
									System.out.print("Bidder:" + br.bidderName +" is Invalid.\r\n");
									System.out.print("Bid for " + c1[0]+ ""+ c2[0] + " is not suitable!\r\n");
									br.valid=false;
									break;
								}
								else{
									br.slots.put(slotString, ab);
								}
							}
							catch(NullPointerException e){
								System.out.print("Bidder:" + br.bidderName +" is Invalid; ");
								System.out.print("Bid for " + c1[0]+ ""+ c2[0] + " is not suitable!\r\n");
								br.valid=false;
								break;
							}
						}
						thisLine=fin.readLine();
						continue;
					}
					if(!thisLine.matches("\\s*")){
						throw new FileCorruptedException(thisLine);
					}
					thisLine=fin.readLine();
				}
				if(br.valid){
					System.out.print(br.bidderName + ": Adding it to matrix.\r\n");
					bidders.add(br.bidderName);
					temp.add(br.slots);
				}
			}
			if(temp.size()==0){
				throw new FileCorruptedException(thisLine);
			}
			else{
				String slotStr[]={"A","B","C","D","AB","AC","AD","BC","BD","CD"};
				matrix=new float[10][temp.size()];
				
				System.out.print("\r\n==INPUT==\r\n");
				System.out.printf("%8s\t%8s\t%8s\t%8s\t%8s\t%8s\t%8s\t%8s\t%8s\t%8s\t%8s\t",
						"BIDDER","A","B","C","D","AB","AC","AD","BC","BD","CD");
				System.out.print("\r\n------------------------------------------------------------------------------------------------------------------------------");
				System.out.print("---------------------------------------------");
				for(int i=0;i<temp.size();i++){
					System.out.printf("\r\n%8s\t",bidders.get(i));
					for(int j=0;j<10;j++){
						if(temp.get(i).get(slotStr[j]) != null)
							matrix[j][i]=temp.get(i).get(slotStr[j]);
						else
							matrix[j][i]=Float.MAX_VALUE;
						if(matrix[j][i] == Float.MAX_VALUE)
							System.out.printf("%8s\t","N/A");
						else
							System.out.printf("%.5f\t",matrix[j][i]);
					}
				}
			}

		} catch (FileNotFoundException e) {
			System.out.print("File not found!!\r\n");
			return false;
		}
		catch (FileCorruptedException e) {
			e.pritCorruptedLine();
			System.out.print("File Corrupted!!\r\n");
			//e.printStackTrace();
			return false;
		}
		
		catch (Exception e) {
			e.printStackTrace();
			return false;
		}

		return true;

	}
}
