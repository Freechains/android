package org.freechains.android

import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.freechains.common.*
import org.freechains.platform.fsRoot
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.net.Socket
import kotlin.concurrent.thread

const val T5m_sync = 30*hour

class MainActivity : AppCompatActivity ()
{
    var isActive = true
    override fun onPause()  { super.onPause()  ; this.isActive=false }
    override fun onResume() { super.onResume() ; this.isActive=true  }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.act_main)
        findNavController(R.id.nav_host_fragment).let {
            this.setupActionBarWithNavController (
                it,
                AppBarConfiguration(setOf(R.id.nav_home, R.id.nav_chains, R.id.nav_peers))
            )
            findViewById<BottomNavigationView>(R.id.nav_view).setupWithNavController(it)
        }
        findViewById<ProgressBar>(R.id.progress).max = 0 // 0=ready

        fsRoot = applicationContext.filesDir.toString()
        //println(fsRoot)
        //Local_delete()

        if (File(fsRoot!!,"/data/").exists()) {
            Local_load()
        } else {
            this.host_recreate()
        }
        this.host_start_bg()
        thread {
            Thread.sleep(500)
            while (true) {
                try {
                    this.runOnUiThread {
                        this.peers_sync(false)
                    }
                    Thread.sleep(T5m_sync)
                } catch (e: Throwable) {
                    // may fail on "Reset": just try again
                }
            }
        }
    }

    ////////////////////////////////////////

    fun setWaiting (v: Boolean) {
        val wait = findViewById<View>(R.id.wait)
        if (v) {
            wait.visibility = View.VISIBLE
            this.getWindow().setFlags (
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            );
        } else {
            wait.visibility = View.INVISIBLE
            this.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        }
    }

    fun showNotification (title: String, message: String) {
        val mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel("FREECHAINS_CHANNEL_ID",
                "FREECHAINS_CHANNEL_NAME",
                NotificationManager.IMPORTANCE_DEFAULT)
            channel.description = "FREECHAINS_NOTIFICATION_CHANNEL_DESCRIPTION"
            mNotificationManager.createNotificationChannel(channel)
        }
        val mBuilder = NotificationCompat.Builder(applicationContext, "FREECHAINS_CHANNEL_ID")
            .setSmallIcon(R.drawable.ic_freechains_notify) // notification icon
            .setContentTitle(title) // title for notification
            .setContentText(message)// message for notification
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setAutoCancel(true) // clear notification after click
        val intent = Intent(applicationContext, MainActivity::class.java)
        val pi = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        mBuilder.setContentIntent(pi)
        mNotificationManager.notify(0, mBuilder.build())
    }

    ////////////////////////////////////////

    fun host_start_bg () {
        thread {
            this.runOnUiThread {
                this.setWaiting(true)
            }
            thread {
                main_(arrayOf("host","start","/data/"))
            }
            Thread.sleep(500)
            this.runOnUiThread {
                LOCAL!!.reloadPeers() {}
                LOCAL!!.reloadChains() {}
                this.setWaiting(false)
            }
            thread {
                try {
                    val socket = Socket("localhost", PORT_8330)
                    val writer = DataOutputStream(socket.getOutputStream()!!)
                    val reader = DataInputStream(socket.getInputStream()!!)
                    writer.writeLineX("$PRE chains listen")
                    while (true) {
                        val v = reader.readLineX()
                        val (n) = Regex("(\\d+) .*").find(v)!!.destructured
                        if (n.toInt() > 0) {
                            this.showNotification("New blocks:", v)
                        }
                    }
                } catch (e: Throwable) {
                    // may fail on "Reset": do nothing since it will all restart
                }
            }
        }
    }

    fun host_recreate () {
        main_(arrayOf("host", "stop"))
        File(fsRoot!!, "/").deleteRecursively()
        main_(arrayOf("host","create","/data/"))
        Local_load()
    }

    fun host_recreate_ask () {
        AlertDialog.Builder(this)
            .setTitle("!!! Reset Freechains !!!")
            .setMessage("Delete all data?")
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton(android.R.string.yes, { _, _ ->
                this.setWaiting(true)
                thread {
                    this.host_recreate()
                    this.host_start_bg()
                    Thread.sleep(500)
                    this.runOnUiThread {
                        this.setWaiting(false)
                        Toast.makeText(
                            applicationContext,
                            "Freechains reset OK!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            })
            .setNegativeButton(android.R.string.no, null).show()
    }

    ////////////////////////////////////////

    fun chains_join_ask (cb: ()->Unit) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Join chain:")

        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT

        builder.setView(input)
        builder.setNegativeButton ("Cancel", null)
        builder.setPositiveButton("OK") { _,_ ->
            val chain = input.text.toString()
            this.chains_join(chain, cb)
        }

        builder.show()
    }

    fun chains_join (chain: String, cb: ()->Unit) {
        thread {
            main_(arrayOf("chains", "join", chain))
            LOCAL!!.reloadChains {
                this.runOnUiThread { cb() }
            }
        }
        Toast.makeText(
            this.applicationContext,
            "Added $chain.",
            Toast.LENGTH_SHORT
        ).show()
    }

    fun chains_leave_ask (chain: String, cb: ()->Unit) {
        AlertDialog.Builder(this)
            .setTitle("Leave $chain?")
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton(android.R.string.yes, { _, _ ->
                thread {
                    main_(arrayOf("chains", "leave", chain))
                    LOCAL!!.reloadChains() {
                        this.runOnUiThread { cb() }
                    }
                }
                Toast.makeText(
                    this.applicationContext,
                    "Left $chain.", Toast.LENGTH_LONG
                ).show()
            })
            .setNegativeButton(android.R.string.no, null).show()
    }

    ////////////////////////////////////////

    fun chain_get (chain: String, mode: String, block: String) {
        thread {
            val pay = main_(arrayOf("chain","get",chain,mode,block))
            this.runOnUiThread {
                if (this.isActive) {
                    AlertDialog.Builder(this)
                        .setTitle("Block ${block.block2id()}:")
                        .setMessage(pay)
                        .show()
                }
            }
        }
    }

    ////////////////////////////////////////

    fun peers_add_ask (cb: ()->Unit) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Add peer:")

        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT

        builder.setView(input)
        builder.setNegativeButton ("Cancel", null)
        builder.setPositiveButton("OK") { _,_ ->
            val host = input.text.toString()
            LOCAL!!.peersAdd(host) {
                this.runOnUiThread { cb() }
            }
            Toast.makeText(
                applicationContext,
                "Added peer $host.",
                Toast.LENGTH_SHORT
            ).show()
        }

        builder.show()
    }

    fun peers_remove_ask (host: String, cb: ()->Unit) {
        AlertDialog.Builder(this)
            .setTitle("Remove peer $host?")
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton(android.R.string.yes, { _, _ ->
                LOCAL!!.peersRem(host) {
                    this.runOnUiThread { cb() }
                }
                Toast.makeText(
                    applicationContext,
                    "Removed peer $host.", Toast.LENGTH_LONG
                ).show()
            })
            .setNegativeButton(android.R.string.no, null).show()
    }

    fun peers_sync (showProgress: Boolean) {
        val hosts  = LOCAL!!.peers
        val chains = LOCAL!!.chains

        val progress = findViewById<ProgressBar>(R.id.progress)
        if (progress.max > 0) {
            return  // already running
        }
        if (showProgress) {
            progress.visibility = View.VISIBLE
        }
        progress.max = hosts.map {
            it.chains.count {
                chains.any { chain ->
                    chain.name == it
                }
            }
        }.sum() * 2
        if (progress.max == 0) {
            return
        }

        if (showProgress) {
            Toast.makeText(
                applicationContext,
                //"Total steps: ${progress.max}",
                "Synchronizing...",
                Toast.LENGTH_LONG
            ).show()
        }

        var min = 0
        var max = 0
        val notis = mutableMapOf<String,Int>()

        for (chain in chains) {
            // parallalelize accross chains
            synchronized (this) {
                notis[chain.name] = 0
            }
            thread {
                val hs = hosts.filter { it.chains.contains(chain.name) }
                // but not inside each chain
                for (h in hs) {
                    fun f (dir: String, v: String) {
                        //println(v)
                        val (v1,v2) = Regex("(\\d+) / (\\d+)").find(v)!!.destructured
                        min += v1.toInt()
                        max += v2.toInt()
                        if (dir == "<-") {
                            synchronized (this) {
                                notis[chain.name] = notis[chain.name]!! + v1.toInt()
                            }
                        }
                        this.runOnUiThread {
                            progress.progress += 1
                            if (showProgress && v1.toInt()>0) {
                                Toast.makeText(
                                    applicationContext,
                                    "${chain.name}: $v1 $dir", Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                    main_(arrayOf("peer","send",h.name,chain.name)).let {
                        f("->", it)
                    }
                    main_(arrayOf("peer","recv",h.name,chain.name)).let {
                        f("<-", it)
                    }
                    this.runOnUiThread {
                        if (progress.progress == progress.max) {
                            val noti = notis.toList()
                                .filter { it.second > 0 }
                                .map { "${it.second} ${it.first}" }
                                .joinToString("\n")
                            if (noti.isNotEmpty()) {
                                this.showNotification("New blocks:", noti)
                            }
                            if (showProgress) {
                                progress.visibility = View.INVISIBLE
                                Toast.makeText(
                                    applicationContext,
                                    "Synchronized $min blocks.", Toast.LENGTH_LONG
                                ).show()
                            }
                            progress.max = 0
                        }
                    }
                }
            }
        }
    }
}
