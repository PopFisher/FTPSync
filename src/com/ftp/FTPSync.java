package com.ftp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.codec.digest.DigestUtils;


/**
 * 文件上传到FTP，再下载下来进行文件MD5校验
 *
 */
public class FTPSync {

	public static void main(String[] args) {
		uploadDirectory("I:\\testlocal\\", "I:\\testdownload\\", "/test/v2/");
		
//		checkFileMD5("I:\\testlocal", "I:\\testdownload\\testlocal");
	}
	
	/**
	 * 上传到服务器并下载下来进行MD5校验
	 * @param uploadDirectory	待上传的文件目录（完整路径）
	 * @param downloadSaveRootPath	保存下载目录的本地根路径，会在此路径下创建与uploadDirectory一样的目录结构
	 * @param serverSaveRootPath	FTP服务保存上传目录的根路径（相对路径，根路径下为"/"），需要/结尾，如："/test/"
	 */
	public static void uploadDirectory(final String uploadDirectory, final String downloadSaveRootPath, final String serverSaveRootPath) {
		uploadDirectory(uploadDirectory, serverSaveRootPath, new Runnable() {
			@Override
			public void run() {
				downloadDirectory(downloadSaveRootPath, serverSaveRootPath, new Runnable() {
					@Override
					public void run() {
						File file = new File(uploadDirectory);
						checkFileMD5(uploadDirectory, downloadSaveRootPath);
					}
				});
			}
		});
	}
	
	public static void uploadDirectory(final String localDirectory, final String serverSaveRootPath, final Runnable finishRunnable) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				FTPSyncCore ftpClient = new FTPSyncCore();
				if (ftpClient.connectServer()) {
					List<Object> list = ftpClient.uploadManyFile(localDirectory, serverSaveRootPath);
					int failCount = (int) list.get(0);
					int successCount = (int) list.get(1);
					String resultString = (String) list.get(2);
					System.out.println("失败个数：" + failCount + " 成功个数：" + successCount + " 详情：" + resultString);
					ftpClient.closeConnect();	// 关闭连接
					if (finishRunnable != null) {
						finishRunnable.run();
					}
				}
			}
		}).start();
	}
	
	public static void downloadDirectory(final String saveDirectory, final String serverSaveRootPath, final Runnable finishRunnable) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				FTPSyncCore ftpClient = new FTPSyncCore();
				if (ftpClient.connectServer()) {
					boolean success = ftpClient.loadDirectory(serverSaveRootPath, saveDirectory);
					if (success) {
						System.out.println("下载文件成功");
					}
					ftpClient.closeConnect();	// 关闭连接
					if (finishRunnable != null) {
						finishRunnable.run();
					}
				}
			}
		}).start();
	}
	
	public static void checkFileMD5(final String uploadDirectory, final String downloadDirectory) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				List<String> failLog = new ArrayList<String>();
				boolean success = checkFileMD5(new File(uploadDirectory), new File(downloadDirectory), failLog);
				if (success) {
					System.out.println("文件MD5校验成功，两个目录文件完全一致：" + uploadDirectory + " " + downloadDirectory);
				} else {
					for (String string : failLog) {
						System.out.println(string);
					}
				}
			}
		}).start();
	}
	
	public static boolean checkFileMD5(File uploadFileRootFile, File downloadFileRootFile, List<String> failLogList) {
		boolean result = true;
		if (uploadFileRootFile.isDirectory()) {
			File[] uploadFiles = uploadFileRootFile.listFiles();
			File[] dowloadFiles = downloadFileRootFile.listFiles();
			if (uploadFiles == null || dowloadFiles == null 
					|| uploadFiles.length == 0 || dowloadFiles.length == 0
					|| uploadFiles.length != dowloadFiles.length) {
				failLogList.add("文件目录结构不一致：" + uploadFileRootFile.getAbsolutePath() + " 与  " + downloadFileRootFile.getAbsolutePath());
				return false;
			}
			for (int i = 0; i < uploadFiles.length; i++) {
				File uploadFile = uploadFiles[i];
				File downloadFile = dowloadFiles[i];
				if (uploadFile.isDirectory()) {
					checkFileMD5(uploadFile,  downloadFile, failLogList);
				} else if (uploadFile.isFile()) {
					try {
						String uploadFileMD5 = DigestUtils.md5Hex(new FileInputStream(uploadFile));
						String downloadFileMD5 = DigestUtils.md5Hex(new FileInputStream(downloadFile));
						if (!uploadFileMD5.contains(downloadFileMD5)) {
							result = false;
							failLogList.add("文件MD5不一致：" 
									+ uploadFileRootFile.getAbsolutePath() + " MD5：" + uploadFileMD5
									+ " 与  " 
									+ downloadFileRootFile.getAbsolutePath() + " MD5：" + downloadFileMD5);
						}
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
		return result;
	}
}
