package inaka.com.mangosta.models.requests;

public class CreateMessageRequest {

    String to;
    String body;

    public CreateMessageRequest(String to, String body) {
        this.to = to;
        this.body = body;
    }

}
