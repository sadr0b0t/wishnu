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

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import edu.phystech.wishnu.AcWishNuStarter.TIndicatorState;

public class SysStartingSystem {

	public static String getLastError() {
		switch(lastError) {
			case 0:
				return "No error";
			case 1:
				return "Error: OS start failed (bad program identificator)";
			case 2:
				return "Error: OS start failed (timeout expired)";
			default:
				return "Unnknown Error";
		}
	}

	public static TIndicatorState getStatus() {
		switch (currentStage) {
		case SSS_ERROR:
			return TIndicatorState.IS_ERROR;
		case SSS_NONE:
			return TIndicatorState.IS_NONE;
		case SSS_IN_PROGRESS:
			return TIndicatorState.IS_IN_PROGRESS;
		case SSS_COMPLETED:
			return TIndicatorState.IS_COMPLETED;
		}
		return TIndicatorState.IS_ERROR;
	}

	public static boolean start() {
		return start("");
	}

	public static boolean start(final String scriptName) {
		AcWishNuStarter
				.writeToLog("\n############Starting System############\n");
		currentStage = checkCurrentStage();
		switch (currentStage) {
		case SSS_NONE:
			// Allow to use internet
			currentStage = TStartingSystemStages.SSS_IN_PROGRESS;
			SysTools.executeSUConsoleCommand("busybox sysctl -w net.ipv4.ip_forward=1");
			// Start system
			AcWishNuStarter.writeToLog("\n#busybox chroot "
					+ SysPreparingBaseSystem.SYSTEM_FOLDER + " /bin/bash -l "
					+ scriptName + "\n");
			executeChroot(scriptName);
		case SSS_COMPLETED:
			return true;
		}
		// program actually never reach this statement
		return false;
	}

	public static boolean stop() {
		AcWishNuStarter
				.writeToLog("\n############Starting System Umount############\n");
		currentStage = checkCurrentStage();
		switch (currentStage) {
		case SSS_COMPLETED:
			currentStage = TStartingSystemStages.SSS_IN_PROGRESS;
			needCheck = false;
			sleeptime = 2000;
			currentStage = TStartingSystemStages.SSS_NONE;
		case SSS_NONE:
			AcWishNuStarter.writeToLog("\n***kill all processes***\n");
			// IN_PROGRESS status to disable stop and start buttons
			currentStage = TStartingSystemStages.SSS_IN_PROGRESS;
			killAllProcesses();
			currentStage = TStartingSystemStages.SSS_NONE;
			AcWishNuStarter
					.writeToLog("\n############Starting System Umount Finishes############\n");
		}
		return currentStage != TStartingSystemStages.SSS_ERROR;
	}

	public static void checkReadiness() {
		if (checkCurrentStage() == TStartingSystemStages.SSS_COMPLETED) {
			currentStage = TStartingSystemStages.SSS_COMPLETED;
		}
	}

	private static int sleeptime = 2000;
	private static boolean needCheck = false;
	private static TStartingSystemStages currentStage = TStartingSystemStages.SSS_NONE;
	private static Process chroot;
	private static int lastError = 0;

	private enum TStartingSystemStages {
		SSS_ERROR, SSS_NONE, SSS_IN_PROGRESS, SSS_COMPLETED
	}

	private static int executeChroot(final String scriptName) {
		int debugID = -1;
		try {
			String command = "busybox chroot "
					+ SysPreparingBaseSystem.SYSTEM_FOLDER + " /bin/bash -l "
					+ scriptName;
			System.out.print(command);
			command = "su -c \"" + command + "\"";
			chroot = Runtime.getRuntime().exec("su");
			new SysStreamGobbler(chroot.getErrorStream(), System.out).start();
			new SysStreamGobbler(chroot.getInputStream(), System.out).start();
			OutputStream p = chroot.getOutputStream();
			PrintStream ps = new PrintStream(p);
			// export all required variables
			ps.println("export kit=" + SysPreparingBaseSystem.IMAGE_FOLDER);
			ps.println("export bin=/data/local/bin");
			ps.println("export mnt=" + SysPreparingBaseSystem.SYSTEM_FOLDER);
			ps.println("export PATH=$bin:/usr/bin:/usr/sbin:/bin:$PATH");
			ps.println("export TERM=linux");
			ps.println("export HOME=/root");
			ps.println(command);
			ps.flush();
			needCheck = true;
			if (!PIDCheck.isAlive()) {
				PIDCheck.start();
			}
			try {
				debugID = chroot.waitFor();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return debugID;
	}

	private static TStartingSystemStages checkCurrentStage() {
		AcWishNuStarter
				.writeToLog("\n############Starting System Check############\n");
		if (!PIDCheck.isAlive()) {
			PIDCheck.start();
		}
		int result = checkVNCServer();
		switch (result) {
		case 1:
			AcWishNuStarter
					.writeToLog("\n############All Correct############\n");
			return TStartingSystemStages.SSS_COMPLETED;
		case 0:
			AcWishNuStarter
					.writeToLog("\n############start from ip_forward############\n");
			return TStartingSystemStages.SSS_NONE;
		case -1:
			AcWishNuStarter.writeToLog("\n**Kill All Processes**\n");
			killAllProcesses();
			AcWishNuStarter
					.writeToLog("\n############start from ip_forward############\n");
			return TStartingSystemStages.SSS_NONE;
		default:
			return TStartingSystemStages.SSS_ERROR;
		}
	}

	// set the thread for checking state file once in 5 sec
	private final static Thread PIDCheck = new Thread(new Runnable() {
		@Override
		public void run() {
			int timer = 0;
			while (true) {
				int result;
				try {
					Thread.sleep(sleeptime);
					if (needCheck) {
						result = checkVNCServer();
						switch (result) {
						case 1:
							if (currentStage != TStartingSystemStages.SSS_COMPLETED) {
								AcWishNuStarter
										.writeToLog("\n############Start Complete############\n");
							}
							timer = 0;
							currentStage = TStartingSystemStages.SSS_COMPLETED;
							sleeptime = 30000;
							break;
						case 0:
							timer++;
							if (currentStage == TStartingSystemStages.SSS_COMPLETED) {
								currentStage = TStartingSystemStages.SSS_NONE;
								sleeptime = 2000;
								needCheck = false;
							}
							break;
						case -1:
							timer++;
							// killAllProcesses();
							lastError = 1;
							currentStage = TStartingSystemStages.SSS_ERROR;
							sleeptime = 2000;
							needCheck = false;
							break;
						}
						if ((timer > 5)
								&& currentStage != TStartingSystemStages.SSS_COMPLETED) {
							lastError = 2;
							currentStage = TStartingSystemStages.SSS_ERROR;
							sleeptime = 2000;
							needCheck = false;
							if (chroot != null) {
								chroot.destroy();
							}
							timer = 0;
							// kill any started process in our system
							// killAllProcesses();
							AcWishNuStarter
									.writeToLog("\n############Start Failed############\n");
						}
					}
				} catch (InterruptedException e) {
				}
			}
		}
	});

	// read pidFile and check existence of programs
	// programsToCheck - number of pids to check; -1 to check all pids
	// return:
	// -1: invalid pid
	// 0: not enough programs
	// 1: all clear
	private static int checkVNCServer() {
		int systemReady = 0;
		// check pid file, if not found - return 0
		String content = SysTools.suInputStreamChecker("cat "
				+ AcWishNuStarter.pidFilePath);
		if (content == "not found") {
			return systemReady;
		}
		// check existence of this pid and fact, that it is VNC's pid
		if (SysTools.suInputStreamChecker("ps " + content).contains("vnc")) {
			systemReady = 1;
		} else {
			systemReady = -1;
		}
		return systemReady;
	}

	private static void killAllProcesses() {
		SysTools.executeSUConsoleCommand("busybox fuser -k "
				+ SysPreparingBaseSystem.SYSTEM_FOLDER);
		// delete pid file
		SysTools.executeSUConsoleCommand("rm " + AcWishNuStarter.pidFilePath);
	}

}
