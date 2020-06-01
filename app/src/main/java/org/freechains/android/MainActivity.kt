package org.freechains.android

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.freechains.common.main
import org.freechains.common.main_
import org.freechains.platform.fsRoot
import java.io.File
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    fun reset () {
        main(arrayOf("host", "stop"))
        File(fsRoot!!, "/").deleteRecursively()
        main(arrayOf("host","create","/data/"))
        Local_load()
    }
    fun start () {
        main(arrayOf("host","start","/data/"))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        fsRoot = applicationContext.filesDir.toString()
        //println(fsRoot)
        //Local_reset()
        Local_load()

        val table = findViewById<View>(R.id.table)
        val wait  = findViewById<View>(R.id.wait)

        table.visibility = View.INVISIBLE
        wait.visibility = View.VISIBLE

        thread {
            if (!File(fsRoot!!,"/data/").exists()) {
                this.reset()
            }
            thread {
                this.start()
            }
            Thread.sleep(500)
            this.runOnUiThread {
                LOCAL!!.hostsReload() {}
                LOCAL!!.chainsReload() {}

                wait.visibility = View.INVISIBLE
                table.visibility = View.VISIBLE
            }
        }
    }

    fun onClick_Hosts (view: View) {
        startActivity (
            Intent(this, HostsActivity::class.java)
        )
    }
    fun onClick_Chains(view: View) {
        startActivity (
            Intent(this, ChainsActivity::class.java)
        )
    }
    fun onClick_Sync(view: View) {
        val hosts  = LOCAL!!.hosts
        val chains = LOCAL!!.chains

        val progress = findViewById<ProgressBar>(R.id.progress)
        progress.visibility = View.VISIBLE
        progress.max = hosts.map {
            it.chains.count {
                chains.any { chain ->
                    chain.name == it
                }
            }
        }.sum() * 2

        Toast.makeText(
            applicationContext,
            "Total steps: ${progress.max}", Toast.LENGTH_LONG
        ).show()

        var min = 0
        var max = 0

        for (chain in chains) {
            // parallalelize accross chains
            thread {
                val hs = hosts.filter { it.chains.contains(chain.name) }
                // but not inside each chain
                for (h in hs) {
                    fun f (dir: String, v: String) {
                        //println(v)
                        val (v1,v2) = Regex("(\\d+) / (\\d+)").find(v)!!.destructured
                        min += v1.toInt()
                        max += v2.toInt()
                        runOnUiThread {
                            progress.progress += 1
                            if (v1.toInt() > 0) {
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
                    runOnUiThread {
                        if (progress.progress == progress.max) {
                            progress.visibility = View.INVISIBLE
                            Toast.makeText(
                                applicationContext,
                                "Synchronized $min blocks.", Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
        }
    }
    fun onClick_Reset (view: View) {
        AlertDialog.Builder(this)
            .setTitle("!!! Reset Freechains !!!")
            .setMessage("Delete all data?")
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton(android.R.string.yes, { _, _ ->
                val table = findViewById<View>(R.id.table)
                val wait  = findViewById<View>(R.id.wait)
                table.visibility = View.INVISIBLE
                wait.visibility = View.VISIBLE

                thread {
                    this.reset()
                    thread {
                        this.start()
                    }
                    Thread.sleep(500)
                    this.runOnUiThread {
                        wait.visibility  = View.INVISIBLE
                        table.visibility = View.VISIBLE
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
}
