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
package com.sadko.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

/**
 * 
 * @author benderamp
 * 
 */
public class NetworkUtil {

	public static void download(final String address, final OutputStream out,
			final TaskController controller) throws MalformedURLException,
			IOException, TaskAbortedException {
		IOException ex = null;
		URLConnection conn = null;
		InputStream in = null;

		final URL url = new URL(address);
		try {
			conn = url.openConnection();
			in = conn.getInputStream();
			final byte[] buffer = new byte[1024];
			int numRead;
			long numWritten = 0;

			// try to calculate progress
			long remoteFileSize = conn.getContentLength();
			controller.setProgress(0);

			while (!controller.isAborted() && (numRead = in.read(buffer)) != -1) {
				out.write(buffer, 0, numRead);
				numWritten += numRead;

				controller
						.setProgress((int)numWritten);
			}
			controller.setProgress((int)remoteFileSize);
		} catch (IOException exception) {
			ex = exception;
		} finally {
			try {
				if (in != null) {
					in.close();
				}
				if (out != null) {
					out.close();
				}
			} catch (IOException ioe) {
			}
		}
		// rethrow exception
		if (ex != null) {
			throw ex;
		}
		if (controller.isAborted()) {
			throw new TaskAbortedException();
		}
	}

	public static void download(final String address,
			final String localFileName, final TaskController controller)
			throws MalformedURLException, IOException, TaskAbortedException {
		download(address, new BufferedOutputStream(new FileOutputStream(
				localFileName)), controller);
	}

	public static void download(final String address, final String localFileName)
			throws MalformedURLException, IOException {
		IOException ex = null;
		OutputStream out = null;
		URLConnection conn = null;
		InputStream in = null;

		final URL url = new URL(address);
		try {
			out = new BufferedOutputStream(new FileOutputStream(localFileName));
			conn = url.openConnection();
			in = conn.getInputStream();
			final byte[] buffer = new byte[1024];
			int numRead;
			long numWritten = 0;
			while ((numRead = in.read(buffer)) != -1) {
				out.write(buffer, 0, numRead);
				numWritten += numRead;
			}
		} catch (final IOException exception) {
			(new File(localFileName)).delete();
			ex = exception;
		} finally {
			try {
				if (in != null) {
					in.close();
				}
				if (out != null) {
					out.close();
				}
			} catch (IOException ioe) {
			}
		}
		if (ex != null) {
			throw ex;
		}
	}
}
