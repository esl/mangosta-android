package inaka.com.mangosta.models;


import java.util.List;
import java.util.UUID;

import inaka.com.mangosta.network.MongooseService;
import inaka.com.mangosta.models.requests.AddUserRequest;
import inaka.com.mangosta.models.requests.CreateMUCLightMessageRequest;
import inaka.com.mangosta.models.requests.CreateMUCLightRequest;
import inaka.com.mangosta.models.requests.CreateMessageRequest;
import inaka.com.mangosta.models.responses.MongooseIdResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.mock.BehaviorDelegate;

public class MongooseServiceMock implements MongooseService {

    private final BehaviorDelegate<MongooseService> delegate;

    public MongooseServiceMock(BehaviorDelegate<MongooseService> delegate) {
        this.delegate = delegate;
    }

    @Override
    public Call<List<MongooseMessage>> getMessages(@Query("limit") int limit, @Query("before") long before) {
        return delegate.returningResponse(null).getMessages(limit, before);
    }

    @Override
    public Call<List<MongooseMessage>> getMessages(@Path("with") String with, @Query("limit") int limit, @Query("before") long before) {
        return delegate.returningResponse(null).getMessages(with, limit, before);
    }

    @Override
    public Call<List<MongooseMessage>> getMessages(@Path("with") String with, @Query("limit") int limit) {
        return delegate.returningResponse(null).getMessages(with, limit);
    }

    @Override
    public Call<MongooseIdResponse> sendMessage(@Body CreateMessageRequest body) {
        MongooseIdResponse mongooseIdResponse = new MongooseIdResponse();
        mongooseIdResponse.setId(UUID.randomUUID().toString());
        return delegate.returningResponse(mongooseIdResponse).sendMessage(body);
    }

    @Override
    public Call<List<MongooseMUCLight>> getMUCLights() {
        return delegate.returningResponse(null).getMUCLights();
    }

    @Override
    public Call<MongooseIdResponse> createMUCLight(@Body CreateMUCLightRequest body) {
        MongooseIdResponse mongooseIdResponse = new MongooseIdResponse();
        mongooseIdResponse.setId(UUID.randomUUID().toString());
        return delegate.returningResponse(mongooseIdResponse).createMUCLight(body);
    }

    @Override
    public Call<MongooseMUCLight> getMUCLightDetails(@Path("id") String id) {
        return delegate.returningResponse(null).getMUCLightDetails(id);
    }

    @Override
    public Call<Object> addUserToMUCLight(@Path("id") String id, @Body AddUserRequest body) {
        return delegate.returningResponse(null).addUserToMUCLight(id, body);
    }

    @Override
    public Call<List<MongooseMUCLightMessage>> getMUCLightMessages(@Path("id") String id) {
        return delegate.returningResponse(null).getMUCLightMessages(id);
    }

    @Override
    public Call<MongooseIdResponse> sendMessageToMUCLight(@Path("id") String id, @Body CreateMUCLightMessageRequest body) {
        return delegate.returningResponse(null).sendMessageToMUCLight(id, body);
    }

    @Override
    public Call<Object> removeUserFromMUCLight(@Path("id") String id, @Path("user") String user) {
        return delegate.returningResponse(null).removeUserFromMUCLight(id, user);
    }

}
