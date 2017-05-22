package inaka.com.mangosta.videostream;

import android.os.AsyncTask;
import android.util.Log;

import org.ice4j.ResponseCollector;
import org.ice4j.StunException;
import org.ice4j.StunMessageEvent;
import org.ice4j.StunResponseEvent;
import org.ice4j.StunTimeoutEvent;
import org.ice4j.Transport;
import org.ice4j.TransportAddress;
import org.ice4j.message.Indication;
import org.ice4j.message.Request;
import org.ice4j.security.LongTermCredential;
import org.ice4j.security.LongTermCredentialSession;
import org.ice4j.socket.IceUdpSocketWrapper;
import org.ice4j.stack.MessageEventHandler;
import org.ice4j.stack.StunStack;
import org.ice4j.stack.TransactionID;
import org.ice4j.stunclient.BlockingRequestSender;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;

import inaka.com.mangosta.models.IceConfiguration;
import io.realm.RealmChangeListener;
import io.realm.RealmModel;

/**
 * Created by rafalslota on 22/05/2017.
 */

public class IceClient implements ResponseCollector {
    private static final String TAG = "IceClient";
    private TransportAddress serverAddr;
    private final StunStack stack;
    private final BlockingRequestSender blockingClient;
    private LongTermCredential credentials;
    private LongTermCredentialSession authSession;
    private final IceConfiguration conf;
    private TransportAddress localAddr;
    private DatagramSocket socket;
    private MessageEventHandler handler;

    public IceClient(IceConfiguration conf, MessageEventHandler handler) throws SocketException {
        this.handler = handler;
        this.conf = conf;

        // Init stun stack
        stack = new StunStack();
        setupSocket();
        blockingClient = new BlockingRequestSender(stack, localAddr);
        stack.addSocket(new IceUdpSocketWrapper(socket));

        reloadConfiguration();
    }

    private void reloadConfiguration() {
        conf.load();

        // Init addresses
        serverAddr = new TransportAddress(conf.getTurnAddress(), conf.getTurnPort(), Transport.UDP);

        Log.e("OMG", conf.getTurnRealm() + " " + conf.getTurnRealm().length());
        Log.e("OMG", conf.getTurnUsername() + " " + conf.getTurnUsername().length());
        credentials = new LongTermCredential(conf.getTurnUsername(), conf.getTurnPassword());

        if(authSession != null)
            stack.getCredentialsManager().unregisterAuthority(authSession);

        authSession = new LongTermCredentialSession(credentials, conf.getTurnRealm().getBytes());
        authSession.setNonce("nonce".getBytes());

        stack.getCredentialsManager().registerAuthority(authSession);
    }



    void setupSocket() throws SocketException {
        DatagramSocket newSocket = new DatagramSocket(new InetSocketAddress("0.0.0.0", 0));
        TransportAddress newLocalAddr = new TransportAddress("::", newSocket.getLocalPort(), Transport.UDP);

        if(this.localAddr != null) {
            stack.removeSocket(this.localAddr);
            stack.removeIndicationListener(this.localAddr, null);
        }

        this.socket = newSocket;
        this.localAddr = newLocalAddr;
        stack.addSocket(new IceUdpSocketWrapper(this.socket));
        if(handler != null)
            stack.addIndicationListener(this.localAddr, handler);

        Log.d(TAG, "New relay local socket created at: " + newSocket.getLocalPort());
    }

    private StunStack getStack() {
        return stack;
    }

    private TransportAddress getServerAddr() {
        return serverAddr;
    }

    public LongTermCredentialSession getAuthSession() {
        return authSession;
    }

    private BlockingRequestSender getBlockingClient() {
        return blockingClient;
    }

    public void sendIndication(Indication sendIndication) throws StunException {
        getStack().sendIndication(sendIndication, getServerAddr(), localAddr);
    }

    public StunMessageEvent sendRequestAndWaitForResponse(Request request) throws IOException, StunException {
        getAuthSession().addAttributes(request);
        return getBlockingClient().sendRequestAndWaitForResponse(request, serverAddr);
    }

    public StunMessageEvent sendRequestAndWaitForResponse(Request request, TransactionID tid) throws IOException, StunException {
        getAuthSession().addAttributes(request);
        return getBlockingClient().sendRequestAndWaitForResponse(request, serverAddr, tid);
    }

    public void sendRequest(Request request) throws IOException {
        getStack().sendRequest(request, serverAddr, localAddr, this);
    }

    @Override
    public void processResponse(StunResponseEvent stunResponseEvent) {

    }

    @Override
    public void processTimeout(StunTimeoutEvent stunTimeoutEvent) {

    }
}
