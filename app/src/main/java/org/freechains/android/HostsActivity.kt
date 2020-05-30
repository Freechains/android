package org.freechains.android

import android.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.*
import java.io.File

class HostsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hosts)
        this.update()
    }

    fun update () {
        val hosts = File(LOCAL()).readText().fromJsonToHosts()

        val list = findViewById<ListView>(R.id.list)
        list.setAdapter (
            ArrayAdapter<String> (
                this,
                android.R.layout.simple_list_item_1,
                listOf("Add host...") + hosts.hosts
            )
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
