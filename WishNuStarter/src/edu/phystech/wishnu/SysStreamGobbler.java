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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

public class SysStreamGobbler extends Thread 
{

	private final InputStream in;
	private final OutputStream out;

	public SysStreamGobbler(final InputStream in, final OutputStream out) 
	{
		this.in = in;
		this.out = out;
	}

	@Override
	public void run() 
	{
		try {
			String b;
			InputStreamReader inp = new InputStreamReader(in);
			BufferedReader buffReader = new BufferedReader(inp, 8*1024);
			while( ( b = buffReader.readLine() ) != null ) {
				final String s = b;
				if( out != null ) {
					// out.write(b);
					//System.out.print(s);
					AcWishNuStarter.writeToLog(s);
				}
			}
		} catch ( IOException ex ) {
		}
	}
}
