package inaka.com.mangosta.models;

import android.os.Parcelable;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class User implements Parcelable {

    public User() {
    }

    @SerializedName("login")
    @Expose
    private String login;

    @SerializedName("id")
    @Expose
    private Integer id;

    @SerializedName("avatar_url")
    @Expose
    private String avatarUrl;

    @SerializedName("gravatar_id")
    @Expose
    private String gravatarId;

    @SerializedName("html_url")
    @Expose
    private String htmlUrl;

    @SerializedName("followers_url")
    @Expose
    private String followersUrl;

    @SerializedName("following_url")
    @Expose
    private String followingUrl;

    @SerializedName("gists_url")
    @Expose
    private String gistsUrl;

    @SerializedName("starred_url")
    @Expose
    private String starredUrl;

    @SerializedName("subscriptions_url")
    @Expose
    private String subscriptionsUrl;

    @SerializedName("organizationsUrl")
    @Expose
    private String organizationsUrl;

    @SerializedName("repos_url")
    @Expose
    private String reposUrl;

    @SerializedName("type")
    @Expose
    private String type;

    @SerializedName("name")
    @Expose
    private String name;

    @SerializedName("company")
    @Expose
    private String company;


    @SerializedName("location")
    @Expose
    private String location;


    @SerializedName("email")
    @Expose
    private String email;

    @SerializedName("public_repos")
    @Expose
    private Integer publicRepos;

    @SerializedName("public_gists")
    @Expose
    private Integer publicGists;

    @SerializedName("followers")
    @Expose
    private Integer followers;

    @SerializedName("following")
    @Expose
    private Integer following;

    @SerializedName("created_at")
    @Expose
    private String createdAt;

    @SerializedName("updated_at")
    @Expose
    private String updatedAt;

    @SerializedName("total_private_repos")
    @Expose
    private Integer totalPrivateRepos;

    @SerializedName("owned_private_repos")
    @Expose
    private Integer ownedPrivateRepos;

    @SerializedName("private_gists")
    @Expose
    private Integer privateGists;

    @SerializedName("collaborators")
    @Expose
    private Integer collaborators;

    @SerializedName("bio")
    @Expose
    private String bio;

    @SerializedName("blog")
    @Expose
    private String blog;


    @SerializedName("hireable")
    @Expose
    private boolean hireable;

    public String getBlog() {
        return blog;
    }

    public void setBlog(String blog) {
        this.blog = blog;
    }

    public boolean isHireable() {
        return hireable;
    }

    public void setHireable(boolean hireable) {
        this.hireable = hireable;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getGravatarId() {
        return gravatarId;
    }

    public void setGravatarId(String gravatarId) {
        this.gravatarId = gravatarId;
    }

    public String getHtmlUrl() {
        return htmlUrl;
    }

    public void setHtmlUrl(String htmlUrl) {
        this.htmlUrl = htmlUrl;
    }

    public String getFollowersUrl() {
        return followersUrl;
    }

    public void setFollowersUrl(String followersUrl) {
        this.followersUrl = followersUrl;
    }

    public String getFollowingUrl() {
        return followingUrl;
    }

    public void setFollowingUrl(String followingUrl) {
        this.followingUrl = followingUrl;
    }

    public String getGistsUrl() {
        return gistsUrl;
    }

    public void setGistsUrl(String gistsUrl) {
        this.gistsUrl = gistsUrl;
    }

    public String getStarredUrl() {
        return starredUrl;
    }

    public void setStarredUrl(String starredUrl) {
        this.starredUrl = starredUrl;
    }

    public String getSubscriptionsUrl() {
        return subscriptionsUrl;
    }

    public void setSubscriptionsUrl(String subscriptionsUrl) {
        this.subscriptionsUrl = subscriptionsUrl;
    }

    public String getOrganizationsUrl() {
        return organizationsUrl;
    }

    public void setOrganizationsUrl(String organizationsUrl) {
        this.organizationsUrl = organizationsUrl;
    }

    public String getReposUrl() {
        return reposUrl;
    }

    public void setReposUrl(String reposUrl) {
        this.reposUrl = reposUrl;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Integer getPublicRepos() {
        return publicRepos;
    }

    public void setPublicRepos(Integer publicRepos) {
        this.publicRepos = publicRepos;
    }

    public Integer getPublicGists() {
        return publicGists;
    }

    public void setPublicGists(Integer publicGists) {
        this.publicGists = publicGists;
    }

    public Integer getFollowers() {
        return followers;
    }

    public void setFollowers(Integer followers) {
        this.followers = followers;
    }

    public Integer getFollowing() {
        return following;
    }

    public void setFollowing(Integer following) {
        this.following = following;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Integer getTotalPrivateRepos() {
        return totalPrivateRepos;
    }

    public void setTotalPrivateRepos(Integer totalPrivateRepos) {
        this.totalPrivateRepos = totalPrivateRepos;
    }

    public Integer getOwnedPrivateRepos() {
        return ownedPrivateRepos;
    }

    public void setOwnedPrivateRepos(Integer ownedPrivateRepos) {
        this.ownedPrivateRepos = ownedPrivateRepos;
    }

    public Integer getPrivateGists() {
        return privateGists;
    }

    public void setPrivateGists(Integer private_gists) {
        this.privateGists = private_gists;
    }

    public Integer getCollaborators() {
        return collaborators;
    }

    public void setCollaborators(Integer collaborators) {
        this.collaborators = collaborators;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(android.os.Parcel dest, int flags) {
        dest.writeString(this.login);
        dest.writeValue(this.id);
        dest.writeString(this.avatarUrl);
        dest.writeString(this.gravatarId);
        dest.writeString(this.htmlUrl);
        dest.writeString(this.followersUrl);
        dest.writeString(this.followingUrl);
        dest.writeString(this.gistsUrl);
        dest.writeString(this.starredUrl);
        dest.writeString(this.subscriptionsUrl);
        dest.writeString(this.organizationsUrl);
        dest.writeString(this.reposUrl);
        dest.writeString(this.type);
        dest.writeString(this.name);
        dest.writeString(this.company);
        dest.writeString(this.location);
        dest.writeString(this.email);
        dest.writeValue(this.publicRepos);
        dest.writeValue(this.publicGists);
        dest.writeValue(this.followers);
        dest.writeValue(this.following);
        dest.writeString(this.createdAt);
        dest.writeString(this.updatedAt);
        dest.writeValue(this.totalPrivateRepos);
        dest.writeValue(this.ownedPrivateRepos);
        dest.writeValue(this.privateGists);
        dest.writeValue(this.collaborators);
        dest.writeString(this.bio);
        dest.writeString(this.blog);
        dest.writeByte(this.hireable ? (byte) 1 : (byte) 0);

    }

    protected User(android.os.Parcel in) {
        this.login = in.readString();
        this.id = (Integer) in.readValue(Integer.class.getClassLoader());
        this.avatarUrl = in.readString();
        this.gravatarId = in.readString();
        this.htmlUrl = in.readString();
        this.followersUrl = in.readString();
        this.followingUrl = in.readString();
        this.gistsUrl = in.readString();
        this.starredUrl = in.readString();
        this.subscriptionsUrl = in.readString();
        this.organizationsUrl = in.readString();
        this.reposUrl = in.readString();
        this.type = in.readString();
        this.name = in.readString();
        this.company = in.readString();
        this.location = in.readString();
        this.email = in.readString();
        this.publicRepos = (Integer) in.readValue(Integer.class.getClassLoader());
        this.publicGists = (Integer) in.readValue(Integer.class.getClassLoader());
        this.followers = (Integer) in.readValue(Integer.class.getClassLoader());
        this.following = (Integer) in.readValue(Integer.class.getClassLoader());
        this.createdAt = in.readString();
        this.updatedAt = in.readString();
        this.totalPrivateRepos = (Integer) in.readValue(Integer.class.getClassLoader());
        this.ownedPrivateRepos = (Integer) in.readValue(Integer.class.getClassLoader());
        this.privateGists = (Integer) in.readValue(Integer.class.getClassLoader());
        this.collaborators = (Integer) in.readValue(Integer.class.getClassLoader());
        this.bio = in.readString();
        this.blog = in.readString();
        this.hireable = in.readByte() != 0;
    }

    public static final Creator<User> CREATOR = new Creator<User>() {
        public User createFromParcel(android.os.Parcel source) {
            return new User(source);
        }

        public User[] newArray(int size) {
            return new User[size];
        }
    };
}
