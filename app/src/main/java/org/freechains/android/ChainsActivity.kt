package org.freechains.android

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.freechains.common.main_
import kotlin.concurrent.thread

class ChainsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chains)
        this.update()
    }

    fun update () {
        val list = findViewById<ListView>(R.id.list)
        val wait = findViewById<View>(R.id.wait)

        list.visibility = View.INVISIBLE
        wait.visibility = View.VISIBLE

        thread {
            val chains = main_(arrayOf("chains","list")).let {
                if (it.isEmpty()) {
                    emptyList()
                } else {
                    it.split(' ')
                }
            }
            //Thread.sleep(5000)
            this.runOnUiThread {
                list.setAdapter (
                    ArrayAdapter<String> (
                        this,
                        android.R.layout.simple_list_item_1,
                        chains
                    )
                )
                list.setOnItemClickListener { parent, view, position, id ->
                    Toast.makeText (
                        applicationContext,
                        "Click ListItem Number $position", Toast.LENGTH_LONG
                    ).show()
                }

                wait.visibility = View.INVISIBLE
                list.visibility = View.VISIBLE
            }
        }
    }

    fun onClick_join (view: View) {
        val list = findViewById<ListView>(R.id.list)
        val wait = findViewById<View>(R.id.wait)

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Join chain...")

        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT

        builder.setView(input)
        builder.setNegativeButton ("Cancel", null)
        builder.setPositiveButton("OK") { _,_ ->
            list!!.visibility = View.INVISIBLE
            wait!!.visibility = View.VISIBLE

            thread {
                main_(arrayOf("chains", "join", input.text.toString()))
                this.runOnUiThread {
                    this.update()
                }
            }
        }

        builder.show()
    }
}

