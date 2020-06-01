package org.freechains.android

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.widget.AdapterView.OnItemLongClickListener
import androidx.appcompat.app.AppCompatActivity
import org.freechains.common.main_
import kotlin.concurrent.thread


class HostsActivity : AppCompatActivity() {
    val ctx = this

    override fun onCreate (savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hosts)

        findViewById<ExpandableListView>(R.id.list).let {
            it.setAdapter(this.adapter)
            it.setOnItemLongClickListener { _,view,x,y ->
                if (view is LinearLayout && view.tag is String) {
                    val host = view.tag.toString()
                    AlertDialog.Builder(ctx)
                        .setTitle("Remove $host?")
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(android.R.string.yes, { _, _ ->
                            LOCAL!!.hostsRem(host) {
                                runOnUiThread {
                                    ctx.adapter.notifyDataSetChanged()
                                }
                            }
                            Toast.makeText(
                                applicationContext,
                                "Removed $host.", Toast.LENGTH_LONG
                            ).show()
                        })
                        .setNegativeButton(android.R.string.no, null).show()
                    true
                } else {
                    false
                }
            }
        }

        LOCAL!!.hostsReload() {
            runOnUiThread {
                this.adapter.notifyDataSetChanged()
            }
        }
    }

    private val adapter = object : BaseExpandableListAdapter () {
        override fun hasStableIds(): Boolean {
            return false
        }
        override fun isChildSelectable (i: Int, j: Int): Boolean {
            return true
        }
        override fun getChild (i: Int, j: Int): Any? {
            return LOCAL!!.hosts[i].chains[j]
        }
        override fun getChildId (i: Int, j: Int): Long {
            return i*10+j.toLong()
        }
        override fun getChildView (i: Int, j: Int, isLast: Boolean,
                                   convertView: View?, parent: ViewGroup?): View? {
            val view = View.inflate(ctx, R.layout.activity_hosts_chain,null)
            val chain = LOCAL!!.hosts[i].chains[j]
            view.findViewById<TextView>(R.id.chain).text = chain
            if (!LOCAL!!.chains.contains(LOCAL!!.hosts[i].chains[j])) {
                view.findViewById<ImageButton>(R.id.add).let {
                    it.visibility = View.VISIBLE
                    it.tag = chain
                }
            }
            return view
        }
        override fun getChildrenCount (i: Int): Int {
            return LOCAL!!.hosts[i].chains.size
        }
        override fun getGroupCount(): Int {
            return LOCAL!!.hosts.size
        }
        override fun getGroup (i: Int): Any {
            return LOCAL!!.hosts[i]
        }
        override fun getGroupId (i: Int): Long {
            return i.toLong()
        }
        override fun getGroupView (i: Int, isExpanded: Boolean, convertView: View?, parent: ViewGroup?): View? {
            val view = View.inflate(ctx, R.layout.activity_hosts_host,null)
            view.findViewById<TextView>(R.id.ping).text = LOCAL!!.hosts[i].ping
            view.findViewById<TextView>(R.id.host).text = LOCAL!!.hosts[i].name
            view.tag = LOCAL!!.hosts[i].name
            return view
        }
    }

    fun onClick_add_host (view: View) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Add host...")

        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT

        builder.setView(input)
        builder.setNegativeButton ("Cancel", null)
        builder.setPositiveButton("OK") { _,_ ->
            val host = input.text.toString()
            LOCAL!!.hostsAdd(host) {
                runOnUiThread {
                    this.adapter.notifyDataSetChanged()
                }
            }
            Toast.makeText(
                this,
                "Added $host.",
                Toast.LENGTH_SHORT
            ).show()
        }

        builder.show()
    }

    fun onClick_add_chain (view: View) {
        view.visibility = View.INVISIBLE
        val chain = view.tag as String
        thread {
            main_(arrayOf("chains", "join", chain))
            runOnUiThread {
                LOCAL!!.hostsReload() {
                    runOnUiThread {
                        this.adapter.notifyDataSetChanged()
                    }
                }
            }
        }
        Toast.makeText(
            this,
            "Added $chain.",
            Toast.LENGTH_SHORT
        ).show()
    }
}
