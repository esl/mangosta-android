package inaka.com.mangosta.interfaces;

import java.util.List;

import inaka.com.mangosta.models.MongooseMUCLight;
import inaka.com.mangosta.models.MongooseMUCLightMessage;
import inaka.com.mangosta.models.MongooseMessage;
import inaka.com.mangosta.models.requests.AddUserRequest;
import inaka.com.mangosta.models.requests.CreateMUCLightMessageRequest;
import inaka.com.mangosta.models.requests.CreateMUCLightRequest;
import inaka.com.mangosta.models.requests.CreateMessageRequest;
import inaka.com.mangosta.models.responses.MongooseIdResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface MongooseService {

    /**
     * Get messages.
     *
     * @param limit  is the limit amount of messages to retrieve
     * @param before is the timestamp in milliseconds. If set, only messages before this date will be returned.
     * @return the list of messages
     */
    @GET("messages")
    Call<List<MongooseMessage>> getMessages(@Query("limit") int limit,
                                            @Query("before") long before);

    /**
     * Get messages.
     *
     * @param with   is the JID with
     * @param before is the timestamp in milliseconds. If set, only messages before this date will be returned.
     * @param limit  is the limit amount of messages to retrieve
     * @return the list of messages
     */
    @GET("messages/{with}")
    Call<List<MongooseMessage>> getMessages(@Path("with") String with,
                                            @Query("limit") int limit,
                                            @Query("before") long before);

    /**
     * Get messages.
     *
     * @param with  is the JID with
     * @param limit is the limit amount of messages to retrieve
     * @return the list of messages
     */
    @GET("messages/{with}")
    Call<List<MongooseMessage>> getMessages(@Path("with") String with,
                                            @Query("limit") int limit);

    /**
     * Send a message.
     *
     * @param body is the message
     * @return the id of the new message
     */
    @POST("messages")
    Call<MongooseIdResponse> sendMessage(@Body CreateMessageRequest body);

    /**
     * Get my MUC Light rooms.
     *
     * @return the list of rooms
     */
    @GET("rooms")
    Call<List<MongooseMUCLight>> getMUCLights();

    /**
     * Create a MUC Light.
     *
     * @param body is the room
     * @return the id of the new room
     */
    @POST("rooms")
    Call<MongooseIdResponse> createMUCLight(@Body CreateMUCLightRequest body);

    /**
     * Get MUC Light details.
     *
     * @param id is the id of the room.
     * @return the room details
     */
    @GET("rooms/{id}")
    Call<MongooseMUCLight> getMUCLightDetails(@Path("id") String id);

    /**
     * Add user to a MUC Light.
     *
     * @param id   is the id of the MUC Light.
     * @param body has the user to add.
     * @return a response
     */
    @POST("rooms/{id}/users")
    Call<Object> addUserToMUCLight(@Path("id") String id,
                                   @Body AddUserRequest body);

    /**
     * Get messages from a MUC Light.
     *
     * @param id is the id of the MUC Light.
     * @return the list of messages
     */
    @GET("rooms/{id}/messages")
    Call<List<MongooseMUCLightMessage>> getMUCLightMessages(@Path("id") String id);

    /**
     * Send message to a MUC Light.
     *
     * @param id   is the id of the MUC Light.
     * @param body is the message.
     * @return the id of the new message sent
     */
    @POST("rooms/{id}/messages")
    Call<MongooseIdResponse> sendMessageToMUCLight(@Path("id") String id,
                                                   @Body CreateMUCLightMessageRequest body);

    /**
     * Remove a user from a MUC Light
     *
     * @param id   is the id of the MUC Light.
     * @param user is the JID of the user
     * @return a response
     */
    @DELETE("rooms/{id}/users/{user}")
    Call<Object> removeUserFromMUCLight(@Path("id") String id,
                                        @Path("user") String user);

}
