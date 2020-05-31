package org.freechains.android

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import org.freechains.common.main_
import java.io.File
import kotlin.concurrent.thread


class HostsActivity : AppCompatActivity() {
    val ctx = this

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hosts)
        this.update()
    }

    fun update () {
        val local = File(LOCAL()).readText().fromJsonToHosts()

        val list = findViewById<ExpandableListView>(R.id.list)
        list.setAdapter (
            object : BaseExpandableListAdapter () {
                private val hosts = listOf("Add host...") + Local_load().hosts

                override fun hasStableIds(): Boolean {
                    return false
                }

                override fun isChildSelectable (i: Int, j: Int): Boolean {
                    return true
                }
                override fun getChild (i: Int, j: Int): Any? {
                    return "child"
                }
                override fun getChildId (i: Int, j: Int): Long {
                    return j.toLong()
                }
                override fun getChildView (i: Int, j: Int, isLast: Boolean,
                                           convertView: View?, parent: ViewGroup?): View? {
                    return View.inflate(ctx, android.R.layout.simple_list_item_1,null)
                }
                override fun getChildrenCount (i: Int): Int {
                    return 1
                }

                override fun getGroupCount(): Int {
                    return hosts.size
                }
                override fun getGroup (i: Int): Any {
                    return hosts[i]
                }
                override fun getGroupId (i: Int): Long {
                    return i.toLong()
                }
                override fun getGroupView (i: Int, isExpanded: Boolean, convertView: View?, parent: ViewGroup?): View? {
                    val view = View.inflate(ctx, R.layout.hosts_line,null)
                    val host = view.findViewById(R.id.host) as TextView
                    host.text = hosts[i]
                    val state = view.findViewById(R.id.state) as TextView
                    if (i > 0) {
                        state.text = "?"
                    }
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

        thread {
            Thread.sleep(500)
            val hosts = Local_load().hosts
            for (i in 0 until hosts.size) {
                thread {
                    println(">>> MS = antes // ${hosts[i]}")
                    val ms = main_(arrayOf("peer", "ping", hosts[i]+":8330"))
                    println(">>> MS = $ms // ${hosts[i]}")
                    this.runOnUiThread {
                        list.findViewById<ListView>(R.id.list)
                            .getChildAt(i + 1)    // +1 skip "Add host..."
                            .findViewById<TextView>(R.id.state)
                            .text = if (ms.isEmpty()) "down" else ms+"ms"
                    }
                }
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
