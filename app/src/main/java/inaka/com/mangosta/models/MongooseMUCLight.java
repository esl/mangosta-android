package inaka.com.mangosta.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class MongooseMUCLight {

    public MongooseMUCLight() {

    }

    @SerializedName("id")
    @Expose
    private String id;

    @SerializedName("subject")
    @Expose
    private String subject;

    @SerializedName("name")
    @Expose
    private String name;

    @SerializedName("participants")
    @Expose
    private List<MongooseParticipant> participants;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public List<MongooseParticipant> getParticipants() {
        return participants;
    }

    public void setParticipants(List<MongooseParticipant> participants) {
        this.participants = participants;
    }

}
