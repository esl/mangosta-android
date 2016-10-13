package inaka.com.mangosta.chat;

public class ChatConnection {
    public enum ChatConnectionStatus {
        Connecting,
        Connected,
        Authenticated,
        Disconnected
    }

    private ChatConnectionStatus mStatus;

    public ChatConnection(ChatConnectionStatus status) {
        this.mStatus = status;
    }

    public ChatConnectionStatus getStatus() {
        return mStatus;
    }
}
