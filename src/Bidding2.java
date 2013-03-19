import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;


public class Bidding2 {
	private static final int A=0;
	private static final int B=1;
	private static final int C=2;
	private static final int D=3;
	private static final int AB=4;
	private static final int AC=5;
	private static final int AD=6;
	private static final int BC=7;
	private static final int BD=8;
	private static final int CD=9;
	private static ArrayList<Object> resultList;
	
	public static float min(float f,float matrix){
		if(f<matrix)
			return f;
		else
			return matrix;
	}
	
	public static float add(float cost_A, float cost_B){
		if (cost_A==Float.MAX_VALUE || cost_B==Float.MAX_VALUE )
			return Float.MAX_VALUE;
		else
			return cost_A+cost_B;
	}
	
	
	public static void main(String args[]){
		
		if(!InputMatrix.fillInputMatrix())
			return;
		
	
		int[] result = new int[4];
		float previousCost=Float.MAX_VALUE;
		for(int i=0;i<InputMatrix.matrix[A].length;i++){
			//create slot
			int[] slots = new int[InputMatrix.matrix[A].length];
			float cost_A=InputMatrix.matrix[A][i];

			if(cost_A != Float.MAX_VALUE){
				slots[i] = 1;
				result[A]=i;
			}
			else{
				continue;
			}
			//System.out.println("AERRRRRRRRRRRRRRRR!!   " + i);
			

			for(int j=0;j<InputMatrix.matrix[B].length;j++){
				float cost_B,cost_AB;
				cost_B=InputMatrix.matrix[B][j];

				if(i==j){
					cost_AB = min (add(cost_A,cost_B),InputMatrix.matrix[AB][i]);
					if(cost_AB != Float.MAX_VALUE){
						slots[j] = 2;
						result[B]=j;
					}
					else{
						continue;
					}
				}
				else
				{
					if(cost_B != Float.MAX_VALUE){
						slots[j] = 1;
						result[B]=j;
						cost_AB=add(cost_A,cost_B);
					}
					else
						continue;

				}
				//System.out.println("BERRRRRRRRRRRRRRRR!!   " + j + "AB:" + cost_AB );

				for(int k=0;k<InputMatrix.matrix[C].length;k++){
					float cost_C=0.0f,cost_AC=0.0f,cost_BC=0.0f,cost_ABC=0.0f;
					cost_C=InputMatrix.matrix[C][k];
					cost_AC=cost_A + cost_C; 
					cost_BC=cost_B + cost_C;

					/*if(slots[k] == 2){
						System.out.println(slots[k] + "==2 " + k);
						continue;
					}*/
					if (i==j && j==k)
						continue;
					
					if(i==k){
						cost_AC = min (add(cost_A,cost_C),InputMatrix.matrix[AC][i]);
						if(cost_AC != Float.MAX_VALUE){
							slots[k] = 2;
							result[C]=k;
							cost_ABC=add(cost_AC,cost_B);
							//System.out.println("AC+B");
						}
						else{
							//System.out.println("i==k but AC MAX");
							continue;
						}
					}
					else if(j==k){
						cost_BC = min (add(cost_B,cost_C),InputMatrix.matrix[BC][j]);
						if(cost_BC != Float.MAX_VALUE){
							slots[k] = 2;
							result[C]=k;
							cost_ABC=add(cost_BC,cost_A);
							//System.out.println("BC+A");
						}
						else{
							//System.out.println("j==k but BC MAX");
							continue;
						}
					}
					else{
						if(cost_C != Float.MAX_VALUE){
							slots[k] = 1;
							result[C]=k;
							cost_ABC=add(cost_AB,cost_C);
							//System.out.println("AB+C");
						}
						else{
							//System.out.println("C MAX "+ k);
							continue;
						}
					}
					
					//System.out.println("CERRRRRRRRRRRRRRRR!!   " + k + "ABC:" + cost_ABC);


					for(int l=0;l<InputMatrix.matrix[D].length;l++){
						float cost_D=0.0f,cost_AD=0.0f,cost_BD=0.0f,cost_CD=0.0f,cost_ABCD=0.0f;
						/*if(slots[l] == 2)
							continue;*/

						if ((j==k && k==l) || (i==k && k==l) || (i==j && j==l))
							continue;
						cost_D= InputMatrix.matrix[D][l];
						cost_AD=cost_A + cost_D; 
						cost_BD=cost_B + cost_D;
						cost_CD=cost_C + cost_D;

						if(i==l){
							cost_AD = min (add(cost_A,cost_D),InputMatrix.matrix[AD][i]);

							if(cost_AD != Float.MAX_VALUE){
								slots[l] = 2;
								result[D]=l;
								cost_ABCD= add(cost_AD ,cost_BC);
								//System.out.println("BC+AD");
							}
							else
								continue;
						}
						else if(j==l){
							cost_BD = min (add(cost_B,cost_D),InputMatrix.matrix[BD][j]);
							if(cost_BD != Float.MAX_VALUE){
								slots[l] = 2;
								result[D]=l;
								cost_ABCD= add(cost_BD , cost_AC);
								//System.out.println("AC+BD");
							}
							else
								continue;
						}
						else if(k==l){
							cost_CD = min (add(cost_C,cost_D),InputMatrix.matrix[CD][k]);
							if(cost_CD != Float.MAX_VALUE){
								slots[l] = 2;
								result[D]=l;
								cost_ABCD= add(cost_CD , cost_AB);
								//System.out.println("AB+CD");
							}
							else
								continue;
						}
						else{
							if(cost_D != Float.MAX_VALUE){
								slots[l] = 1;
								result[D]=l;
								cost_ABCD= add(cost_ABC , cost_D);
								//System.out.println("ABC+D");
							}
							else
								continue;
						}
						
						//System.out.println("DERRRRRRRRRRRRRRRR!!   " + l + "ABCD:" + cost_ABCD);
						
						if(cost_ABCD < previousCost){
							previousCost=cost_ABCD;
							resultList=new ArrayList<Object>();
							resultList.add(result.clone()); //solution set
							resultList.add(cost_ABCD); //cost
						}
						else if(cost_ABCD == previousCost){
							resultList.add(result.clone()); //solution set
							resultList.add(cost_ABCD); //cost
						}
						
						/*System.out.println("leaf node: " + previousCost);
						System.out.println("alloted: "+previousResult[0] + " "
								+ previousResult[1] + " "
								+ previousResult[2] + " "
								+ previousResult[3] + " "
								);*/
					}
				}
			}
		}
		//System.out.println();
		/*for(int i=0;i<InputMatrix.bidders.length;i++){
			System.out.print(InputMatrix.bidders[i] + "\t");
		}*/

		if(resultList == null){
			System.out.println("\r\n\nNot Enough bidders!!");
			return;
		}
		System.out.print("\r\n\r\n==SOLUTIONS==\r\n\r\n");
		
		String slotStr[]={"A","B","C","D"};
		Hashtable<String,Integer> hMap= new Hashtable<String, Integer>();
		int count=4;
		for(int j=0;j<slotStr.length;j++){
			for(int k=j+1;k<slotStr.length;k++){
				String str=slotStr[j].concat(",").concat(slotStr[k]);
				//System.out.println(str + ":" + (count));
				hMap.put(str, count++);
			}
		}
		
		for(int i=0;i<resultList.size();i++){
			int[] list= (int [])resultList.get(i); 
			Hashtable<String,String> hName= new Hashtable<String, String>();
			Hashtable<String,Float> hCost= new Hashtable<String, Float>();
			for(int j=0;j<slotStr.length;j++){
				if(hName.get(InputMatrix.bidders.get(list[j])) == null){
					hName.put(InputMatrix.bidders.get(list[j]),slotStr[j]);
					hCost.put(InputMatrix.bidders.get(list[j]),InputMatrix.matrix[j][list[j]]);
				}
				else{
					hName.put(InputMatrix.bidders.get(list[j]),
							hName.get(InputMatrix.bidders.get(list[j])).concat(",")
							.concat(slotStr[j]));
					hCost.put(InputMatrix.bidders.get(list[j]),InputMatrix.matrix[hMap.get(hName.get(InputMatrix.bidders.get(list[j])))][list[j]]);
				}
			}
					
			Enumeration<String> keys=hName.keys();	
			while(keys.hasMoreElements()){
				String key=keys.nextElement();
				System.out.print("  ["+key+"->" + hName.get(key) + "  " + "Cost:" + hCost.get(key) + "]  ");
			}
					
			
			System.out.print("Total Cost:" + (Float)resultList.get(++i) + "\r\n");
		}

	}
}
