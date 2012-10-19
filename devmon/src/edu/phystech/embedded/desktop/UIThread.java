//   Copyright (C) 2011  Alexey Nikiforov
//   This program is free software: you can redistribute it and/or modify
//   it under the terms of the GNU General Public License as published by
//   the Free Software Foundation, either version 3 of the License, or
//   (at your option) any later version.
//
//   This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//   GNU General Public License for more details.

//   You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package edu.phystech.embedded.desktop;


import java.awt.Frame;
import java.awt.ScrollPane;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

import javax.swing.JScrollPane;

import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Monitor;

import copy.com.android.ddms.AdbCommandRejectedException;
import copy.com.android.ddms.AndroidDebugBridge;
import copy.com.android.ddms.DevicePanel;
import copy.com.android.ddms.DevicePanel.IUiSelectionListener;
import copy.com.android.ddms.IDevice;
import copy.com.android.ddms.TimeoutException;
import copy.tightvnc.VncViewer;

/**
 * This acts as the UI builder. This cannot be its own thread since this prevent using AWT in an
 * SWT application. So this class mainly builds the ui, and manages communication between the panels
 * when {@link IDevice} / {@link Client} selection changes.
 */
public class UIThread implements IUiSelectionListener {
    /*
     * UI tab panel definitions. The constants here must match up with the array
     * indices in mPanels. PANEL_CLIENT_LIST is a "virtual" panel representing
     * the client list.
     */
	 private final static int VNC_LOCAL_PORT = 4300;
	 private final static int VNC_SERVER_PORT = 5901;
	 private final static String VNC_PASS = "android";
    // singleton instance
    private static UIThread mInstance = new UIThread();

    // our display
    private Display mDisplay;
    
    // the table we show in the left-hand pane
    private DevicePanel mDevicePanel;

    private IDevice mCurrentDevice = null;
    

    /**
     * Generic constructor.
     */
    private UIThread() {
 }

    /**
     * Get singleton instance of the UI thread.
     */
    public static UIThread getInstance() {
        return mInstance;
    }

    /**
     * Create SWT objects and drive the user interface event loop.
     * @param location location of the folder that contains ddms.
     */
    public void runUI() {
        Display.setAppName("devmon");
        mDisplay = new Display();
        final Shell shell = new Shell(mDisplay);
//        FileReader adbPath=null;
//        try {
//        	adbPath = new FileReader(PROPERTIES_FILE_NAME);
//        }
//        catch(FileNotFoundException e) {
//        }
//        try {
//        	pathInfo.load(adbPath);
//        }
//        catch(IOException i){
//        	pathInfo=null;
//        }
//        catch(NullPointerException in){
//        	pathInfo=null;
//        }
//        
//        if (pathInfo==null){
//        	manualInput(shell);
//        }
//        else {
//        	String path = pathInfo.getProperty("adbPath");
//        	if (path==null){
//        		manualInput(shell);
//        	}
//        	else {
//        		shell.setText("Device Monitor");
//        		createAdb(shell, path);
//        	}
//        }
//       	try {
//       	adbPath.close();
//       	}
//       	catch(IOException i) {
//       	}
//       	catch(NullPointerException ex){
//       	}
        shell.setText("Device Monitor");
        //Set path to adb
        String path = "lib" + File.separator + "adb" + File.separator;
        String OsName = System.getProperty("os.name").toLowerCase();
        if (OsName.indexOf("win")>=0) {
        	path += "win";
        } else if (OsName.indexOf("nix") >= 0 || OsName.indexOf("nux") >= 0) {
        	path += "lin";
        } else if (OsName.indexOf("mac") >= 0) {
        	path += "mac";
        } else {
        	mDisplay.dispose();
        	System.out.println("Invalid operating system!");
        	return;
        }
        path += File.separator + "adb";
        createAdb(shell, path);
        while (!shell.isDisposed()) {
            if (!mDisplay.readAndDispatch())
                mDisplay.sleep();
        }
//        FileWriter properties=null;
//        try {
//        properties = new FileWriter(PROPERTIES_FILE_NAME);
//        }
//        catch(IOException ex){			            
//        }
//        try {
//        	pathInfo.store(properties, "");
//        	properties.close();
//        }
//        catch(IOException ex){
//        	
//        }
        if (mDevicePanel!=null){
        	mDevicePanel.dispose();
        }
        AndroidDebugBridge.terminate();
        mDisplay.dispose();
    }
    
// This was used to manually specify the path to an Android SDK folder.
//ADB is included in the programm now so this doesn't need to be used

//    private void manualInput(final Shell shell) {
//    	pathInfo = new Properties();
//    	final Shell dialog = new Shell (mDisplay, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
//		dialog.setText("Device Monitor");
//		FormLayout formLayout = new FormLayout ();
//		formLayout.marginWidth = 10;
//		formLayout.marginHeight = 10;
//		formLayout.spacing = 14;
//		dialog.setLayout (formLayout);
//		Label label = new Label (dialog, SWT.NONE);
//		label.setText ("Path to your Android SDK folder, for example \nC:\\Program Files\\android-sdk\\ for Windows users:");
//		FormData data = new FormData ();
//		label.setLayoutData (data);
//		Button cancel = new Button (dialog, SWT.PUSH);
//		cancel.setText ("Cancel");
//		data = new FormData ();
//		data.width = 60;
//		data.right = new FormAttachment (100, 0);
//		data.bottom = new FormAttachment (100, 0);
//		cancel.setLayoutData (data);
//		cancel.addSelectionListener (new SelectionAdapter () {
//			public void widgetSelected (SelectionEvent e) {
//				dialog.close ();
//				shell.close();
//				return;
//			}
//		});
//
//		final Text text = new Text (dialog, SWT.BORDER);
//		data = new FormData ();
//		data.width = 200;
//		data.height = 15;
//		data.left = new FormAttachment (0, 0);
//		data.top = new FormAttachment (label, 0, SWT.DEFAULT);
//		data.bottom = new FormAttachment (cancel, 0, SWT.DEFAULT);
//		text.setLayoutData (data);
//		Button browse = new Button(dialog, SWT.PUSH);
//		browse.setText("Browse...");
//		browse.addSelectionListener (new SelectionAdapter() {
//			public void widgetSelected (SelectionEvent e) {
//				DirectoryDialog browseDialog = new DirectoryDialog (dialog);
//				String platform = SWT.getPlatform();
//				browseDialog.setFilterPath (platform.equals("win32") || platform.equals("wpf") ? "c:\\" : "/");
//				String path = browseDialog.open();
//				if (path!= null) {
//					text.setText(path);
//				}
//			}
//		});
//		data = new FormData();
//		data.width = 60;
//		data.left = new FormAttachment (text,0,SWT.DEFAULT);
//		data.bottom = new FormAttachment(cancel,0, SWT.DEFAULT);
//		browse.setLayoutData(data);
//		Button ok = new Button (dialog, SWT.PUSH);
//		ok.setText ("OK");
//		data = new FormData ();
//		data.width = 60;
//		data.right = new FormAttachment (cancel, 0, SWT.DEFAULT);
//		data.bottom = new FormAttachment (100, 0);
//		ok.setLayoutData (data);
//		ok.addSelectionListener (new SelectionAdapter () {
//			public void widgetSelected (SelectionEvent e) {
//				String adbLocation= text.getText ();
//			    if (adbLocation != null && adbLocation.length() != 0) {
//			    		if(!adbLocation.endsWith(File.separator)){
//			    			adbLocation+=File.separator;
//			    		}
//			            // check if there's a platform-tools folder
//			            File platformTools = new File(adbLocation+"platform-tools");  
//			            if (platformTools.isDirectory()) {
//			                adbLocation = platformTools.getAbsolutePath() + File.separator + "adb";
//			                shell.setText("Device Monitor");
//			                pathInfo.setProperty("adbPath", adbLocation);
//			               FileWriter properties=null;
//			                try {
//			               properties = new FileWriter(PROPERTIES_FILE_NAME);
//			                }
//			               catch(IOException ex){			            
//			               }
//			              try {
//			                	pathInfo.store(properties, "");
//			                	properties.close();
//			               }
//			               catch(IOException ex){
//			                	
//			              }
//			            } else {
//			                adbLocation = "adb"; 
//			                shell.setText("Device Monitor: WARNING! SDK FOLDER NOT FOUND");
//			            }
//			        } else {
//			            adbLocation = "adb"; 
//			            shell.setText("Device Monitor: WARNING! SDK FOLDER NOT FOUND");
//			        }
//
//				dialog.close ();
//				createAdb(shell,adbLocation);
//				}
//			});
//		dialog.setDefaultButton (ok);
//		dialog.pack ();
//		dialog.open ();
//    }
    private void createAdb(final Shell shell, String path) {
    	AndroidDebugBridge.init(true);
    	AndroidDebugBridge.createBridge(path, true);
    	createMenus(shell);
        createWidgets(shell);
        shell.pack();
        shell.setMinimumSize(400, 200);
        shell.setBounds(400, 350, 450, 200);
        shell.open();
    }
     /*
     * Create the menu bar and items.
     */
    private void createMenus(final Shell shell) {
        // create menu bar
        Menu menuBar = new Menu(shell, SWT.BAR);

        // create top-level items
        MenuItem fileItem = new MenuItem(menuBar, SWT.CASCADE);
        fileItem.setText("&File");
       

        // create top-level menus
        Menu fileMenu = new Menu(menuBar);
        fileItem.setMenu(fileMenu);
       MenuItem item;

 
 
        item = new MenuItem(fileMenu, SWT.NONE);
        item.setText("E&xit\tCtrl-Q");
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                shell.close();
            }
        });


        // tell the shell to use this menu
        shell.setMenuBar(menuBar);
    }

    /*
     * Create the widgets in the main application window. The basic layout is a
     * two-panel sash, with a scrolling list of VMs on the left and detailed
     * output for a single VM on the right.
     */
    private void createWidgets(final Shell shell) {
        shell.setLayout(new GridLayout(1, false));

        final Composite panelArea = new Composite(shell, SWT.BORDER);

        // make the panel area absorb all space
        panelArea.setLayoutData(new GridData(GridData.FILL_BOTH));

  
        Composite topPanel = new Composite(panelArea, SWT.NONE);
        panelArea.setLayout(new FormLayout());
        final Composite vncSwtFrame = new Composite(panelArea, SWT.EMBEDDED | SWT.NO_BACKGROUND);
        final Frame vncFrame = SWT_AWT.new_Frame(vncSwtFrame);
	    FormData data = new FormData();
        data.top = new FormAttachment(0, 0);
        data.bottom = new FormAttachment(100, 0);
        data.left = new FormAttachment(0,152);
        data.right = new FormAttachment(100, 0);
        vncSwtFrame.setLayoutData(data);
        final VncViewer v = new VncViewer(){
	    	public String getParameter (String name) {
	    		if (name.equalsIgnoreCase("host")) {
	    				return "localhost";
	    		}
	    		if (name.equalsIgnoreCase("port")){
	    			return Integer.toString(VNC_LOCAL_PORT);
	    		}
	    		if (name.equalsIgnoreCase("password")) {
	    			return VNC_PASS;
	    		}
	    		if (name.equalsIgnoreCase("Offer Relogin")) {
	    			return "No";
	    		}
	    		return null;	
	    	}
	    };
	    shell.addListener(SWT.Close, new Listener() {
	        	public void handleEvent(Event e) {
	        		if(VncViewer.refApplet!=null) {
	        			v.stop();
	        			v.destroy();
	        		}
	        	}
	        
	        });
	    //Added Scroll bar. 2 strings here
	    final ScrollPane scrollView = new ScrollPane();
	    scrollView.add(v);
        topPanel.setLayoutData(new GridData(GridData.FILL_BOTH));
        mDevicePanel = new DevicePanel(true /* showPorts */);
        Control mTree = mDevicePanel.createPanel(topPanel);
        mTree.addListener (SWT.MouseDoubleClick, new Listener () {
    		public void handleEvent (Event event) {
    			if(mCurrentDevice!=null) {
    				try{
    					mCurrentDevice.createForward(VNC_LOCAL_PORT, VNC_SERVER_PORT);
    				    if (VncViewer.refApplet !=null) {
    				    	v.stop();
    				    	v.destroy();
    				    }
    				    vncFrame.removeAll();
    				    //Added scrollbar changed v with scrollView
    				    vncFrame.add(scrollView);
    				    vncFrame.setVisible(true);
    				    v.init();
    				    v.start();
    				    
    			  
    			        if (shell.getSize().x<960) {
    			        	shell.setSize(960, shell.getSize().y);
    			        }
    			        if (shell.getSize().y<690) {
    			        	shell.setSize(shell.getSize().x, 690);
    			        }
    			        Monitor primary = mDisplay.getPrimaryMonitor();
    			        Rectangle bounds = primary.getBounds();
    			        Rectangle rect = shell.getBounds();
    			        
    			        int x = bounds.x + (bounds.width - rect.width) / 2;
    			        int y = bounds.y + (bounds.height - rect.height) / 2;
    			        shell.setLocation(x, y);
    				}
    				catch(IOException e){
  
    				}
    				catch( AdbCommandRejectedException a){
    				
    				}
    				catch(TimeoutException e){
    					
    				}
    				
    			}
    			
    		}
    	});
        // add ourselves to the device panel selection listener
        mDevicePanel.addSelectionListener(this);
        FormData topData = new FormData();
        topData.top = new FormAttachment(0, 0);
        topData.bottom = new FormAttachment(100, 0);
        topData.left = new FormAttachment(0, 0);
        topData.right = new FormAttachment(0, 151);
        topPanel.setLayoutData(topData);

  
  

    }


      
    /**
     * Sent when a new {@link IDevice} is selected.
     * @param selectedDevice the selected device. If null, no devices are selected.
     * @see IUiSelectionListener
     */

    public void selectionChanged(IDevice selectedDevice){
        if (mCurrentDevice != selectedDevice) {
            mCurrentDevice = selectedDevice;
    }
    }

}

