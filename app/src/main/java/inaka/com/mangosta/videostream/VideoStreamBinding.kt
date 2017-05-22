package inaka.com.mangosta.videostream

import android.app.Activity
import android.content.Context
import android.support.v7.app.AlertDialog
import android.util.Log
import android.util.Pair
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast

import org.ice4j.TransportAddress
import org.jivesoftware.smack.SmackException
import org.jivesoftware.smack.StanzaListener
import org.jivesoftware.smack.filter.StanzaFilter
import org.jivesoftware.smack.packet.Message
import org.jivesoftware.smack.packet.Stanza
import org.jxmpp.stringprep.XmppStringprepException

import java.util.Collections
import java.util.HashSet

import inaka.com.mangosta.R
import inaka.com.mangosta.xmpp.XMPPSession

class VideoStreamBinding(private val proxyRTP: ProxyRTPServer, activity: Activity) : BindingConfirmator, StanzaListener {
    val userInterface: UserInterface
    private val newPeerHandler: NewPeerHandler
    private val currentPeers: MutableSet<String>

    init {
        userInterface = UserInterface(activity)
        newPeerHandler = proxyRTP
        currentPeers = Collections.synchronizedSet(HashSet<String>())
    }

    fun startBinding() {
        userInterface.showStreamFromAlert(this)
    }

    private fun sendBinding(jid: String, relays: Pair<TransportAddress, TransportAddress>) {
        userInterface.debugToast("JID: " + jid + " && Data: " + relays.first.hostAddress + ":" +
                relays.first.port + " && Control: " + relays.second.hostAddress +
                ":" + relays.second.port)

        Log.d(TAG, "Sending streamTo: " + relays.first.hostAddress + ":" +
                relays.first.port + " && Control: " + relays.second.hostAddress +
                ":" + relays.second.port)


        if (!XMPPSession.isInstanceNull() && XMPPSession.getInstance().isConnectedAndAuthenticated) {
            val xmpp = XMPPSession.getInstance()
            try {
                val messageText = relays.first.hostAddress + ":" +
                        relays.first.port + ";" + relays.second.hostAddress +
                        ":" + relays.second.port

                for (oldPeer in currentPeers) {
                    Log.d(TAG, "Sending STOP to: " + oldPeer)
                    xmpp.sendStanza(Message(oldPeer, "stop"))
                }

                xmpp.sendStanza(Message(jid, messageText))
                xmpp.xmppConnection.addAsyncStanzaListener(this, StanzaFilter { stanza -> stanza.javaClass == Message::class.java && stanza.from.asBareJid().toString().equals(jid, ignoreCase = true) })
            } catch (e: SmackException.NotConnectedException) {
                e.printStackTrace()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            } catch (e: XmppStringprepException) {
                e.printStackTrace()
            }

        } else {
            Log.e(TAG, "No XMPP session!")
        }
    }

    override fun confirmBinding(jid: String) {
        sendBinding(jid, proxyRTP.relayAddrs)
    }

    @Throws(SmackException.NotConnectedException::class, InterruptedException::class)
    override fun processPacket(stanza: Stanza) {
        val msg = stanza as Message
        Log.d(TAG, "stanza received: " + msg.type.toString() + " " + msg.body + " " + msg.toString())

        if (msg.type == Message.Type.normal) {
            val peer = msg.body.trim { it <= ' ' }
            newPeerHandler.onNewPeerDiscovered(peer)
            currentPeers.add(stanza.getFrom().toString())
        }
    }

    inner class UserInterface(private val activity: Activity) {
        private var lastJID = ""

        fun debugToast(s: String) {
            Toast.makeText(activity, s, Toast.LENGTH_LONG).show()
        }

        fun showNotReadyError() {
            val builder = AlertDialog.Builder(activity, R.style.AlertDialogCustom)
            builder.setTitle("VideoStream not ready")
            builder.setMessage("TURN data relay is not ready yet. Please try again later.")

            builder.setNeutralButton("Continue") { dialog, which -> dialog.cancel() }

            val alert = builder.create()
            alert.show()
        }

        fun showStreamFromAlert(confirmator: BindingConfirmator) {
            val builder = AlertDialog.Builder(activity, R.style.AlertDialogCustom)
            builder.setTitle("Enter JID to stream from:")

            val input = EditText(activity)
            input.setText(lastJID)
            val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT)
            input.layoutParams = lp
            builder.setView(input)

            builder.setPositiveButton("Stream!") { dialog, which ->
                dialog.cancel()
                val jid = input.text.toString().trim { it <= ' ' }
                lastJID = jid
                confirmator.confirmBinding(jid)
            }

            builder.setNegativeButton("Cancel") { dialog, which -> dialog.cancel() }

            builder.create().show()
        }
    }

    companion object {
        private val TAG = "VideoStreamBinding"
    }
}
