package org.freechains.android

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.io.File


class HostsActivity : AppCompatActivity() {
    val ctx = this

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hosts)
        this.update()
    }

    fun update () {
        val local = File(LOCAL()).readText().fromJsonToHosts()

        val list = findViewById<ListView>(R.id.list)
        list.setAdapter (
            object : BaseAdapter() {
                private val hosts = listOf("Add host...") + Local_load().hosts
                override fun getCount(): Int {
                    return hosts.size
                }
                override fun getItem (i: Int): Any {
                    return hosts[i]
                }
                override fun getItemId (i: Int): Long {
                    return i.toLong()
                }
                override fun getView (i: Int, convertView: View?, parent: ViewGroup): View {
                    val view = View.inflate(ctx, R.layout.hosts_line,null)
                    val host = view.findViewById(R.id.host) as TextView
                    host.text = hosts[i]
                    val state = view.findViewById(R.id.state) as TextView
                    state.text = "zzz"
                    return view
                }
            }
        )
        list.setOnItemClickListener { parent, view, position, id ->
            if (position == 0) {
                this.add()
            } else {
                Toast.makeText(
                    applicationContext,
                    "Click ListItem Number $position", Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    fun add () {
        val list = findViewById<ListView>(R.id.list)
        val wait = findViewById<View>(R.id.wait)

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Add host...")

        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT

        builder.setView(input)
        builder.setNegativeButton ("Cancel", null)
        builder.setPositiveButton("OK") { _,_ ->
            val hosts = File(LOCAL()).readText().fromJsonToHosts()
            hosts.hosts.add(input.text.toString())
            File(LOCAL()).writeText(hosts.toJson())
            this.update()
        }

        builder.show()
    }
}
