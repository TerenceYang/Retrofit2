package retrofit2.client;

import android.text.TextUtils;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.zip.GZIPInputStream;

import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static java.lang.Thread.MIN_PRIORITY;

/**
 * UrlConnectionCall
 * Created by Terence on 2016/5/7.
 */
public class UrlConnectionCall implements Call {

    private static final String TAG = "UrlConnectionCall";

    private static final int CHUNK_SIZE = 4096;

    static final int CONNECT_TIMEOUT_MILLIS = 15 * 1000; // 15s
    static final int READ_TIMEOUT_MILLIS = 20 * 1000; // 20s

    static final String THREAD_PREFIX = "Retrofit-";
    static final String IDLE_THREAD_NAME = THREAD_PREFIX + "Idle";

    private static final int BUFFER_SIZE = 0x1000;

    /** The application's original request unadulterated by redirects or auth headers. */
    Request originalRequest;

    private final Executor httpExecutor;

    protected UrlConnectionCall(Request originalRequest) {
        this.originalRequest = originalRequest;
        httpExecutor = getHttpExecutor();
    }

    @Override
    public Call clone() {
        return null;
    }

    @Override
    public Request request() {
        return originalRequest;
    }

    @Override
    public Response execute() throws IOException {
        HttpURLConnection connection = openConnection(originalRequest);
        prepareRequest(connection, originalRequest);
        return readResponse(connection, originalRequest);
    }

    @Override
    public void enqueue(final Callback callback) {
        httpExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    HttpURLConnection connection = openConnection(originalRequest);
                    prepareRequest(connection, originalRequest);
                    Response response = readResponse(connection, originalRequest);
                    callback.onResponse(UrlConnectionCall.this, response);
                } catch (Exception ex) {
                    callback.onFailure(UrlConnectionCall.this, ex);
                }
            }
        });
    }

    @Override
    public boolean isExecuted() {
        return false;
    }

    @Override
    public void cancel() {

    }

    @Override
    public boolean isCanceled() {
        return false;
    }

    private Executor getHttpExecutor() {
        return Executors.newCachedThreadPool(new ThreadFactory() {
            @Override
            public Thread newThread(final Runnable r) {
                return new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Thread.currentThread().setPriority(MIN_PRIORITY);
                        r.run();
                    }
                }, IDLE_THREAD_NAME);
            }
        });
    }


    protected HttpURLConnection openConnection(Request request) throws IOException {
        HttpURLConnection connection =
                (HttpURLConnection) new URL(request.url().toString()).openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
        connection.setReadTimeout(READ_TIMEOUT_MILLIS);
        return connection;
    }

    void prepareRequest(HttpURLConnection connection, Request request) throws IOException {
        connection.setRequestMethod(request.method());
        connection.setDoInput(true);

        Set<String> headers = request.headers().names();
        for (String header : headers) {
            connection.addRequestProperty(header, request.header(header));
        }

        RequestBody body = request.body();
        if (body != null) {
            connection.setDoOutput(true);
            connection.addRequestProperty("Content-Type", body.contentType().toString());
            long length = body.contentLength();
            if (length != -1) {
                connection.setFixedLengthStreamingMode((int) length);
                connection.addRequestProperty("Content-Length", String.valueOf(length));
            } else {
                connection.setChunkedStreamingMode(CHUNK_SIZE);
            }
        }
    }

    Response readResponse(HttpURLConnection connection, Request request) throws IOException {
        int status = connection.getResponseCode();
        String reason = connection.getResponseMessage();
        if (reason == null) reason = ""; // HttpURLConnection treats empty reason as null.

        Headers.Builder builder = new Headers.Builder();
        for (Map.Entry<String, List<String>> field : connection.getHeaderFields().entrySet()) {
            String name = field.getKey();
            if (!TextUtils.isEmpty(name)) {
                for (String value : field.getValue()) {
                    builder.add(name, value);
                }
            }

        }
        Headers headers = builder.build();

        String mimeType = connection.getContentType();
//        int length = connection.getContentLength();
        InputStream inputStream;
        GZIPInputStream gzin = null;
        if (status >= 400) {
            inputStream = connection.getErrorStream();
        } else {
            inputStream = connection.getInputStream();
        }
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            if (inputStream != null) {
                byte[] buf = new byte[BUFFER_SIZE];
                int r;
                while ((r = inputStream.read(buf)) != -1) {
                    baos.write(buf, 0, r);
                }
                byte[] data = baos.toByteArray();
                if (baos != null) {
                    baos.close();
                }
                if (data != null && data.length > 0) {
                    ResponseBody responseBody = ResponseBody.create(MediaType.parse(mimeType), data);
                    Protocol protocol = Protocol.HTTP_1_0;
                    try {
                        protocol = Protocol.get(connection.getURL().getProtocol());
                    } catch (IOException ioException) {
                        protocol = Protocol.HTTP_1_1;
                    }
                    return new Response.Builder()
                            .code(status)
                            .message(reason)
                            .request(request)
                            .protocol(protocol)
                            .headers(headers)
                            .body(responseBody)
                            .build();
                }
            }
        } catch (Exception ex) {
            Log.d(TAG, "readResponse: " + ex.toString());
        }
        return null;
    }



}
