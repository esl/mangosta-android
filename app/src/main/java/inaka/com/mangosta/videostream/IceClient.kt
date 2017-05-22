package inaka.com.mangosta.videostream

import android.util.Log

import org.ice4j.ResponseCollector
import org.ice4j.StunException
import org.ice4j.StunMessageEvent
import org.ice4j.StunResponseEvent
import org.ice4j.StunTimeoutEvent
import org.ice4j.Transport
import org.ice4j.TransportAddress
import org.ice4j.message.Indication
import org.ice4j.message.Request
import org.ice4j.security.LongTermCredential
import org.ice4j.security.LongTermCredentialSession
import org.ice4j.socket.IceUdpSocketWrapper
import org.ice4j.stack.MessageEventHandler
import org.ice4j.stack.StunStack
import org.ice4j.stack.TransactionID
import org.ice4j.stunclient.BlockingRequestSender

import java.io.IOException
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.net.SocketException

import inaka.com.mangosta.models.IceConfiguration

class IceClient @Throws(SocketException::class)
constructor(private val conf: IceConfiguration, private val handler: MessageEventHandler?) : ResponseCollector {
    private var serverAddr: TransportAddress? = null
    private val stack: StunStack = StunStack()
    private var blockingClient: BlockingRequestSender? = null
    private var credentials: LongTermCredential? = null
    var authSession: LongTermCredentialSession? = null
        private set
    private var localAddr: TransportAddress? = null
    private var socket: DatagramSocket? = null

    init {
        // Init stun stack
        setupSocket()
        reloadConfiguration()
    }

    private fun reloadConfiguration() {
        conf.load()

        // Init addresses
        serverAddr = TransportAddress(conf.turnAddress, conf.turnPort, Transport.UDP)

        credentials = LongTermCredential(conf.turnUsername, conf.turnPassword)

        if (authSession != null)
            stack.credentialsManager.unregisterAuthority(authSession)

        authSession = LongTermCredentialSession(credentials, conf.turnRealm.toByteArray())
        authSession?.nonce = "nonce".toByteArray()

        stack.credentialsManager.registerAuthority(authSession)
    }


    @Throws(SocketException::class)
    internal fun setupSocket() {
        val newSocket = DatagramSocket(InetSocketAddress("0.0.0.0", 0))
        val newLocalAddr = TransportAddress("::", newSocket.localPort, Transport.UDP)

        if (this.localAddr != null) {
            stack.removeSocket(this.localAddr)
            stack.removeIndicationListener(this.localAddr, null)
        }

        this.socket = newSocket
        this.localAddr = newLocalAddr
        stack.addSocket(IceUdpSocketWrapper(this.socket))
        if (handler != null)
            stack.addIndicationListener(this.localAddr, handler)

        blockingClient = BlockingRequestSender(stack, localAddr)

        Log.d(TAG, "New relay local socket created at: " + newSocket.localPort)
    }

    @Throws(StunException::class)
    fun sendIndication(sendIndication: Indication) {
        stack.sendIndication(sendIndication, serverAddr, localAddr)
    }

    @Throws(IOException::class, StunException::class)
    fun sendRequestAndWaitForResponse(request: Request): StunMessageEvent? {
        authSession?.addAttributes(request)
        return blockingClient?.sendRequestAndWaitForResponse(request, serverAddr)
    }

    @Throws(IOException::class, StunException::class)
    fun sendRequestAndWaitForResponse(request: Request, tid: TransactionID): StunMessageEvent? {
        authSession?.addAttributes(request)
        return blockingClient?.sendRequestAndWaitForResponse(request, serverAddr, tid)
    }

    @Throws(IOException::class)
    fun sendRequest(request: Request) {
        authSession?.addAttributes(request)
        stack.sendRequest(request, serverAddr, localAddr, this)
    }

    override fun processResponse(stunResponseEvent: StunResponseEvent) {

    }

    override fun processTimeout(stunTimeoutEvent: StunTimeoutEvent) {

    }

    companion object {
        private val TAG = IceClient.toString()
    }
}
