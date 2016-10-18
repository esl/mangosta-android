package inaka.com.mangosta.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class MongooseMUCLightMessage {

    public MongooseMUCLightMessage() {

    }

    @SerializedName("id")
    @Expose
    private String id;

    @SerializedName("from")
    @Expose
    private String from;

    @SerializedName("type")
    @Expose
    private String type;

    @SerializedName("body")
    @Expose
    private String body;

    @SerializedName("user")
    @Expose
    private String user;

    @SerializedName("affiliation")
    @Expose
    private String affiliation;

    @SerializedName("timestamp")
    @Expose
    private long timestamp;

    public String getAffiliation() {
        return affiliation;
    }

    public void setAffiliation(String affiliation) {
        this.affiliation = affiliation;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
