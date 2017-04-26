package inaka.com.mangosta.videostream;

/**
 * Created by rafalslota on 26/04/2017.
 */
public abstract class RelayDataHandler {
    public abstract void handleData(String peerAddr, int peerPort, byte[] data);
}
