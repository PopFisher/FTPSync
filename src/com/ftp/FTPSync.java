package com.ftp;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.codec.digest.DigestUtils;

import com.ftp.util.FileUtils;
import com.ftp.util.IOUtils;
import com.ftp.util.Log;

/**
 * 文件上传到FTP，再下载下来进行文件MD5校验
 *
 */
public class FTPSync {
	
	/**
	 * 上传到服务器并下载下来进行MD5校验
	 * 调用方式 uploadDirectory("I:\\testlocal\\", "I:\\testdownload\\");
	 * @param uploadDirectory	待上传的文件目录（完整路径）
	 * @param downloadSaveRootPath	保存下载目录的本地根路径，会在此路径下创建与uploadDirectory一样的目录结构
	 */
	public static void uploadDirectory(String uploadDirectory, String downloadSaveRootPath) throws Exception {
		boolean unloadSuccess = uploadDirectoryToServer(uploadDirectory, FTPConfig.getRootPath());
		if (!unloadSuccess) {
			throw new RuntimeException("Failed to upload FTP => " + uploadDirectory);
		}
		
		boolean downloadResutl = downloadDirectory(uploadDirectory, downloadSaveRootPath, FTPConfig.getRootPath());
		if (!downloadResutl) {
			throw new RuntimeException("Failed to download FTP => " + uploadDirectory);
		}
		
		boolean checkSuccess = checkFileMD5(uploadDirectory, downloadSaveRootPath);
		if (!checkSuccess) {
			throw new RuntimeException("Failed to validate FTP Files => " + uploadDirectory);
		}
	}
	
	private static boolean uploadDirectoryToServer(String localDirectory, String serverSaveRootPath) {
		Log.info("start upload dir: " + localDirectory + " to server: " + serverSaveRootPath);
		File localDirectoryFile = new File(localDirectory);
		if (!localDirectoryFile.exists()) {
			Log.error("target upload directory file not exists " + localDirectory);
			return false;
		}
		try {
			localDirectory = localDirectoryFile.getCanonicalPath();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if ("".equals(localDirectory)) {
			Log.error("target upload directory is empty");
			return false;
		}
		localDirectory.replaceAll("\\\\", "/");
		if (!localDirectory.endsWith("/")) {
			localDirectory += "/";
		}
		FTPSyncCore ftpClient = new FTPSyncCore();
		ftpClient.setLocalRootPath(localDirectory);
		if (ftpClient.connectServer()) {
			List<Object> list = ftpClient.uploadManyFile(localDirectory, serverSaveRootPath);
			if (list == null || list.size() < 3) {
				Log.error("uploadManyFile failed");
				return false;
			}
			int failCount = (int) list.get(0);
			int successCount = (int) list.get(1);
			String resultString = (String) list.get(2);
			Log.info("upload dir finished, failed Count：" + failCount + " success count：" + successCount + " detail：" + resultString);
			ftpClient.closeConnect();	// 关闭连接
			if (failCount <= 0) {
				return true;
			}
		}
		return false;
	}
	
	private static boolean downloadDirectory(String uploadDirectory, String saveDirectory, String ftpRootPath) {
		Log.info("start down dir: " + ftpRootPath + " to local: " + saveDirectory);
		try {
			File downloadDirectoryFile = new File(saveDirectory);
			FileUtils.deleteDirs(downloadDirectoryFile);
			downloadDirectoryFile.mkdirs();
			saveDirectory = downloadDirectoryFile.getCanonicalPath();
		} catch (Exception e) {
			e.printStackTrace();
			Log.error("download direcroty mkdirs failed");
			return false;
		}
		if ("".equals(saveDirectory)) {
			Log.error("download direcroty is empty");
			return false;
		}
		FTPSyncCore ftpClient = new FTPSyncCore();
		if (ftpClient.connectServer()) {
			boolean success = ftpClient.loadDirectory(uploadDirectory, ftpRootPath, saveDirectory);
			if (success) {
				Log.info("downloadDirectory(" + ftpRootPath + ") => SUCCESS");
			} else {
				Log.error("downloadDirectory(" + ftpRootPath + ") => FAILED");
			}
			ftpClient.closeConnect();	// 关闭连接
			return success;
		}
		return false;
	}
	
	private static boolean checkFileMD5(String uploadDirectory, String downloadDirectory) {
		Log.info("checkFileMD5(" + uploadDirectory + " <=> " + downloadDirectory + ") => START");
		File localDirectoryFile = new File(uploadDirectory);
		if (!localDirectoryFile.exists()) {
			Log.error("upload directory not exists " + uploadDirectory);
			return false;
		}
		File downloadDirectoryFile = new File(downloadDirectory);
		if (!downloadDirectoryFile.exists()) {
			Log.error("download directory not exists " + downloadDirectoryFile);
			return false;
		}
		uploadDirectory.replaceAll("\\\\", "/");
		downloadDirectory.replaceAll("\\\\", "/");
		List<String> failLog = new ArrayList<String>();
		boolean success = checkFileMD5(new File(uploadDirectory), new File(downloadDirectory), failLog);
		if (success) {
			Log.info("checkFileMD5(" + uploadDirectory + " <=> " + downloadDirectory + ") => SUCCESS");
		} else {
			Log.error("checkFileMD5(" + uploadDirectory + " <=> " + downloadDirectory + ") => FAILED");
			for (String string : failLog) {
				Log.error(string);
			}
		}
		return success;
	}
	
	private static boolean checkFileMD5(File uploadFileRootFile, File downloadFileRootFile, List<String> failLogList) {
		boolean result = true;
		if (!uploadFileRootFile.isDirectory()) {
			return false;
		}
		File[] uploadFiles = uploadFileRootFile.listFiles();
		File[] dowloadFiles = downloadFileRootFile.listFiles();
		if (uploadFiles == null || dowloadFiles == null 
				|| uploadFiles.length == 0 || dowloadFiles.length == 0
				|| uploadFiles.length != dowloadFiles.length) {
			failLogList.add("file directory structure is inconsistent：" + uploadFileRootFile.getAbsolutePath() + " and  " + downloadFileRootFile.getAbsolutePath());
			return false;
		}
		for (int i = 0; i < uploadFiles.length; i++) {
			File uploadFile = uploadFiles[i];
			File downloadFile = dowloadFiles[i];
			if (uploadFile.isDirectory()) {
				checkFileMD5(uploadFile,  downloadFile, failLogList);
			} else if (uploadFile.isFile()) {
				FileInputStream uploadInput = null;
				FileInputStream downloadInput = null;
				try {
					uploadInput = new FileInputStream(uploadFile);
					downloadInput = new FileInputStream(downloadFile);
					
					String uploadFileMD5 = DigestUtils.md5Hex(uploadInput);
					String downloadFileMD5 = DigestUtils.md5Hex(downloadInput);
					if (!uploadFileMD5.contains(downloadFileMD5)) {
						result = false;
						failLogList.add("file MD5 is inconsistent：" 
								+ uploadFileRootFile.getAbsolutePath() + " MD5：" + uploadFileMD5
								+ " and  " 
								+ downloadFileRootFile.getAbsolutePath() + " MD5：" + downloadFileMD5);
					}
				} catch (Exception e) {
					Log.error("checkFileMD5 Exception", e);
				} finally {
					IOUtils.silenceClose(uploadInput);
					IOUtils.silenceClose(downloadInput);
				}
			}
		}
		return result;
	}
}
