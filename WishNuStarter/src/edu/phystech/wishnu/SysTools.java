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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.channels.FileChannel;

public class SysTools {
	public static int executeSUConsoleCommand(String command) 
	{
		int debugID = -1;
		try {
			AcWishNuStarter.writeToLog("\n#" + command + "\n");
			System.out.print(command);
			command = "su -c \"" + command + "\"";
			Process execute = Runtime.getRuntime().exec("su");
			new SysStreamGobbler(execute.getErrorStream(), System.out).start();
			new SysStreamGobbler(execute.getInputStream(), System.out).start();
			OutputStream p = execute.getOutputStream();
			PrintStream ps = new PrintStream(p);
			ps.println(command);
			ps.println("exit");
			ps.flush();
			try {
				debugID = execute.waitFor();
				AcWishNuStarter.writeToLog("\nReturn code: " + debugID + "\n");
			} catch( InterruptedException e ) {
				e.printStackTrace();
			}
		} catch ( IOException e ) {
			e.printStackTrace();
		}
		return debugID;
	}
	public static boolean fileExists(String path)
	{
		return (new File(path)).exists();
	}
	public static String errorStreamChecker(String command)
	{
		String result = "";
		try {
			Process p1 = Runtime.getRuntime().exec(command);
        	InputStream in = p1.getErrorStream();
        	p1.waitFor();
        	int b;
        	while( ( b = in.read() ) != -1 ) {
        		final char c = (char) b;
        		System.out.print(c);
        		result = result + c;
        	}
        } catch ( InterruptedException e ) {
        	e.printStackTrace();
        } catch ( IOException e ) {
			e.printStackTrace();
			result = "not found";
		}
		return result;
	}
	public static String suInputStreamChecker(String command)
	{
		String result = "";
		try {
			Process p1 = Runtime.getRuntime().exec("su");
			OutputStream p = p1.getOutputStream();
			PrintStream ps = new PrintStream(p);
			ps.println(command);
			ps.println("exit");
			ps.flush();
        	InputStream in = p1.getInputStream();
        	if( p1.waitFor() != 0 ) {
        		result = "not found";
        	}
        	int b;
        	while( ( b = in.read() ) != -1 ) {
        		final char c = (char) b;
        		System.out.print(c);
        		result = result + c;
        	}
        } catch ( InterruptedException e ) {
        	e.printStackTrace();
        } catch ( IOException e ) {
			e.printStackTrace();
			result = "not found";
		}
		return result;
	}
	public static String inputStreamChecker(String command)
	{
		String result = "";
		try {
			Process p1 = Runtime.getRuntime().exec(command);
        	InputStream in = p1.getInputStream();
        	p1.waitFor();
        	int b;
        	while( ( b = in.read() ) != -1 ) {
        		final char c = (char) b;
        		System.out.print(c);
        		result = result + c;
        	}
        } catch ( InterruptedException e ) {
        	e.printStackTrace();
        } catch ( IOException e ) {
			e.printStackTrace();
			result = "not found";
		}
		return result;
	}
	// Check if path directory is mountpoint
	public static boolean isMountPoint(String path) 
	{
		int debugID = -1;
		String result = "";
		try {
			AcWishNuStarter.writeToLog("\n#busybox mountpoint " + path + "\n");
			Process p1 = Runtime.getRuntime().exec("busybox mountpoint " + path);
        	InputStream in = p1.getInputStream();
        	debugID = p1.waitFor();
        	AcWishNuStarter.writeToLog("\nReturn code: " + debugID + "\n");
        	int b;
        	while( ( b = in.read() ) != -1 ) {
        		result = result + (char)b;
        	}
        } catch( InterruptedException e ) {
        	e.printStackTrace();
        } catch( IOException e ) {
			e.printStackTrace();
		}
        return debugID == 0;
	}
	// Check existence of program with the given name and pid
	public static boolean hasPidProgram(String cur_pid, String progname) 
	{
		String result = "";
		try {
			Process p1 = Runtime.getRuntime().exec("busybox pidof " + progname);
        	InputStream in = p1.getInputStream();
        	p1.waitFor();
        	int b;
        	while( ( b = in.read() ) != -1 ) {
        		result = result + (char)b;
        	}
        } catch( InterruptedException e )  {
        	e.printStackTrace();
        } catch( IOException e ) {
			e.printStackTrace();
		}
		return result.contains(cur_pid);
	}
	public static boolean checkRoot() 
	{
		AcWishNuStarter.writeToLog("-------Checking Root-------\n");
		AcWishNuStarter.writeToLog("command: su; result equals: empty string");
		String result = "";
		try {
			Process p1 = Runtime.getRuntime().exec("su");
			InputStream in = p1.getErrorStream();
        	OutputStream p = p1.getOutputStream();
			PrintStream ps = new PrintStream(p);
			ps.println("exit");
			ps.flush();
        	int b;
        	while( ( b = in.read() ) != -1 ) {
        		result = result + (char)b;
        	}
        	int waitfor = p1.waitFor();
        	AcWishNuStarter.writeToLog("Return code: " + waitfor);
        	return waitfor == 0;
        } catch ( InterruptedException e ) {
        	e.printStackTrace();
        } catch ( IOException e ) {
			e.printStackTrace();
			result = "not found";
		}
		//return  result.contentEquals("");
        return false;
	}
	public static boolean checkExt2() 
	{
		System.out.print("-------Checking Ext2-------\n");
		System.out.print("command: cat /proc/filesystems; result contains: ext2\n");
		return SysTools.inputStreamChecker("cat /proc/filesystems").contains("ext2");
	}
	public static boolean checkExt3() 
	{
		System.out.print("-------Checking Ext3-------\n");
		System.out.print("command: cat /proc/filesystems; result contains: ext3\n");
		return SysTools.inputStreamChecker("cat /proc/filesystems").contains("ext3");
	}
	public static boolean checkExt4() 
	{
		System.out.print("-------Checking Ext4-------\n");
		System.out.print("command: cat /proc/filesystems; result contains: ext4\n");
		return SysTools.inputStreamChecker("cat /proc/filesystems").contains("ext4");
	}
	public static boolean checkLoop() 
	{
		return true;
	}
	// checks modprobe, mount, mknod, sysctl, chroot modules
	public static boolean checkBusybox() 
	{
		System.out.print("-------Checking BusyBox-------\n");
		System.out.print("commands: modprobe,mount,mknod,sysctl,chroot; result not contains: not found\n");
		return ( !SysTools.errorStreamChecker("busybox modprobe").contains("not found") ) &&
				( !SysTools.errorStreamChecker("busybox mount").contains("not found") ) &&
				( !SysTools.errorStreamChecker("busybox mknod").contains("not found") ) &&
				( !SysTools.errorStreamChecker("busybox sysctl").contains("not found") ) &&
				( !SysTools.errorStreamChecker("busybox chroot").contains("not found") );
	}
	public static boolean checkImage() 
	{
		return SysTools.fileExists(SysPreparingBaseSystem.IMAGE_FOLDER + "/system.img");
	}
	public static String humanReadableByteCount(long bytes) {
	    int unit = 1024;
	    if (bytes < unit) return bytes + " B";
	    int exp = (int) (Math.log(bytes) / Math.log(unit));
	    String pre = ("KMGTPE").charAt(exp-1) + ("i");
	    return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
	}
	public static boolean CopyFile(File sourceFile, File targetFile) {
		try {
			AcWishNuStarter.writeToLog("##Copy file "+ sourceFile.getAbsolutePath() + " to " + targetFile.getAbsolutePath() + "##\n");
			if(!targetFile.exists()) {
				if( !targetFile.createNewFile() ) {
					AcWishNuStarter.writeToLog("!!Can't create target file!!\n");
					return false;
				}
		    }
	
		    FileChannel source = null;
		    FileChannel destination = null;
	
		    try {
		        source = new FileInputStream(sourceFile).getChannel();
		        destination = new FileOutputStream(targetFile).getChannel();
		        destination.transferFrom(source, 0, source.size());
		    }
		    finally {
		        if(source != null) {
		            source.close();
		        }
		        if(destination != null) {
		            destination.close();
		        }
		    }
		} catch(Exception e) {
			AcWishNuStarter.writeToLog("!!Can't copy target file!!\n");
			return false;
		}
		AcWishNuStarter.writeToLog("!!File was copied!!\n");
		return true;
	}
}
