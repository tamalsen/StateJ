package com.srts.phaseI.statemodel;

import java.util.HashMap;


public final class StateModelRegister {
	private StateModelRegister(){
		
	}
	private static HashMap<String,StateModel> map=new HashMap<String, StateModel>();
	public static void addStateModel(String className,StateModel sm){
		map.put(className,sm);
	}
	public static StateModel getStateModel(String string){
		return map.get(string); 
	}
	
	public static String[] getSubclasses(String cls){
		return new String[]{cls};
	}
}
