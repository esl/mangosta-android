package inaka.com.mangosta.activities

import android.app.Activity
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.Toast

import org.ice4j.StunException
import org.ice4j.attribute.Attribute
import org.ice4j.attribute.ErrorCodeAttribute
import org.ice4j.attribute.NonceAttribute
import org.ice4j.message.MessageFactory

import java.io.IOException

import inaka.com.mangosta.R
import inaka.com.mangosta.models.IceConfiguration
import inaka.com.mangosta.realm.RealmManager
import inaka.com.mangosta.videostream.IceClient

import kotlinx.android.synthetic.main.activity_configure_ice.*

class ConfigureIceActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_configure_ice)

        setSupportActionBar(toolbar)
        toolbar?.setNavigationIcon(R.drawable.ic_arrow_back)

        buttonTestConnection.setOnClickListener { view -> testConnection(view) }
        buttonSaveConfiguration.setOnClickListener { saveAndExit() }
        buttonResetConfiguration.setOnClickListener { reloadTextFields() }
    }

    internal fun testConnection(view: View) {
        val task = object : AsyncTask<Void, Void, Void>() {
            override fun doInBackground(vararg params: Void): Void? {
                val button = view as Button
                val buttonPrevText = button.text.toString()
                runOnUiThread {
                    button.isEnabled = false
                    button.text = getString(R.string.connecting)
                }

                doCheckIceConnectivity()

                runOnUiThread {
                    button.isEnabled = true
                    button.text = buttonPrevText
                }

                return null
            }
        }

        task.execute()
    }

    internal fun saveAndExit() {
        saveCurrentConfiguration()
        setResult(Activity.RESULT_OK, Intent())
        this@ConfigureIceActivity.finish()
    }

    private fun doCheckIceConnectivity() {
        try {
            val localConf = IceConfiguration()
            loadConfigurationFromForm(localConf)
            val iceClient = IceClient(localConf, null)

            val e = testAllocate(iceClient, 3)
            if (e == null) {
                toast("Connected successfully to " + localConf.turnRealm + "!")
            } else {
                toast("Error: " + ErrorCodeAttribute.getDefaultReasonPhrase(e.errorCode))
            }
        } catch (e: Exception) {
            toast("Error: " + e.toString())
            e.printStackTrace()
        }

    }

    @Throws(IOException::class, StunException::class)
    private fun testAllocate(iceClient: IceClient, tries: Int): ErrorCodeAttribute? {
        if (tries <= 0) {
            throw RuntimeException("unable to negotiate STUN nonce")
        }

        val request = MessageFactory.createAllocateRequest(17.toByte(), false)
        val event = iceClient.sendRequestAndWaitForResponse(request) ?: throw RuntimeException("STUN request timeout")

        val message = event.message
        val e = message.getAttribute(Attribute.ERROR_CODE) as ErrorCodeAttribute?
        if (e != null) {
            val nonce = message.getAttribute(Attribute.NONCE) as NonceAttribute?
            if (nonce != null)
                iceClient.authSession?.nonce = nonce.nonce

            if (e.errorCode == ErrorCodeAttribute.STALE_NONCE) {
                return testAllocate(iceClient, tries - 1)
            }
        } else {
            val cancelAllocation = MessageFactory.createRefreshRequest(0)
            iceClient.sendRequest(cancelAllocation)
        }

        return e
    }

    private fun toast(msg: String) {
        runOnUiThread { Toast.makeText(this@ConfigureIceActivity, msg, Toast.LENGTH_LONG).show() }
    }

    private fun loadConfigurationFromForm(conf: IceConfiguration) {
        conf.turnAddress = turnAddress.text.toString().trim { it <= ' ' }
        conf.turnPort = Integer.parseInt(turnPort.text.toString().trim { it <= ' ' })
        conf.turnRealm = turnRealm.text.toString().trim { it <= ' ' }
        conf.turnUsername = turnUsername.text.toString().trim { it <= ' ' }
        conf.turnPassword = turnPassword.text.toString().trim { it <= ' ' }
    }

    private fun saveCurrentConfiguration() {
        try {
            val realm = RealmManager.getInstance().realm
            val conf = configuration

            realm.beginTransaction()

            loadConfigurationFromForm(conf)

            realm.commitTransaction()
            realm.close()

            reloadTextFields()
        } catch (e: NumberFormatException) {
            e.printStackTrace()
        }

    }

    override fun onResume() {
        super.onResume()

        reloadTextFields()
    }


    private fun reloadTextFields() {
        val conf = configuration
        turnAddress.setText(conf.turnAddress)
        turnPort.setText(conf.turnPort.toString())
        turnRealm.setText(conf.turnRealm)
        turnUsername.setText(conf.turnUsername)
        turnPassword.setText(conf.turnPassword)
    }

    private val configuration: IceConfiguration
        get() = RealmManager.getInstance().iceConfiguration

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }

        return true
    }

}
