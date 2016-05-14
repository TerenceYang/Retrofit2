package retrofit2.client;

import okhttp3.Request;
import retrofit2.Call;

/**
 * UrlConnectionCallFactory
 * Created by Yangjing on 2016/5/6.
 */
public class UrlConnectionCallFactory implements Call.Factory {

    /**
     * Create an instance using a default instance for UrlConnection callFactory.
     */
    public static UrlConnectionCallFactory create() {
        return new UrlConnectionCallFactory();
    }

    @Override
    public Call newCall(Request request) {
        return new UrlConnectionCall(request);
    }



}
