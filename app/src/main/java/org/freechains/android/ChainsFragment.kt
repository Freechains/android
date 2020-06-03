package org.freechains.android

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.ExpandableListView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton

class ChainsFragment : Fragment ()
{
    val outer = this
    lateinit var main: MainActivity

    override fun onCreateView (inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        main = this.activity as MainActivity
        inflater.inflate(R.layout.frag_chains, container, false).let { view ->
            view.findViewById<ExpandableListView>(R.id.list).let {
                it.setAdapter(this.adapter)
                it.setOnItemLongClickListener { _,view,_,_ ->
                    if (view is LinearLayout && view.tag is String) {
                        this.main.chains_leave_ask(view.tag.toString()) {
                            this.adapter.notifyDataSetChanged()
                        }
                        true
                    } else {
                        false
                    }
                }
            }
            view.findViewById<FloatingActionButton>(R.id.but_join).let {
                it.setOnClickListener {
                    this.main.chains_join_ask() {
                        this.adapter.notifyDataSetChanged()
                    }
                }
            }
            return view
        }
    }

    private val adapter = object : BaseExpandableListAdapter() {
        override fun hasStableIds(): Boolean {
            return false
        }
        override fun isChildSelectable (i: Int, j: Int): Boolean {
            return true
        }
        override fun getChild (i: Int, j: Int): Any? {
            return LOCAL!!.chains[i].blocks[j]
        }
        override fun getChildId (i: Int, j: Int): Long {
            return i*10+j.toLong()
        }
        override fun getChildView (i: Int, j: Int, isLast: Boolean,
                                   convertView: View?, parent: ViewGroup?): View? {
            val chain = LOCAL!!.chains[i]
            val block = chain.blocks[j]
            val view = View.inflate(outer.main, R.layout.simple,null)
            view.findViewById<TextView>(R.id.text).text = block.block2id()

            view.setOnLongClickListener {
                outer.main.chain_get(chain.name, "block", block)
                true
            }
            view.setOnClickListener {
                outer.main.chain_get(chain.name, "payload", block)
            }
            return view
        }
        override fun getChildrenCount (i: Int): Int {
            return LOCAL!!.chains[i].blocks.size
        }
        override fun getGroupCount(): Int {
            return LOCAL!!.chains.size
        }
        override fun getGroup (i: Int): Any {
            return LOCAL!!.chains[i]
        }
        override fun getGroupId (i: Int): Long {
            return i.toLong()
        }
        override fun getGroupView (i: Int, isExpanded: Boolean, convertView: View?, parent: ViewGroup?): View? {
            val view = View.inflate(outer.main, R.layout.frag_chains_chain,null)
            val chain = LOCAL!!.chains[i]
            view.findViewById<TextView>(R.id.chain).text = chain.name.chain2id()
            view.findViewById<TextView>(R.id.heads).text = chain.heads
                .map { it.block2id() }
                .joinToString("\n")
            view.tag = chain.name
            return view
        }
    }
}