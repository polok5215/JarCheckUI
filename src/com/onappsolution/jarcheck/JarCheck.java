/*
 * [JarCheck.java]
 *
 * Summary: Ensures javac -target versions of the class files in a jar are as expected.
 *
 * Copyright: (c) 2006-2016 Roedy Green, Canadian Mind Products, http://mindprod.com
 *
 * Licence: This software may be copied and used freely for any purpose but military.
 *          http://mindprod.com/contact/nonmil.html
 *
 * Requires: JDK 1.8+
 *
 * Created with: JetBrains IntelliJ IDEA IDE http://www.jetbrains.com/idea/
 *
 * Version History:
 *  1.0 2006-01-16 initial version
 *  1.1 2006-01-16
 *  1.2 2006-03-05 reformat with IntelliJ, add Javadoc
 *  1.3 2008-04-21 display version number of each class file checked.
 *  1.4 2011-07-30 add support for Java 1.7
 *  1.5 2014-03-23 add support for Java 11.8
 */
package com.onappsolution.jarcheck;

import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


/**
 * Ensures javac -target versions of the class files in a jar are as expected.
 *
 * @author Roedy Green, Canadian Mind Products
 * @version 1.5 2014-03-23 add support for Java 1.8
 * @since 2006-01-16
 */
public final class JarCheck {
	/**
	 * how many bytes at beginning of class file we read<br>
	 * 4=ca-fe-ba-be + 2=minor + 2=major
	 */
	private static final int chunkLength = 8;

	private static final int FIRST_COPYRIGHT_YEAR = 2006;

	/**
	 * undisplayed copyright notice
	 */
	private static final String EMBEDDED_COPYRIGHT = "Copyright: (c) 2006-2016 Roedy Green, Canadian Mind Products, http://mindprod.com";

	private static final String RELEASE_DATE = "2014-03-23";

	/**
	 * embedded version string.
	 */
	private static final String VERSION_STRING = "1.5";

	/**
	 * translate class file major version number to human JVM version
	 */
	private static final HashMap<Integer, String> convertMachineToHuman = new HashMap<>(23);

	/**
	 * translate from human JDK version to class file major version number
	 */
	public static final HashMap<String, Integer> convertHumanToMachine = new HashMap<>(23);

	/**
	 * expected first 4 bytes of a class file
	 */
	private static final byte[] expectedMagicNumber = { (byte) 0xca, (byte) 0xfe, (byte) 0xba, (byte) 0xbe };

	static {
		convertHumanToMachine.put("1.0", 44);
		convertHumanToMachine.put("1.1", 45);
		convertHumanToMachine.put("1.2", 46);
		convertHumanToMachine.put("1.3", 47);
		convertHumanToMachine.put("1.4", 48);
		convertHumanToMachine.put("1.5", 49);
		convertHumanToMachine.put("1.6", 50);
		convertHumanToMachine.put("1.7", 51);
		convertHumanToMachine.put("1.8", 52);
		convertHumanToMachine.put("1.9", 53);
		convertHumanToMachine.put("1.10", 54);
		convertHumanToMachine.put("1.11", 55);
	}

	static {
		convertMachineToHuman.put(44, "1.0");
		convertMachineToHuman.put(45, "1.1");
		convertMachineToHuman.put(46, "1.2");
		convertMachineToHuman.put(47, "1.3");
		convertMachineToHuman.put(48, "1.4");
		convertMachineToHuman.put(49, "1.5");
		convertMachineToHuman.put(50, "1.6");
		convertMachineToHuman.put(51, "1.7");
		convertMachineToHuman.put(52, "1.8");
		convertMachineToHuman.put(53, "1.9");
		convertMachineToHuman.put(54, "1.10");
		convertMachineToHuman.put(55, "1.11");
	}

	/**
	 * check one jar to make sure all class files have compatible versions.
	 *
	 * @param jarFilename
	 *            name of jar file whose classes are to be tested.
	 * @param low
	 *            low bound for major version e.g. 44
	 * @param high
	 *            high bound for major version. e.g. 50
	 *
	 * @return true if all is ok. False if not, with long on System.err of
	 *         problems.
	 */
	public static String checkJar(String jarFilename, int low, int high) {
		String finalStr="";
		FileInputStream fis;
		ZipInputStream zip = null;
		
		try {
			try {
				fis = new FileInputStream(jarFilename);
				zip = new ZipInputStream(fis);
				// loop for each jar entry
				entryLoop: while (true) {
					ZipEntry entry = zip.getNextEntry();
					if (entry == null) {
						break;
					}
					// relative name with slashes to separate dirnames.
					String elementName = entry.getName();
					if (!elementName.endsWith(".class")) {
						// ignore anything but a .final class file
						continue;
					}
					byte[] chunk = new byte[chunkLength];
					int bytesRead = zip.read(chunk, 0, chunkLength);
					zip.closeEntry();
					if (bytesRead != chunkLength) {
						finalStr+= "<span style='color: #ff0000;'><b>## Corrupt class file: " + elementName+"</span></b><br/>";
						continue;
					}
					// make sure magic number signature is as expected.
					for (int i = 0; i < expectedMagicNumber.length; i++) {
						if (chunk[i] != expectedMagicNumber[i]) {
							finalStr+= "<span style='color: #ff0000;'><b>## Bad magic number in " + elementName+"</span></b><br/>";
							continue entryLoop;
						}
					}
					/*
					 * pick out big-endian ushort major version in last two
					 * bytes of chunk
					 */
					int major = ((chunk[chunkLength - 2] & 0xff) << 8) + (chunk[chunkLength - 1] & 0xff);
					/*
					 * F I N A L L Y. All this has been leading up to this TEST
					 */
					if (low <= major && major <= high) {
						finalStr+= "--OK "+convertMachineToHuman.get(major) + " (" + major + ") " + elementName+"<br/>";
					} else {
						finalStr+= "--<span style='color: #ff0000;'>BAD "+convertMachineToHuman.get(major) + " (" + major + ") " + elementName+"</span><br/>";
					}
				}
				// end while
			} catch (EOFException e) {
				// normal exit
			}
			zip.close();
		} catch (IOException e) {
			finalStr+= "<span style='color: #ff0000;'><b>## Problem reading jar file."+"</span></b><br/>";
		}
		
		return finalStr;
	}
	
	public static byte[] createChecksum(String filename) throws Exception {
       InputStream fis =  new FileInputStream(filename);

       byte[] buffer = new byte[1024];
       MessageDigest complete = MessageDigest.getInstance("MD5");
       int numRead;

       do {
           numRead = fis.read(buffer);
           if (numRead > 0) {
               complete.update(buffer, 0, numRead);
           }
       } while (numRead != -1);

       fis.close();
       return complete.digest();
   }

   // see this How-to for a faster way to convert
   // a byte array to a HEX string
   public static String getMD5Checksum(String filename) throws Exception {
       byte[] b = createChecksum(filename);
       String result = "";

       for (int i=0; i < b.length; i++) {
           result += Integer.toString( ( b[i] & 0xff ) + 0x100, 16).substring( 1 );
       }
       return result;
   }

}
