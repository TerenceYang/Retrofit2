package com.queen.rxjavaretrofit20.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;
import android.widget.TextView;

import com.queen.rxjavaretrofit20.R;
import com.queen.rxjavaretrofit20.entity.MovieEntity;
import com.queen.rxjavaretrofit20.http.MovieService;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.client.OkHttpCallFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    @Bind(R.id.click_me_BN)
    Button clickMeBN;
    @Bind(R.id.result_TV)
    TextView resultTV;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
    }

    @OnClick(R.id.click_me_BN)
    public void onClick() {
        getMovie();
    }

    //进行网络请求
    private void getMovie(){
        String baseUrl = "https://api.douban.com/v2/movie/";

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(OkHttpCallFactory.create())
//                .client(UrlConnectionCallFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        MovieService movieService = retrofit.create(MovieService.class);
        final Call<MovieEntity> call = movieService.getTopMovie(0, 10);
        // enqueue test
        call.enqueue(new Callback<MovieEntity>() {
            @Override
            public void onResponse(Call<MovieEntity> call, Response<MovieEntity> response) {
                resultTV.setText(response.body().toString());
            }

            @Override
            public void onFailure(Call<MovieEntity> call, Throwable t) {
                resultTV.setText(t.getMessage());
            }
        });

        // excute test
//        new AsyncTask<Void, Void, Response<MovieEntity>>(){
//
//            @Override
//            protected void onPreExecute() {
//                super.onPreExecute();
//            }
//
//            @Override
//            protected Response<MovieEntity> doInBackground(Void... params) {
//                try {
//                    Response<MovieEntity> response = call.execute();
//                    return response;
//                } catch (Exception ex) {
//                    Log.d(TAG, ex.toString());
//                }
//                return null;
//            }
//
//            @Override
//            protected void onPostExecute(Response<MovieEntity> response) {
//                resultTV.setText(response.body().toString());
//            }
//        }.execute();



    }
}
