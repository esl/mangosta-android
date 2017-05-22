package inaka.com.mangosta.videostream

import android.util.Log
import org.ice4j.TransportAddress
import java.io.IOException
import java.io.InterruptedIOException
import java.net.DatagramPacket
import java.net.DatagramSocket

internal class DataReceiverUDP(private val socket: DatagramSocket, private val relay: RelayThread,
                               private val dataRelay: RelayThread) : Thread() {
    private var running = true
        get() = field && !isInterrupted

    companion object {
        private val TAG: String = DataReceiverUDP.toString()
        private val BUFF_MAX_SIZE: Int = 1024 * 1024
    }

    override fun run() {
        val buf = ByteArray(BUFF_MAX_SIZE)
        while (running) {
            val p = DatagramPacket(buf, buf.size)
            try {
                socket.receive(p)
                Log.d(TAG, "Received " + " " + p.offset + " " + p.length)
                val peerAddr = getPeerAddr() ?: continue
                val sendBuf = ByteArray(p.length)
                System.arraycopy(buf, p.offset, sendBuf, 0, p.length)

                Log.d(TAG, "Received " + " " + sendBuf.toString() + " " + sendBuf.size)
                relay.send(peerAddr.hostAddress, peerAddr.port, sendBuf)
            } catch (e: InterruptedIOException) {
                running = false
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    // If there's no lastPeerAddr on mirrored relay, try to use the port number
    // from data relay + 1. It's just a guess, but in most cases will just work
    private fun  getPeerAddr(): TransportAddress? {
        if (relay.lastPeerAddr != null)
            return relay.lastPeerAddr

        if(dataRelay.lastPeerAddr != null) {
            val last = dataRelay.lastPeerAddr as TransportAddress
            return TransportAddress(last.address, last.port + 1, last.transport)
        }

        return null;
    }


    fun shutdown() {
        running = false
        interrupt()
    }
}