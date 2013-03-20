package com.srts.phaseI.statemodel;

import gov.nasa.jpf.symbc.numeric.Constraint;

import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

public class TransitionIn{
	private State inState;
	private Constraint guard;
	private String stimuli;
	
	public TransitionIn(State q,String st,Constraint g) {
		this.inState=q;
		this.stimuli=st;
		this.guard=g;
	}
	public State getState(){
		return inState;
	}
	public String getStimuli(){
		return stimuli;
	}
	
	public Constraint getGuard(){
		return guard;
	}
	@Override
	public boolean equals(Object o){
		if(o==null)
			return false;
		if(o instanceof TransitionIn){
			TransitionIn t=(TransitionIn)o;
			if(this.inState.equals(t.getState()) 
					&& this.stimuli.equals(t.getStimuli()) 
					&& (this.guard==t.getGuard() || (this.guard!=null && this.guard.equals(t.getGuard())))){
				return true;
			}
		}
		return false;
	}

	/*private boolean isEqualGuardArray(Constraint[] e1, Constraint[] e2){
		if(e1.length != e2.length)
			return false;
		assert(e1.length == e2.length);
		for(int i=0;i<e1.length;i++){
			if(e1[i]==e2[i])
				continue;
			assert (e1[i]!=e2[i]);
			if(e1[i]==null || !e1[i].equals(e2[i]))
					return false;
		}
		return true;
	}
	
	private boolean isEquivalentGuardArray(Constraint[] e1, Constraint[] e2) {
		SymbolicConstraintsGeneral scg=new SymbolicConstraintsGeneral();
		for(int i=0;i<e1.length;i++){ 
			for(int j=0;j<e2.length;j++){
				if(e1[i]==null && e2[j]==null)
					continue;
				if(e1[i]==null || e2[j]==null)
					return false;
				if(e1[i]!=e2[j]){
					e1[i].makeCopy().and=e2[j].makeCopy();
					if(!scg.isSatisfiable(e1[i]))
						return false; 
				}
				else if(scg.isSatisfiable(e1[i]))
					return false;
				}
		}
		return true; 
	}*/
	
}
