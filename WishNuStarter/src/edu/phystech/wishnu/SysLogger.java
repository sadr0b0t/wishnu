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
import java.text.DateFormat;
import java.util.Date;

import android.net.Uri;
import android.os.Build;
import android.os.Environment;

public class SysLogger {
	private static String sdcardLogFolder = Environment.getExternalStorageDirectory().getAbsolutePath() + "/wishnu/logs/";
	private static String standartLogFolder = "/data/data/edu.phystech.wishnu/logs/";
	private static OutputStreamWriter bos;
	private static FileOutputStream fos;
	private static boolean logReady = false;
	private static File logFile;
	public static final String emailTo = "support@wishnu.org";
	public static Uri getUri() {
		if( logReady ) {
			try {
				bos.flush();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			copyLogToSdcard();
			File file = new File(sdcardLogFolder + "/" + logFile.getName());
			if( !file.exists() ) {
				return null;
			}
			return Uri.fromFile(file);
		} else {
			return null;
		}
	}
	public static boolean init() {
		(new File(standartLogFolder)).mkdirs();
		(new File(sdcardLogFolder)).mkdirs();
		String datePart =  DateFormat.getDateInstance().format(new Date()) + "_" + DateFormat.getTimeInstance().format(new Date());
		datePart = datePart.replace(':', '_');
		datePart = datePart.replace('.', '_');
		datePart = datePart.replace(' ', '_');
		datePart = datePart.replace(',', '_');
		datePart = datePart.replace(';', '_');
		String curLogFile = standartLogFolder + datePart + ".log";
		logFile = new File(curLogFile);
		try {
			fos = new FileOutputStream(curLogFile);
			bos = new OutputStreamWriter(fos);
			if( bos != null) {
				SysLogger.writeToLog("#######STARTING LOG#######\n");
				logReady = true;
				return true;
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return false;
	}
	public static void writeToLog(String text) {
			try {
				if( !logReady ) {
					return;
				}
				bos.write(text);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}
	public static boolean finish() {
		SysLogger.writeToLog("#######STOPPING LOG#######\n");
		try {
			if( !logReady ) {
				return true;
			}
			logReady = false;
			bos.flush();
			bos.close();
			fos.close();
			copyLogToSdcard();
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}
	public static void writeSystemInformation() {
		try {
			if( !logReady ) {
				return;
			}
			String systemInfo = "\nBoard: " + Build.BOARD + "\n";
			systemInfo += "Brand: " + Build.BRAND + "\n";
			systemInfo += "Device: " + Build.DEVICE + "\n";
			systemInfo += "Display: " + Build.DISPLAY + "\n";
			systemInfo += "Fingerprint: " + Build.FINGERPRINT + "\n";
			systemInfo += "Host: " + Build.HOST + "\n";
			systemInfo += "ID: " + Build.ID + "\n";
			systemInfo += "Model: " + Build.MODEL + "\n";
			systemInfo += "Product" + Build.PRODUCT + "\n";
			systemInfo += "Tags: " + Build.TAGS + "\n";
			systemInfo += "Type: " + Build.TYPE + "\n";
			systemInfo += "User: " + Build.USER + "\n";
			systemInfo += "Version, Release: " + Build.VERSION.RELEASE + "\n";
			systemInfo += "Version, SDK: " + Build.VERSION.SDK + "\n";
			bos.write(systemInfo);
			bos.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public static void clearLogs() {
		AcWishNuStarter.writeToLog("##Start deleting logs##\n");
		File temp = new File(sdcardLogFolder);
		File deletingFile;
		if (temp.isDirectory()) {
	        String[] children = temp.list();
	        AcWishNuStarter.writeToLog("!!Delete from" + temp.getAbsolutePath() + "!!\n");
	        for (int i = 0; i < children.length; i++) {
	            deletingFile = new File(temp, children[i]);
	            if( !logFile.getAbsolutePath().contains(children[i]) ) {
	            	AcWishNuStarter.writeToLog("!!deleted " + deletingFile.getName() + "!!\n");
	            	deletingFile.delete();
	            }
	        }
	    }
		temp = new File(standartLogFolder);
		if (temp.isDirectory()) {
	        String[] children = temp.list();
	        AcWishNuStarter.writeToLog("!!Delete from" + temp.getAbsolutePath() + "!!\n");
	        for (int i = 0; i < children.length; i++) {
	        	deletingFile = new File(temp, children[i]);
	        	if( !logFile.getAbsolutePath().contains(children[i]) ) {
	        		AcWishNuStarter.writeToLog("!!deleted " + deletingFile.getName() + "!!\n");
	            	deletingFile.delete();
	            }
	        }
	    }
		AcWishNuStarter.writeToLog("##Logs were successfully deleted##\n");
	}
	public static boolean copyLogToSdcard() {
		File sdcardLogFile = new File(sdcardLogFolder + "/" + logFile.getName());
		return copyLogToFile(sdcardLogFile);
	}
	
	public static boolean copyLogToFile(File file) {
		try {
			bos.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return SysTools.CopyFile(logFile, file);
	}
}
