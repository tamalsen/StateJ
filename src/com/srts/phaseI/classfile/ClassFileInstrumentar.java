package com.srts.phaseI.classfile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.bcel.Constants;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ACONST_NULL;
import org.apache.bcel.generic.ArrayType;
import org.apache.bcel.generic.BasicType;
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.DUP;
import org.apache.bcel.generic.FieldGen;
import org.apache.bcel.generic.InstructionFactory;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.RETURN;
import org.apache.bcel.generic.TABLESWITCH;
import org.apache.bcel.generic.Type;

public class ClassFileInstrumentar {

	private static ArrayList<JavaClass> classes=new ArrayList<JavaClass>();

	private static HashMap<String,String[]> symMethods= new HashMap<String, String[]>();


	public static void initClassFiles(String[] classPath, String tempDirPath){
		ReadFiles rf=new ReadFiles(classPath);
		classes=rf.getClassFiles();
		//TypeHierarchy.buildClassHierarchy(classes);

		//delete and create fresh temp
		File tempDir= new File(tempDirPath);
		if(!tempDir.exists()){
			tempDir.mkdir();
		}else{
			tempDir.deleteOnExit();
			tempDir.mkdir();
		}

		for(JavaClass myjc:classes){
			System.out.println("Instrumenting Class: "+ myjc.getClassName());

			ClassGen cgen=new ClassGen(myjc);
			ConstantPoolGen constantPoolGen=cgen.getConstantPool();
			InstructionFactory factory = new InstructionFactory(cgen);
			
			/*
			 * Adding synthetic private static field.
			 */
			FieldGen f= new FieldGen(Constants.ACC_PRIVATE|Constants.ACC_STATIC, new ObjectType(myjc.getClassName()), "staticThis", constantPoolGen);
			cgen.addField(f.getField());
			
			/*
			 * Adding public String state=null;
			 */
			FieldGen f2= new FieldGen(Constants.ACC_PUBLIC, new ObjectType("java.lang.String"), "$state", constantPoolGen);
			cgen.addField(f2.getField());
			
			{
				/*
				 * Adding/Instrumenting synthetic <clinit>
				 */
				Method clinit= cgen.containsMethod("<clinit>", "()V");
				MethodGen methodGen=null;
				InstructionList addedStaticInit=new InstructionList();
				addedStaticInit.append(factory.createNew(myjc.getClassName()));
				addedStaticInit.append(new DUP());
				addedStaticInit.append(factory.createInvoke(myjc.getClassName(), "<init>", Type.VOID, new Type[] {}, Constants.INVOKESPECIAL));
				addedStaticInit.append(factory.createFieldAccess(myjc.getClassName(),"staticThis",new ObjectType(myjc.getClassName()),Constants.PUTSTATIC));

				if(clinit!=null){
					InstructionList ops = null;
					methodGen     = new MethodGen(clinit, myjc.getClassName(), constantPoolGen);
					ops           = methodGen.getInstructionList();
					ops.insert(addedStaticInit);
					methodGen.setMaxStack();
					methodGen.setMaxLocals();
					cgen.replaceMethod(clinit, methodGen.getMethod());

				}
				else{
					addedStaticInit.append(new RETURN());
					methodGen = new MethodGen(8, Type.VOID, new Type[]{}, new String[]{}, "<clinit>", myjc.getClassName(), addedStaticInit, constantPoolGen);
					methodGen.setMaxStack();
					methodGen.setMaxLocals();
					cgen.addMethod(methodGen.getMethod());
				}
			}

			/*
			 * Adding default <init>
			 */
			Method init= cgen.containsMethod("<init>", "()V");
			//System.out.println(init==null?"init null": "init not null");
			MethodGen methodgenInit=null;
			InstructionList addedInit=new InstructionList();
			for(Field myf:myjc.getFields()){

				if(myf.isStatic())
					continue;

				addedInit.append(factory.createLoad(new ObjectType(myjc.getClassName()), 0));
				Type t=myf.getType();
				if(t.getType()== Type.INT.getType() 
						|| t.getType()== Type.SHORT.getType() 
						|| t.getType()== Type.BYTE.getType() 
						|| t.getType()== Type.CHAR.getType()
						|| t.getType()== Type.BOOLEAN.getType()){
					addedInit.append(factory.createConstant(0));
				}
				else if(t.getType()== Type.LONG.getType() ){
					addedInit.append(factory.createConstant(0l));
				}
				else if(t.getType()== Type.FLOAT.getType() ){
					addedInit.append(factory.createConstant(0.0f));
				}
				else if(t.getType()== Type.DOUBLE.getType() ){
					addedInit.append(factory.createConstant(0.0d));
				}
				else if (!(t instanceof BasicType)){
					addedInit.append(new ACONST_NULL());
				}
				addedInit.append(factory.createFieldAccess(myjc.getClassName(), myf.getName(), myf.getType(), Constants.PUTFIELD));
			}

			if(init!=null){
				InstructionList ops = null;
				methodgenInit     = new MethodGen(init, myjc.getClassName(), constantPoolGen);
				ops           = methodgenInit.getInstructionList();
				ops.append(ops.getInstructionHandles()[1],addedInit);
				methodgenInit.setMaxStack();
				methodgenInit.setMaxLocals();
				methodgenInit.setAccessFlags(Constants.ACC_PUBLIC);
				cgen.replaceMethod(init, methodgenInit.getMethod());

			}
			else{
				addedInit.append(new RETURN());
				methodgenInit = new MethodGen(Constants.ACC_PUBLIC, Type.VOID, new Type[]{}, new String[]{}, "<init>", myjc.getClassName(), addedInit, constantPoolGen);
				methodgenInit.setMaxStack();
				methodgenInit.setMaxLocals();
				cgen.addMethod(methodgenInit.getMethod());
			}

			myjc=cgen.getJavaClass();
			ArrayList<Method> methods=new ArrayList<Method>();
			for(Method m:myjc.getMethods()){
				if(!m.isStatic() && ((Constants.ACC_PROTECTED|Constants.ACC_PUBLIC) & m.getModifiers())!=0){
					methods.add(m);
				}
			}
			/*
			 * Adding synthetic main
			 */
			int[] ints= new int[methods.size()];
			InstructionHandle[] cases= new InstructionHandle[methods.size()];
			InstructionList il1= new InstructionList();
			String[] symMethNames=new String[methods.size()];
			int symMethCount=0;

			il1.append(factory.createConstant(new Integer(0)));
			il1.append(factory.createConstant(new Integer(methods.size()-1)));
			il1.append(factory.createInvoke("gov/nasa/jpf/jvm/Verify", "getInt", Type.INT, new Type[]{Type.INT,Type.INT}, Constants.INVOKESTATIC));
			/*			il1.append(factory.createStore(Type.INT, 0));
			il1.append(factory.createLoad(Type.INT, 0));*/
			for(int i=0;i<methods.size();i++){
				InstructionList il2= new InstructionList();
				if(methods.get(i).getName().contains("<init>")){
					//System.out.println("instrumenting method: "+ methods.get(i).getName());
					il2.append(factory.createNew(myjc.getClassName()));
					il2.append(new DUP());
					Type[] argTypes=methods.get(i).getArgumentTypes();
					String symMethName=pushParametersReturnSymMethName(methods.get(i).getName(),argTypes,il2,factory);
					symMethNames[symMethCount++]=myjc.getClassName()+ "."+ symMethName;
					il2.append(factory.createInvoke(myjc.getClassName(), "<init>", Type.VOID, argTypes, Constants.INVOKESPECIAL));
				}
				/*			else if(methods.get(i).isProtected()){
					il2.append(new ALOAD(0));
					Type[] argTypes=methods.get(i).getArgumentTypes();
					pushParameters(argTypes,il2,factory);
					il2.append(factory.createInvoke(myjc.getClassName(), methods.get(i).getName(), methods.get(i).getReturnType(),argTypes, Constants.INVOKESPECIAL));
				}*/
				else if(methods.get(i).isPublic()){
					//System.out.println("instrumenting method: "+ methods.get(i).getName());
					il2.append(factory.createFieldAccess(myjc.getClassName(),"staticThis",new ObjectType(myjc.getClassName()),Constants.GETSTATIC));
					Type[] argTypes=methods.get(i).getArgumentTypes();
					String symMethName=pushParametersReturnSymMethName(methods.get(i).getName(),argTypes,il2,factory);
					symMethNames[symMethCount++]=myjc.getClassName()+ "."+ symMethName;
					il2.append(factory.createInvoke(myjc.getClassName(), methods.get(i).getName(), methods.get(i).getReturnType(), argTypes, Constants.INVOKEVIRTUAL));
				}
				il2.append(new RETURN());
				cases[i]=il1.append(il2);
				ints[i]=i;
			}
			InstructionHandle lastReturn= il1.append(new RETURN());
			il1.insert(cases[0],new TABLESWITCH(ints, cases, lastReturn));

			MethodGen main= new MethodGen(Constants.ACC_PUBLIC|Constants.ACC_STATIC, Type.VOID, new Type[]{new ArrayType(Type.STRING, 1)}, new String[]{"args"}, "main", myjc.getClassName(), il1, constantPoolGen);
			main.setMaxStack();
			main.setMaxLocals();
			cgen.addMethod(main.getMethod());

			cgen.setMajor(49);
			//cgen.setMinor(3);

			JavaClass javaClass=cgen.getJavaClass();
			symMethods.put(javaClass.getClassName(), symMethNames);
			try {
				File dir= new File(tempDir.getAbsolutePath()+"\\"+myjc.getPackageName().replaceAll("\\.", "\\\\")+"\\");
				//System.out.println("Going to dump on: "+ dir.getAbsolutePath());
				dir.mkdirs();
				String s1=myjc.getClassName();
				s1=s1.substring(s1.lastIndexOf(".")+1, s1.length());
				javaClass.dump(dir.getAbsolutePath()+"\\"+s1+".class");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static String[] getSymMethods(String className){
		return symMethods.get(className);
	}

	public static void main(String[] args) {
		initClassFiles(new String[]{"F:\\Project\\EclipseWorkspace4\\TestProject\\bin\\"},"F:\\temp\\");

		for(String clsName: getClassOrder()){
			System.out.println("=="+ clsName + " size: "+ ClassFileInstrumentar.getSymMethods(clsName).length);
			for(String s: ClassFileInstrumentar.getSymMethods(clsName)){
				System.out.println("----"+ s);
			}
		}
	}

	public static ArrayList<String> getClassOrder(){
		ArrayList<String> al= new ArrayList<String>();
		/*for(JavaClass jc: classes){
			al.add(jc.getClassName());
		}*/
		//al.add("J");
		//al.add("B");
		al.add("com.srts.phaseI.test2.PowerState");
		//al.add("com.srts.phaseI.test2.PowerState2");
		al.add("com.srts.phaseI.test2.Television");
		return al;
	}

	private static String pushParametersReturnSymMethName(String methName,Type[] argtypes, InstructionList il2, InstructionFactory  factory){
		methName= methName + "(";
		for(Type t:argtypes){
			methName=methName+ "sym#";
			if(t.getType()== Type.INT.getType() 
					|| t.getType()== Type.SHORT.getType() 
					|| t.getType()== Type.BYTE.getType() 
					|| t.getType()== Type.CHAR.getType()
					|| t.getType()== Type.BOOLEAN.getType()){
				il2.append(factory.createConstant(1));
			}
			else if(t.getType()== Type.LONG.getType()){
				il2.append(factory.createConstant(new Long(1)));
			}
			else if(t.getType()== Type.FLOAT.getType()){
				il2.append(factory.createConstant(new Float(1.0)));
			}
			else if(t.getType()== Type.DOUBLE.getType()){
				il2.append(factory.createConstant(new Double(1.0)));
			}
			else if(t.getType() == Type.STRING.getType()){
				il2.append(factory.createConstant(new String("")));
			}
			else if(!(t instanceof BasicType)){
				il2.append(new ACONST_NULL());
			}

		}
		if(methName.contains("#")){
			methName=methName.substring(0, methName.lastIndexOf('#'));
		}
		methName = methName + ")";
		return methName;
	}

}
