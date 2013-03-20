package com.srts.phaseI.statemodel;

import gov.nasa.jpf.symbc.numeric.Constraint;
import gov.nasa.jpf.symbc.numeric.Expression;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

public class StateModel {
	private Set<State> states=new HashSet<State>();
	private Set<Transition> transitions = new HashSet<Transition>();
	private int stateCounter=0,transitionCounter=0;


	public State getCurrentState(Map<Expression,Expression>  exprMap){
		Iterator<State> it=states.iterator();
		while(it.hasNext()){
			State q=it.next();
			if(q.getConsIfSatisfy(null, exprMap)!=null)
				return q;
		}
		return null;
	}
	public State addState(Set<Constraint> e){
		State s=new State(e,""+(++stateCounter));
		states.add(s);
		return s;
	}
	public State addState(State s){
		states.add(s);
		return s;
	}

	public State addState(Constraint c){
		State s=new State(c,""+(++stateCounter));
		states.add(s);
		return s;
	}

	private State defaultState;
	public State addDefaultState(){
		if(stateCounter>0){
			System.out.println("StateModel.java:47 !!ERROR: Default state already added or not needed.");
			return null;
		}
		if(defaultState==null){
			defaultState= new State((Constraint)null,"1");
			states.add(defaultState);
		}
		return defaultState;
	}


	private State startState;

	public State addStartState(){
		states.add(this.getStartState());
		return this.getStartState();
	}
	public State getStartState(){
		if(startState==null){
			startState = new State((Constraint)null,"0");
			startState.setAsStartState();
		}
		return startState;
	}

	private State exceptionState;
	public State addExceptionState(){
		states.add(this.getExceptionState());
		return this.getExceptionState();
	}
	public State getExceptionState(){
		if(exceptionState==null){
			exceptionState = new State((Constraint)null,"-1");
			exceptionState .setAsExceptionState();
		}
		return exceptionState;
	}

	private class TransitionHashKey{
		String fromState;
		String toState;
		String stmtCovHash;
		String stimuli;
		String retVal;
		Set<String> stmtc;
		TransitionHashKey(State fromState,State toState, Set<String> stmtCov, String stimuli,Expression retVal) {
			this.fromState=fromState.getId();
			this.toState=toState.getId();
			this.stmtCovHash=(stmtCov==null?"":(stmtCov.hashCode()+""));
			this.stimuli=stimuli;
			this.retVal=(retVal==null?"":retVal.toString());
			this.stmtc=stmtCov;
		}
		@Override
		public String toString() {
			return  fromState + toState + stimuli;
		}
	}
	/**
	 * A hash map from a set of statements to a set of transitions
	 */
	public HashMap<String,Set<Transition>> hm= new HashMap<String,Set<Transition>>();

	public String addTransition(State inState,String stimuli,Constraint guard,State outState,Expression retVal,Set<String> stmtCoverage){
		TransitionHashKey thk= new TransitionHashKey(inState, outState, stmtCoverage, stimuli, retVal);
		Set<Transition> st=hm.get(thk.toString());
		if(st==null){
			st=new HashSet<Transition>();
		}
		TransitionIn tIn=new TransitionIn(inState, stimuli, guard);
		TransitionOut tOut=new TransitionOut(outState, retVal);
		Transition t=new Transition(tIn, tOut,stmtCoverage,++transitionCounter+"");
		transitions.add(t);
		st.add(t);
		hm.put(thk.toString(), st);
		return t.toString();
	}

	public String addTransition(State inState,String stimuli,Constraint guard,State outState,Expression retVal,Set<String> stmtCoverage,boolean hasLoop){
		TransitionHashKey thk= new TransitionHashKey(inState, outState, stmtCoverage, stimuli, retVal);
		Set<Transition> st=hm.get(thk.toString());
		if(st==null){
			st=new HashSet<Transition>();
		}
		TransitionIn tIn=new TransitionIn(inState, stimuli, guard);
		TransitionOut tOut=new TransitionOut(outState, retVal);
		Transition t=new Transition(tIn, tOut,stmtCoverage,++transitionCounter+"");
		t.setHasLoop(hasLoop);
		transitions.add(t);
		st.add(t);
		hm.put(thk.toString(), st);
		return t.toString();
	}


	public String addTransition(Transition t){
		this.transitions.add(t);
		return t.getId();
	}

	/**
	 * TODO: take the class name as parameter and create transitions specific to that
	 * particular class.
	 */
	private static StateModel defaultSM;
	public static StateModel getDefaultStateModel(){
		if(defaultSM==null){
			defaultSM= new StateModel();
			State s1=defaultSM.addDefaultState();
			State s0=defaultSM.addStartState();
			defaultSM.addTransition(s0, null, null, s1, null, null);
			defaultSM.addTransition(s1, null, null, s1, null, null);
		}
		return defaultSM;
	}

	public Set<State> getStates(){
		return states;
	}

	public Set<Transition> getTransitions(){
		return transitions;
	}

	/**
	 * Search transitions with a specific event type for which the guard is satisfied by values in sv 
	 * @param event: fullName of the method 
	 * @param sv: a map between parameter names of event and their values
	 * @return a set of transitions
	 */
	public Set<Transition> getTransitions(String stateId,String event,Map<Expression,Expression> sv){
		Iterator<Transition> it= transitions.iterator();
		Set<Transition> trans=new HashSet<Transition>();

		/*TODO: **NEED REVIEW**
		 * If a default StateModel instance invokes this with an event name containing "<init>", 
		 * return the transition having source_state=start_state, 
		 * return the other transition otherwise.  
		 */
		if(this.equals(getDefaultStateModel())){
			Transition t1=it.next();
			Transition t2=it.next();
			if(event.contains("<init>")){
				if(t1.getTransitionIn().getState().isStartState()){	
					trans.add(t1);
					return trans;
				}
				else{
					trans.add(t2);
					return trans;
				}
			}
			else{
				if(t1.getTransitionIn().getState().isStartState()){	
					trans.add(t2);
					return trans;
				}
				else{
					trans.add(t1);
					return trans;
				}
			}
		}
		while(it.hasNext()){
			Transition t=it.next();
			TransitionIn tIn=t.getTransitionIn();
			if(tIn.getStimuli().contains(event) && (stateId==null || stateId.equals(tIn.getState().getId()))){
				trans.add(t);
			}
		}
		return trans;
	}

	@Override
	public boolean equals(Object o) {
		return (o instanceof StateModel) && (o!=null) 
				&& (states.equals(((StateModel)o).getStates()) 
						&& (transitions.equals(((StateModel)o).getTransitions())));
	}


	private String getStateLabel(State s){
		return s.getId();//(!s.isExceptionState()?("S"+s.getId()):("Ex"+(s.getId().replace("-", ""))));
	}


	public static void main(String args[]){
		StateModel sm= new StateModel();
		sm.loadFromFile(new File("F:\\Project\\Graphviz\\com.srts.phaseI.test2.PowerState.sm"), "com.srts.phaseI.test2.PowerState");
	}
	public void loadFromFile(File file,String clsName){
		try {
			FileReader fin = new FileReader(file);
			BufferedReader br= new BufferedReader(fin);
			String str=br.readLine();
			if(str.trim().equals(clsName)){
				this.states= new HashSet<State>();
				this.transitions= new HashSet<Transition>();
				this.stateCounter=0;
				this.transitionCounter=0;
				HashMap<String,State> sMap= new HashMap<String,State>(); 
				str=br.readLine();//should be "States:"
				while(!(str=br.readLine()).equals("Transitions:")){
					String sRecord[]=str.split(":");
					System.out.println(Arrays.toString(sRecord));
					State s=new State((Constraint)null, sRecord[0].trim());
					sMap.put(s.getId(), s);
					this.addState(s);
				}
				while((str=br.readLine())!=null){
					String tName=str.substring(0, str.indexOf(":"));
					String tRecord[]=str.substring(str.indexOf(":")+1).split(",");
					Constraint guard = getConstraintFromString(tRecord[3]); 
					Expression retVal=getExpressionFromString(tRecord[4]);
					this.addTransition(sMap.get(tRecord[0]), tRecord[2], guard, sMap.get(tRecord[1]), retVal, null);

					System.out.println(tName + ": " + Arrays.toString(tRecord));
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private Expression getExpressionFromString(String exprStr){
		return null;
	}
	
	private Constraint getConstraintFromString(String constrStr){
		//use getExpressionFromString()
		return null;
	}
	
	private String[] getParamTypes(String mName) {
		String str=mName.replaceAll(".*\\((.*)\\).*", "$1");
		char[] chars=str.toCharArray();
		ArrayList<String> paramList= new ArrayList<String>();
		for(int i=0;i<chars.length; i++){
			/***
			B	byte	signed byte
			C	char	Unicode character code point in the Basic Multilingual Plane, encoded with UTF-16
			D	double	double-precision floating-point value
			F	float	single-precision floating-point value
			I	int	integer
			J	long	long integer
			L ClassName ;	reference	an instance of class ClassName
			S	short	signed short
			Z	boolean	true or false
			[	reference	one array dimension
			 ***/
			char c=chars[i];
			switch(c){
			case 'Z':
			case 'B':
			case 'S':
			case 'C':
			case 'I':
				paramList.add("INT");
				break;
			case 'J':
			case 'F':
			case 'D':
				paramList.add("REAL");
				break;
			case 'L':
				String s="";
				for(;i!=';';i++){
					s+=(char)i;
				}
				paramList.add(s);
				break;
			case '[':
				s="";
				for(;i!=';';i++){
					s+=(char)i;
				}
				paramList.add(s);
				break;

			default:
				break;
			}
		}
		return null;
	}


	public void printToFile(File file,String clsName){
		try {
			FileWriter fos = new FileWriter(file);
			StringBuffer sbuf = new StringBuffer(); 

			sbuf.append(clsName+"\n");
			sbuf.append("States:\n");
			Iterator<State> it=states.iterator();
			while(it.hasNext()){
				State s=it.next();
				sbuf.append(getStateLabel(s)+ ": ");
				if(!s.isStartState())
					sbuf.append(s.getStateCondition()==null?"Default":s.getStateCondition().stringPC().replaceAll("\n", " "));
				else
					sbuf.append("start");
				sbuf.append("\n");
			}

			Vector<Transition> v = new Vector<Transition>();
			v.addAll(transitions);
			Collections.sort(v);
			Iterator<Transition> it2=v.iterator();
			StringBuffer sb= new StringBuffer();
			sbuf.append("Transitions:\n");
			while(it2.hasNext()){
				Transition t=it2.next();
				TransitionIn tIn=t.getTransitionIn();
				TransitionOut tOut=t.getTransitionOut();
				String guard=tIn.getGuard()==null?"-":tIn.getGuard().stringPC().replace("\n", " ");
				sbuf.append(""+t.getId() + ":"+ getStateLabel(tIn.getState())+","+getStateLabel(tOut.getState())+","+tIn.getStimuli()+","
						+((guard==null||guard.equals("null"))?"-":guard+","
								+((tOut.getReturnVal()==null)?"-":tOut.getReturnVal())));
				sbuf.append("\n");

			}
			fos.write(sbuf.toString());
			fos.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public void printAsGraphViz(File file){
		try {
			FileWriter fos = new FileWriter(file);
			fos.write("digraph G {\n");
			Iterator<State> it=states.iterator();
			while(it.hasNext()){
				State s=it.next();
				fos.write(s.getId()+ (s.isExceptionState()
						?"[shape=rectangle  fontsize=9 fillcolor=RED, style=\"filled\" label=\"ex"+s.getId().replace("-", "")+"\\nEXCEPTION\"]\n"
								:(s.isStartState()
										?"[shape=circle, width=.2,height=.2,label=\"\",fillcolor=blue, style=\"filled\"]\n"
												:("[shape=rectangle  fontsize=9 fillcolor=yellow, style=\"filled\" label=\"q"+s.getId()+"\\n"+((s.getStateCondition()==null)?"Default":s.getStateCondition().stringPC().replaceAll("\n", "\\n"))+"\"]\n"))));
			}
			Vector<Transition> v = new Vector<Transition>();
			v.addAll(transitions);
			Collections.sort(v);
			Iterator<Transition> it2=v.iterator();
			StringBuffer sb= new StringBuffer();
			while(it2.hasNext()){
				Transition t=it2.next();
				TransitionIn tIn=t.getTransitionIn();
				TransitionOut tOut=t.getTransitionOut();
				fos.write(tIn.getState().getId()+"->"+tOut.getState().getId()
						+"[label=\"t"+t.getId()+"\" arrowhead=onormal arrowsize=.7  fontsize=9 "+(t.containsLoop()?"color=ORANGE ":"")+"]\n");
				sb.append("<tr><td>t"+t.getId()+"</td>\n");
				sb.append("<td>q"+tIn.getState().getId()+"</td>\n");
				sb.append(tOut.getState().isExceptionState()?("<td>ex"+tOut.getState().getId().replace("-", "")+"</td>\n"):("<td>q"+tOut.getState().getId()+"</td>\n"));
				sb.append("<td>"+tIn.getStimuli().replace("<", "&lt;").replace(">", "&gt;")+"</td>\n");
				String guard=tIn.getGuard()==null?"":tIn.getGuard().stringPC().replace("\n", " ");
				sb.append("<td>"+((guard==null||guard.equals("null"))?"--":guard.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"))+"</td>\n");
				sb.append("<td>"+((tOut.getReturnVal()==null)?"--":tOut.getReturnVal())+"</td></tr>\n");
			}
			fos.write("{ rank = sink;\n");
			fos.write("Legend [shape=none, margin=0, label=<\n");
			fos.write("<table><tr colspan=\"6\"><td>Legend</td></tr> <tr><td> Transition Id </td>\n");
			fos.write("<td>Source</td>\n");
			fos.write("<td>Target</td>\n");
			fos.write("<td>Event</td>\n");
			fos.write("<td>Guard</td>\n");
			fos.write("<td>Return</td></tr>\n");
			fos.write(sb.toString());
			fos.write("</table>\n");
			fos.write("\n>];}\n");
			fos.write("}");
			fos.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	private String convertGuardsToString(Set<Constraint> g){
		StringBuffer sb=new StringBuffer();
		Iterator<Constraint> it=g.iterator();
		while(it.hasNext()){
			sb.append(it.next());
			if(it.hasNext())
				sb.append("||");
		}
		return sb.toString();
	}
}
