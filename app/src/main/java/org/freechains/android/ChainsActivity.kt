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

        findViewById<ExpandableListView>(R.id.list).let {
            it.setAdapter(this.adapter)
            it.setOnItemLongClickListener { _,view,_,_ ->
                if (view is LinearLayout && view.tag is String) {
                    val chain = view.tag.toString()
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
                                "Left $chain.", Toast.LENGTH_LONG
                            ).show()
                        })
                        .setNegativeButton(android.R.string.no, null).show()
                    true
                } else {
                    false
                }
            }
        }

        this.update()
    }

    fun update () {
        LOCAL!!.chainsReload () {
            runOnUiThread {
                findViewById<ListView>(R.id.list).visibility = View.VISIBLE
                findViewById<View>    (R.id.wait).visibility = View.INVISIBLE
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
            return LOCAL!!.chains[i].blocks[j]
        }
        override fun getChildId (i: Int, j: Int): Long {
            return i*10+j.toLong()
        }
        override fun getChildView (i: Int, j: Int, isLast: Boolean,
                                   convertView: View?, parent: ViewGroup?): View? {
            val block = LOCAL!!.chains[i].blocks[j].block2id()
            val view = View.inflate(ctx, R.layout.simple,null)
            view.findViewById<TextView>(android.R.id.text1).text = block
            view.setOnClickListener {
                Toast.makeText (
                    applicationContext,
                    "Clicked $block.", Toast.LENGTH_LONG
                ).show()
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
            val view = View.inflate(ctx, R.layout.activity_chains_chain,null)
            val chain = LOCAL!!.chains[i]
            view.findViewById<TextView>(R.id.chain).text = chain.name.chain2id()
            view.findViewById<TextView>(R.id.heads).text = chain.heads
                .map { it.block2id() }
                .joinToString("\n")
            view.tag = chain.name
            return view
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

