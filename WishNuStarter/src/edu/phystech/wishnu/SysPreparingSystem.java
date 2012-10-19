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

import edu.phystech.wishnu.AcWishNuStarter.TIndicatorState;

public class SysPreparingSystem {

	private static TPrepareSystemStages currentStage = TPrepareSystemStages.PSS_NONE;
	private static int lastError = 0;
	private static int mountSdCardNum = 0;
	
	private enum TPrepareSystemStages {
		PSS_ERROR, PSS_CHECKERROR, PSS_NONE, PSS_MOUNTDEVPTS, PSS_MOUNTPROC, PSS_MOUNTSYS, PSS_MOUNTSDCARD, PSS_COMPLETED
	}

	public static void refuseForceStop() {
		if( currentStage == TPrepareSystemStages.PSS_CHECKERROR ) {
			currentStage = TPrepareSystemStages.PSS_ERROR;
		}
	}
	
	public static String getLastError() {
		switch(lastError) {
			case 0:
				return "No error";
			case 1:
				return "Error: Can't connect devpts folder to OS";
			case 2:
				return "Error: Can't connect process folder to OS";
			case 3:
				return "Error: Can't connect system folder to OS";
			case 4:
				return "Error: Can't connect sdcard[" + mountSdCardNum + "] to OS";
			case 5:
				return "Error: Can't disconnect sdcard[" + (mountSdCardNum - 1) + "] from OS";
			case 6:
				return "Error: Can't disconnect system folder from OS";
			case 7:
				return "Error: Can't disconnect process folder from OS";
			case 8:
				return "Error: Can't disconnect devpts folder from OS";
			default:
				return "Unnknown Error";
		}
	}

	public static TIndicatorState getStatus() {
		switch (currentStage) {
		case PSS_ERROR:
			return TIndicatorState.IS_ERROR;
		case PSS_CHECKERROR:
			return TIndicatorState.IS_CHECKERROR;
		case PSS_NONE:
			return TIndicatorState.IS_NONE;
		case PSS_MOUNTDEVPTS:
			return TIndicatorState.IS_IN_PROGRESS;
		case PSS_MOUNTPROC:
			return TIndicatorState.IS_IN_PROGRESS;
		case PSS_MOUNTSYS:
			return TIndicatorState.IS_IN_PROGRESS;
		case PSS_MOUNTSDCARD:
			return TIndicatorState.IS_IN_PROGRESS;
		case PSS_COMPLETED:
			return TIndicatorState.IS_COMPLETED;
		}
		return TIndicatorState.IS_ERROR;
	}

	public static boolean start() {
		AcWishNuStarter
				.writeToLog("\n############Preparing System############\n");
		currentStage = checkCurrentStage();
		switch (currentStage) {
		case PSS_MOUNTDEVPTS:
			if (SysTools
					.executeSUConsoleCommand("busybox mount -t devpts  devpts  "
							+ SysPreparingBaseSystem.SYSTEM_FOLDER + "/dev/pts") != 0) {
				lastError = 1;
				currentStage = TPrepareSystemStages.PSS_ERROR;
				break;
			}
			currentStage = TPrepareSystemStages.PSS_MOUNTPROC;
		case PSS_MOUNTPROC:
			if (SysTools.executeSUConsoleCommand("busybox mount -t proc proc "
					+ SysPreparingBaseSystem.SYSTEM_FOLDER + "/proc") != 0) {
				lastError = 2;
				currentStage = TPrepareSystemStages.PSS_ERROR;
				break;
			}
			currentStage = TPrepareSystemStages.PSS_MOUNTSYS;
		case PSS_MOUNTSYS:
			if (SysTools
					.executeSUConsoleCommand("busybox mount -t sysfs sysfs "
							+ SysPreparingBaseSystem.SYSTEM_FOLDER + "/sys") != 0) {
				lastError = 3;
				currentStage = TPrepareSystemStages.PSS_ERROR;
				break;
			}
			currentStage = TPrepareSystemStages.PSS_MOUNTSDCARD;
		case PSS_MOUNTSDCARD:
			for (; mountSdCardNum < SysPreparingBaseSystem.sdcardDirs.size(); mountSdCardNum++) {
				if (SysTools.executeSUConsoleCommand("busybox mount --bind "
						+ SysPreparingBaseSystem.sdcardDirs.get(mountSdCardNum)
						+ " "
						+ SysPreparingBaseSystem.SYSTEM_FOLDER
						+ "/media/"
						+ SysPreparingBaseSystem.sdcardMountDirs
								.get(mountSdCardNum)) != 0) {
					lastError = 4;
					// currentStage = TPrepareSystemStages.PSS_ERROR;
					// break;
				}
			}
			if (currentStage == TPrepareSystemStages.PSS_ERROR) {
				break;
			}
			currentStage = TPrepareSystemStages.PSS_COMPLETED;
		case PSS_COMPLETED:
		}
		AcWishNuStarter
				.writeToLog("\n############Preparing System Ends############\n");
		return currentStage != TPrepareSystemStages.PSS_ERROR;
	}

	public static boolean stop() {
		AcWishNuStarter
				.writeToLog("\n############Preparing System Unmount############\n");
		currentStage = checkCurrentStage();
		switch (currentStage) {
		case PSS_COMPLETED:
			for (; mountSdCardNum > 0; mountSdCardNum--) {
				if (SysTools.executeSUConsoleCommand("busybox umount "
						+ SysPreparingBaseSystem.SYSTEM_FOLDER
						+ "/media/"
						+ SysPreparingBaseSystem.sdcardMountDirs
								.get(mountSdCardNum - 1)) != 0) {
					lastError = 5;
					currentStage = TPrepareSystemStages.PSS_CHECKERROR;
					break;
				}
			}
			if (currentStage == TPrepareSystemStages.PSS_CHECKERROR) {
				break;
			}
			currentStage = TPrepareSystemStages.PSS_MOUNTSDCARD;
		case PSS_MOUNTSDCARD:
			if (SysTools.executeSUConsoleCommand("busybox umount "
					+ SysPreparingBaseSystem.SYSTEM_FOLDER + "/sys") != 0) {
				lastError = 6;
				currentStage = TPrepareSystemStages.PSS_CHECKERROR;
				break;
			}
			currentStage = TPrepareSystemStages.PSS_MOUNTSYS;
		case PSS_MOUNTSYS:
			if (SysTools.executeSUConsoleCommand("busybox umount "
					+ SysPreparingBaseSystem.SYSTEM_FOLDER + "/proc") != 0) {
				lastError = 7;
				currentStage = TPrepareSystemStages.PSS_CHECKERROR;
				break;
			}
			currentStage = TPrepareSystemStages.PSS_MOUNTPROC;
		case PSS_MOUNTPROC:
			if (SysTools.executeSUConsoleCommand("busybox umount "
					+ SysPreparingBaseSystem.SYSTEM_FOLDER + "/dev/pts") != 0) {
				lastError = 8;
				currentStage = TPrepareSystemStages.PSS_CHECKERROR;
				break;
			}
			currentStage = TPrepareSystemStages.PSS_MOUNTDEVPTS;
		case PSS_MOUNTDEVPTS:
			AcWishNuStarter
					.writeToLog("\n############Starting System Unmount Finishes############\n");
			currentStage = TPrepareSystemStages.PSS_NONE;
		}
		return currentStage != TPrepareSystemStages.PSS_ERROR;
	}
	
	public static boolean forceStop() {
		AcWishNuStarter
				.writeToLog("\n############Preparing System Unmount############\n");
		currentStage = checkCurrentStage();
		switch (currentStage) {
		case PSS_COMPLETED:
			for (; mountSdCardNum > 0; mountSdCardNum--) {
				if (SysTools.executeSUConsoleCommand("busybox umount -l "
						+ SysPreparingBaseSystem.SYSTEM_FOLDER
						+ "/media/"
						+ SysPreparingBaseSystem.sdcardMountDirs
								.get(mountSdCardNum - 1)) != 0) {
					lastError = 5;
					currentStage = TPrepareSystemStages.PSS_ERROR;
					break;
				}
			}
			if (currentStage == TPrepareSystemStages.PSS_ERROR) {
				break;
			}
			currentStage = TPrepareSystemStages.PSS_MOUNTSDCARD;
		case PSS_MOUNTSDCARD:
			if (SysTools.executeSUConsoleCommand("busybox umount -l "
					+ SysPreparingBaseSystem.SYSTEM_FOLDER + "/sys") != 0) {
				lastError = 6;
				currentStage = TPrepareSystemStages.PSS_ERROR;
				break;
			}
			currentStage = TPrepareSystemStages.PSS_MOUNTSYS;
		case PSS_MOUNTSYS:
			if (SysTools.executeSUConsoleCommand("busybox umount -l "
					+ SysPreparingBaseSystem.SYSTEM_FOLDER + "/proc") != 0) {
				lastError = 7;
				currentStage = TPrepareSystemStages.PSS_ERROR;
				break;
			}
			currentStage = TPrepareSystemStages.PSS_MOUNTPROC;
		case PSS_MOUNTPROC:
			if (SysTools.executeSUConsoleCommand("busybox umount -l "
					+ SysPreparingBaseSystem.SYSTEM_FOLDER + "/dev/pts") != 0) {
				lastError = 8;
				currentStage = TPrepareSystemStages.PSS_ERROR;
				break;
			}
			currentStage = TPrepareSystemStages.PSS_MOUNTDEVPTS;
		case PSS_MOUNTDEVPTS:
			AcWishNuStarter
					.writeToLog("\n############Starting System Unmount Finishes############\n");
			currentStage = TPrepareSystemStages.PSS_NONE;
		}
		return currentStage != TPrepareSystemStages.PSS_ERROR;
	}

	public static void checkReadiness() {
		if (checkCurrentStage() == TPrepareSystemStages.PSS_COMPLETED) {
			currentStage = TPrepareSystemStages.PSS_COMPLETED;
		}
	}
	
	private static TPrepareSystemStages checkCurrentStage() {
		// Check directories for mounting
		AcWishNuStarter
				.writeToLog("\n############Preparing System Check############\n");
		AcWishNuStarter.writeToLog("\n**Check "
				+ SysPreparingBaseSystem.SYSTEM_FOLDER
				+ "/dev/pts mounted/not mounted**\n");
		if (!SysTools.isMountPoint(SysPreparingBaseSystem.SYSTEM_FOLDER
				+ "/dev/pts")) {
			AcWishNuStarter
					.writeToLog("\n############Start from mount devpts############\n");
			return TPrepareSystemStages.PSS_MOUNTDEVPTS;
		}
		AcWishNuStarter.writeToLog("\n**Check "
				+ SysPreparingBaseSystem.SYSTEM_FOLDER
				+ "/proc mounted/not mounted**\n");
		if (!SysTools.isMountPoint(SysPreparingBaseSystem.SYSTEM_FOLDER
				+ "/proc")) {
			AcWishNuStarter
					.writeToLog("\n############Start from mount proc############\n");
			return TPrepareSystemStages.PSS_MOUNTPROC;
		}
		AcWishNuStarter.writeToLog("\n**Check "
				+ SysPreparingBaseSystem.SYSTEM_FOLDER
				+ "/sys mounted/not mounted**\n");
		if (!SysTools.isMountPoint(SysPreparingBaseSystem.SYSTEM_FOLDER
				+ "/sys")) {
			AcWishNuStarter
					.writeToLog("\n############Start from mount sys############\n");
			return TPrepareSystemStages.PSS_MOUNTSYS;
		}
		for (mountSdCardNum = 0; mountSdCardNum < SysPreparingBaseSystem.sdcardDirs
				.size(); mountSdCardNum++) {
			AcWishNuStarter.writeToLog("\n**Check "
					+ SysPreparingBaseSystem.SYSTEM_FOLDER
					+ "/media/"
					+ SysPreparingBaseSystem.sdcardMountDirs
							.get(mountSdCardNum) + " mounted/not mounted**\n");
			if (!SysTools.isMountPoint(SysPreparingBaseSystem.SYSTEM_FOLDER
					+ "/media/"
					+ SysPreparingBaseSystem.sdcardMountDirs
							.get(mountSdCardNum))) {
				AcWishNuStarter
						.writeToLog("\n############Start from mount SDCard["
								+ mountSdCardNum + "]############\n");
				return TPrepareSystemStages.PSS_MOUNTSDCARD;
			}
		}
		AcWishNuStarter.writeToLog("\n############All Correct############\n");
		return TPrepareSystemStages.PSS_COMPLETED;
	}
}
