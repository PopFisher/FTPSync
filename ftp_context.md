# 快速搭建一个本地的FTP服务器
&emsp;&emsp;如果需要开发FTP文件上传下载功能，那么需要在本机上搭建一个本地FTP服务器，方便调试。

## 第一步：配置IIS Web服务器
### 控制面板中找到“程序”并打开
![](/docpic/1.jpg "控制面板中找到“程序”")

### 程序界面找到“启用或关闭Windows功能”并打开
![](/docpic/2.jpg "启用或关闭Windows功能")

### 从“启用或关闭Windows功能”弹窗中找到Internet Information Services(或者中文版Internet信息服务)并打开
![](/docpic/3.jpg "Internet Information Services")

### 配置IIS并点击确定
![](/docpic/4.png "配置IIS并点击确定")

## 第二步：配置IIS Web站点
### 开始菜单搜索“IIS”并点击进入IIS管理器
![](/docpic/5.png "搜索IIS并点击进入IIS管理器")

### 新建FTP站点

#### 新建FTP服务器根目录文件夹
![](/docpic/6.png "新建FTP服务器根目录文件夹")

#### 查看本机ip地址（打开cmd输入ipconfig）
![](/docpic/7.png "查看本机ip地址")

#### IIS网站管理器界面左边导航栏找到“网站”，右键弹出菜单
![](/docpic/8.png "右键")

#### IIS网站管理器“网站”右键弹出菜单点击“添加FTP站点”
![](/docpic/9.png "添加FTP站点")

#### 配置网站（网站名称：FtpSite 物理路径：E\ftpserver 本机IP地址：192.168.0.105）
**Ftp站点名称和物理路径设置**
![](/docpic/10.png "Ftp站点名称和物理路径设置")

**IP 端口号 SSL设置**
![](/docpic/11.png "IP 端口号 SSL设置")

**身份验证和授权信息设置**
![](/docpic/12.png "身份验证和授权信息设置")

## 第三步：测试FTP站点（先在物理路径：E\ftpserver随便放一个文件）
### 浏览器输入ftp地址（ftp://192.168.0.105）

![](/docpic/13.png "浏览器输入ftp地址")

&emsp;&emsp;输入FTP地址时发现需要用户和密码，可是配置的过程中好像没有看到设置用户和密码的步骤，没关系，我们可以自己设置。
### IIS管理器配置匿名用户密码（禁用密码）

![](/docpic/14.png "FTP身份认证")

![](/docpic/15.png "启用匿名身份认证")

### 再次测试，浏览器输入ftp地址（ftp://192.168.0.105）

![](/docpic/16.png "匿名用户访问成功")

### IIS管理器配置匿名用户密码（使用密码）

![](/docpic/17.png "匿名用户使用密码")

![](/docpic/18.png "匿名用户使用密码")