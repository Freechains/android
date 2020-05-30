package org.freechains.android

import android.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import org.freechains.common.*
import org.freechains.platform.fsRoot
import java.io.File
import kotlin.concurrent.thread

val PATH = fsRoot!! + "/" + "hosts"

@Serializable
data class Hosts (
    val list: ArrayList<String>
)

fun Hosts.toJson () : String {
    @UseExperimental(UnstableDefault::class)
    val json = Json(JsonConfiguration(prettyPrint=true))
    return json.stringify(Hosts.serializer(), this)
}

fun String.fromJsonToHosts () : Hosts {
    @UseExperimental(UnstableDefault::class)
    val json = Json(JsonConfiguration(prettyPrint=true))
    return json.parse(Hosts.serializer(), this)
}

class HostsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hosts)

        val file = File(PATH)
        if (!file.exists()) {
            file.writeText(Hosts(arrayListOf()).toJson())
        }

        this.update()
    }

    fun update () {
        val hosts = File(PATH).readText().fromJsonToHosts()

        val list = findViewById<ListView>(R.id.list)
        list.setAdapter (
            ArrayAdapter<String> (
                this,
                android.R.layout.simple_list_item_1,
                listOf("Add host...") + hosts.list
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
            val hosts = File(PATH).readText().fromJsonToHosts()
            hosts.list.add(input.text.toString())
            File(PATH).writeText(hosts.toJson())
            this.update()
        }

        builder.show()
    }
}
