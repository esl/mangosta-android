package inaka.com.mangosta.videostream

/**
 * Created by rafalslota on 27/04/2017.
 */
internal interface NewPeerHandler {
    fun onNewPeerDiscovered(peerAddr: String)
}
