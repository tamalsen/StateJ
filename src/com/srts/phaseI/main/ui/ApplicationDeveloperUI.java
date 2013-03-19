package com.srts.phaseI.main.ui;
import java.io.File;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;


public class ApplicationDeveloperUI {

	protected Shell shlApplicationDeveloperUi;
	private Text text;
	private Text text_1;
	private Text text_2;
	private Button btnRun;
	private Label lblSrtsForComponent;
	private Button btnOpenTransitionCoverage;

	/**
	 * Launch the application.
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			ApplicationDeveloperUI window = new ApplicationDeveloperUI();
			window.open();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Open the window.
	 */
	public void open() {
		Display display = Display.getDefault();
		createContents();
		shlApplicationDeveloperUi.open();
		shlApplicationDeveloperUi.layout();
		while (!shlApplicationDeveloperUi.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
	}

	/**
	 * Create contents of the window.
	 */
	protected void createContents() {
		shlApplicationDeveloperUi = new Shell();
		shlApplicationDeveloperUi.setImage(SWTResourceManager.getImage("C:\\Users\\Public\\Pictures\\Sample Pictures\\Chrysanthemum.jpg"));
		shlApplicationDeveloperUi.setBackground(SWTResourceManager.getColor(245, 222, 179));
		shlApplicationDeveloperUi.setSize(677, 413);
		shlApplicationDeveloperUi.setText("S-RTS");
		
		text = new Text(shlApplicationDeveloperUi, SWT.BORDER);
		text.setBounds(37, 125, 388, 25);
		
		Button btnOpenFile = new Button(shlApplicationDeveloperUi, SWT.NONE);
		btnOpenFile.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				FileDialog c1 = new FileDialog(shlApplicationDeveloperUi, SWT.OPEN);
				String path = c1.open();
				if(path==null)
					return;
				else{
					File file=new File(path);
					text.setText(file.getAbsolutePath());
				}
				
			}
		});
		btnOpenFile.setBounds(443, 125, 171, 25);
		btnOpenFile.setText("Open Change Info File");
		
		text_1 = new Text(shlApplicationDeveloperUi, SWT.BORDER);
		text_1.setBounds(37, 188, 388, 25);
		
		text_2 = new Text(shlApplicationDeveloperUi, SWT.BORDER);
		text_2.setBounds(37, 250, 388, 25);
		
		Button btnSaveFile = new Button(shlApplicationDeveloperUi, SWT.NONE);
		btnSaveFile.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				FileDialog c1 = new FileDialog(shlApplicationDeveloperUi, SWT.SAVE);
				String path = c1.open();
				if(path==null)
					return;
				else{
					File file=new File(path);
					text_2.setText(file.getAbsolutePath());
				}
			}
		});
		btnSaveFile.setBounds(443, 250, 171, 25);
		btnSaveFile.setText("Specify Output File");
		
		btnRun = new Button(shlApplicationDeveloperUi, SWT.NONE);
		btnRun.setFont(SWTResourceManager.getFont("Segoe UI", 14, SWT.NORMAL));
		btnRun.setBounds(206, 304, 261, 48);
		btnRun.setText("Select Regression Test Cases");
		
		lblSrtsForComponent = new Label(shlApplicationDeveloperUi, SWT.NONE);
		lblSrtsForComponent.setAlignment(SWT.CENTER);
		lblSrtsForComponent.setFont(SWTResourceManager.getFont("Consolas", 16, SWT.NORMAL));
		lblSrtsForComponent.setBounds(165, 26, 337, 58);
		lblSrtsForComponent.setText("Applilcation Developers' Interface");
		
		btnOpenTransitionCoverage = new Button(shlApplicationDeveloperUi, SWT.NONE);
		btnOpenTransitionCoverage.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				FileDialog c1 = new FileDialog(shlApplicationDeveloperUi, SWT.OPEN);
				String path = c1.open();
				if(path==null)
					return;
				else{
					File file=new File(path);
					text_1.setText(file.getAbsolutePath());
				}
			}
		});
		btnOpenTransitionCoverage.setText("Open Transition Coverage File");
		btnOpenTransitionCoverage.setBounds(443, 188, 171, 25);

	}
}



