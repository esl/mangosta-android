package inaka.com.mangosta.models.requests;

public class CreateMUCLightRequest {

    String subject;
    String name;

    public CreateMUCLightRequest(String subject, String name) {
        this.subject = subject;
        this.name = name;
    }

}
