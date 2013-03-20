package com.srts.phaseI.main;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.symbc.numeric.Comparator;
import gov.nasa.jpf.symbc.numeric.Constraint;
import gov.nasa.jpf.symbc.numeric.Expression;
import gov.nasa.jpf.symbc.numeric.PathCondition;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import com.srts.phaseI.JPFlisteners.CoverageAnalyzer;
import com.srts.phaseI.path.Path;
import com.srts.phaseI.path.VSM;
import com.srts.phaseI.statemodel.State;
import com.srts.phaseI.statemodel.StateModel;
import com.srts.phaseI.statemodel.Transition;
import com.srts.phaseI.util.ConstraintWrapper;
import com.srts.phaseI.util.NumericUtils;
import com.srts.phaseI.util.Utility;

public class StateModelBuilder {

	public static StateModel extractStateModel(String className, String classPath,String mainMethod,String[] publicMethods,int loopDepth){
		System.out.println("\n\n---------------Creating State Model for "+ className +"---------------------\n\n");
		Set<Path> paths=extractPathsUsingJPF(className, classPath,mainMethod,publicMethods,loopDepth);
		StateModel sm=buildStateModelFromPaths(className,paths);
		System.out.println("===============State Model Summary==============");
		System.out.println("# States: "+ sm.getStates().size());
		System.out.println("# Transitions: "+ sm.getTransitions().size());
		System.out.println("================================================");
		return sm;
	}

	private static Set<Path> extractPathsUsingJPF(String className, String classPath,String mainMethod,String[] publicMethods,int loopDepth){
		Config jpfConf;
		String[] args;
		args= new String[3];
		args[0]="+site=F:\\Project\\EclipseWorkspace3\\site.properties"; 
		args[1]="+shell.port=4242"; 
		args[2]="NumberExample.jpf";
		jpfConf = new Config(args);
		jpfConf.put("target",className);
		jpfConf.put("listener","com.srts.phaseI.JPFlisteners.CoverageAnalyzer" +
								",com.srts.phaseI.JPFlisteners.InterObjectMethodInvocationListener" +
								",com.srts.phaseI.JPFlisteners.LoopHandlingListener");
		//jpfConf.put("listener","com.srts.phaseI.JPFlisteners.InterObjectMethodInvocationListener");
		//jpfConf.put("listener","gov.nasa.jpf.listener.ExecTracker");
		jpfConf.put("classpath", classPath); //"F:\\Project\\EclipseWorkspace3\\S_RTS_PhaseI\\bin"
		jpfConf.put("type_classpath", classPath); //+","+System.getProperty("java.class.path").replaceAll(";", ",")
		jpfConf.put("main_method", mainMethod);
		jpfConf.put("loop_depth",loopDepth +"");
		StringBuilder sb=new StringBuilder();
		for(int i=0;i<publicMethods.length;i++){
			sb.append(publicMethods[i]+",");
		}
		sb.deleteCharAt(sb.lastIndexOf(","));
		jpfConf.put("symbolic.method", sb.toString()); //"com.srts.phaseI.main.Television.increaseVol(sym),com.srts.phaseI.main.Television.powerButton(sym),com.srts.phaseI.main.Test.<init>(),com.srts.phaseI.main.Television.decreaseVol(),com.srts.phaseI.main.Television.changeChannel(sym)"
		//jpfConf.put("symbolic.class","com.srts.phaseI.main.Television");
		CoverageAnalyzer.flush();
		JPF.start(jpfConf, args);
		Set<Path> paths =new HashSet<Path>(); 
		paths.addAll(CoverageAnalyzer.getPaths());
		return paths;
	}


	private static StateModel buildStateModelFromPaths(String className, Set<Path> spaths){
		Vector<Path> paths=new Vector<Path>();
		paths.addAll(spaths);
		Set<ConstraintWrapper> tempMDLs=new HashSet<ConstraintWrapper>();
		Set<Constraint> nonEqMDLs=new HashSet<Constraint>();
		for(int i=0;i<paths.size();++i){
			PathCondition pc=paths.get(i).getPc();
			System.out.println(i+1 + ": "+ (pc==null?"null":pc.stringPC().replace('\n', ' ')));
			if(pc!=null){
				Constraint temp = pc.header;
				if(!Utility.isSatisfy(temp)){
					//paths.remove(paths.get(i));
					System.out.println("Path should be removed. Path Condition is not satisfiable.");
					continue;
				}

				/*
				 * ConstraintWrapper implements a special equals mechanism for Constraints.
				 * According to that "a>=b" is equal to "a>=b" ,"b<=a" or !(a>=b), whereas 
				 * Constraint only support the first two.  
				 */
				while (temp != null) {
					//System.out.println("Exp: "+ temp+ " type:"+ temp.getType());
					if(temp.getType()==0){
						Constraint c=temp.makeCopy();
						c.and=null;
						ConstraintWrapper cw=new ConstraintWrapper(c);
						tempMDLs.add(cw);
						//System.out.println("added: " +cw);
					}
					temp = temp.and;
				}
				Iterator<ConstraintWrapper> it=tempMDLs.iterator();
				while(it.hasNext())
					nonEqMDLs.add(it.next().c);
			}


			System.out.println("method: "+ paths.get(i).getMethodName());
			Iterator<String> statements=paths.get(i).getInsnSeq().iterator();
			for(;statements.hasNext();){
				String insn=statements.next();
				System.out.println("      "+insn);
			}

			System.out.println("returned Expression: "+paths.get(i).getReturnVal());
			VSM vsm=paths.get(i).getVSM();

			if(vsm==null)
				System.out.println("!!!vsm is null in path:" + paths.get(i));

			Iterator<String> keys=vsm.getNames();
			System.out.println("VSM: updated:" + vsm.isUpdated());
			while (keys.hasNext()){
				String name=keys.next();
				System.out.println(name+"="+vsm.getValue(name) + " updated by:"+ vsm.getUpdatedBy(name));
			}
		}
		Utility.printSet("MDLs", nonEqMDLs);

		/*
		 *Now: lets create the state model 
		 */
		StateModel sm =new StateModel();
		/*
		 * Step 1: Create states by permuting MDLs
		 */
		Vector<Set<Constraint>> states=Utility.getPermutedConstraints(nonEqMDLs);
		Vector<Set<Constraint>> tempStates= new Vector<Set<Constraint>>();

		System.out.println("StateModelBuilder.java: Removing unsat object constraints...");
		for(int i=0;i<states.size();i++){
			HashMap<String,Set<Constraint>> map= new HashMap<String,Set<Constraint>>();
			for(Constraint tmpCons:states.get(i)){
				if(tmpCons.getLeft().toString().matches(".*\\(.*,.*\\)")){
					String key=tmpCons.getLeft().toString().replaceAll("(.*)\\(.*\\)", "$1");
					Set<Constraint> tmpSet=map.get(key);
					if(tmpSet==null){
						tmpSet=new HashSet<Constraint>();
					}
					tmpSet.add(tmpCons);
					map.put(key, tmpSet);
				}
			}

			boolean sat=true;
			for(String key:map.keySet()){
				int eqConsCount=0;
				for(Constraint tmpCons:map.get(key)){
					if(tmpCons.getComparator()==Comparator.EQ){
						eqConsCount++;
					}
				}
				if(eqConsCount>1){
					sat=false;
				}
			}
			if(sat){
				tempStates.add(states.get(i));
				System.out.println("--Added: "+ states.get(i));
			}
			else{
				System.out.println("--Removed: "+ states.get(i));
			}
		}
		states=tempStates;

		for(int i=0;i<states.size();i++){
			if(Utility.isSatisfy(states.get(i))){ //don't create states for unsatisfiable constraints
				Utility.printSet("Adding state with following condition:", states.get(i));
				sm.addState(states.get(i));
			}
		}
		/* add synthetic start state*/
		sm.addStartState();
		sm.addExceptionState();

		/*if no MDL is found create a default state with state condition: null*/
		if(sm.getStates().size()==0){
			sm.addDefaultState();
		}
		/*
		 * Step 2:Create transitions
		 */
		for(int i=0;i<paths.size();++i){
			PathCondition pc=paths.get(i).getPc();
			Constraint pdl=null,mdl=null;
			System.out.println("\n\nAnalysis of Path: "+(i+1) +" pc: " + (pc==null?"null":pc.stringPC().replace('\n', ' ')) +" Constructor: "+ paths.get(i).isConstructorPath());
			if(pc!=null){
				Constraint temp=pc.header;
				if(!Utility.isSatisfy(temp)){
					//paths.remove(paths.get(i));
					System.out.println("Path condition is not satisfiable!");
					continue;
				}
				while (temp != null) {
					if(temp.getType()==2){
						if(pdl==null){
							pdl=temp.makeCopy();
							pdl.and=null;
						}
						else{
							Constraint c1=temp.makeCopy();
							c1.and=null;
							pdl.last().and=c1;

						}
					}
					else if(temp.getType()==0){
						if(mdl==null){
							mdl=temp.makeCopy();
							mdl.and=null;
						}
						else{
							Constraint c1=temp.makeCopy();
							c1.and=null;
							mdl.last().and=c1;
						}
					}
					else{
						System.out.println("StateModelBuilder.java:172!!! getType of " + temp+ " should be mxl. Got: " + temp.getType());
					}
					temp = temp.and;
				}
				System.out.println("pdl: "+((pdl!=null)?pdl.toString().replace('\n', ' '):"null") + " mdl:"+ ((mdl!=null)?mdl.toString().replace('\n', ' '):"null"));
			}

			VSM vsm=paths.get(i).getVSM();
			Expression returnExpr=paths.get(i).getReturnVal();

			Set<State> st=sm.getStates();
			Iterator<State> it=st.iterator();
			while(it.hasNext()){
				State pre=it.next();
				System.out.println("\nState id:"+pre.getId() +" cond:" + ((pre.getStateCondition()!=null)?pre.getStateCondition().stringPC().replace('\n', ' '):null));

				if(pre.isStartState())
					continue;
				if(pre.isExceptionState())
					continue;

				if(!pre.isSatisfy(mdl))
					continue;

				System.out.println("Pre state matched to q"+pre);
				if(paths.get(i).isExceptionPath()){
					System.out.println("Exception path. Post state should be an exception state.");
					sm.addTransition(pre, paths.get(i).getMethodName(), pdl, sm.getExceptionState(), paths.get(i).getReturnVal(), paths.get(i).getInsnSeq(),paths.get(i).isLoopPath());
					continue;
				}
				if(paths.get(i).isConstructorPath()){
					pre=sm.getStartState();
				}

				for(State post:st){
					if(post.isStartState())
						continue;
					if(post.isExceptionState())
						continue;

					Constraint tempC=null;
					System.out.println("--got this expression map from vsm: " + vsm.getExpressionMap());
					Constraint tempC2=(pc==null || pc.header==null)?null:pc.header.makeCopy();
					if(tempC2!=null){
						tempC2.last().and=pre.getStateCondition();
					}
					else{
						tempC2=pre.getStateCondition();
					}
					if((tempC=post.getConsIfSatisfy(tempC2,vsm.getExpressionMap())) != null){
						System.out.println("--post state matched with q"+ post.getId());

						while(tempC!=null){
							if(tempC.getType()==2){ //should be either pdl or mxl
								pdl.last().and=tempC;
							}
							tempC=tempC.and;
						}
						
					String t=sm.addTransition(pre, paths.get(i).getMethodName(), pdl, post, paths.get(i).getReturnVal(), paths.get(i).getInsnSeq(),paths.get(i).isLoopPath());
					System.out.println("Transition Created: "+ t);
					}
				}
				if(paths.get(i).isConstructorPath()){
					break;
				}
			}
		}
		return sm;
	}


	private static Set<Path> driveJPFAndCollectData(String targetClass){
		Config jpfConf;
		String[] args;
		args= new String[3];
		args[0]="+site=F:\\Project\\EclipseWorkspace3\\site.properties"; 
		args[1]="+shell.port=4242"; 
		args[2]="NumberExample.jpf";
		jpfConf = new Config(args);
		jpfConf.put("target",targetClass); 
		//jpfConf.put("target", "com.srts.phaseI.main.FindGreatest");
		//jpfConf.put("target", "com.srts.phaseI.main.Test1");
		jpfConf.put("listener","com.srts.phaseI.JPFlisteners.CoverageAnalyzer");

		jpfConf.put("classpath", "F:\\Project\\EclipseWorkspace3\\S_RTS_PhaseI\\bin");
		//jpfConf.put("symbolic.method", "com.srts.phaseI.main.Television.decreaseVol(),com.srts.phaseI.main.Television.powerButton(sym),com.srts.phaseI.main.Television.increaseVol(),com.srts.phaseI.main.Television.changeChannel(sym)" +""); 
		jpfConf.put("symbolic.method", "com.srts.phaseI.main.Television.increaseVol(sym),com.srts.phaseI.main.Television.powerButton(sym),com.srts.phaseI.main.Test.<init>(),com.srts.phaseI.main.Television.decreaseVol(),com.srts.phaseI.main.Television.changeChannel(sym)");
		//jpfConf.put("symbolic.method", "com.srts.phaseI.main.Television.<init>()");
		//jpfConf.put("symbolic.method", "com.srts.phaseI.main.FindGreatest.<init>(),com.srts.phaseIk.main.ABC.<init>(sym#sym#sym)");
		//jpfConf.put("symbolic.method", "com.srts.phaseI.main.FindGreatest.<init>()");
		//jpfConf.put("symbolic.method", "com.srts.phaseI.main.Test1.test()");
		//jpfConf.put("search.class", "gov.nasa.jpf.search.DFSearch");
		JPF.start(jpfConf, args);
		Set<Path> paths =new HashSet<Path>(); 
		paths.addAll(CoverageAnalyzer.getPaths());
		return paths;
	}
}
