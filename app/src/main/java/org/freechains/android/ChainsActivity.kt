package org.freechains.android

import android.os.Bundle
import android.view.View
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.freechains.common.main_
import kotlin.concurrent.thread


class ChainsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chains)

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
                    ArrayAdapter<String>(this,android.R.layout.simple_list_item_1, chains)
                )
                list.setOnItemClickListener(OnItemClickListener { parent, view, position, id ->
                    Toast.makeText (
                        applicationContext,
                        "Click ListItem Number $position", Toast.LENGTH_LONG
                    ).show()
                })

                wait.visibility = View.INVISIBLE
                list.visibility = View.VISIBLE
            }
        }
    }
}

