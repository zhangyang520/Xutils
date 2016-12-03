/*
 * Copyright (c) 2013. wyouflf (wyouflf@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lidroid.xutils.bitmap.download;

import com.lidroid.xutils.BitmapUtils;
import com.lidroid.xutils.util.IOUtils;
import com.lidroid.xutils.util.LogUtils;
import com.lidroid.xutils.util.OtherUtils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import javax.net.ssl.HttpsURLConnection;

/**
 * 缺省的下载器
 */
public class DefaultDownloader extends Downloader {

    /**
     * Download bitmap to outputStream by uri.
     * 注意:返回值为最后截止的邮搓时间！
     * @param uri          file path, assets path(assets/xxx) or http url.
     * @param outputStream
     * @param task
     * @return The expiry time stamp or -1 if failed to download.
     */
    @Override
    public long downloadToStream(String uri, OutputStream outputStream, final BitmapUtils.BitmapLoadTask<?> task) {

        /*
         *此时判断下载的任务是否为空,是否取消!
         * 最重要的一点是:目标容器是否已被“垃圾回收器”回收！
         */
        if (task == null || task.isCancelled() || task.getTargetContainer() == null) return -1;

        URLConnection urlConnection = null;//url的连接器
        BufferedInputStream bis = null;//缓冲输入流
        //设置https协议的TSL证书
        OtherUtils.trustAllHttpsURLConnection();

        long result = -1;
        long fileLen = 0;
        long currCount = 0;
        try {
            if (uri.startsWith("/")) {
                //如果是本地的文件:直接用文件流进行读取
                FileInputStream fileInputStream = new FileInputStream(uri);
                //长度大小
                fileLen = fileInputStream.available();
                //缓冲读取流
                bis = new BufferedInputStream(fileInputStream);
                //截止时间
                result = System.currentTimeMillis() + this.getDefaultExpiry();
            } else if (uri.startsWith("assets/")) {
                //如果从资源文件中进行读取文件的输入流
                InputStream inputStream = this.getContext().getAssets().open(uri.substring(7, uri.length()));
                fileLen = inputStream.available();
                bis = new BufferedInputStream(inputStream);
                //截止时间
                result = Long.MAX_VALUE;
            } else {
                final URL url = new URL(uri);
                urlConnection = url.openConnection();
//                if(urlConnection instanceof HttpsURLConnection){
//                    System.out.println("urlConnection instanceof HttpsURLConnection");
//                }else if(urlConnection instanceof HttpURLConnection){
//                    System.out.println("urlConnection instanceof HttpURLConnection");
//                }else{
//                    System.out.println("urlConnection is not instanceof HttpURLConnection,HttpsURLConnection");
//                }
                urlConnection.setConnectTimeout(this.getDefaultConnectTimeout());
                urlConnection.setReadTimeout(this.getDefaultReadTimeout());
                //进行urlConnection.getInputStream 默认会进行conn进行连接
                bis = new BufferedInputStream(urlConnection.getInputStream());
                result = urlConnection.getExpiration();
                result = result < System.currentTimeMillis() ? System.currentTimeMillis() + this.getDefaultExpiry() : result;
                fileLen = urlConnection.getContentLength();
            }
            //看任务是否已经被取消 同时看ImageView容器是否为空
            if (task.isCancelled() || task.getTargetContainer() == null) return -1;

            //4kB的大小 写入到其中
            byte[] buffer = new byte[4096];
            int len = 0;
            BufferedOutputStream out = new BufferedOutputStream(outputStream);
            while ((len = bis.read(buffer)) != -1) {
                out.write(buffer, 0, len);
                currCount += len;
                if (task.isCancelled() || task.getTargetContainer() == null) return -1;
                task.updateProgress(fileLen, currCount);
            }
            out.flush();
        } catch (Throwable e) {
            result = -1;
            LogUtils.e(e.getMessage(), e);
        } finally {
            IOUtils.closeQuietly(bis);
        }
        return result;
    }
}
