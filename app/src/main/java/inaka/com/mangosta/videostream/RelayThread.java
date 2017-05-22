package inaka.com.mangosta.videostream;

import android.os.Looper;
import android.util.Log;

import org.ice4j.StunException;
import org.ice4j.StunMessageEvent;
import org.ice4j.Transport;
import org.ice4j.TransportAddress;
import org.ice4j.attribute.Attribute;
import org.ice4j.attribute.DataAttribute;
import org.ice4j.attribute.ErrorCodeAttribute;
import org.ice4j.attribute.NonceAttribute;
import org.ice4j.attribute.XorPeerAddressAttribute;
import org.ice4j.attribute.XorRelayedAddressAttribute;
import org.ice4j.message.Indication;
import org.ice4j.message.Message;
import org.ice4j.message.MessageFactory;
import org.ice4j.message.Request;
import org.ice4j.stack.MessageEventHandler;
import org.ice4j.stack.TransactionID;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import inaka.com.mangosta.realm.RealmManager;

/**
 * Created by rafalslota on 26/04/2017.
 */
public class RelayThread extends Thread implements MessageEventHandler, NewPeerHandler {

    private static final String TAG = "RelayThread";

    private final static String REALM = "ovh";
    private final static String USERNAME = "username";
    private final static String PASSWORD = "Zd5Pb2O2";

    private boolean running = true;
    private boolean allocated = false;
    private RelayDataHandler dataHandler = null;
    private TransportAddress lastPeerAddr = null;
    private TransportAddress relayAddr = null;

    private List<String> allowedPeers;
    private IceClient iceClient;

    public RelayThread() throws SocketException {
    }

    public boolean send(String peerAddr, int peerPort, byte[] data) {
        TransportAddress peer = new TransportAddress(peerAddr, peerPort, Transport.UDP);
        TransactionID tid = TransactionID.createNewTransactionID();
        Indication sendIndication = MessageFactory.createSendIndication(peer, data, tid.getBytes());
        try {
            sendIndication.setTransactionID(tid.getBytes());
            iceClient.sendIndication(sendIndication);

            return true;
        } catch (StunException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void onData(RelayDataHandler handler) {
        this.dataHandler = handler;
    }

    @Override
    public void run() {

        try {
            initIce();
        } catch (SocketException e) {
            e.printStackTrace();
        }

        while(running) {
            try {
                Thread.sleep(1000);
                if(!isAllocated()) { // Not allocated -> allocate
                    allocated = allocate(3);
                }

                if(isAllocated()) { // Allocated -> preserve only if refresh works
                    allocated = refresh(3);
                }

                if(!isAllocated()) { // Allocation or refresh failed -> retry
                    continue;
                }

                if(isAllocated()) { // Still allocated -> refresh / create permissions
                    for(String peerAddr: allowedPeers) {
                        create_permission(peerAddr);
                    }
                }
            } catch (IllegalArgumentException | InterruptedException e) {
                e.printStackTrace();
            }

            try {
                Thread.sleep(5000 + new Random().nextInt(5000)); // A bit spam-ish, but well... whatever, it's just for the demo
            } catch (InterruptedException e) {
                running = false;
                e.printStackTrace();
            }
        }
    }

    private void initIce() throws SocketException {
        this.iceClient = new IceClient(RealmManager.getInstance().getIceConfiguration(), this);
        allowedPeers = Collections.synchronizedList(new ArrayList<String>());
    }

    private int handleError(Message message) {
        ErrorCodeAttribute e = (ErrorCodeAttribute) message.getAttribute(Attribute.ERROR_CODE);
        if(e != null) {
            Log.w(TAG, "TURN error for request " + message.getName() + ": " + ErrorCodeAttribute.getDefaultReasonPhrase(e.getErrorCode()));
            NonceAttribute nonce = (NonceAttribute) message.getAttribute(Attribute.NONCE);
            if (nonce != null)
                iceClient.getAuthSession().setNonce(nonce.getNonce());

            return (int) e.getErrorCode();
        } else {
            Log.w(TAG, "TURN request " + message.getName() + " successfully processed");
            return 0;
        }
    }

    private boolean refresh(int tries) {
        if(tries <= 0)
            return false;

        try {
            Request request = MessageFactory.createRefreshRequest();
            StunMessageEvent event = iceClient.sendRequestAndWaitForResponse(request);
            if(event == null)
                return refresh(tries - 1);

            Message message = event.getMessage();

            return handleError(message) == 0 || refresh(tries - 1);
        } catch (StunException | IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    private boolean create_permission(String peerAddr) {

        try {
            TransportAddress fromAddr = new TransportAddress(peerAddr, 0, Transport.UDP);
            TransactionID tid = TransactionID.createNewTransactionID();
            Request request = MessageFactory.createCreatePermissionRequest(fromAddr, tid.getBytes());

            StunMessageEvent event = iceClient.sendRequestAndWaitForResponse(request, tid);
            if(event == null)
                return false;

            Message message = event.getMessage();

            return handleError(message) == 0;
        } catch (StunException | IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    private boolean allocate(int tries) {
        if(tries <= 0)
            return false;

        try {
            Request request = MessageFactory.createAllocateRequest((byte) 17, false);

            StunMessageEvent event = iceClient.sendRequestAndWaitForResponse(request);
            if(event == null)
                return allocate(tries - 1);

            Message message = event.getMessage();

            int errorCode = handleError(message);
            if(errorCode > 0) {
                if(errorCode == ErrorCodeAttribute.ALLOCATION_MISMATCH) {
                    iceClient.setupSocket();
                }

                return allocate(tries - 1);
            } else {
                XorRelayedAddressAttribute relayAddrAttr = (XorRelayedAddressAttribute) message.getAttribute(Attribute.XOR_RELAYED_ADDRESS);
                relayAddr = relayAddrAttr.getAddress(message.getTransactionID());
                return true;
            }
        } catch (StunException | IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public void handleMessageEvent(StunMessageEvent event) {
        Message message = event.getMessage();
        if(message.getMessageType() == Message.DATA_INDICATION) {
            DataAttribute data = (DataAttribute) message.getAttribute(Attribute.DATA);
            XorPeerAddressAttribute peer = (XorPeerAddressAttribute) message.getAttribute(Attribute.XOR_PEER_ADDRESS);
            TransportAddress peerAddr = peer.getAddress(message.getTransactionID());
            lastPeerAddr = peerAddr;
            dataHandler.handleData(peerAddr.getHostAddress(), peerAddr.getPort(), data.getData());
        }
    }

    public TransportAddress getLastPeerAddr() {
        return lastPeerAddr;
    }

    public boolean isAllocated() {
        return allocated;
    }

    public TransportAddress getRelayAddr() {
        return relayAddr;
    }

    @Override
    public void onNewPeerDiscovered(String peerAddr) {
        Log.d(TAG, "NewPeerDiscovered: " + peerAddr);
        allowedPeers.add(peerAddr);
    }
}
