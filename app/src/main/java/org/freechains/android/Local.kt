package org.freechains.android

import android.R
import android.app.Activity
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import kotlinx.serialization.Serializable
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import org.freechains.common.main_
import org.freechains.platform.fsRoot
import java.io.File
import kotlin.concurrent.thread

@Serializable
data class Host (
    val name   : String,
    var ping   : String = "?",
    var chains : List<String> = emptyList()
)

@Serializable
data class Local (
    var hosts  : List<Host>,
    var chains : List<String>
)

var LOCAL: Local? = null

fun Local_load () {
    val file = File(fsRoot!! + "/" + "local.json")
    if (!file.exists()) {
        LOCAL = Local(emptyList(), emptyList())
    } else {
        @UseExperimental(UnstableDefault::class)
        val json = Json(JsonConfiguration(prettyPrint=true))
        LOCAL = json.parse(Local.serializer(), file.readText())
    }
    LOCAL!!.save()
}

fun Local.save () {
    @UseExperimental(UnstableDefault::class)
    val json = Json(JsonConfiguration(prettyPrint=true))
    File(fsRoot!! + "/" + "local.json").writeText(json.stringify(Local.serializer(), this))
}

@Synchronized
fun Local.hostsAdd (name: String, f: ()->Unit) {
    this.hosts += Host(name)
    this.save()
    f()
    this.hostsReload(f)
}

@Synchronized
fun Local.hostsRem (i: Int, f: ()->Unit) {
    this.hosts = this.hosts.filter { it.name != this.hosts[i].name }
    this.save()
    f()
}

@Synchronized
fun Local.hostsReload (f: ()->Unit) {
    for (i in 0 until this.hosts.size) {
        val host = this.hosts[i].name + ":8330"
        thread {
            synchronized (this) {
                val ms = main_(arrayOf("peer", "ping", host)).let {
                    if (it.isEmpty()) "down" else it+"ms"
                }
                //println(">>> $i // $ms // ${this.hosts[i]}")
                val chains = main_(arrayOf("peer", "chains", host)).let {
                    if (it.isEmpty()) emptyList() else it.split(' ')
                }
                this.hosts[i].ping = ms
                this.hosts[i].chains = chains
                this.save()
                f()
            }
        }
    }
}

@Synchronized
fun Local.chainsReload (f: ()->Unit) {
    thread {
        val chains = main_(arrayOf("chains","list")).let {
            if (it.isEmpty()) {
                emptyList()
            } else {
                it.split(' ')
            }
        }
        //Thread.sleep(5000)
        synchronized (this) {
            this.chains = chains
            this.save()
            f()
        }
    }
}
