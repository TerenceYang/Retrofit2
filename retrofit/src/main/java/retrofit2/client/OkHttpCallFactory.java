package retrofit2.client;

import okhttp3.Request;
import retrofit2.Call;

/**
 * OkHttpCallFactory
 * Created by Yangjing on 2016/5/9.
 */
public class OkHttpCallFactory implements Call.Factory {

    /**
     * Create an instance using a default instance for UrlConnection callFactory.
     */
    public static OkHttpCallFactory create() {
        return new OkHttpCallFactory();
    }

    @Override
    public Call newCall(Request request) {
        return new OkHttpCall(request);
    }
}
