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

import com.sadko.util.NetworkUtil;
import com.sadko.util.TaskController;

public class SystemStatusInformation {
	
	private static boolean checked = false;
	private static boolean checkRoot;
	private static boolean checkExt;
	private static boolean checkLoop;
	private static boolean checkBusybox;
	private static boolean checkImage;
	public static TaskController downloadController = null;
	private static File downloadedFile = null;
	private static boolean downloadSuccess = false;
	private static Thread downloadThread = new Thread() {
		@Override
		public void run() {
			downloadFile(AcSystemStatus.currentImageUri, SysPreparingBaseSystem.IMAGE_FOLDER, "system", ".img");
		}
	};
	public static void startDownloadThread() {
		if( !downloadThread.isAlive() ) {
			downloadThread = new Thread() {
				@Override
				public void run() {
					downloadFile(AcSystemStatus.currentImageUri, SysPreparingBaseSystem.IMAGE_FOLDER, "system", ".img");
				}
			};
			downloadThread.start();
		}
	}
	public static boolean getRootState() {
		return checkRoot;
	}
	public static boolean getExtState() {
		return checkExt;
	}
	public static boolean getLooptState() {
		return checkLoop;
	}
	public static boolean getBusyboxState() {
		return checkBusybox;
	}
	public static boolean getImageState() {
		return checkImage;
	}
	public static boolean isChecked() {
		return checked;
	}
	public static boolean isDownloadSucceded() {
		return downloadSuccess;
	}
	public static boolean isDownloading() {
		return downloadThread.isAlive();
	}
	
	public static void checkSystem() 
	{
		Thread checkSystem = new Thread (new Runnable() 
	    {
			@Override
	        public void run() 
			{
				checkRoot = SysTools.checkRoot();
				checkExt = SysTools.checkExt4();
				if( !checkExt ) {
					checkExt = SysTools.executeSUConsoleCommand("busybox modprobe ext4") == 0;
				}
				if( !checkExt ) {
					checkExt = SysTools.checkExt3();
				}
				if( !checkExt ) {
					checkExt = SysTools.executeSUConsoleCommand("busybox modprobe ext3") == 0;
				}
				if( !checkExt ) {
					checkExt = SysTools.checkExt2();
				}
				if( !checkExt ) {
					checkExt = SysTools.executeSUConsoleCommand("busybox modprobe ext2") == 0;
				}
				checkLoop = SysTools.checkLoop();
				checkBusybox = SysTools.checkBusybox();
				checkImage = SysTools.checkImage();
				checked = true;
	        }
		});
		checkSystem.start();
		try {
			checkSystem.join(5000);
		} catch (InterruptedException e) {
			// nothing to do
		}
	}
	// All methods, connected with downloading
	
	public static void downloadFile(String uri, String path, String prefix, String suffix) 
	{
		File temp = null;
		try {
			downloadSuccess = false;
			downloadController = new TaskController();
			downloadedFile = new File(path + "/" + prefix + suffix);
			File dirCheck = new File (path);
			if ( !dirCheck.exists() ) {
				dirCheck.mkdirs();
			}
			final File tmpFile = File.createTempFile(prefix, suffix, new File(path));
			temp = tmpFile;
			NetworkUtil.download(uri, tmpFile.getAbsolutePath(),
					downloadController);
			tmpFile.renameTo(downloadedFile);
			downloadSuccess = true;
		} catch( Exception ex ) {
			ex.printStackTrace();
			downloadSuccess = false;
			if( temp != null ) {
				temp.delete();
			}
		}
	}

}
