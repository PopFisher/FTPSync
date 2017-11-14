package com.ftp.util;
import java.io.File;
import java.io.FileInputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.codec.digest.DigestUtils;

public class MD5Util {
    /**
     * 使用md5的算法进行加密
     */
    public static String md5(String plainText) {
        byte[] secretBytes = null;
        try {
            secretBytes = MessageDigest.getInstance("md5").digest(
                    plainText.getBytes());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("没有md5这个算法！");
        }
        String md5code = new BigInteger(1, secretBytes).toString(16);// 16进制数字
        // 如果生成数字未满32位，需要前面补0
        for (int i = 0; i < 32 - md5code.length(); i++) {
            md5code = "0" + md5code;
        }
        return md5code;
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

    public static void main(String[] args) {
        System.out.println(md5("123"));
        System.out.println(DigestUtils.md5Hex("123"));
    }

}