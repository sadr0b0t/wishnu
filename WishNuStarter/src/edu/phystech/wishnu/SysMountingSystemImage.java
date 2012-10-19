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

import edu.phystech.wishnu.AcWishNuStarter.TIndicatorState;

public class SysMountingSystemImage {
	
	private static TMountSystemStages currentStage = TMountSystemStages.MSS_NONE;
	private static int lastError = 0;
	private static int allowSdCardNum = 0;
	private enum TMountSystemStages
	{
		MSS_ERROR,
		MSS_CHECKERROR,
		MSS_NONE,
		MSS_CREATELOOP,
		MSS_MOUNTIMAGE,
		MSS_ALLOWSDCARDS,
		MSS_COMPLETED
	}
	
	public static void refuseForceStop() {
		if( currentStage == TMountSystemStages.MSS_CHECKERROR ) {
			currentStage = TMountSystemStages.MSS_ERROR;
		}
	}
	
	public static String getLastError() {
		switch(lastError) {
			case 0:
				return "No error";
			case 1:
				return "Error: Can't prepare OS (Check your System image at System Status screen)";
			case 2:
				return "Error: Can't connect OS image";
			case 3:
				return "Error: Can't make sdcard[" + allowSdCardNum + "]directory at system directory";
			case 4:
				return "Error: Can't disconnect OS image";
			case 5:
				return "Error: Can't release OS";
			default:
				return "Unnknown Error";
		}
	}
	public static TIndicatorState getStatus() {
		switch( currentStage ) {
		case MSS_ERROR :
			return TIndicatorState.IS_ERROR;
		case MSS_CHECKERROR :
			return TIndicatorState.IS_CHECKERROR;
		case MSS_NONE :
			return TIndicatorState.IS_NONE;
		case MSS_CREATELOOP :
			return TIndicatorState.IS_IN_PROGRESS;
		case MSS_MOUNTIMAGE :
			return TIndicatorState.IS_IN_PROGRESS;
		case MSS_ALLOWSDCARDS :
			return TIndicatorState.IS_IN_PROGRESS;
		case MSS_COMPLETED :
			return TIndicatorState.IS_COMPLETED;
		}
		return TIndicatorState.IS_ERROR;
	}
	
	public static boolean start() {
		AcWishNuStarter.writeToLog("\n############Mounting System Image############\n");
		currentStage = checkCurrentStage();
		// mount image & share SDcard with sharedSD folder
		switch(currentStage) {
		case MSS_CREATELOOP:
			if( SysTools.executeSUConsoleCommand("busybox losetup /dev/block/wishnuloop " + SysPreparingBaseSystem.IMAGE_FOLDER + "/system.img") != 0 ) {
				lastError = 1;
				currentStage = TMountSystemStages.MSS_ERROR;
				break;
			}
			currentStage = TMountSystemStages.MSS_MOUNTIMAGE;
		case MSS_MOUNTIMAGE:
			boolean mounted = false;
			if( SysPreparingBaseSystem.hasExt4() ) {
				if( SysTools.executeSUConsoleCommand("busybox mount -t ext4 /dev/block/wishnuloop " + SysPreparingBaseSystem.SYSTEM_FOLDER) == 0 ) {
					mounted = true;
				}
			} 
			if( !mounted && SysPreparingBaseSystem.hasExt3() ) {
				if( SysTools.executeSUConsoleCommand("busybox mount -t ext3 /dev/block/wishnuloop " + SysPreparingBaseSystem.SYSTEM_FOLDER) == 0 ) {
					mounted = true;
				}
			}
			if( !mounted && SysPreparingBaseSystem.hasExt2() ) {
				if( SysTools.executeSUConsoleCommand("busybox mount -t ext2 /dev/block/wishnuloop " + SysPreparingBaseSystem.SYSTEM_FOLDER) == 0 ) {
					mounted = true;
				}
			}
			if( !mounted ) {
				lastError = 2;
				currentStage = TMountSystemStages.MSS_ERROR;
				break;
			}
			currentStage = TMountSystemStages.MSS_ALLOWSDCARDS;
		case MSS_ALLOWSDCARDS:
			for( ; allowSdCardNum < SysPreparingBaseSystem.sdcardMountDirs.size(); allowSdCardNum++) {
				if( !(new File(SysPreparingBaseSystem.SYSTEM_FOLDER + "/media/" + SysPreparingBaseSystem.sdcardMountDirs.get(allowSdCardNum))).isDirectory() ) {
					if( SysTools.executeSUConsoleCommand("busybox mkdir -p " + SysPreparingBaseSystem.SYSTEM_FOLDER + "/media/" + SysPreparingBaseSystem.sdcardMountDirs.get(allowSdCardNum)) != 0 ) {
						lastError = 3;
						currentStage = TMountSystemStages.MSS_ERROR;
						break;
					}
				}
			}
			if( currentStage == TMountSystemStages.MSS_ERROR ) {
				break;
			}
			currentStage = TMountSystemStages.MSS_COMPLETED;
		case MSS_COMPLETED:
		}
		AcWishNuStarter.writeToLog("\n############Mounting System Image Ends############\n");
		return currentStage != TMountSystemStages.MSS_ERROR;
	}
	public static boolean stop() {
		AcWishNuStarter.writeToLog("\n############Unmount System Image############\n");
		currentStage = checkCurrentStage();
		switch(currentStage) {
		case MSS_COMPLETED:
			// no need to delete sharedSD folder in mount directory, so skip this step
			currentStage = TMountSystemStages.MSS_ALLOWSDCARDS;
		case MSS_ALLOWSDCARDS:
			if( SysTools.executeSUConsoleCommand("busybox umount " + SysPreparingBaseSystem.SYSTEM_FOLDER) != 0 ) {
				lastError = 4;
				currentStage = TMountSystemStages.MSS_CHECKERROR;
				break;
			}
			currentStage = TMountSystemStages.MSS_MOUNTIMAGE;
		case MSS_MOUNTIMAGE:
			if( SysTools.executeSUConsoleCommand("busybox losetup -d /dev/block/wishnuloop") != 0 ) {
				lastError = 5;
				currentStage = TMountSystemStages.MSS_ERROR;
				break;
			}
			currentStage = TMountSystemStages.MSS_CREATELOOP;
		case MSS_CREATELOOP:
			AcWishNuStarter.writeToLog("\n############Unmount System Image Finishes############\n");
			currentStage = TMountSystemStages.MSS_NONE;
		}
		return currentStage != TMountSystemStages.MSS_ERROR && currentStage != TMountSystemStages.MSS_CHECKERROR;
	}
	public static boolean forceStop() {
		AcWishNuStarter.writeToLog("\n############Force Unmount System Image############\n");
		currentStage = checkCurrentStage();
		switch(currentStage) {
		case MSS_COMPLETED:
			// no need to delete sharedSD folder in mount directory, so skip this step
			currentStage = TMountSystemStages.MSS_ALLOWSDCARDS;
		case MSS_ALLOWSDCARDS:
			if( SysTools.executeSUConsoleCommand("busybox umount -l " + SysPreparingBaseSystem.SYSTEM_FOLDER) != 0 ) {
				lastError = 4;
				currentStage = TMountSystemStages.MSS_ERROR;
				break;
			}
			currentStage = TMountSystemStages.MSS_MOUNTIMAGE;
		case MSS_MOUNTIMAGE:
			if( SysTools.executeSUConsoleCommand("busybox losetup -d /dev/block/wishnuloop") != 0 ) {
				lastError = 5;
				currentStage = TMountSystemStages.MSS_ERROR;
				break;
			}
			currentStage = TMountSystemStages.MSS_CREATELOOP;
		case MSS_CREATELOOP:
			AcWishNuStarter.writeToLog("\n############Force Unmount System Image Finishes############\n");
			currentStage = TMountSystemStages.MSS_NONE;
		}
		return currentStage != TMountSystemStages.MSS_ERROR;
	}
	public static void checkReadiness() {
		if( checkCurrentStage() == TMountSystemStages.MSS_COMPLETED ) {
			currentStage = TMountSystemStages.MSS_COMPLETED; 
		}
	}
	private static TMountSystemStages checkCurrentStage() {
		AcWishNuStarter.writeToLog("\n############Mounting System Check############\n");
		AcWishNuStarter.writeToLog("\n**Checking /dev/block/wishnuloop mounted/not mounted**\n");
		if( SysTools.executeSUConsoleCommand("busybox losetup /dev/block/wishnuloop") != 0 ) {
			AcWishNuStarter.writeToLog("\n############Start from losetup############\n");
			return TMountSystemStages.MSS_CREATELOOP;
		}
		AcWishNuStarter.writeToLog("\n**Check " + SysPreparingBaseSystem.SYSTEM_FOLDER + " mounted/not mounted**\n");
		if( !SysTools.isMountPoint(SysPreparingBaseSystem.SYSTEM_FOLDER) ) {
			AcWishNuStarter.writeToLog("\n############Start from mount############\n");
			return TMountSystemStages.MSS_MOUNTIMAGE;
		}
		for(allowSdCardNum = 0; allowSdCardNum < SysPreparingBaseSystem.sdcardMountDirs.size(); allowSdCardNum++) {
			if( !(new File(SysPreparingBaseSystem.SYSTEM_FOLDER + "/media/" + SysPreparingBaseSystem.sdcardMountDirs.get(allowSdCardNum))).isDirectory() ) {
				AcWishNuStarter.writeToLog("\n############Start from creating SDCard############\n");
				return TMountSystemStages.MSS_ALLOWSDCARDS;
			}
		}
		AcWishNuStarter.writeToLog("\n############All Correct############\n");
		return TMountSystemStages.MSS_COMPLETED;
	}
}
