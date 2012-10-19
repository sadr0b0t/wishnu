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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import edu.phystech.wishnu.AcWishNuStarter.TIndicatorState;

public class SysPreparingBaseSystem {

	public static String getLastError() {
		switch(lastError) {
			case 0:
				return "No error";
			case 1:
				return "Error: System doesn't contain ext2, ext3 and ext4 (Check System Status screen)";
			case 2:
				return "Error: Can't make system directory(" + SYSTEM_FOLDER + ")";
			case 3:
				return "Error: Can't find sdcard directory(" + sdcardDirs + ")";
			case 4:
				return "Error: Can't make system node";
			default:
				return "Unnknown Error";
		}
	}

	public static TIndicatorState getStatus() {
		if (running) {
			return TIndicatorState.IS_IN_PROGRESS;
		}
		if (!error
				&& ((!hasExt2 && !hasExt3 && !hasExt4) || !hasMountpathDir
						|| !hasSdcardDir || !hasLoopNode)) {
			return TIndicatorState.IS_NONE;
		}
		if (!error && (hasExt2 || hasExt3 || hasExt4) && hasMountpathDir
				&& hasSdcardDir && hasLoopNode) {
			return TIndicatorState.IS_COMPLETED;
		}
		return TIndicatorState.IS_ERROR;
	}

	public static boolean start() {
		AcWishNuStarter
				.writeToLog("\n############Preparing Base System############\n");
		checkCurrentStage();
		running = true;
		if (!hasExt4) {
			hasExt4 = SysTools.executeSUConsoleCommand("busybox modprobe ext4") == 0;
			if (!hasExt4 && !hasExt3) {
				hasExt3 = SysTools.executeSUConsoleCommand("busybox modprobe ext3") == 0;
				if (!hasExt3 && !hasExt2) {
					hasExt2 = SysTools.executeSUConsoleCommand("busybox modprobe ext2") == 0;
					if (!hasExt2) {
						lastError = 1;
						error = true;
					}
				}
			}
		}
		if (!hasMountpathDir) {
			hasMountpathDir = SysTools.executeSUConsoleCommand("busybox mkdir "	+ SYSTEM_FOLDER) == 0;
			if (!hasMountpathDir) {
				lastError = 2;
				error = true;
			}
		}
		if (!hasSdcardDir) {
			lastError = 3;
			error = true;
		}
		if (!hasLoopNode) {
			hasLoopNode = SysTools.executeSUConsoleCommand("busybox mknod /dev/block/wishnuloop b 7 256") == 0;
			if (!hasLoopNode) {
				lastError = 4;
				error = true;
			}
		}
		running = false;
		AcWishNuStarter
				.writeToLog("\n############Preparing Base System Ends############\n");
		return !error;
	}

	public static boolean stop() {
		AcWishNuStarter
				.writeToLog("\n############Preparing Base System Unmount############\n");
		hasExt2 = false;
		hasExt3 = false;
		hasExt4 = false;
		hasMountpathDir = false;
		hasSdcardDir = false;
		hasLoopNode = false;
		error = false;
		AcWishNuStarter
				.writeToLog("\n############Preparing Base System Unmount Finishes############\n");
		return true;
	}

	public static void checkReadiness() {
		checkCurrentStage();
	}

	public static boolean hasExt4() {
			return hasExt4;
	}
	public static boolean hasExt3() {
		return hasExt3;
	}
	public static boolean hasExt2() {
		return hasExt2;
}

	// public final static String sdcardDefaultDir = "/sdcard";
	public final static String sdcardDefaultDir = "/flash";
	public final static String SYSTEM_FOLDER = "/data/data/edu.phystech.wishnu/system";
	public static List<String> sdcardDirs = new ArrayList<String>();
	public static List<String> sdcardMountDirs = new ArrayList<String>();
	public static String IMAGE_FOLDER = "/wishnu";
	private static int lastError = 0;
	private static boolean hasExt2 = false;
	private static boolean hasExt3 = false;
	private static boolean hasExt4 = false;
	private static boolean hasMountpathDir = false;
	private static boolean hasSdcardDir = false;
	private static boolean hasLoopNode = false;
	private static boolean error = false;
	private static boolean running = false;

	private static void checkCurrentStage() {
		AcWishNuStarter
				.writeToLog("\n############Preparing Base System Check############\n");
		if (sdcardDirs.isEmpty()) {
			setSDCardDirectory();
		}
		error = false;
		AcWishNuStarter.writeToLog("\n***Checking Ext2 exists/not exists***\n");
		hasExt2 = SysTools.checkExt2();
		hasExt3 = SysTools.checkExt3();
		hasExt4 = SysTools.checkExt4();
		AcWishNuStarter.writeToLog("\n" + hasExt2 + "\n");
		hasMountpathDir = (new File(SYSTEM_FOLDER)).isDirectory();
		hasSdcardDir = (new File(sdcardDirs.get(0))).isDirectory();
		AcWishNuStarter
				.writeToLog("\n***Checking system loop device file '/dev/block/wishnuloop' exists***\n");
		hasLoopNode = (new File("/dev/block/wishnuloop")).exists();
		AcWishNuStarter.writeToLog("\n" + hasLoopNode + "\n");
		AcWishNuStarter
				.writeToLog("\n############Preparing Base System Check Finishes############\n");
	}

	public static void setSDCardDirectory() {
		AcWishNuStarter.writeToLog("\n####Finding SDCARD Directories####\n");
		// check vold.fstab file, if exists
		File temp = new File("/system/etc/vold.fstab");
		if (temp.exists()) {
			try {
				AcWishNuStarter.writeToLog("##vold.fstab found, parsing##\n");
				String content;
				BufferedReader buffreader = new BufferedReader(new FileReader(
						temp));
				while ((content = buffreader.readLine()) != null) {
					String[] parsed = content.split("\\s++");
					if (parsed.length < 3) {
						continue;
					}
					if (parsed[0].contentEquals("dev_mount")
							&& (parsed[1].contains("sdcard") || parsed[1]
									.contains("flash"))) {
						int index = parsed[2].indexOf(':');
						if (index != -1) {
							AcWishNuStarter.writeToLog("!!Found " + parsed[2].substring(0, index) + "!!\n");
							sdcardDirs.add(parsed[2].substring(0, index));
						} else {
							AcWishNuStarter.writeToLog("!!Found " + parsed[2] + "!!\n");
							sdcardDirs.add(parsed[2]);
						}
					}
				}
				AcWishNuStarter.writeToLog("##parse complete##\n");
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			// check vold.conf file, if exists
			temp = new File("/system/etc/vold.conf");
			if (temp.exists()) {
				try {
					AcWishNuStarter.writeToLog("##vold.conf found, parsing##\n");
					String content;
					BufferedReader buffreader = new BufferedReader(
							new FileReader(temp));
					boolean sdcardFlag = false;
					while ((content = buffreader.readLine()) != null) {
						String[] parsed = content.split("\\s++");
						if (parsed.length < 3) {
							continue;
						}
						if (parsed[0].contentEquals("volume_sdcard")) {
							sdcardFlag = true;
						}
						if (sdcardFlag) {
							// check first tab or ignore it
							if (parsed[0].contentEquals("mount_point")) {
								int index = parsed[1].indexOf(':');
								if (index != -1) {
									AcWishNuStarter.writeToLog("!!Found " + parsed[1].substring(0, index) + "!!\n");
									sdcardDirs.add(parsed[1].substring(0, index));
								} else {
									AcWishNuStarter.writeToLog("!!Found " + parsed[1] + "!!\n");
									sdcardDirs.add(parsed[1]);
								}
							}
							if (parsed[1].contentEquals("mount_point")) {
								int index = parsed[2].indexOf(':');
								if (index != -1) {
									AcWishNuStarter.writeToLog("!!Found " + parsed[2].substring(0, index) + "!!\n");
									sdcardDirs.add(parsed[2].substring(0, index));
								} else {
									AcWishNuStarter.writeToLog("!!Found " + parsed[1] + "!!\n");
									sdcardDirs.add(parsed[2]);
								}
							}
						}
					}
					AcWishNuStarter.writeToLog("##parse complete##\n");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		// set default value, if both files don't exist
		if (sdcardDirs.isEmpty()) {
			AcWishNuStarter.writeToLog("!!Set to default " + sdcardDefaultDir + "!!\n");
			sdcardDirs.add(sdcardDefaultDir);
		}
		// prepare mounting dirs
		AcWishNuStarter.writeToLog("##Set mounting directories##\n");
		for (int i = 0; i < sdcardDirs.size(); i++) {
			String[] split = sdcardDirs.get(i).split("/");
			int j = 0;
			if (split[0].contentEquals("")) {
				j = 1;
			}
			String newName = "";
			for (; j < split.length; j++) {
				newName += split[j];
				if (j < split.length - 1) {
					newName += "_";
				}
			}
			AcWishNuStarter.writeToLog("!!Add " + newName + "!!\n");
			sdcardMountDirs.add(newName);
		}
		chooseWishnuDir();
		AcWishNuStarter.writeToLog("\n####Search complete####\n");
	}

	private static void chooseWishnuDir() {
		AcWishNuStarter.writeToLog("\n##Choosing Wishnu Directory##\n");
		for (int i = 0; i < sdcardDirs.size(); i++) {
			if (new File(sdcardDirs.get(i) + "/wishnu/system.img").exists()) {
				AcWishNuStarter.writeToLog(sdcardDirs.get(i) + "/wishnu/system.img choosed\n");
				IMAGE_FOLDER = sdcardDirs.get(i) + "/wishnu";
				return;
			} else {
				AcWishNuStarter.writeToLog(sdcardDirs.get(i) + "/wishnu/system.img not found\n");
			}
		}
		// set default dir
		AcWishNuStarter.writeToLog("!! Wishnu System not found, choosed default path: " + sdcardDirs.get(0) + "/wishnu!!\n");
		IMAGE_FOLDER = sdcardDirs.get(0) + "/wishnu";
	}
}
