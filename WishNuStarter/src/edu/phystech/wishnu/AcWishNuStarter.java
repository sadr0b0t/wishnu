/**
 * Copyright (C) 2010-2012 Dmitry Kuklin, Wishnu Team (wishnu.org)
 * Contact mail: support@wishnu.org
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package edu.phystech.wishnu;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import android.androidVNC.COLORMODEL;
import android.androidVNC.ConnectionBean;
import android.androidVNC.VncConstants;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;

public class AcWishNuStarter extends Activity {
	private final static int MENU_ITEM_TOGGLE_LOG = Menu.FIRST;
	private final static int MENU_ITEM_SEND_LOG = Menu.FIRST+1;
	private final static int MENU_ITEM_COPY_LOGS_TO_SDCARD = Menu.FIRST + 2;
	private final static int MENU_ITEM_DELETE_LOGS = Menu.FIRST + 3;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.ac_wishnu_starter);
		startButton = (Button) findViewById(R.id.startbutton);
		sysStatusButton = (Button) findViewById(R.id.sysstatusbutton);
		stopButton = (Button) findViewById(R.id.stopbutton);
		logTxt = (TextView) findViewById(R.id.log_txt);
		showScreen = (Button) findViewById(R.id.showscreen);
		screenOptimizedForGrp = (RadioGroup) findViewById(R.id.screen_for_grp);
		IVPreparingBaseSystem = (ImageView) findViewById(R.id.IVPreparingBaseSystem);
		IVMountingSystemImage = (ImageView) findViewById(R.id.IVMountingSystemImage);
		IVPreparingSystem = (ImageView) findViewById(R.id.IVPreparingSystem);
		IVStartingSystem = (ImageView) findViewById(R.id.IVStartingSystem);
		SysLogger.init();
		new Thread(new Runnable() {

			@Override
			public void run() {
				SysLogger.writeSystemInformation();
			}

		}).start();
	}

	@Override
	public void onStart() {
		super.onStart();
		// call menu of system status
		final Intent intent = new Intent();
		intent.setClass(this, AcSystemStatus.class);
		sysStatusButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				startActivity(intent);
			}
		});
		// debug log activation
		logTxt.setMovementMethod(new ScrollingMovementMethod());
		
		stopButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				stoppingScript();
			};
		});

		startButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				startingScript();
			};
		});

		showScreen.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				showLinuxScreen();
			};
		});
	}

	@Override
	public void onResume() {
		super.onResume();
		initialStateChecker();
		if (!interfaceRefresher.isAlive()) {
			interfaceRefresher.start();
		}

	}

	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		if (logTxt != null) {
			savedInstanceState.putCharSequence("logTxt", logTxt.getText());
		}
		savedInstanceState.putString("errorDialogText", errorDialogText);
		savedInstanceState.putString("currentState", currentState.toString());
		super.onSaveInstanceState(savedInstanceState);
	}

	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		CharSequence temp;
		if ((temp = savedInstanceState.getCharSequence("logTxt")) != null) {
			logTxt.setText(temp);
		}
		errorDialogText = savedInstanceState.getString("errorDialogText");
		currentState = TStates.valueOf(savedInstanceState
				.getString("currentState"));
	}

	@Override
	public void onDestroy() {
		SysLogger.finish();
		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		// Options
		menu.add(8, MENU_ITEM_TOGGLE_LOG, Menu.NONE, "Toggle Log");
		menu.add(8, MENU_ITEM_SEND_LOG, Menu.NONE, "Send Log by e-mail");
		menu.add(8, MENU_ITEM_COPY_LOGS_TO_SDCARD, Menu.NONE, "Copy log file to sdcard");
		menu.add(8, MENU_ITEM_DELETE_LOGS, Menu.NONE, "Delete old log files");
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

		case MENU_ITEM_TOGGLE_LOG:
			if (logTxt.getVisibility() == View.VISIBLE) {
				logTxt.setVisibility(View.GONE);
			} else {
				logTxt.setVisibility(View.VISIBLE);
				logTxt.scrollTo(
						0,
						logTxt.getLineHeight() * logTxt.getLineCount()
								- logTxt.getLineHeight() * 15);
			}
			return true;
		case MENU_ITEM_SEND_LOG:
			AcWishNuStarter.writeToLog("##Sending log via e-mail##\n");
			final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
		    emailIntent.setType("text/plain");
		    emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, 
		        new String[]{SysLogger.emailTo});
		    emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Wishnu Log");
		    Uri uri = SysLogger.getUri();
		    if( uri == null ) {
		    	AcWishNuStarter.writeToLog("##Can't send log file - use text##\n");
		    	//showDialog(DIALOG_MAILERROR_ID);
		    	//return true;
		    	//emailIntent.putExtra(Intent.EXTRA_TEXT, logTxt.getText());
		    	String text = "";
		    	text += logTxt.getText();
		    	emailIntent.putExtra(Intent.EXTRA_TEXT, text);
		    } else {
		    	emailIntent.putExtra(Intent.EXTRA_STREAM, uri);
		    }
		    startActivity(Intent.createChooser(emailIntent, "Send mail..."));
		    return true;
		case MENU_ITEM_COPY_LOGS_TO_SDCARD:
			if( !SysLogger.copyLogToSdcard() ) {
				showDialog(DIALOG_COPYLOGERROR_ID);
			}
			return true;
		case MENU_ITEM_DELETE_LOGS:
			SysLogger.clearLogs();
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	public static void writeToLog(final String c) {
		Log.d("Wishnu", c);
		SysLogger.writeToLog(c);
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				logTxt.append(c);
				logTxt.scrollTo(
						0,
						logTxt.getLineHeight() * logTxt.getLineCount()
								- logTxt.getLineHeight() * 15);
			}
		});
	}

	public enum TStates {
		S_Initial, S_Executing, S_Final, S_Middle;
	}

	public enum TIndicatorState {
		IS_NONE, IS_IN_PROGRESS, IS_COMPLETED, IS_ERROR, IS_CHECKERROR
	}

	public final static String pidFilePath = SysPreparingBaseSystem.SYSTEM_FOLDER
			+ "/root/.vnc/localhost:1.pid";
	private final int TIMER = 200;
	private static final int DIALOG_ERROR_ID = 0;
	private static final int DIALOG_FORCESTOP_ID = DIALOG_ERROR_ID + 1;
	private static final int DIALOG_MAILERROR_ID = DIALOG_ERROR_ID + 2;
	private static final int DIALOG_COPYLOGERROR_ID = DIALOG_ERROR_ID + 3;
	// Need handler for callbacks to the UI thread
	private final static Handler mHandler = new Handler();
	// Create runnable for posting
	private final Runnable mUpdateUI = new Runnable() {
		@Override
		public void run() {
			updateUI();
		}
	};
	private final Runnable mShowErrorDialog = new Runnable() {
		@Override
		public void run() {
			if (!isFinishing()) {
				showDialog(DIALOG_ERROR_ID);
			}
		}
	};
	
	private final Runnable mShowForceStopDialog = new Runnable() {
		@Override
		public void run() {
			if (!isFinishing()) {
				showDialog(DIALOG_FORCESTOP_ID);
			}
		}
	};

	// basic variables
	private TStates currentState = TStates.S_Initial;
	private String errorDialogText = "";
	private Button startButton;
	private Button sysStatusButton;
	private Button stopButton;
	private Button showScreen;
	private RadioGroup screenOptimizedForGrp;
	private static TextView logTxt;
	private ImageView IVPreparingBaseSystem;
	private ImageView IVMountingSystemImage;
	private ImageView IVPreparingSystem;
	private ImageView IVStartingSystem;
	private TIndicatorState indicatorPreparingBaseSystem = TIndicatorState.IS_NONE;
	private TIndicatorState indicatorMountingSystemImage = TIndicatorState.IS_NONE;
	private TIndicatorState indicatorPreparingSystem = TIndicatorState.IS_NONE;
	private TIndicatorState indicatorStartingSystem = TIndicatorState.IS_NONE;
	private boolean indicatorChanged = false;

	private final Thread interfaceRefresher = new Thread(new Runnable() {
		@Override
		public void run() {
			while (true) {
				try {
					Thread.sleep(TIMER);
					prepareUpdatingUI();
					mHandler.post(mUpdateUI);
				} catch (InterruptedException e) {
				}
			}
		}
	});

	private void prepareUpdatingUI() {
		boolean executing = false;
		boolean firstStepReady = false;
		boolean lastStepReady = false;
		switch (SysPreparingBaseSystem.getStatus()) {
		case IS_ERROR:
			if (indicatorPreparingBaseSystem != TIndicatorState.IS_ERROR) {
				errorDialogText = SysPreparingBaseSystem.getLastError();
				mHandler.post(mShowErrorDialog);
			}
			firstStepReady = false;
			break;
		case IS_NONE:
			firstStepReady = false;
			break;
		case IS_IN_PROGRESS:
			firstStepReady = false;
			executing = true;
			break;
		case IS_COMPLETED:
			firstStepReady = true;
			break;
		}
		indicatorChanged = indicatorChanged
				|| !(indicatorPreparingBaseSystem == SysPreparingBaseSystem
						.getStatus());
		indicatorPreparingBaseSystem = SysPreparingBaseSystem.getStatus();
		switch (SysMountingSystemImage.getStatus()) {
		case IS_ERROR:
			if (indicatorMountingSystemImage != TIndicatorState.IS_ERROR) {
				errorDialogText = SysMountingSystemImage.getLastError();
				mHandler.post(mShowErrorDialog);
			}
			break;
		case IS_CHECKERROR:
			if (indicatorMountingSystemImage != TIndicatorState.IS_CHECKERROR) {
				mHandler.post(mShowForceStopDialog);
			}
			break;
		case IS_NONE:
			break;
		case IS_IN_PROGRESS:
			executing = true;
		case IS_COMPLETED:
			break;
		}
		indicatorChanged = indicatorChanged
				|| !(indicatorMountingSystemImage == SysMountingSystemImage
						.getStatus());
		indicatorMountingSystemImage = SysMountingSystemImage.getStatus();
		switch (SysPreparingSystem.getStatus()) {
		case IS_ERROR:
			if (indicatorPreparingSystem != TIndicatorState.IS_ERROR) {
				errorDialogText = SysPreparingSystem.getLastError();
				mHandler.post(mShowErrorDialog);
			}
			break;
		case IS_NONE:
			break;
		case IS_IN_PROGRESS:
			executing = true;
			break;
		case IS_COMPLETED:
			break;
		}
		indicatorChanged = indicatorChanged
				|| !(indicatorPreparingSystem == SysPreparingSystem.getStatus());
		indicatorPreparingSystem = SysPreparingSystem.getStatus();
		switch (SysStartingSystem.getStatus()) {
		case IS_ERROR:
			if (indicatorStartingSystem != TIndicatorState.IS_ERROR) {
				errorDialogText = SysStartingSystem.getLastError();
				mHandler.post(mShowErrorDialog);
			}
			lastStepReady = false;
			break;
		case IS_NONE:
			lastStepReady = false;
			break;
		case IS_IN_PROGRESS:
			lastStepReady = false;
			executing = true;
			break;
		case IS_COMPLETED:
			lastStepReady = true;
			break;
		}
		indicatorChanged = indicatorChanged
				|| !(indicatorStartingSystem == SysStartingSystem.getStatus());
		indicatorStartingSystem = SysStartingSystem.getStatus();
		if (executing) {
			setExecutingState();
		} else if (lastStepReady && firstStepReady) {
			setFinalState();
		} else if (!lastStepReady && !firstStepReady) {
			setInitialState();
		} else {
			setMiddleState();
		}
	}

	// update our UI with this method
	private void updateUI() {
		if (indicatorChanged) {
			switch (indicatorPreparingBaseSystem) {
			case IS_NONE:
				IVPreparingBaseSystem
						.setImageResource(R.drawable.ic_stat_box_grey);
				break;
			case IS_IN_PROGRESS:
				IVPreparingBaseSystem
						.setImageResource(R.drawable.ic_stat_box_yellow);
				break;
			case IS_COMPLETED:
				IVPreparingBaseSystem
						.setImageResource(R.drawable.ic_stat_box_green);
				break;
			case IS_ERROR:
				IVPreparingBaseSystem
						.setImageResource(R.drawable.ic_stat_box_red);
				break;
			}
			switch (indicatorMountingSystemImage) {
			case IS_NONE:
				IVMountingSystemImage
						.setImageResource(R.drawable.ic_stat_box_grey);
				break;
			case IS_IN_PROGRESS:
				IVMountingSystemImage
						.setImageResource(R.drawable.ic_stat_box_yellow);
				break;
			case IS_COMPLETED:
				IVMountingSystemImage
						.setImageResource(R.drawable.ic_stat_box_green);
				break;
			case IS_ERROR:
				IVMountingSystemImage
						.setImageResource(R.drawable.ic_stat_box_red);
				break;
			case IS_CHECKERROR:
				IVMountingSystemImage
						.setImageResource(R.drawable.ic_stat_box_yellow);
				break;
			}
			switch (indicatorPreparingSystem) {
			case IS_NONE:
				IVPreparingSystem.setImageResource(R.drawable.ic_stat_box_grey);
				break;
			case IS_IN_PROGRESS:
				IVPreparingSystem
						.setImageResource(R.drawable.ic_stat_box_yellow);
				break;
			case IS_COMPLETED:
				IVPreparingSystem
						.setImageResource(R.drawable.ic_stat_box_green);
				break;
			case IS_ERROR:
				IVPreparingSystem.setImageResource(R.drawable.ic_stat_box_red);
				break;
			case IS_CHECKERROR:
				IVMountingSystemImage
						.setImageResource(R.drawable.ic_stat_box_yellow);
				break;
			}
			switch (indicatorStartingSystem) {
			case IS_NONE:
				IVStartingSystem.setImageResource(R.drawable.ic_stat_box_grey);
				break;
			case IS_IN_PROGRESS:
				IVStartingSystem
						.setImageResource(R.drawable.ic_stat_box_yellow);
				break;
			case IS_COMPLETED:
				IVStartingSystem.setImageResource(R.drawable.ic_stat_box_green);
				break;
			case IS_ERROR:
				IVStartingSystem.setImageResource(R.drawable.ic_stat_box_red);
				break;
			}
			indicatorChanged = false;
		}
		int childCount = screenOptimizedForGrp.getChildCount();
		switch (currentState) {
		case S_Initial:
			for(int i = 0; i < childCount; i++) {
				screenOptimizedForGrp.getChildAt(i).setEnabled(true);
			}
			startButton.setClickable(true);
			startButton.setEnabled(true);
			stopButton.setClickable(false);
			stopButton.setEnabled(false);
			showScreen.setClickable(false);
			showScreen.setEnabled(false);
			break;
		case S_Executing:
			for(int i = 0; i < childCount; i++) {
				screenOptimizedForGrp.getChildAt(i).setEnabled(false);
			}
			startButton.setClickable(false);
			startButton.setEnabled(false);
			stopButton.setClickable(false);
			stopButton.setEnabled(false);
			showScreen.setClickable(false);
			showScreen.setEnabled(false);
			break;
		case S_Final:
			for(int i = 0; i < childCount; i++) {
				screenOptimizedForGrp.getChildAt(i).setEnabled(false);
			}
			startButton.setClickable(false);
			startButton.setEnabled(false);
			stopButton.setClickable(true);
			stopButton.setEnabled(true);
			showScreen.setClickable(true);
			showScreen.setEnabled(true);
			break;
		case S_Middle:
			for(int i = 0; i < childCount; i++) {
				screenOptimizedForGrp.getChildAt(i).setEnabled(true);
			}
			startButton.setClickable(true);
			startButton.setEnabled(true);
			stopButton.setClickable(true);
			stopButton.setEnabled(true);
			showScreen.setClickable(false);
			showScreen.setEnabled(false);
			break;
		default:
			break;
		}
	}

	// sets the state of linux: stopped, starting or started
	private void setInitialState() {
		currentState = TStates.S_Initial;
	}

	private void setExecutingState() {
		currentState = TStates.S_Executing;
	}

	private void setFinalState() {
		currentState = TStates.S_Final;
	}

	private void setMiddleState() {
		currentState = TStates.S_Middle;
	}

	private void startingScript() {
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				if (!SysPreparingBaseSystem.start()) {
					return;
				}
				if (!SysMountingSystemImage.start()) {
					return;
				}
				if (!SysPreparingSystem.start()) {
					return;
				}
				final String scriptName;
				switch(screenOptimizedForGrp.getCheckedRadioButtonId()) 
				{
					case R.id.screen_for_device:
						scriptName = "/root/.wishnu-devicerc";
						createScriptFile(".wishnu-devicerc", R.id.screen_for_device);
						break;
					case R.id.screen_for_desktop:
						scriptName = "/root/.wishnu-desktoprc";
						createScriptFile(".wishnu-desktoprc", R.id.screen_for_desktop);
						break;
					default:
						scriptName = "";
						break;
				}
				if (!SysStartingSystem.start(scriptName)) {
					return;
				}
			}
		});
		t.start();
	}

	private void stoppingScript() {
		Thread s = new Thread(new Runnable() {
			@Override
			public void run() {
				if (!SysStartingSystem.stop()) {
					return;
				}
				if (!SysPreparingSystem.stop()) {
					return;
				}
				if (!SysMountingSystemImage.stop()) {
					return;
				}
				if (!SysPreparingBaseSystem.stop()) {
					return;
				}
			}
		});
		s.start();
	}
	
	private void forceStoppingScript() {
		Thread s = new Thread(new Runnable() {
			@Override
			public void run() {
				if (!SysPreparingSystem.forceStop()) {
					return;
				}
				if (!SysMountingSystemImage.forceStop()) {
					return;
				}
				if (!SysPreparingBaseSystem.stop()) {
					return;
				}
			}
		});
		s.start();
	}
	private void refuseForceStop() {
		SysPreparingSystem.refuseForceStop();
		SysMountingSystemImage.refuseForceStop();
	}

	private void showLinuxScreen() {
		ConnectionBean selected = new ConnectionBean();
		selected.setAddress("localhost");
		selected.setPassword("android");
		selected.setPort(5901);
		selected.setColorModel(COLORMODEL.C24bit.nameString());
		Intent intent = new Intent();
		// intent.setClassName("edu.phystech.wishnu",
		// "android.androidVNC.VncCanvasActivity.class");
		intent.setClassName(this.getClass().getPackage().getName(),
				android.androidVNC.VncCanvasActivity.class.getName());
		intent.putExtra(VncConstants.CONNECTION, selected.Gen_getValues());
		startActivity(intent);
	}

	// read state file and change programm's state
	private void initialStateChecker() {
		AcWishNuStarter
				.writeToLog("\n############Initial System Check############\n");
		SysPreparingBaseSystem.checkReadiness();
		indicatorPreparingBaseSystem = SysPreparingBaseSystem.getStatus();
		SysMountingSystemImage.checkReadiness();
		indicatorMountingSystemImage = SysMountingSystemImage.getStatus();
		SysPreparingSystem.checkReadiness();
		indicatorPreparingSystem = SysPreparingSystem.getStatus();
		SysStartingSystem.checkReadiness();
		indicatorStartingSystem = SysStartingSystem.getStatus();
		indicatorChanged = true;
		AcWishNuStarter
				.writeToLog("\n############Initial System Check Finishes############\n");
	}
	
	private void createScriptFile(String fileName, int id) {
		if( !SysTools.suInputStreamChecker("ls -a " + SysPreparingBaseSystem.SYSTEM_FOLDER + "/root").contains(fileName) ) {
			try {
				File file = new File(SysPreparingBaseSystem.IMAGE_FOLDER + "/script.txt");
				FileOutputStream fos = new FileOutputStream(file);
				OutputStreamWriter osw = new OutputStreamWriter(fos);
				switch(id) {
					case R.id.screen_for_device:
						Display display = getWindowManager().getDefaultDisplay();
						Point size = new Point();
						size.y = display.getHeight();
						size.x = display.getWidth();
						//display.getSize(size);
						osw.write("#!/bin/sh\n");
						if( size.x < size.y ) {
							int y = size.y;
							size.y = size.x;
							size.x = y;
						}
						osw.write("vncserver -geometry " + size.x + "x" + size.y + " :1\n");
						break;
					case R.id.screen_for_desktop:
						osw.write("#!/bin/sh\n");
						osw.write("vncserver -geometry 1024x768 :1\n");
						break;
				}
				osw.flush();
				osw.close();
				fos.close();
				
				SysTools.executeSUConsoleCommand("busybox cp " + file.getAbsolutePath() + " " + SysPreparingBaseSystem.SYSTEM_FOLDER + "/root/" + fileName);
				file.delete();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog;
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		switch (id) {
		case DIALOG_ERROR_ID:
			builder.setMessage(errorDialogText)
					.setTitle("Start failed!")
					.setCancelable(true)
					.setNegativeButton("Ok",
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int id) {
									dialog.cancel();
								}
							});
			dialog = builder.create();
			break;
		case DIALOG_FORCESTOP_ID:
			builder.setMessage("Some programs are still using our system! Do you want to force close it?")
			.setTitle("Alert!")
			.setCancelable(false)
			.setPositiveButton("Ok",
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog,
								int id) {
							forceStoppingScript();
							dialog.cancel();
						}
					})
			.setNegativeButton("Cancel",
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog,
								int id) {
							refuseForceStop();
							dialog.cancel();
						}
					});
			dialog = builder.create();
			break;
		case DIALOG_MAILERROR_ID:
			builder.setMessage("Can't send log (only available with sdcard)")
			.setTitle("Send failed!")
			.setCancelable(true)
			.setNegativeButton("Ok",
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog,
								int id) {
							dialog.cancel();
						}
					});
			dialog = builder.create();
			break;
		case DIALOG_COPYLOGERROR_ID:
			builder.setMessage("Can't copy log (only available with sdcard)")
			.setTitle("Copy failed!")
			.setCancelable(true)
			.setNegativeButton("Ok",
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog,
								int id) {
							dialog.cancel();
						}
					});
			dialog = builder.create();
			break;
		default:
			dialog = null;
		}
		return dialog;
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		switch (id) {
		case DIALOG_ERROR_ID:
			AlertDialog temp = (AlertDialog) dialog;
			temp.setMessage(errorDialogText);
			break;
		}
	}
}
