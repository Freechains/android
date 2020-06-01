package org.freechains.android

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import org.freechains.common.main_
import kotlin.concurrent.thread

class ChainsActivity : AppCompatActivity() {
    val ctx = this

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chains)
        this.update()
    }

    fun update () {
        LOCAL!!.chainsReload () {
            runOnUiThread {
                findViewById<ListView>(R.id.list).visibility = View.VISIBLE
                findViewById<View>    (R.id.wait).visibility = View.INVISIBLE

                findViewById<ListView>(R.id.list).let {
                    it.setAdapter (
                        ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,LOCAL!!.chains)
                    )
                    it.setOnItemClickListener { _,_,i,_ ->
                        val chain = LOCAL!!.chains[i]
                        Toast.makeText (
                            applicationContext,
                            "Clicked $chain.", Toast.LENGTH_LONG
                        ).show()
                    }
                    it.setOnItemLongClickListener { _,_,i,_ ->
                        val chain = LOCAL!!.chains[i]
                        AlertDialog.Builder(ctx)
                            .setTitle("Leave $chain?")
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setPositiveButton(android.R.string.yes, { _, _ ->
                                thread {
                                    main_(arrayOf("chains", "leave", chain))
                                    runOnUiThread {
                                        ctx.update()
                                    }
                                }
                                Toast.makeText(
                                    applicationContext,
                                    "Removed $chain.", Toast.LENGTH_LONG
                                ).show()
                            })
                            .setNegativeButton(android.R.string.no, null).show()
                        true
                    }
                }
            }
        }
    }

    fun onClick_join (view: View) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Join chain...")

        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT

        builder.setView(input)
        builder.setNegativeButton ("Cancel", null)
        builder.setPositiveButton("OK") { _,_ ->
            findViewById<ListView>(R.id.list).visibility = View.INVISIBLE
            findViewById<View>    (R.id.wait).visibility = View.VISIBLE

            val chain = input.text.toString()
            thread {
                main_(arrayOf("chains", "join", chain))
                this.runOnUiThread {
                    this.update()
                }
            }

            Toast.makeText(
                this,
                "Added $chain.",
                Toast.LENGTH_SHORT
            ).show()
        }

        builder.show()
    }
}

