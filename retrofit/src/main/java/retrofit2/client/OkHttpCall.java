package retrofit2.client;

import android.util.Log;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * OkHttpCall
 * Created by Yangjing on 2016/5/9.
 */
public class OkHttpCall implements Call {

    private static final String TAG = "OkHttpCall";
    /** The application's original request unadulterated by redirects or auth headers. */
    Request originalRequest;
    OkHttpClient okHttpClient;

    private okhttp3.Call rawCall;
    private volatile boolean canceled;

    protected OkHttpCall(Request originalRequest) {
        this.originalRequest = originalRequest;
        this.okHttpClient = new OkHttpClient();
    }

    @Override
    public Call clone() {
        return null;
    }

    @Override
    public Request request() {
        return this.originalRequest;
    }

    @Override
    public void enqueue(final Callback aRetrofitCallback) {
        try {
            rawCall = createRawCall();
            rawCall.enqueue(new okhttp3.Callback() {

                @Override
                public void onResponse(okhttp3.Call call, okhttp3.Response rawResponse) {
                    Log.d(TAG, "onResponse()");
                    Response response;
                    try {
                        response = parseResponse(rawResponse);
                    } catch (Exception e) {
                        callFailure(e);
                        return;
                    }
                    callSuccess(response);
                }

                @Override
                public void onFailure(okhttp3.Call call, IOException e) {
                    callFailure(e);
                }

                private void callFailure(Exception e) {
                    try {
                        aRetrofitCallback.onFailure(OkHttpCall.this, e);
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }

                private void callSuccess(Response response) {
                    try {
                        aRetrofitCallback.onResponse(OkHttpCall.this, response);
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }
            });
        } catch (IOException ioEx) {
            Log.e(TAG, ioEx.toString());
        }
    }

    @Override
    public Response execute() throws IOException {
        rawCall = createRawCall();
        return parseResponse(rawCall.execute());
    }

    private okhttp3.Call createRawCall() throws IOException {
        okhttp3.Call call = okHttpClient.newCall(originalRequest);
        if (call == null) {
            throw new NullPointerException("Call.Factory returned null.");
        }
        return call;
    }


    private Response parseResponse(okhttp3.Response rawResponse) {
        return new Response.Builder()
                .code(rawResponse.code())
                .message(rawResponse.message())
                .request(originalRequest)
                .protocol(rawResponse.protocol())
                .headers(rawResponse.headers())
                .body(rawResponse.body())
                .build();
    }

    @Override
    public boolean isExecuted() {
        return false;
    }

    @Override
    public boolean isCanceled() {
        Log.d(TAG, "isCanceled");
        return canceled;
    }

    @Override
    public void cancel() {
        canceled = true;

        okhttp3.Call call;
        synchronized (this) {
            call = rawCall;
        }
        if (call != null) {
            call.cancel();
        }
    }


}
