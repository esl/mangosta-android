package inaka.com.mangosta.network;

import android.text.TextUtils;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.jivesoftware.smack.util.stringencoder.Base64;

import java.io.IOException;

import inaka.com.mangosta.utils.Preferences;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MongooseAPI {

    private static final boolean ACCEPT_INVALID_CERTIFICATES = false; //for debug testing ONLY!

    public static final String BASE_URL = "https://31.172.186.62:5285/api/";

    private static MongooseAPI mInstance;

    public static MongooseAPI getInstance() {
        if (mInstance == null) {
            mInstance = new MongooseAPI();
        }
        return mInstance;
    }

    public static void setSpecialInstanceForTesting(MongooseAPI mongooseAPI) {
        mInstance = mongooseAPI;
    }

    public MongooseService getServiceWithAuthentication(String username, String password) {

        Gson gsonConfig = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .create();

        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        final String token = Base64.encode(username + ":" + password);
        Interceptor requestInterceptor = new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Request request = chain.request();
                if (!TextUtils.isEmpty(token)) {
                    request = request.newBuilder()
                            .addHeader("Content-Type", "application/json")
                            .addHeader("Accept", "application/json")
                            .addHeader("Authorization", "Basic " + token)
                            .build();
                }
                return chain.proceed(request);
            }
        };

        OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
        httpClient.hostnameVerifier(HttpClientBuilder.getTrustAllHostnameVerifier());
        if (ACCEPT_INVALID_CERTIFICATES) {
            httpClient.sslSocketFactory(HttpClientBuilder.getUnsafeSSLSocketFactory(),
                    HttpClientBuilder.getTrustAllCerts());
        }

        httpClient.addInterceptor(logging);
        httpClient.addInterceptor(requestInterceptor);

        Retrofit.Builder builder = new Retrofit.Builder();
        builder.client(httpClient.build());

        Retrofit retrofit = builder
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(gsonConfig))
                .build();

        return retrofit.create(MongooseService.class);
    }

    public MongooseService getAuthenticatedService() {
        Preferences preferences = Preferences.getInstance();
        String userJid = preferences.getUserXMPPJid();
        String password = preferences.getUserXMPPPassword();

        if (userJid != null && password != null && !userJid.equals("") && !password.equals("")) {
            return new MongooseAPI().getServiceWithAuthentication(userJid, password);
        } else {
            return null;
        }

    }

}
