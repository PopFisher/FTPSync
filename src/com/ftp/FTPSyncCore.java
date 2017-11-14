package com.ftp;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import com.ftp.util.IOUtils;
import com.ftp.util.Log;

public class FTPSyncCore {

	private static int READ_TIMEOUT = 120000;
	private static String ENCODING = "GBK";

	private FTPClient ftpClient = null; // FTP 客户端代理
	private String mLocalRootPath = "";
	
	public FTPSyncCore() {
	}
	
	/**
	 * 连接到服务器
	 * 
	 * @return true 连接服务器成功，false 连接服务器失败
	 */
	public boolean connectServer() {
		boolean flag = false;
		String address = FTPConfig.getServerAddress();
		int port = FTPConfig.getServerPort();
		
		if (ftpClient == null) {
			try {
				ftpClient = new FTPClient();
				ftpClient.setControlEncoding(ENCODING);
				ftpClient.configure(getFtpConfig());
				ftpClient.connect(address, port);
				ftpClient.login(FTPConfig.getUserName(), FTPConfig.getPassword());
				ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
				ftpClient.enterLocalPassiveMode();
				ftpClient.setFileTransferMode(FTP.STREAM_TRANSFER_MODE);
				ftpClient.setDataTimeout(READ_TIMEOUT);
				int reply = ftpClient.getReplyCode();

				if (FTPReply.isPositiveCompletion(reply)) {
					flag = true;
					Log.info("FTP connect success!");
				}
				else {
					Log.warn("FTP refused to connect!");
					ftpClient.disconnect();
				}
			} catch (Exception e) {
				Log.error("Failed to login ftp " + address + ":" + port, e);
			}
		}
		return flag;
	}

	/**
	 * upload file
	 * 
	 * @param remoteFile
	 *            远程文件路径,支持多级目录嵌套
	 * @param localFile
	 *            本地文件名称，绝对路径
	 * 
	 */
	public boolean uploadFile(String remoteFile, File localFile) throws IOException {
		boolean flag = false;
		InputStream input = null;
		
		try {
			input = new FileInputStream(localFile);
			String remote = new String(remoteFile.getBytes("GBK"), "iso-8859-1");
			if (ftpClient.storeFile(remote, input)) {
				flag = true;
			}
		} finally {
			IOUtils.silenceClose(input);
		}
		
		Log.info("push file (" + localFile.getCanonicalPath() + ") => " + (flag ? "SUCCESS" : "FAILED"));
		return flag;
	}

	/**
	 * 上传单个文件，并重命名
	 * 
	 * @param localFile
	 *            --本地文件路径
	 * @param localRootFile
	 *            --本地文件父文件夹路径
	 * @param distFolder
	 *            --新的文件名,可以命名为空""
	 * @return true 上传成功，false 上传失败
	 * @throws IOException
	 */
	public boolean uploadFile(String local, String remote) throws IOException {
		boolean flag = true;
		String remoteFileName = remote;
		if (remote.contains("/")) {
			remoteFileName = remote.substring(remote.lastIndexOf("/") + 1);
			// 创建服务器远程目录结构，创建失败直接返回
			if (!createDirecroty(remote)) {
				return false;
			}
		}
		File f = new File(local);
		if (!uploadFile(remoteFileName, f)) {
			flag = false;
		}
		return flag;
	}

	public void setLocalRootPath(String localRootPath) {
		mLocalRootPath = localRootPath;
	}
	
	/**
	 * 上传文件夹内的所有文件
	 * 
	 * @param filename
	 *            本地文件夹绝对路径
	 * @param uploadpath
	 *            上传到FTP的路径,形式为/或/dir1/dir2/../
	 * @return true 上传成功，false 上传失败
	 * @throws IOException
	 */
	public List<Object> uploadManyFile(String filename, String uploadpath) {
		boolean flag = true;
		List<Object> list = new ArrayList<Object>();
		StringBuffer strBuf = new StringBuffer();
		int failCount = 0; // 上传失败的文件个数
		int m = 0; // 上传成功的文件个数
		try {
			ftpClient.changeWorkingDirectory("/");
			File file = new File(filename);
			File fileList[] = file.listFiles();
			for (File upfile : fileList) {
				if (upfile.isDirectory()) {
					uploadManyFile(upfile.getAbsoluteFile().toString(), uploadpath);
				} else {
					String local = upfile.getCanonicalPath().replaceAll("\\\\", "/");
					String remote = uploadpath.replaceAll("\\\\", "/");
					if (!remote.endsWith("/")) {
						remote += "/";
					}
					remote += local.substring(mLocalRootPath.length() + (mLocalRootPath.endsWith("/") ? 0 : 1));
					flag = uploadFile(local, remote);
					ftpClient.changeWorkingDirectory("/");
				}
				if (!flag) {
					failCount++;
					strBuf.append(upfile.getName() + ",");
					Log.info("File［" + upfile.getName() + "］upload failed");
				} else {
					m++;
				}
			}
			list.add(0, failCount);
			list.add(1, m);
			list.add(2, strBuf.toString());
		} catch (NullPointerException e) {
			e.printStackTrace();
			Log.error("local file upload failed, the file not found！" + e);
		} catch (Exception e) {
			e.printStackTrace();
			Log.error("local file upload failed ！" + e);
		}
		return list;
	}

	/**
	 * 下载文件
	 * 
	 * @param remoteFileName
	 *            --服务器上的文件名
	 * @param localFileName
	 *            --本地文件名
	 * @return true 下载成功，false 下载失败
	 */
	public boolean loadFile(String remoteFileName, String localFileName) {
		boolean flag = true;
		// 下载文件
		BufferedOutputStream buffOut = null;
		try {
			buffOut = new BufferedOutputStream(new FileOutputStream(localFileName));
			flag = ftpClient.retrieveFile(remoteFileName, buffOut);
		} catch (Exception e) {
			e.printStackTrace();
			Log.info("local file download failed ！" + e);
		} finally {
			IOUtils.silenceClose(buffOut);
		}
		return flag;
	}
	
	private String rootWorkingDirectory = "";
	private int count = 0;
	
	public boolean loadDirectory(String uploadDirectory, String remoteDirectory, String localSavePath) {
		rootWorkingDirectory = "";
		count = 0;
		localSavePath.replaceAll("\\\\", "/");
		if (!localSavePath.endsWith("/")) {
			localSavePath += "/";
		}
		if (!remoteDirectory.endsWith("/")) {
			remoteDirectory += "/";
		}
		boolean change = false;
		try {
			change = ftpClient.changeWorkingDirectory(remoteDirectory);
			Log.info("FTP Server changeWorkingDirectory " + remoteDirectory + " result: " + change);
			if ("".equals(rootWorkingDirectory)) {
				rootWorkingDirectory = ftpClient.printWorkingDirectory();
			}
		} catch (IOException e) {
			e.printStackTrace();
			Log.info("changeWorkingDirectory exception " + e);
		}
		
		// 需要从指定服务器目录下拉的文件名列表
		File file = new File(uploadDirectory);
		File[] files = file.listFiles();
		ArrayList<String> fileNameList = new ArrayList<String>();
		for (File file2 : files) {
			fileNameList.add(file2.getName());
		}
		
		return loadDirectoryInner(localSavePath, fileNameList);
	}
	
	public boolean loadDirectoryInner(String localSavePath, List<String> loadList) {
		// 获取文件列表
		FTPFile[] fs;
		boolean result = true;
		OutputStream is = null;
		try {
			count++;
			if (count >= 20) {	// 避免死循环创建过多目录
				Log.info("make directory more than 20");
				result = false;
				return result;
			}
	
			fs = ftpClient.listFiles();
			if (fs == null || fs.length == 0) {
				Log.info("ftpClient.listFiles() lenght is 0");
				return false;
			}
			String fileNameString;
			for (FTPFile ff : fs) {
				fileNameString = ff.getName();
				// 如果指定的下拉列表则需要进行过滤，不在列表里面的文件或者目录就不下载，直接跳过
				if (loadList != null && !loadList.contains(fileNameString)) {
					continue;
				}
				if (ff.isDirectory()) {
					File rootFile1 = new File(localSavePath + fileNameString);
					if (!rootFile1.exists()) {
						rootFile1.mkdirs();
					}
					
					changeWorkingDirectory(fileNameString);
					loadDirectoryInner(localSavePath + fileNameString + File.separator, null);
				} else if(ff.isFile()) {
					File saveFile = new File(localSavePath + fileNameString);
					is = new FileOutputStream(saveFile);
					ftpClient.retrieveFile(ff.getName(), is);
					IOUtils.silenceClose(is);
					if (saveFile.exists()) {
						Log.info("pull file (" + ftpClient.printWorkingDirectory() + "/" + fileNameString +") => OK " + saveFile.length());
					} else {
						Log.info("pull file (" + ftpClient.printWorkingDirectory() + "/" + fileNameString +") => ERROR");
						result = false;
					}
				}
			}
			if (!rootWorkingDirectory.equals(ftpClient.printWorkingDirectory())) {
				ftpClient.changeToParentDirectory();
				Log.info("change FTP Directory => " + ftpClient.printWorkingDirectory());
			}
		} catch (Exception e) {
			e.printStackTrace();
			result = false;
			Log.error("loadDirectoryInner exception: " + e);
		} finally {
			IOUtils.silenceClose(is);
		}
		
		return result;
	}

	/**
	 * 删除一个文件
	 */
	public boolean deleteFile(String filename) {
		boolean flag = true;
		try {
			flag = ftpClient.deleteFile(filename);
			if (flag) {
				Log.info("delete " + filename + " successfully!");
			} else {
				Log.info("delete " + filename + " failed!");
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		return flag;
	}

	/**
	 * 删除目录
	 */
	public void deleteDirectory(String pathname) {
		try {
			File file = new File(pathname);
			if (file.isDirectory()) {
				File file2[] = file.listFiles();
			} else {
				deleteFile(pathname);
			}
			ftpClient.removeDirectory(pathname);
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	/**
	 * 删除空目录
	 */
	public void deleteEmptyDirectory(String pathname) {
		try {
			ftpClient.removeDirectory(pathname);
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	/**
	 * 关闭连接
	 */
	public void closeConnect() {
		try {
			if (ftpClient != null) {
				ftpClient.logout();
				ftpClient.disconnect();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 设置传输文件的类型[文本文件或者二进制文件]
	 * 
	 * @param fileType
	 *            --BINARY_FILE_TYPE、ASCII_FILE_TYPE
	 * 
	 */
	public void setFileType(int fileType) {
		try {
			ftpClient.setFileType(fileType);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	/**
	 * 进入到服务器的某个目录下
	 * 
	 * @param directory
	 */
	public boolean changeWorkingDirectory(String directory) {
		boolean change = true;
		try {
			change = ftpClient.changeWorkingDirectory(directory);
			if (change) {
				Log.info("change FTP Directory => " + ftpClient.printWorkingDirectory());
			} else {
				Log.info("change FTP Directory(" + directory + ") => FAILED"); 
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		return change;
	}

	/**
	 * 返回到上一层目录
	 */
	public void changeToParentDirectory() {
		try {
			ftpClient.changeToParentDirectory();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	/**
	 * 重命名文件
	 * 
	 * @param oldFileName
	 *            --原文件名
	 * @param newFileName
	 *            --新文件名
	 */
	public void renameFile(String oldFileName, String newFileName) {
		try {
			ftpClient.rename(oldFileName, newFileName);
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	/**
	 * 设置FTP客服端的配置--一般可以不设置
	 * 
	 * @return ftpConfig
	 */
	private FTPClientConfig getFtpConfig() {
		FTPClientConfig ftpConfig = new FTPClientConfig(FTPConfig.getSystem());
		ftpConfig.setServerLanguageCode(FTP.DEFAULT_CONTROL_ENCODING);
		return ftpConfig;
	}

	/**
	 * 转码[ISO-8859-1 -> GBK] 不同的平台需要不同的转码
	 * 
	 * @param obj
	 * @return ""
	 */
	private String iso8859togbk(Object obj) {
		try {
			if (obj == null)
				return "";
			else
				return new String(obj.toString().getBytes("iso-8859-1"), "GBK");
		} catch (Exception e) {
			return "";
		}
	}

	/**
	 * 在服务器上创建一个文件夹
	 * 
	 * @param dir
	 *            文件夹名称，不能含有特殊字符，如 \ 、/ 、: 、* 、?、 "、 <、>...
	 */
	public boolean makeDirectory(String dir) {
		boolean flag = true;
		try {
			flag = ftpClient.makeDirectory(dir);
			if (flag) {
				Log.info("make directory " + dir + " successfully ！");
			} else {
				Log.info("make directory " + dir + " failed ！");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return flag;
	}

	// 检查路径是否存在，存在返回true，否则false
	public boolean existFile(String path) throws IOException {
		boolean flag = false;
		FTPFile[] ftpFileArr = ftpClient.listFiles(path);
		/*
		 * for (FTPFile ftpFile : ftpFileArr) { if (ftpFile.isDirectory() &&
		 * ftpFile.getName().equalsIgnoreCase(path)) { flag = true; break; } }
		 */
		if (ftpFileArr.length > 0) {
			flag = true;
		}
		return flag;
	}

	/**
	 * 递归创建远程服务器目录
	 * 
	 * @param remote
	 *            远程服务器文件绝对路径
	 * 
	 * @return 目录创建是否成功
	 * @throws IOException
	 */
	public boolean createDirecroty(String remote) throws IOException {
		boolean success = true;
		String directory = remote.substring(0, remote.lastIndexOf("/") + 1);
		// 如果远程目录不存在，则递归创建远程服务器目录
		if (!directory.equalsIgnoreCase("/")
				&& !changeWorkingDirectory(new String(directory))) {
			int start = 0;
			int end = 0;
			if (directory.startsWith("/")) {
				start = 1;
			} else {
				start = 0;
			}
			end = directory.indexOf("/", start);
			while (true) {
				String subDirectory = new String(remote.substring(start, end)
						.getBytes(ENCODING), "iso-8859-1");
				if (!changeWorkingDirectory(subDirectory)) {
					if (makeDirectory(subDirectory)) {
						changeWorkingDirectory(subDirectory);
					} else {
						Log.info("make directory [" + subDirectory + "] failed");
						success = false;
						return success;
					}
				}
				start = end + 1;
				end = directory.indexOf("/", start);
				// 检查所有目录是否创建完毕
				if (end <= start) {
					break;
				}
			}
		}
		return success;
	}
}