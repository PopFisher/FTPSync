package com.ftp;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

public class FTPSyncCore {

	private static final Log logger = new Log();
	private static int READ_TIMEOUT = 120000;
	private static String ENCODING = "GBK";

	private String userName; // FTP 登录用户名
	private String password; // FTP 登录密码
	private String ip; // FTP 服务器地址IP地址
	private int port; // FTP 端口
	private FTPClient ftpClient = null; // FTP 客户端代理
	
	public FTPSyncCore() {
		setArg();
	}
	
	/**
	 * 连接到服务器
	 * 
	 * @return true 连接服务器成功，false 连接服务器失败
	 */
	public boolean connectServer() {
		boolean flag = true;
		if (ftpClient == null) {
			int reply;
			try {
				ftpClient = new FTPClient();
				ftpClient.setControlEncoding(ENCODING);
//				ftpClient.configure(getFtpConfig());
				ftpClient.connect(ip, port);
				ftpClient.login(userName, password);
				ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
				// 这句话不设置会导致下载文件的时候ftpClient.listFiles()返回数据长度为0，不过在windows上貌似不会
				// 调用FTPClient.enterLocalPassiveMode();这个方法的意思就是每次数据连接之前，
				// ftp client告诉ftp server开通一个端口来传输数据。为什么要这样做呢，因为ftp server可能每次开启不同的端口来传输数据，
				// 但是在linux上，由于安全限制，可能某些端口没有开启，所以就出现阻塞。
				ftpClient.enterLocalPassiveMode();
				ftpClient.setFileTransferMode(FTP.STREAM_TRANSFER_MODE);
				ftpClient.setDataTimeout(READ_TIMEOUT);
				reply = ftpClient.getReplyCode();
				System.out.println("ftpClient buf: " + ftpClient.getBufferSize());
				if (!FTPReply.isPositiveCompletion(reply)) {
					ftpClient.disconnect();
					logger.debug("FTP 服务拒绝连接！");
					flag = false;
				}
			} catch (SocketException e) {
				flag = false;
				e.printStackTrace();
				logger.debug("登录ftp服务器 " + ip + " 失败,连接超时！");
			} catch (IOException e) {
				flag = false;
				e.printStackTrace();
				logger.debug("登录ftp服务器 " + ip + " 失败，FTP服务器无法打开！");
			}
		}
		return flag;
	}

	/**
	 * 上传文件
	 * 
	 * @param remoteFile
	 *            远程文件路径,支持多级目录嵌套
	 * @param localFile
	 *            本地文件名称，绝对路径
	 * 
	 */
	public boolean uploadFile(String remoteFile, File localFile)
			throws IOException {
		boolean flag = false;
		InputStream in = new FileInputStream(localFile);
		String remote = new String(remoteFile.getBytes("GBK"), "iso-8859-1");
		if (ftpClient.storeFile(remote, in)) {
			flag = true;
			logger.debug(localFile.getAbsolutePath() + "上传文件成功！");
		} else {
			logger.debug(localFile.getAbsolutePath() + "上传文件失败！");
		}
		in.close();
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
		FTPFile[] files = ftpClient.listFiles(new String(remoteFileName));
		File f = new File(local);
		if (!uploadFile(remoteFileName, f)) {
			flag = false;
		}
		return flag;
	}

	private String mCurPreRootPath = "";
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
		if (mCurPreRootPath.isEmpty()) {
			mCurPreRootPath = filename;
			mCurPreRootPath = mCurPreRootPath.replaceAll("\\\\", "/");
		}
		boolean flag = true;
		List<Object> list = new ArrayList<Object>();
		StringBuffer strBuf = new StringBuffer();
		int n = 0; // 上传失败的文件个数
		int m = 0; // 上传成功的文件个数
		try {
			ftpClient.changeWorkingDirectory("/");
			File file = new File(filename);
			File fileList[] = file.listFiles();
			for (File upfile : fileList) {
				if (upfile.isDirectory()) {
					uploadManyFile(upfile.getAbsoluteFile().toString(),
							uploadpath);
				} else {
					String local = upfile.getCanonicalPath().replaceAll("\\\\",
							"/");
					String remote = uploadpath.replaceAll("\\\\", "/")
							+ local.substring(mCurPreRootPath.length());
					flag = uploadFile(local, remote);
					ftpClient.changeWorkingDirectory("/");
				}
				if (!flag) {
					n++;
					strBuf.append(upfile.getName() + ",");
					logger.debug("文件［" + upfile.getName() + "］上传失败");
				} else {
					m++;
				}
			}
			list.add(0, n);
			list.add(1, m);
			list.add(2, strBuf.toString());
		} catch (NullPointerException e) {
			e.printStackTrace();
			logger.debug("本地文件上传失败！找不到上传文件！", e);
		} catch (Exception e) {
			e.printStackTrace();
			logger.debug("本地文件上传失败！", e);
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
			buffOut = new BufferedOutputStream(new FileOutputStream(
					localFileName));
			flag = ftpClient.retrieveFile(remoteFileName, buffOut);
		} catch (Exception e) {
			e.printStackTrace();
			logger.debug("本地文件下载失败！", e);
		} finally {
			try {
				if (buffOut != null) {
					buffOut.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return flag;
	}
	
	private String rootWorkingDirectory = "";
	private int count = 0;
	
	public boolean loadDirectory(String remoteDirectory, String localSavePath) {
		rootWorkingDirectory = "";
		count = 0;
		if (!localSavePath.endsWith(File.separator)) {
			logger.debug("loadDirectory: " + localSavePath + " not endWith \\");
			localSavePath += File.separator;
		}
		File rootFile = new File(localSavePath);
		if (!rootFile.exists()) {
			if (rootFile.mkdirs()) {
				logger.debug("loadDirectory: create directory: " + rootFile.getPath());
			}
		}
		
		if (!remoteDirectory.endsWith("/")) {
			logger.debug("loadDirectory: " + remoteDirectory + " not endWith \\");
			remoteDirectory += "/";
		}
		boolean change = false;
		try {
			change = ftpClient.changeWorkingDirectory(remoteDirectory);
			logger.debug("loadDirectory: changeWorkingDirectory " + remoteDirectory + " result: " + change);
			if ("".equals(rootWorkingDirectory)) {
				rootWorkingDirectory = ftpClient.printWorkingDirectory();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return loadDirectoryInner(localSavePath);
	}
	
	public boolean loadDirectoryInner(String localSavePath) {
		// 获取文件列表
		FTPFile[] fs;
		boolean result = true;
		try {
			count++;
			if (count >= 10) {
				logger.debug("loadDirectory: error");
				result = false;
				return result;
			}
	
			fs = ftpClient.listFiles();
			if (fs == null || fs.length == 0) {
				logger.debug("ftpClient.listFiles() lenght is 0");
				return false;
			}
			String fileNameString;
			for (FTPFile ff : fs) {
				fileNameString = ff.getName();
				if (ff.isDirectory()) {
					logger.debug("loadDirectory: " + fileNameString + " is directory");
					logger.debug("loadDirectory: workingDirectory " + ftpClient.printWorkingDirectory());
					File rootFile1 = new File(localSavePath + fileNameString);
					if (!rootFile1.exists()) {
						if (rootFile1.mkdirs()) {
							logger.debug("loadDirectory: create directory: " + rootFile1.getPath());
						}
					}
					boolean change = ftpClient.changeWorkingDirectory(fileNameString);
					logger.debug("loadDirectory: changeWorkingDirectory result: " + change);
					loadDirectoryInner(localSavePath + fileNameString + File.separator);
				} else if(ff.isFile()) {
					logger.debug("loadDirectory: " + fileNameString + " is file 开始下载");
					logger.debug("loadDirectory: workingDirectory " + ftpClient.printWorkingDirectory());
					File saveFile = new File(localSavePath + fileNameString);
					OutputStream is = new FileOutputStream(saveFile);
					ftpClient.retrieveFile(ff.getName(), is);
					is.close();
					if (saveFile.exists()) {
						logger.debug("loadDirectory: " + fileNameString + " 下载成功");
					}
				}
			}
			if (!rootWorkingDirectory.equals(ftpClient.printWorkingDirectory())) {
				ftpClient.changeToParentDirectory();
				logger.debug("loadDirectory: changeToParentDirectory " + ftpClient.printWorkingDirectory());
			}
		} catch (IOException e) {
			e.printStackTrace();
			result = false;
		} catch (Exception e) {
			e.printStackTrace();
			result = false;
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
				logger.debug("删除文件" + filename + "成功！");
			} else {
				logger.debug("删除文件" + filename + "成功！");
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
	 * 列出服务器上文件和目录
	 * 
	 * @param regStr
	 *            --匹配的正则表达式
	 */
	public void listRemoteFiles(String regStr) {
		try {
			String files[] = ftpClient.listNames(regStr);
			if (files == null || files.length == 0) {
				logger.debug("没有任何文件!");
			} else {
				for (int i = 0; i < files.length; i++) {
					System.out.println(files[i]);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 列出Ftp服务器上的所有文件和目录
	 */
	public void listRemoteAllFiles() {
		try {
			String[] names = ftpClient.listNames();
			for (int i = 0; i < names.length; i++) {
				System.out.println(names[i]);
			}
		} catch (Exception e) {
			e.printStackTrace();
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
	 * 设置参数
	 */
	private boolean setArg() {
		userName = FTPConfig.USERNAME;
		password = FTPConfig.USERPSD;
		ip = FTPConfig.SERVER_IP;
		port = FTPConfig.SERVER_PORT;
		return true;
	}

	/**
	 * 进入到服务器的某个目录下
	 * 
	 * @param directory
	 */
	public boolean changeWorkingDirectory(String directory) {
		boolean flag = true;
		try {
			flag = ftpClient.changeWorkingDirectory(directory);
			if (flag) {
				logger.debug("进入文件夹" + directory + " 成功！");
			} else {
				logger.debug("进入文件夹" + directory + " 失败！");
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		return flag;
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
		// 这里要根据服务器的系统来设置，比如widows要设置为  FTPClientConfig.SYST_NT 
		FTPClientConfig ftpConfig = new FTPClientConfig(
				FTPClientConfig.SYST_UNIX);
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
				logger.debug("创建文件夹" + dir + " 成功！");

			} else {
				logger.debug("创建文件夹" + dir + " 失败！");
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
						logger.debug("创建目录[" + subDirectory + "]失败");
						System.out.println("创建目录[" + subDirectory + "]失败");
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