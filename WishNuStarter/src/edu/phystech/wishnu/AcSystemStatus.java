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

import java.net.URL;
import java.net.URLConnection;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;

public class AcSystemStatus extends Activity {
	// private final static String MIPS_IMAGE_URI =
	// "http://wishnu.org/system/debian-squeeze-mips.img";
	private final static String MIPS_IMAGE_URI = "http://ec2-184-72-137-48.compute-1.amazonaws.com/debian/system.img";

	private final static String ARM_IMAGE_URI = "http://wishnu.org/system/debian-lenny-arm.img";
	public static String currentImageUri;
	private static long imageSize = 0;
	private Button check;
	private Button ok;
	private Button fixImage;
	private ImageView rootImageView;
	private ImageView extImageView;
	private ImageView loopImageView;
	private ImageView busyboxImageView;
	private ImageView systemimageImageView;

	private static ProgressDialog progressDialog;
	final static Handler downloadHandler = new Handler();

	private void changeView() {
		if (SystemStatusInformation.isChecked()) {
			if (SystemStatusInformation.getRootState()) {
				rootImageView.setImageResource(R.drawable.ic_stat_check_green);
			} else {
				rootImageView.setImageResource(R.drawable.ic_stat_check_red);
			}
			if (SystemStatusInformation.getExtState()) {
				extImageView.setImageResource(R.drawable.ic_stat_check_green);
			} else {
				extImageView.setImageResource(R.drawable.ic_stat_check_red);
			}
			if (SystemStatusInformation.getLooptState()) {
				loopImageView.setImageResource(R.drawable.ic_stat_check_green);
			} else {
				loopImageView.setImageResource(R.drawable.ic_stat_check_red);
			}
			if (SystemStatusInformation.getBusyboxState()) {
				busyboxImageView
						.setImageResource(R.drawable.ic_stat_check_green);
			} else {
				busyboxImageView.setImageResource(R.drawable.ic_stat_check_red);
			}
			if (SystemStatusInformation.getImageState()) {
				systemimageImageView
						.setImageResource(R.drawable.ic_stat_check_green);
				fixImage.setVisibility(View.GONE);
				fixImage.setClickable(false);
			} else {
				systemimageImageView
						.setImageResource(R.drawable.ic_stat_check_red);
				fixImage.setVisibility(View.VISIBLE);
				fixImage.setClickable(true);
			}
		}
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.ac_system_status);

		rootImageView = (ImageView) findViewById(R.id.AcSystemStatusRootImage);
		extImageView = (ImageView) findViewById(R.id.AcSystemStatusExtImage);
		loopImageView = (ImageView) findViewById(R.id.AcSystemStatusLoopImage);
		busyboxImageView = (ImageView) findViewById(R.id.AcSystemStatusBusyboxImage);
		systemimageImageView = (ImageView) findViewById(R.id.AcSystemStatusSystemImage);

		fixImage = (Button) findViewById(R.id.AcSystemStatusFixImageButton);
		fixImage.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				chooseImageArchitecture();
				// run new thread to check image size
				Thread checkURI = new Thread(new Runnable() {
					@Override
					public void run() {
						imageSize = checkImageUrl();
					}
				});
				checkURI.start();
				try {
					checkURI.join(2000);
				} catch (InterruptedException e) {
					// nothing to do
				}
				if (imageSize != -1) {
					showDownloadConfirmDialog(SysTools
							.humanReadableByteCount(imageSize));
				} else {
					showConnectFailedDialog();
				}
			}
		});

		check = (Button) findViewById(R.id.AcSystemStatusCheckButton);
		check.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				SystemStatusInformation.checkSystem();
				changeView();
			}
		});
		ok = (Button) findViewById(R.id.AcSystemStatusOkButton);
		ok.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				finish();
			}
		});
		
		
		// restore state:
		changeView();
		if (SystemStatusInformation.isDownloading()) {
			chooseImageArchitecture();
			startDownloadOfImage();
		}
	}

	private void chooseImageArchitecture() {
		String temp = SysTools.inputStreamChecker("busybox uname -a");
		if (temp.contains("arm")) {
			currentImageUri = ARM_IMAGE_URI;
		} else if (temp.contains("mips")) {
			currentImageUri = MIPS_IMAGE_URI;
		}
	}

	private void startDownloadOfImage() {
		/*
		 * progressDialog = ProgressDialog.show(this, "Please wait...",
		 * "Downloading...", false, true, new ProgressDialog.OnCancelListener()
		 * {
		 * 
		 * @Override public void onCancel(DialogInterface dialog) {
		 * cancelDownloadAction(); } });
		 */
		progressDialog = new ProgressDialog(this);
		progressDialog.setMax((int) imageSize);
		progressDialog.setTitle("Please wait...");
		progressDialog.setMessage("Downloading...");
		progressDialog.setIndeterminate(false);
		progressDialog.setCancelable(true);
		progressDialog.setButton("Run in background",
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						finish();
					}
				});
		progressDialog.setButton3("Cancel",
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						cancelDownloadAction();
						dialog.cancel();
					}
				});
		progressDialog
				.setOnCancelListener(new ProgressDialog.OnCancelListener() {
					@Override
					public void onCancel(DialogInterface dialog) {
						cancelDownloadAction();
					}
				});
		progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);

		progressDialog.show();
		final Thread checkDownloadThread = new Thread() {
			@Override
			public void run() {
				SystemStatusInformation.startDownloadThread();
				while (SystemStatusInformation.isDownloading()) {
					downloadHandler.post(updateProgressRun);
					try {
						sleep(1000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				downloadHandler.post(downloadCompleteRun);
			}
		};
		if (!checkDownloadThread.isAlive()) {
			checkDownloadThread.start();
		}

	}

	private void cancelDownloadAction() {
		if (SystemStatusInformation.downloadController != null) {
			SystemStatusInformation.downloadController.setAborted(true);
		}
	}

	public final Runnable updateProgressRun = new Runnable() {
		@Override
		public void run() {
			if (SystemStatusInformation.downloadController != null) {
				progressDialog
						.setProgress(SystemStatusInformation.downloadController
								.getProgress());
			}
		}
	};

	public final Runnable downloadCompleteRun = new Runnable() {
		@Override
		public void run() {
			progressDialog.dismiss();
			if (SystemStatusInformation.isDownloadSucceded()) {
				showDownloadCompleteDialog();
				SystemStatusInformation.checkSystem();
				changeView();
			} else {
				showDownloadFailedDialog();
			}
		}
	};

	private long checkImageUrl() {
		System.out.println(currentImageUri);
		URLConnection conn = null;
		long imageSize = 0;
		try {
			final URL url = new URL(currentImageUri);
			conn = url.openConnection();
			imageSize = conn.getContentLength();
			return imageSize;
		} catch (Exception e1) {
			e1.printStackTrace();
			// can't connect to server
			return -1;
		}

	}

	private void showConnectFailedDialog() {
		final Dialog alert = new AlertDialog.Builder(this)
				.setTitle("Failed")
				.setMessage(
						"Can't connect to the server. Please, check your internet connection.")
				.setNeutralButton(android.R.string.ok, null).create();
		try {
			alert.show();
		} catch (WindowManager.BadTokenException e) {
			// Happens when the Back button is used to exit the activity.
			// ignore.
		}
	}

	private void showDownloadConfirmDialog(String imageSize) {
		final Dialog alert = new AlertDialog.Builder(this)
				.setTitle("Confirm")
				.setMessage(
						"You are going to download system image from "
								+ currentImageUri + " to "
								+ SysPreparingBaseSystem.IMAGE_FOLDER
								+ "/system.img.\nSize: " + imageSize
								+ "\nContinue?")
				.setPositiveButton(android.R.string.yes,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								startDownloadOfImage();
								dialog.cancel();

							}
						})
				.setNegativeButton(android.R.string.no,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								dialog.cancel();

							}
						}).create();
		try {
			alert.show();
		} catch (WindowManager.BadTokenException e) {
			// Happens when the Back button is used to exit the activity.
			// ignore.
		}
	}

	private void showDownloadFailedDialog() {
		final Dialog alert = new AlertDialog.Builder(this).setTitle("Failed")
				.setMessage("Download failed.")
				.setNeutralButton(android.R.string.ok, null).create();
		try {
			alert.show();
		} catch (WindowManager.BadTokenException e) {
			// Happens when the Back button is used to exit the activity.
			// ignore.
		}
	}

	private void showDownloadCompleteDialog() {
		final Dialog alert = new AlertDialog.Builder(this)
				.setTitle("Complete!").setMessage("Download complete!")
				.setNeutralButton(android.R.string.ok, null).create();
		try {
			alert.show();
		} catch (WindowManager.BadTokenException e) {
			// Happens when the Back button is used to exit the activity.
			// ignore.
		}
	}
}