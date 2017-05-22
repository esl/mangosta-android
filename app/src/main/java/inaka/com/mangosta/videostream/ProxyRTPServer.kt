package inaka.com.mangosta.videostream

import android.util.Log
import android.util.Pair

import org.ice4j.TransportAddress

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.net.SocketException

class ProxyRTPServer @Throws(SocketException::class)
constructor() : Thread(), NewPeerHandler {
    private val localSockData: DatagramSocket = DatagramSocket(0)
    private val localSockControl: DatagramSocket = DatagramSocket(0)

    private val dataRelay: RelayThread = RelayThread()
    private val controlRelay: RelayThread = RelayThread()
    private val dataReceiver: DataReceiverUDP
    private val controlReceiver: DataReceiverUDP
    private var running = true

    companion object {
        private val TAG: String = ProxyRTPServer.toString()
    }

    init {
        dataRelay.onData { _: String, _: Int, data: ByteArray ->
            val p = DatagramPacket(data, data.size)
            localSockData.send(p)
        }

        controlRelay.onData {_: String, _: Int, data: ByteArray ->
            val p = DatagramPacket(data, data.size)
            localSockControl.send(p)
        }

        dataReceiver = DataReceiverUDP(localSockData, dataRelay, dataRelay)
        controlReceiver = DataReceiverUDP(localSockControl, controlRelay, dataRelay)

        Log.i(TAG, "ProxyRTP initialized. Listen on: " + localSockData.localPort + "/" + localSockControl.localPort)
    }

    fun getServerSockPorts(): Pair<Int, Int> {
        return Pair(localSockData.localPort, localSockControl.localPort)
    }

    override fun run() {
        try {
            localSockData.connect(InetSocketAddress("127.0.0.1", 5006))
            localSockControl.connect(InetSocketAddress("127.0.0.1", 5007))

            dataRelay.start()
            controlRelay.start()

            dataReceiver.start()
            controlReceiver.start()
        } catch (e: SocketException) {
            e.printStackTrace()
        }

    }

    val isRelayReady: Boolean
        get() = dataRelay.isAllocated && controlRelay.isAllocated

    val relayAddrs: Pair<TransportAddress, TransportAddress>
        get() = Pair<TransportAddress, TransportAddress>(dataRelay.relayAddr, controlRelay.relayAddr)

    override fun onNewPeerDiscovered(peerAddr: String) {
        dataRelay.onNewPeerDiscovered(peerAddr)
        controlRelay.onNewPeerDiscovered(peerAddr)
    }

    fun shutdown() {
        dataRelay.shutdown()
        controlRelay.shutdown()
        dataReceiver.shutdown()
        controlReceiver.shutdown()
        running = false
        interrupt()

        localSockControl.close()
        localSockData.close()
    }
}
