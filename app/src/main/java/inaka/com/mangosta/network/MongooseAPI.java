package inaka.com.mangosta.network;

import android.text.TextUtils;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.jivesoftware.smack.util.stringencoder.Base64;

import java.io.IOException;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

import inaka.com.mangosta.interfaces.MongooseService;
import inaka.com.mangosta.utils.Preferences;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MongooseAPI {

    public static final int RESPONSE_OK = 204;
    public static final int NOT_FOUND = 404;

    private static final String BASE_URL = "https://31.172.186.62:5285/api/";

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
        httpClient.addInterceptor(logging);
        httpClient.addInterceptor(requestInterceptor);
        httpClient.hostnameVerifier(new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        });

        Retrofit.Builder builder = new Retrofit.Builder();
        builder.client(httpClient.build());

        Retrofit retrofit = builder
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(gsonConfig))
                .build();

        return retrofit.create(MongooseService.class);
    }

    public static MongooseService getAuthenticatedService() {
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
