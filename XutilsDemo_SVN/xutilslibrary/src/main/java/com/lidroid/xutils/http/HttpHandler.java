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

package com.lidroid.xutils.http;

import android.os.SystemClock;

import com.lidroid.xutils.HttpUtils;
import com.lidroid.xutils.exception.HttpException;
import com.lidroid.xutils.exception.HttpExceptionState;
import com.lidroid.xutils.http.callback.DefaultHttpRedirectHandler;
import com.lidroid.xutils.http.callback.FileDownloadHandler;
import com.lidroid.xutils.http.callback.HttpRedirectHandler;
import com.lidroid.xutils.http.callback.RequestCallBack;
import com.lidroid.xutils.http.callback.RequestCallBackHandler;
import com.lidroid.xutils.http.callback.StringDownloadHandler;
import com.lidroid.xutils.task.PriorityAsyncTask;
import com.lidroid.xutils.util.OtherUtils;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolException;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.RedirectHandler;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.protocol.HttpContext;

import java.io.File;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.UnknownHostException;

/**
 * http的句柄:
 *      继承优先权的任务栈
 *      实现请求回调的handler
 */

public class HttpHandler<T> extends PriorityAsyncTask<Object, Object, Void> implements RequestCallBackHandler {

    private final AbstractHttpClient client;
    private final HttpContext context;

    private HttpRedirectHandler httpRedirectHandler;

    public void setHttpRedirectHandler(HttpRedirectHandler httpRedirectHandler) {
        if (httpRedirectHandler != null) {
            this.httpRedirectHandler = httpRedirectHandler;
        }
    }

    private String requestUrl;
    private String requestMethod;
    private HttpRequestBase request;
    private boolean isUploading = true;
    private RequestCallBack<T> callback;

    private int retriedCount = 0;
    private String fileSavePath = null;
    private boolean isDownloadingFile = false;
    private boolean autoResume = false; // Whether the downloading could continue from the point of interruption.
    private boolean autoRename = false; // Whether rename the file by response header info when the download completely.
    private String charset; // The default charset of response header info.

    public HttpHandler(AbstractHttpClient client, HttpContext context, String charset, RequestCallBack<T> callback) {
        this.client = client;
        this.context = context;
        this.callback = callback;
        this.charset = charset;
        this.client.setRedirectHandler(notUseApacheRedirectHandler);
    }

    private State state = State.WAITING;

    public State getState() {
        return state;
    }

    private long expiry = HttpCache.getDefaultExpiryTime();

    public void setExpiry(long expiry) {
        this.expiry = expiry;
    }

    public void setRequestCallBack(RequestCallBack<T> callback) {
        this.callback = callback;
    }

    public RequestCallBack<T> getRequestCallBack() {
        return this.callback;
    }

    // 执行请求
    @SuppressWarnings("unchecked")
    private ResponseInfo<T> sendRequest(HttpRequestBase request) throws HttpException {
//        System.out.println("httpHandler sendRequest ......");
        HttpRequestRetryHandler retryHandler = client.getHttpRequestRetryHandler();
        while (true) {
            if (autoResume && isDownloadingFile) {
                File downloadFile = new File(fileSavePath);
                long fileLen = 0;
                if (downloadFile.isFile() && downloadFile.exists()) {
                    fileLen = downloadFile.length();
                }
                if (fileLen > 0) {
                    request.setHeader("RANGE", "bytes=" + fileLen + "-");
                }
            }

            boolean retry = false;
            IOException exception = null;
            HttpExceptionState httpExceptionState;
            try {
                requestMethod = request.getMethod();
                if (HttpUtils.sHttpCache.isEnabled(requestMethod)) {
                    String result = HttpUtils.sHttpCache.get(requestUrl);
                    if (result != null) {
                        return new ResponseInfo<T>(null, (T) result, true);
                    }
                }
//                System.out.println("httpHandler sendRequest 1111......");
                ResponseInfo<T> responseInfo = null;
                if (!isCancelled()) {
//                    System.out.println("httpHandler client.execute 222......");
                    HttpResponse response = client.execute(request, context);
//                    System.out.println("httpHandler handleResponse 33333......");
                    responseInfo = handleResponse(response);
                }
                return responseInfo;
            } catch (UnknownHostException e) {
                exception = e;
                httpExceptionState=HttpExceptionState.IP_CONNECT_ERROR;
                retry = retryHandler.retryRequest(exception, ++retriedCount, context);
            }catch(ConnectTimeoutException e){
//                System.out.println("httpHandler sendRequest ConnectTimeoutException......");
                exception = e;
                httpExceptionState=HttpExceptionState.CONNECT_TIME_OUT;
                retry = retryHandler.retryRequest(exception, ++retriedCount, context);
            }catch(SocketTimeoutException e){
                httpExceptionState=HttpExceptionState.CONNECT_TIME_OUT;
//                System.out.println("httpHandler sendRequest SocketTimeoutException......");
                exception = e;
                retry = retryHandler.retryRequest(exception, ++retriedCount, context);
            }catch (IOException e){
                httpExceptionState=HttpExceptionState.IP_CONNECT_ERROR;
                e.printStackTrace();
//                System.out.println("httpHandler sendRequest IOException......");
                exception = e;
                retry = retryHandler.retryRequest(exception, ++retriedCount, context);
            }catch (HttpException e) {
                e.printStackTrace();
                httpExceptionState=HttpExceptionState.OTHER_EXCEPTION;
                throw new HttpException(httpExceptionState,e);
//                System.out.println("httpHandler sendRequest HttpException......");
            }catch(RuntimeException e){
                e.printStackTrace();
                httpExceptionState=HttpExceptionState.OTHER_EXCEPTION;
//                System.out.println("httpHandler sendRequest HttpException......");
                throw new HttpException(httpExceptionState,e);
            }catch (Throwable e) {
                e.printStackTrace();
                httpExceptionState=HttpExceptionState.OTHER_EXCEPTION;
//                System.out.println("httpHandler sendRequest Throwable......");
                exception = new IOException(e.getMessage());
                exception.initCause(e);
                retry = retryHandler.retryRequest(exception, ++retriedCount, context);
            }
            if (!retry) {
//                System.out.println("httpHandler sendRequest retry false......");
                throw new HttpException(httpExceptionState);
            }
        }
    }

    @Override
    protected Void doInBackground(Object... params) {
        if (this.state == State.CANCELLED || params == null || params.length == 0) return null;

        if (params.length > 3) {
            fileSavePath = String.valueOf(params[1]);
            isDownloadingFile = fileSavePath != null;
            autoResume = (Boolean) params[2];
            autoRename = (Boolean) params[3];
        }

        try {
            if (this.state == State.CANCELLED) return null;
            // init request & requestUrl
            request = (HttpRequestBase) params[0];
            requestUrl = request.getURI().toString();
            if (callback != null) {
                callback.setRequestUrl(requestUrl);
            }

            this.publishProgress(UPDATE_START);

            lastUpdateTime = SystemClock.uptimeMillis();

            ResponseInfo<T> responseInfo = sendRequest(request);
            if (responseInfo != null) {
                this.publishProgress(UPDATE_SUCCESS, responseInfo);
                return null;
            }
        } catch (HttpException e) {
            this.publishProgress(UPDATE_FAILURE, e, e.getMessage());
        }

        return null;
    }

    private final static int UPDATE_START = 1;
    private final static int UPDATE_LOADING = 2;
    private final static int UPDATE_FAILURE = 3;
    private final static int UPDATE_SUCCESS = 4;

    @Override
    @SuppressWarnings("unchecked")
    protected void onProgressUpdate(Object... values) {
        System.out.println("httpHandler onProgressUpdate..");
        if (this.state == State.CANCELLED || values == null || values.length == 0 || callback == null) return;
        switch ((Integer) values[0]) {
            case UPDATE_START:
                this.state = State.STARTED;
                callback.onStart();
                break;
            case UPDATE_LOADING:
                if (values.length != 3) return;
                this.state = State.LOADING;
                callback.onLoading(
                        Long.valueOf(String.valueOf(values[1])),
                        Long.valueOf(String.valueOf(values[2])),
                        isUploading);
                break;
            case UPDATE_FAILURE:
                if (values.length != 3) return;
                this.state = State.FAILURE;
                callback.onFailure((HttpException) values[1], (String) values[2]);
                break;
            case UPDATE_SUCCESS:
                if (values.length != 2) return;
                this.state = State.SUCCESS;
                callback.onSuccess((ResponseInfo<T>) values[1]);
                break;
            default:
                break;
        }
    }

    @SuppressWarnings("unchecked")
    private ResponseInfo<T> handleResponse(HttpResponse response) throws HttpException, IOException {
        System.out.println("httpHandler handleResponse..");
        if (response == null) {
            throw new HttpException("response is null");
        }
        if (isCancelled()) return null;

        StatusLine status = response.getStatusLine();
        int statusCode = status.getStatusCode();
        if (statusCode < 300) {
            Object result = null;
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                isUploading = false;
                if (isDownloadingFile) {
                    autoResume = autoResume && OtherUtils.isSupportRange(response);
                    String responseFileName = autoRename ? OtherUtils.getFileNameFromHttpResponse(response) : null;
                    FileDownloadHandler downloadHandler = new FileDownloadHandler();
                    result = downloadHandler.handleEntity(entity, this, fileSavePath, autoResume, responseFileName);
                } else {
                    StringDownloadHandler downloadHandler = new StringDownloadHandler();
                    result = downloadHandler.handleEntity(entity, this, charset);
                    if (HttpUtils.sHttpCache.isEnabled(requestMethod)) {
                        HttpUtils.sHttpCache.put(requestUrl, (String) result, expiry);
                    }
                }
            }
            return new ResponseInfo<T>(response, (T) result, false);
        } else if (statusCode == 301 || statusCode == 302) {
            if (httpRedirectHandler == null) {
                httpRedirectHandler = new DefaultHttpRedirectHandler();
            }
            HttpRequestBase request = httpRedirectHandler.getDirectRequest(response);
            if (request != null) {
                return this.sendRequest(request);
            }
        } else if (statusCode == 416) {
            throw new HttpException(statusCode, "maybe the file has downloaded completely");
        } else {
            throw new HttpException(statusCode, status.getReasonPhrase());
        }
        return null;
    }

    /**
     * cancel request task.
     */
    @Override
    public void cancel() {
        this.state = State.CANCELLED;

        if (request != null && !request.isAborted()) {
            try {
                request.abort();
            } catch (Throwable e) {
            }
        }
        if (!this.isCancelled()) {
            try {
                this.cancel(true);
            } catch (Throwable e) {
            }
        }

        if (callback != null) {
            callback.onCancelled();
        }
    }

    private long lastUpdateTime;

    @Override
    public boolean updateProgress(long total, long current, boolean forceUpdateUI) {
        System.out.println("httpHandler updateProgress..");
        if (callback != null && this.state != State.CANCELLED) {
            if (forceUpdateUI) {
                this.publishProgress(UPDATE_LOADING, total, current);
            } else {
                long currTime = SystemClock.uptimeMillis();
                if (currTime - lastUpdateTime >= callback.getRate()) {
                    lastUpdateTime = currTime;
                    this.publishProgress(UPDATE_LOADING, total, current);
                }
            }
        }
        return this.state != State.CANCELLED;
    }

    public enum State {
        WAITING(0), STARTED(1), LOADING(2), FAILURE(3), CANCELLED(4), SUCCESS(5);
        private int value = 0;

        State(int value) {
            this.value = value;
        }

        public static State valueOf(int value) {
            switch (value) {
                case 0:
                    return WAITING;
                case 1:
                    return STARTED;
                case 2:
                    return LOADING;
                case 3:
                    return FAILURE;
                case 4:
                    return CANCELLED;
                case 5:
                    return SUCCESS;
                default:
                    return FAILURE;
            }
        }

        public int value() {
            return this.value;
        }
    }

    private static final NotUseApacheRedirectHandler notUseApacheRedirectHandler = new NotUseApacheRedirectHandler();

    private static final class NotUseApacheRedirectHandler implements RedirectHandler {
        @Override
        public boolean isRedirectRequested(HttpResponse httpResponse, HttpContext httpContext) {
            return false;
        }

        @Override
        public URI getLocationURI(HttpResponse httpResponse, HttpContext httpContext) throws ProtocolException {
            return null;
        }
    }
}
