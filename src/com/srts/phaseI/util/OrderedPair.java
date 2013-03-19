package com.srts.phaseI.util;

public class OrderedPair <T1,T2> {
	private T1 t1;
	private T2 t2;
	
	public OrderedPair(){}
	
	public OrderedPair(T1 t1,T2 t2){
		this.t1=t1;
		this.t2=t2;
	}
	public T1 getFirstElement() {
		return t1; 
	}
	public void setFirstElement(T1 t1) {
		this.t1 = t1;
	}
	public T2 getSecondElement(){
		return t2;
	}
	public void setSecondElement(T2 t2) {
		this.t2 = t2;
	}
	
}
