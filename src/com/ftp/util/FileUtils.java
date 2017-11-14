package com.ftp.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.LineNumberReader;
import java.math.BigInteger;
import java.security.MessageDigest;

public class FileUtils {
	public static void deleteDirs(File file) {
		if (file.isDirectory()) {
			for (File f : file.listFiles()) {
				deleteDirs(f);
			}
			file.delete();
		} else {
			file.delete();
		}
	}

	public static int getFileLineNumber(String fileName) {
		File file = new File(fileName);
		LineNumberReader rf = null;
		try {
			rf = new LineNumberReader(new FileReader(file));
			if (rf != null) {
				rf.skip(file.length());
				return rf.getLineNumber();
			}
		} catch (Exception e) {

		} finally {
			IOUtils.silenceClose(rf);
		}
		return 0;
	}

	public static void checkFileExists(String fileName) throws RuntimeException {
		File file = new File(fileName);
		if (!file.exists() || !file.isFile()) {
			throw new RuntimeException("Missing File => "
					+ file.getAbsolutePath());
		}
	}

	public static void isFolderEmpty(String folderPath) throws Exception {
		File outFolder = new File(folderPath);
		if (outFolder.exists()) {
			if (outFolder.isFile()) {
				outFolder.delete();
			} else {
				File[] files = outFolder.listFiles();
				if (files != null && files.length > 0) {
					throw new RuntimeException("Output Folder Exists => "
							+ outFolder.getAbsolutePath());
				}
			}
		}
		outFolder.mkdirs();
	}

	public static String getFileMD5(File file) {
		FileInputStream input = null;
		try {
			input = new FileInputStream(file);
			byte[] buffer = new byte[0x4000];
			MessageDigest md = MessageDigest.getInstance("MD5");
			int length = -1;
			while ((length = input.read(buffer)) > 0)
				md.update(buffer, 0, length);
			byte[] arrayOfByte = md.digest();
			return new BigInteger(1, arrayOfByte).toString(16);
		} catch (Exception ex) {
			Log.error("getFileMD5(" + file.getAbsolutePath() + ")", ex);
		} finally {
			IOUtils.silenceClose(input);
		}
		return null;
	}

	public static boolean writeStringToFile(String fileName, String data) {
		File file = new File(fileName);
		if (file.exists() && file.isFile())
			file.delete();

		FileWriter fw = null;
		try {
			fw = new FileWriter(fileName, false);
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(data);
			bw.close();
			fw.close();
			return true;
		} catch (Exception ex) {
			Log.error("writeStringToFile(" + fileName + ")", ex);
		} finally {
			IOUtils.silenceClose(fw);
		}
		return false;
	}

	public static void copyFile(String fromPath, String toPath) {
		FileInputStream is = null;
		FileOutputStream fos = null;
		int bufferCount;
		byte[] buffer = new byte[1024];
		try {
			is = new FileInputStream(new File(fromPath));
			fos = new FileOutputStream(new File(toPath));
			while ((bufferCount = is.read(buffer)) != -1) {
				fos.write(buffer, 0, bufferCount);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				fos.flush();
				fos.close();
				is.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
