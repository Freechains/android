package org.freechains.android

import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.text.InputType
import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.URLSpan
import android.view.View
import android.view.WindowManager
import android.widget.*
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

fun makeLinkClickable (strBuilder: SpannableStringBuilder, span: URLSpan, cb: (String)->Unit) {
    val start: Int = strBuilder.getSpanStart(span)
    val end: Int = strBuilder.getSpanEnd(span)
    val flags: Int = strBuilder.getSpanFlags(span)
    val clickable: ClickableSpan = object : ClickableSpan() {
        override fun onClick(view: View) {
            cb(span.url)
        }
    }
    strBuilder.setSpan(clickable, start, end, flags)
    strBuilder.removeSpan(span)
}

fun TextView.setTextViewHTML (html: String, cb: (String)->Unit) {
    val sequence: CharSequence = Html.fromHtml(html)
    val strBuilder = SpannableStringBuilder(sequence)
    val urls = strBuilder.getSpans(0, sequence.length, URLSpan::class.java)
    for (span in urls) {
        makeLinkClickable(strBuilder, span!!, cb)
    }
    this.text = strBuilder
    this.movementMethod = LinkMovementMethod.getInstance()
}

const val T5m_sync    = 30*hour
const val LEN1000_pay = 1000
const val LEN10_shared = 10
const val LEN20_pubpbt = 20

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

        //println(">>> VERSION = $VERSION")
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
        val view = View.inflate(this, R.layout.frag_chains_join, null)
        AlertDialog.Builder(this)
            .setTitle("Join chain:")
            .setView(view)
            .setNegativeButton ("Cancel", null)
            .setPositiveButton("OK") { _,_ ->
                val name  = view.findViewById<EditText>(R.id.edit_name) .text.toString()
                val pass1 = view.findViewById<EditText>(R.id.edit_pass1).text.toString()
                val pass2 = view.findViewById<EditText>(R.id.edit_pass2).text.toString()
                if (!name.startsWith('$') || (pass1.length>=LEN10_shared && pass1==pass2)) {
                    val size = LOCAL!!.read { it.ids.size }
                    thread {
                        this.bg_chains_join(name,pass1)
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

    fun bg_chains_join (chain: String, pass: String = "") : Wait {
        val t = thread {
            val cmd =
                if (chain.startsWith('$')) {
                    val key = main__(arrayOf("crypto", "create", "shared", pass))
                    arrayOf("chains", "join", chain, key)
                } else {
                    arrayOf("chains", "join", chain)
                }
            main_(cmd).let { (ok, err) ->
                if (ok) {
                    LOCAL!!.bg_reloadChains()()
                    this.runOnUiThread {
                        this.notify("update view w/ list of chains")
                        this.peers_sync(true)
                        Toast.makeText(
                            this.applicationContext,
                            "Added chain $chain.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    this.runOnUiThread {
                        Toast.makeText(
                            this.applicationContext,
                            "Error joining chain $chain: " + err, Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
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
                    "Left chain $chain.", Toast.LENGTH_LONG
                ).show()
            })
            .setNegativeButton(android.R.string.no, null).show()
    }

    fun chain_get (chain: String, mode: String, block: String) {
        thread {
            val pay = main__(arrayOf("chain", chain, "get", mode, block)).take(LEN1000_pay)
            //val pay1 = pay0.replace("\\s+".toRegex(), " ")
            this.runOnUiThread {
                if (this.isActive) {
                    //val msg = TextView(this)
                    //msg.setTextViewHTML("<a href='xxx'><u>more</u></a)>") { println(it) }
                    AlertDialog.Builder(this)
                        .setTitle("Block ${block.block2id()}:")
                        //.setView(msg)
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
                    LOCAL!!.write { it.peers += Peer(host) }
                    LOCAL!!.bg_reloadPeers()

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
                LOCAL!!.write {
                    it.peers = it.peers.filter { it.name != host }
                }

                this.notify("update views w/ list of peers")
                Toast.makeText(
                    applicationContext,
                    "Removed peer $host.", Toast.LENGTH_LONG
                ).show()
            })
            .setNegativeButton(android.R.string.no, null).show()
    }

    fun peers_sync (showProgress: Boolean) {
        val hosts  = LOCAL!!.read { it.peers  }
        val chains = LOCAL!!.read { it.chains }

        val progress = findViewById<ProgressBar>(R.id.progress)
        if (progress.max > 0) {
            return  // already running
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
            progress.visibility = View.VISIBLE
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
                        println(v)
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
                    f("->", main__(arrayOf("peer",h.name,"send",chain.name)))
                    f("<-", main__(arrayOf("peer",h.name,"recv",chain.name)))
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
                if (pass1.length>= LEN20_pubpbt && pass1==pass2) {
                    thread {
                        val pub = main__(arrayOf("crypto", "create", "pubpvt", pass1)).split(' ')[0]
                        var added = LOCAL!!.write {
                            if (it.ids.none { it.nick==nick || it.pub==pub }) {
                                it.ids += Id(nick, pub)
                                true
                            } else {
                                false
                            }
                        }

                        this.runOnUiThread {
                            val ret = if (added) {
                                this.notify("update views w/ list of ids")
                                this.bg_chains_join("@" + LOCAL!!.read { it.ids.first { it.nick == nick }.pub })
                                "Added identity $nick."
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
                LOCAL!!.write {
                    it.ids = it.ids.filter { it.nick != nick }
                }
                this.notify("update views w/ list of ids")
                Toast.makeText(
                    applicationContext,
                    "Removed identity $nick.", Toast.LENGTH_LONG
                ).show()
            })
            .setNegativeButton(android.R.string.no, null).show()
    }

    ////////////////////////////////////////

    fun cts_add_ask (cb: ()->Unit) {
        val view = View.inflate(this, R.layout.frag_cts_add, null)
        AlertDialog.Builder(this)
            .setTitle("New contact:")
            .setView(view)
            .setNegativeButton ("Cancel", null)
            .setPositiveButton("OK") { _,_ ->
                val nick = view.findViewById<EditText>(R.id.edit_nick).text.toString()
                val pub  = view.findViewById<EditText>(R.id.edit_pub) .text.toString()

                val ok = LOCAL!!.write_tst {
                    if (it.cts.none { it.nick==nick || it.pub==pub }) {
                        it.cts += Id(nick, pub)
                        true
                    } else {
                        false
                    }
                }
                val ret =
                    if (ok) {
                        this.notify("update views w/ list of cts")
                        this.bg_chains_join("@" + pub)
                        "Added contact $nick."
                    } else {
                        "Contact already exists."
                    }
                Toast.makeText(
                    applicationContext,
                    ret,
                    Toast.LENGTH_SHORT
                ).show()
            }
            .show()
    }

    fun cts_remove_ask (nick: String) {
        AlertDialog.Builder(this)
            .setTitle("Remove contact $nick?")
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton(android.R.string.yes, { _, _ ->
                LOCAL!!.write {
                    it.cts = it.cts.filter { it.nick != nick }
                }
                this.notify("update views w/ list of cts")
                Toast.makeText(
                    applicationContext,
                    "Removed contact $nick.", Toast.LENGTH_LONG
                ).show()
            })
            .setNegativeButton(android.R.string.no, null).show()
    }
}
