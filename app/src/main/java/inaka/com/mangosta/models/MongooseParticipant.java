package inaka.com.mangosta.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class MongooseParticipant {

    public MongooseParticipant() {

    }

    @SerializedName("user")
    @Expose
    private String user;

    @SerializedName("role")
    @Expose
    private String role;

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

}
