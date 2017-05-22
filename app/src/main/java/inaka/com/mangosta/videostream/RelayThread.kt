package inaka.com.mangosta.videostream

import android.util.Log

import org.ice4j.StunException
import org.ice4j.StunMessageEvent
import org.ice4j.Transport
import org.ice4j.TransportAddress
import org.ice4j.attribute.Attribute
import org.ice4j.attribute.DataAttribute
import org.ice4j.attribute.ErrorCodeAttribute
import org.ice4j.attribute.NonceAttribute
import org.ice4j.attribute.XorPeerAddressAttribute
import org.ice4j.attribute.XorRelayedAddressAttribute
import org.ice4j.message.Message
import org.ice4j.message.MessageFactory
import org.ice4j.stack.MessageEventHandler
import org.ice4j.stack.TransactionID

import java.io.IOException
import java.net.SocketException
import java.util.ArrayList
import java.util.Collections
import java.util.Random

import inaka.com.mangosta.realm.RealmManager

internal class RelayThread @Throws(SocketException::class)
constructor() : Thread(), MessageEventHandler, NewPeerHandler {
    private var running = true
        get() = field && !isInterrupted

    var isAllocated = false
        private set
    private var dataHandler: ReplayDataHandler? = null
    var lastPeerAddr: TransportAddress? = null
        private set
    var relayAddr: TransportAddress? = null
        private set

    private var allowedPeers: MutableList<String> = Collections.synchronizedList(ArrayList<String>())
    private var iceClient: IceClient? = null

    fun send(peerAddr: String, peerPort: Int, data: ByteArray): Boolean {
        val peer = TransportAddress(peerAddr, peerPort, Transport.UDP)
        val tid = TransactionID.createNewTransactionID()
        val sendIndication = MessageFactory.createSendIndication(peer, data, tid.bytes)
        try {
            sendIndication.transactionID = tid.bytes
            iceClient?.sendIndication(sendIndication)

            return true
        } catch (e: StunException) {
            e.printStackTrace()
        }

        return false
    }

    fun onData(handler: ReplayDataHandler) {
        this.dataHandler = handler
    }



    override fun run() {
        while (running) {
            try {
                iceClient ?: initIceClient()

                Thread.sleep(1000)
                if (!isAllocated) { // Not allocated -> allocate
                    isAllocated = allocate(3)
                }

                if (isAllocated) { // Allocated -> preserve only if refresh works
                    isAllocated = refresh(3)
                }

                if (!isAllocated) { // Allocation or refresh failed -> retry
                    continue
                }

                if (isAllocated) { // Still allocated -> refresh / create permissions
                    for (peerAddr in allowedPeers) {
                        create_permission(peerAddr)
                    }
                }
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }

            try {
                Thread.sleep((5000 + Random().nextInt(5000)).toLong()) // A bit spam-ish, but well... whatever, it's just for the demo
            } catch (e: InterruptedException) {
                running = false
                e.printStackTrace()
            }

        }
    }

    fun shutdown() {
        running = false
        interrupt()
    }

    @Throws(SocketException::class)
    private fun initIceClient() {
        this.iceClient = IceClient(RealmManager.getInstance().iceConfiguration, this)
        allowedPeers.clear()
    }

    private fun handleError(message: Message): Int {
        val e = message.getAttribute(Attribute.ERROR_CODE) as ErrorCodeAttribute?
        if (e != null) {
            Log.w(TAG, "TURN error for request " + message.name + ": " + ErrorCodeAttribute.getDefaultReasonPhrase(e.errorCode))
            val nonce = message.getAttribute(Attribute.NONCE) as NonceAttribute?
            if (nonce != null)
                iceClient?.authSession?.nonce = nonce.nonce

            return e.errorCode.toInt()
        } else {
            Log.w(TAG, "TURN request " + message.name + " successfully processed")
            return 0
        }
    }

    private fun refresh(tries: Int): Boolean {
        if (tries <= 0)
            return false

        try {
            val request = MessageFactory.createRefreshRequest()
            val event = iceClient?.sendRequestAndWaitForResponse(request) ?: return refresh(tries - 1)

            val message = event.message

            return handleError(message) == 0 || refresh(tries - 1)
        } catch (e: StunException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return false
    }

    private fun create_permission(peerAddr: String): Boolean {

        try {
            val fromAddr = TransportAddress(peerAddr, 0, Transport.UDP)
            val tid = TransactionID.createNewTransactionID()
            val request = MessageFactory.createCreatePermissionRequest(fromAddr, tid.bytes)

            val event = iceClient?.sendRequestAndWaitForResponse(request, tid) ?: return false

            val message = event.message

            return handleError(message) == 0
        } catch (e: StunException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return false
    }

    private fun allocate(tries: Int): Boolean {
        if (tries <= 0)
            return false

        try {
            val request = MessageFactory.createAllocateRequest(17.toByte(), false)

            val event = iceClient?.sendRequestAndWaitForResponse(request) ?: return allocate(tries - 1)

            val message = event.message

            val errorCode = handleError(message)
            if (errorCode > 0) {
                if (errorCode == ErrorCodeAttribute.ALLOCATION_MISMATCH.toInt()) {
                    iceClient?.setupSocket()
                }

                return allocate(tries - 1)
            } else {
                val relayAddrAttr = message.getAttribute(Attribute.XOR_RELAYED_ADDRESS) as XorRelayedAddressAttribute?
                relayAddr = relayAddrAttr?.getAddress(message.transactionID) ?: relayAddr
                return true
            }
        } catch (e: StunException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return false
    }

    override fun handleMessageEvent(event: StunMessageEvent) {
        val message = event.message
        if (message.messageType == Message.DATA_INDICATION) {
            val data = message.getAttribute(Attribute.DATA) as DataAttribute? ?: return
            val peer = message.getAttribute(Attribute.XOR_PEER_ADDRESS) as XorPeerAddressAttribute?
            val peerAddr = peer?.getAddress(message.transactionID) ?: return
            lastPeerAddr = peerAddr
            dataHandler?.let { it(peerAddr.hostAddress, peerAddr.port, data.data) }
        }
    }

    override fun onNewPeerDiscovered(peerAddr: String) {
        Log.d(TAG, "NewPeerDiscovered: " + peerAddr)
        allowedPeers.add(peerAddr)
    }

    companion object {
        private val TAG = RelayThread.toString()
    }
}
