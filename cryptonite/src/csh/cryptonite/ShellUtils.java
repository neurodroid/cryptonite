// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.

// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

// Copyright (c) 2012, Christoph Schmidt-Hieber

package csh.cryptonite;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Scanner;

public class ShellUtils {

    private static String join(String[] sa, String delimiter) {
        Collection<String> s = Arrays.asList(sa);
        StringBuffer buffer = new StringBuffer();
        Iterator<String> iter = s.iterator();
        while (iter.hasNext()) {
            buffer.append(iter.next());
            if (iter.hasNext()) {
                buffer.append(delimiter);
            }
        }
        return buffer.toString();
    }
    
    private static String getChmod() throws IOException {
        String chmod = "/system/bin/chmod";
        if (!(new File(chmod)).exists()) {
            chmod = "/system/xbin/chmod";
            if (!(new File(chmod)).exists()) {
                throw new IOException("Couldn't find chmod on your system");
            }
        }
        return chmod;
    }
    
    public static void chmod(String filePath, String mod) throws IOException {
        String[] chmodlist = {getChmod(), mod, filePath};
        ShellUtils.runBinary(chmodlist);
    }
    
    public static boolean supportsFuse() {
        return (new File("/dev/fuse")).exists();
    }
    
    public static String runBinary(String[] binName) {
        return runBinary(binName, "/", null, false);
    }

    public static String runBinary(String[] binName, String binDir) {
        return runBinary(binName, binDir, null, false);
    }

    /** Run a binary using binDir as the wd. Return stdout
     *  and optinally stderr
     */
    public static String runBinary(String[] binName, String binDirPath, String toStdIn, boolean root) {
        try {
            File binDir = new File(binDirPath);
            if (!binDir.exists()) {
                binDir.mkdirs();
            }
            
            String NL = System.getProperty("line.separator");
            ProcessBuilder pb = new ProcessBuilder(binName);
            pb.directory(binDir);
            pb.redirectErrorStream(true);
            Process process;
            
            if (root) {
                String[] sucmd = {"su", "-c", join(binName, " ")};
                pb.command(sucmd);
                process = pb.start();
            } else {
                pb.command(binName);
                process = pb.start();
            }
            
            if (toStdIn != null) {
                BufferedWriter writer = new BufferedWriter(
                                                           new OutputStreamWriter(process.getOutputStream()) );
                writer.write(toStdIn + "\n");
                writer.flush();
            }

            process.waitFor();
                
            String output = "";
            Scanner outscanner = new Scanner(new BufferedInputStream(process.getInputStream()));
            try {
                while (outscanner.hasNextLine()) {
                    output += outscanner.nextLine();
                    output += NL;
                }
            }
            finally {
                outscanner.close();
            }

            return output;

        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
