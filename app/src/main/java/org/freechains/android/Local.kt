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

fun LOCAL () : String {
    return fsRoot!! + "/" + "local.json"
}

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

fun Local_load () : Local {
    val file = File(LOCAL())
    if (!file.exists()) {
        return Local(emptyList(), emptyList())
    } else {
        @UseExperimental(UnstableDefault::class)
        val json = Json(JsonConfiguration(prettyPrint=true))
        return json.parse(Local.serializer(), file.readText())
    }
}

fun Local.save () {
    @UseExperimental(UnstableDefault::class)
    val json = Json(JsonConfiguration(prettyPrint=true))
    File(LOCAL()).writeText(json.stringify(Local.serializer(), this))
}

fun Local.hostsAdd (act: Activity, name: String, f: ()->Unit) {
    thread {
        synchronized (this) {
            this.hosts += Host(name)
            this.save()
        }
        this.hostsReload(act, f)
    }
}

fun Local.hostsReload (act: Activity, f: ()->Unit) {
    for (i in 0 until this.hosts.size) {
        val host = this.hosts[i].name + ":8330"
        thread {
            synchronized (this) {
                val ms = main_(arrayOf("peer", "ping", host)).let {
                    if (it.isEmpty()) "down" else it+"ms"
                }
                //println(">>> $i // $ms // ${hosts[i]}")
                val chains = main_(arrayOf("peer", "chains", host)).let {
                    if (it.isEmpty()) emptyList() else it.split(' ')
                }
                act.runOnUiThread {
                    this.hosts[i].ping = ms
                    this.hosts[i].chains = chains
                    this.save()
                    f()
                }
            }
        }
    }
}

fun Local.chainsReload (act: Activity, f: ()->Unit) {
    thread {
        val chains = main_(arrayOf("chains","list")).let {
            if (it.isEmpty()) {
                emptyList()
            } else {
                it.split(' ')
            }
        }
        //Thread.sleep(5000)
        act.runOnUiThread {
            this.chains = chains
            this.save()
            f()
        }
    }
}
