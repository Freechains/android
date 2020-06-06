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
import android.widget.BaseExpandableListAdapter
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

const val T5m_sync    = 30*hour
const val LEN1000_pay = 1000

class MainActivity : AppCompatActivity ()
{
    var isActive = true
    val adapters: MutableSet<BaseExpandableListAdapter> = mutableSetOf()

    fun notify (tp: String) {
        this.adapters.forEach {
            it.notifyDataSetChanged()
        }
    }

    override fun onPause()  { super.onPause()  ; this.isActive=false }
    override fun onResume() { super.onResume() ; this.isActive=true  }

    override fun onDestroy () {
        main_(arrayOf("host", "stop"))
        super.onDestroy()
    }

    override fun onCreate (savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.act_main)

        findNavController(R.id.nav_host_fragment).let {
            this.setupActionBarWithNavController (
                it,
                AppBarConfiguration(setOf(R.id.nav_home, R.id.nav_chains, R.id.nav_peers, R.id.nav_identity))
            )
            findViewById<BottomNavigationView>(R.id.nav_view).setupWithNavController(it)
        }
        findViewById<ProgressBar>(R.id.progress).max = 0 // 0=ready

        fsRoot = applicationContext.filesDir.toString()
        //File(fsRoot!!, "/").deleteRecursively() ; error("OK")
        Local_load()

        this.setWaiting(true)

        // background start
        val wait = findViewById<View>(R.id.wait)
        wait.visibility = View.VISIBLE
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        thread {
            main_(arrayOf("host","start","/data/"))
        }
        Thread.sleep(500)

        this.setWaiting(false)

        // background sync/reload
        thread {
            while (true) {
                LOCAL!!.bg_reloadPeers()()
                this.runOnUiThread {
                    this.notify("update views w/ peer pings")
                    this.peers_sync(false)
                }
                Thread.sleep(T5m_sync)
            }
        }

        // background listen
        thread {
            val socket = Socket("localhost", PORT_8330)
            val writer = DataOutputStream(socket.getOutputStream()!!)
            val reader = DataInputStream(socket.getInputStream()!!)
            writer.writeLineX("$PRE chains listen")
            while (true) {
                val v = reader.readLineX()
                val (n) = Regex("(\\d+) .*").find(v)!!.destructured
                if (n.toInt() > 0) {
                    this.showNotification("New blocks:", v)
                    LOCAL!!.bg_reloadChains()()
                    this.runOnUiThread {
                        this.notify("update views w/ contents of chains")
                    }
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

    fun host_recreate_ask () {
        AlertDialog.Builder(this)
            .setTitle("!!! Reset Freechains !!!")
            .setMessage("Delete all data?")
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton(android.R.string.yes, { _, _ ->
                File(fsRoot!!, "/").deleteRecursively()
                this.finishAffinity()
                this.setWaiting(true)
                Toast.makeText(
                    applicationContext,
                    "Please, restart Freechains...",
                    Toast.LENGTH_SHORT
                ).show()
            })
            .setNegativeButton(android.R.string.no, null).show()
    }

    ////////////////////////////////////////

    fun chains_join_ask () {
        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT

        AlertDialog.Builder(this)
            .setTitle("Join chain:")
            .setView(input)
            .setNegativeButton ("Cancel", null)
            .setPositiveButton("OK") { _,_ ->
                this.bg_chains_join(input.text.toString())
            }
            .show()
    }

    fun bg_chains_join (chain: String) : Wait {
        val t = thread {
            main_(arrayOf("chains", "join", chain))
            this.runOnUiThread {
                LOCAL!!.bg_reloadChains()()
                this.runOnUiThread {
                    this.notify("update view w/ list of chains")
                }
                this.peers_sync(true)
            }
        }
        Toast.makeText(
            this.applicationContext,
            "Added $chain.",
            Toast.LENGTH_SHORT
        ).show()
        return { t.join() }
    }

    fun chains_leave_ask (chain: String) {
        AlertDialog.Builder(this)
            .setTitle("Leave $chain?")
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton(android.R.string.yes, { _, _ ->
                thread {
                    main_(arrayOf("chains", "leave", chain))
                    LOCAL!!.bg_reloadChains()()
                    this.runOnUiThread {
                        this.notify("update views w/ list of chains")
                    }
                }
                Toast.makeText(
                    this.applicationContext,
                    "Left $chain.", Toast.LENGTH_LONG
                ).show()
            })
            .setNegativeButton(android.R.string.no, null).show()
    }

    fun chain_get (chain: String, mode: String, block: String) {
        thread {
            val pay = main_(arrayOf("chain", chain, "get", mode, block)).second!!.take(LEN1000_pay)
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
        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT

        AlertDialog.Builder(this)
            .setTitle("Add peer:")
            .setView(input)
            .setNegativeButton ("Cancel", null)
            .setPositiveButton("OK") { _,_ ->
                val host = input.text.toString()
                thread {
                    LOCAL!!.bg_peersAdd(host)()
                    this.runOnUiThread {
                        this.notify("update views w/ list of peers")
                        this.peers_sync(true)
                    }
                }
                Toast.makeText(
                    applicationContext,
                    "Added peer $host.",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .show()
    }

    fun peers_remove_ask (host: String) {
        AlertDialog.Builder(this)
            .setTitle("Remove peer $host?")
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton(android.R.string.yes, { _, _ ->
                LOCAL!!.peersRem(host)
                this.notify("update views w/ list of peers")
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
        val counts = mutableMapOf<String,Int>()

        for (chain in chains) {
            // parallalelize accross chains
            synchronized (this) {
                counts[chain.name] = 0
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
                                counts[chain.name] = counts[chain.name]!! + v1.toInt()
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
                    main_(arrayOf("peer",h.name,"send",chain.name)).let {
                        f("->", it.second!!)
                    }
                    main_(arrayOf("peer",h.name,"recv",chain.name)).let {
                        f("<-", it.second!!)
                    }
                    this.runOnUiThread {
                        if (progress.progress == progress.max) {
                            val news = counts.toList()
                                .filter { it.second > 0 }
                                .map { "${it.second} ${it.first}" }
                                .joinToString("\n")
                            if (news.isNotEmpty()) {
                                this.showNotification("New blocks:", news)
                                thread {
                                    LOCAL!!.bg_reloadChains()()
                                    this.runOnUiThread {
                                        this.notify("update views w/ contents of chains")
                                    }
                                }
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


    ////////////////////////////////////////

    fun ids_add_ask (cb: ()->Unit) {
        val view = View.inflate(this, R.layout.frag_ids_add, null)

        AlertDialog.Builder(this)
            .setTitle("New identity:")
            .setView(view)
            .setNegativeButton ("Cancel", null)
            .setPositiveButton("OK") { _,_ ->
                val nick  = view.findViewById<EditText>(R.id.edit_nick).text.toString()
                val pass1 = view.findViewById<EditText>(R.id.edit_pass1).text.toString()
                val pass2 = view.findViewById<EditText>(R.id.edit_pass2).text.toString()
                if (pass1.length>=20 && pass1==pass2) {
                    val size = LOCAL!!.ids.size
                    thread {
                        LOCAL!!.bg_idsAdd(nick, pass1)()
                        this.runOnUiThread {
                            val ret = if (size < LOCAL!!.ids.size) {
                                this.notify("update views w/ list of ids")
                                this.bg_chains_join("@" + LOCAL!!.ids.first { it.nick == nick }.pub)
                                "Added $nick."
                            } else {
                                "Identity already exists."
                            }
                            Toast.makeText(
                                applicationContext,
                                ret,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } else {
                    Toast.makeText(
                        applicationContext,
                        "Invalid password.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .show()
    }

    fun ids_remove_ask (nick: String) {
        AlertDialog.Builder(this)
            .setTitle("Remove identity $nick?")
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton(android.R.string.yes, { _, _ ->
                LOCAL!!.idsRem(nick)
                this.notify("update views w/ list of ids")
                Toast.makeText(
                    applicationContext,
                    "Removed identity $nick.", Toast.LENGTH_LONG
                ).show()
            })
            .setNegativeButton(android.R.string.no, null).show()
    }


}
