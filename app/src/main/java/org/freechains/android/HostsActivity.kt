package org.freechains.android

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class HostsActivity : AppCompatActivity() {
    val ctx = this

    override fun onCreate (savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hosts)
        findViewById<ExpandableListView>(R.id.table).setAdapter(this.adapter)
        LOCAL!!.hostsReload(this) {
            this.adapter.notifyDataSetChanged()
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
            return j.toLong()
        }
        override fun getChildView (i: Int, j: Int, isLast: Boolean,
                                   convertView: View?, parent: ViewGroup?): View? {
            val view = View.inflate(ctx, R.layout.hosts_item,null)
            view.findViewById<TextView>(R.id.host).text = LOCAL!!.hosts[i].chains[j]
            if (!LOCAL!!.chains.contains(LOCAL!!.hosts[i].chains[j])) {
                view.findViewById<ImageButton>(R.id.add).visibility = View.VISIBLE
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
            val view = View.inflate(ctx, R.layout.hosts_group,null)
            view.findViewById<TextView>(R.id.host).text  = LOCAL!!.hosts[i].name
            view.findViewById<TextView>(R.id.add).text = LOCAL!!.hosts[i].ping
            return view
        }
    }

    fun onClick_add (view: View) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Add host...")

        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT

        builder.setView(input)
        builder.setNegativeButton ("Cancel", null)
        builder.setPositiveButton("OK") { _,_ ->
            LOCAL!!.hostsAdd(this, input.text.toString()) {
                this.adapter.notifyDataSetChanged()
            }
        }

        builder.show()
    }
}
