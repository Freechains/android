package org.freechains.android

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.freechains.common.main
import org.freechains.platform.fsRoot
import java.io.File
import kotlin.concurrent.thread


const val EXTRA_MESSAGE = "org.freechains.android.MESSAGE"

class MainActivity : AppCompatActivity() {

    fun reset () {
        main(arrayOf("host", "stop"))
        File(fsRoot!!, "/").deleteRecursively()
        main(arrayOf("host","create","/data/"))
    }
    fun start () {
        main(arrayOf("host","start","/data/"))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        fsRoot = applicationContext.filesDir.toString()
        //println(fsRoot)

        val list = findViewById<View>(R.id.list)
        val wait = findViewById<View>(R.id.wait)

        list.visibility = View.INVISIBLE
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
                wait.visibility = View.INVISIBLE
                list.visibility = View.VISIBLE
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
    fun onClick_Reset (view: View) {
        AlertDialog.Builder(this)
            .setTitle("!!! Reset Freechains !!!")
            .setMessage("Delete all data?")
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton(android.R.string.yes,
                DialogInterface.OnClickListener { _, _ ->
                    val list = findViewById<View>(R.id.list)
                    val wait = findViewById<View>(R.id.wait)
                    list.visibility = View.INVISIBLE
                    wait.visibility = View.VISIBLE

                    thread {
                        this.reset()
                        thread {
                            this.start()
                        }
                        Thread.sleep(500)
                        this.runOnUiThread {
                            wait.visibility = View.INVISIBLE
                            list.visibility = View.VISIBLE
                            Toast.makeText(
                                this@MainActivity,
                                "Freechains reset OK!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                })
            .setNegativeButton(android.R.string.no, null).show()
    }
}
