package com.srts.phaseI.main.ui;

import java.io.File;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.graph.jsdg.JavaSystemDependenceGraph;
import com.graph.jsdg.JsdgMartix;

public class ConstructDepGraUI {

	protected Shell parent;
	protected Shell shlSrtsDependence;
	private Text text;
	private Text text_1;

	/**
	 * Launch the application.
	 * @param args
	 * @wbp.parser.entryPoint
	 */
	public static void main(String[] args) {
		try {
			ConstructDepGraUI window = new ConstructDepGraUI(null);
			window.open();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public ConstructDepGraUI(Shell parent) {
		this.parent=parent;
	}
	/**
	 * Open the window.
	 */
	public void open() {
		Display display = Display.getDefault();
		createContents();
		shlSrtsDependence.open();
		shlSrtsDependence.layout();
		while (!shlSrtsDependence.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
	}

	/**
	 * Create contents of the window.
	 */
	protected void createContents() {
		shlSrtsDependence = new Shell(parent);
		shlSrtsDependence.setBackground(SWTResourceManager.getColor(153, 204, 204));
		shlSrtsDependence.setSize(593, 281);
		shlSrtsDependence.setText("S-RTS - Dependence Graph Constructor");

		Label label = new Label(shlSrtsDependence, SWT.NONE);
		label.setText("Change Impact Analyzer");
		label.setFont(SWTResourceManager.getFont("Consolas", 16, SWT.NORMAL));
		label.setAlignment(SWT.CENTER);
		label.setBounds(128, 20, 337, 32);

		text = new Text(shlSrtsDependence, SWT.BORDER);
		text.setBounds(10, 70, 388, 25);

		Button btnSpecifyProjectHome = new Button(shlSrtsDependence, SWT.NONE);
		btnSpecifyProjectHome.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog dd=new DirectoryDialog(shlSrtsDependence);
				String path = dd.open();
				if(path==null)
					return;
				else{
					File file=new File(path);
					text.setText(file.getAbsolutePath());
				}
			}
		});
		btnSpecifyProjectHome.setText("Specify Project Home Dir");
		btnSpecifyProjectHome.setBounds(416, 70, 151, 25);

		Button button_1 = new Button(shlSrtsDependence, SWT.NONE);
		button_1.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				FileDialog c1 = new FileDialog(shlSrtsDependence, SWT.SAVE);
				String path = c1.open();
				if(path==null)
					return;
				else{
					File file=new File(path);
					text_1.setText(file.getAbsolutePath());  
				}
			}
		});
		button_1.setText("Specify Output File");
		button_1.setBounds(416, 113, 124, 25);

		text_1 = new Text(shlSrtsDependence, SWT.BORDER);
		text_1.setBounds(10, 113, 388, 25);

		Button btnConstructDependenceGraph = new Button(shlSrtsDependence, SWT.NONE);
		btnConstructDependenceGraph.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				try{
					String workSpace=text.getText();
					workSpace="F:\\Project\\EclipseWorkspace3\\DependenceAnalysis\\bin\\com\\test";
					JavaSystemDependenceGraph jsdg=new JavaSystemDependenceGraph(workSpace);
					JsdgMartix jv=new JsdgMartix(jsdg);
					jv.printAllAsString();
					String outFile=text_1.getText();
					outFile="C:\\Users\\Tamal\\Desktop\\out.txt";
					jv.printToFile(outFile);
					CompletedDialog cd=new CompletedDialog(shlSrtsDependence, 5);
					cd.open();
				}
				catch(Exception ex){
					ex.printStackTrace();
					ErrorDialog ed=new ErrorDialog(shlSrtsDependence, 5);
					ed.open();
				}
			}
		});
		btnConstructDependenceGraph.setText("Construct Dependence Graph");
		btnConstructDependenceGraph.setFont(SWTResourceManager.getFont("Segoe UI", 12, SWT.NORMAL));
		btnConstructDependenceGraph.setBounds(171, 168, 250, 47);

	}

}
