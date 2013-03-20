package com.srts.phaseI.path;

import gov.nasa.jpf.symbc.numeric.Expression;
import gov.nasa.jpf.symbc.numeric.ExpressionType;
import gov.nasa.jpf.symbc.numeric.IntegerConstant;
import gov.nasa.jpf.symbc.numeric.IntegerExpression;
import gov.nasa.jpf.symbc.numeric.ObjectExpression;
import gov.nasa.jpf.symbc.numeric.RealExpression;
import gov.nasa.jpf.symbc.numeric.SymbolicInteger;
import gov.nasa.jpf.symbc.numeric.SymbolicReal;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class VSM {
	private HashMap<String,ValueString> hs=new HashMap<String, VSM.ValueString>();
	private boolean updated=false;
	private boolean valid=true;
	
	public VSM(String name){
		hs.put(name,null);
	}
	
	public VSM(String name,Expression value){
		hs.put(name,new ValueString(value));
	} 
	
	public VSM() {
		// TODO Auto-generated constructor stub
	}

	public void justUpdated(){
		updated=true;
	}
	public boolean isUpdated(){
		return updated;
	}
	public void setUpdatedBy(String name, int pos,String in, String meth){
		try{
			hs.get(name).setUpdatedBy(pos+ " " + in+ " of "+ meth);
		}
		catch(NullPointerException npe){
			ValueString v=new ValueString(null);
			v.setUpdatedBy(in);
			hs.put(name, v);
		}
	}
	public String getUpdatedBy(String name){
		return (hs.get(name)!=null)? hs.get(name).getUpdatedBy():null;
	}
	
	public HashMap<Expression,Expression> getExpressionMap(){
		Iterator<String> it=hs.keySet().iterator();
		HashMap<Expression,Expression> hm=new HashMap<Expression, Expression>();
		while(it.hasNext()){
			final String s=it.next();
			//hs.get(s).getUpdatedBy()==null)  
			boolean valueNull=false;
			Expression value= hs.get(s).getValue();
			if(value.getType()==ExpressionType.UNKNOWN || (hs.get(s).getUpdatedBy()==null)){
				valueNull=true;
			}
			Expression e=null;
			if(value instanceof RealExpression){
				e=new SymbolicReal(s);
				hm.put(e, valueNull?null:value);
			} 
			else if(value instanceof IntegerExpression){
				e=new SymbolicInteger(s);
				hm.put(e, valueNull?null:value);
			}
			else if(value instanceof ObjectExpression){
				Expression e1=new SymbolicInteger(s+"("+(valueNull?"null":((ObjectExpression)value).getInstanceOf())+","+(valueNull?"null":((ObjectExpression)value).getState())+")");
				Expression e2=(new IntegerConstant(1));
				if(hs.get(s).getUpdatedBy()!=null){
					hm.put(e1, e2);
				}
			}
			else {
				System.out.println("VSM.java:65 Error: other than Integer, Real or Object expression!!" + e);
			}
		}
		return hm;
	}
	
	
	public Iterator<String> getNames(){
		return hs.keySet().iterator();
	}
	
	public Set<String> getNameSet(){
		return hs.keySet();
	}
	public void updateValue(String name,Expression value){
		justUpdated();
		ValueString vs=hs.get(name);
		if (vs==null)
			hs.put(name,new ValueString(value));		
		else
			vs.value=value;
	}
	
	/*public void updateValue(String name,Expression value,Instruction updatedBy){
		hs.put(name,new ValueString(value,updatedBy));
	}*/
	public void invalidate(){
		valid=false;
/*		ValueString vs=hs.get(name);
		vs.invalidate();*/
	}
	public boolean isValid(){
		return valid;//hs.get(name).isValid();
	}
	public Expression getValue(String name){
		return hs.get(name).getValue();
	}
	
	public boolean equals(Object o){
		VSM v=(o instanceof VSM)?(VSM)o:null;
		if(updated==v.isUpdated() && valid==v.isValid()){
			Map<Expression,Expression> m1=getExpressionMap(),m2= v.getExpressionMap();
			if(m1==m2)
				return true;
			Iterator<Expression> it1= m1.keySet().iterator();
			Iterator<Expression> it2=m2.keySet().iterator();
			while(it1.hasNext()|| it2.hasNext()){
				Expression e1=it1.next(),e2=it2.next();
				if(!e1.equals(e2) && !m1.get(e1).equals(m2.get(e2)))
					return false;
			}
			return true;
		}
		return false;
	}

	class ValueString{
		private Expression value;
		private String updatedBy;
		private boolean valid=true;
		public boolean isValid(){
			return valid;
		}
		public ValueString(Expression value) {
			this.value=value;
		}
		public ValueString(Expression value,String updatedBy) {
			this.value=value;
			this.updatedBy=updatedBy;
		}
		public void invalidate(){
			valid=false;
		}
		public Expression getValue() {
			return value;
		}
		public void setValue(Expression value) {
			this.value = value;
		}
		public String getUpdatedBy() {
			return updatedBy;
		}
		public void setUpdatedBy(String updatedBy) {
			this.updatedBy = updatedBy;
		}
		
		public boolean equals(Object o){
			ValueString v=(o instanceof ValueString)?(ValueString)o:null;
			return (v!=null) 
					&& (value==v.getValue() ||(value!=null && value.equals(v.getValue())))
					&& (updatedBy==v.getUpdatedBy() ||(updatedBy!=null && updatedBy.equals(v.getUpdatedBy())))
					&& (valid==v.isValid());
		}
	}
	
}