package org.freechains.android

import kotlinx.serialization.Serializable
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import org.freechains.common.main_
import org.freechains.platform.fsRoot
import java.io.File
import kotlin.concurrent.thread

typealias Wait = (() -> Unit)

fun String.block2id () : String {
    return this.take(5) + "..." + this.takeLast(3)
}

fun String.chain2id () : String {
    if (this.startsWith('@')) {
        val n = if (this.startsWith("@!")) 5 else 4
        return this.take(n) + "..." + this.takeLast(3)
    } else {
        return this
    }
}

@Serializable
data class Peer (
    val name   : String,
    var ping   : String = "?",
    var chains : List<String> = emptyList()
)

@Serializable
data class Chain (
    var name   : String,
    var heads  : List<String>,
    var blocks : List<String>
)

@Serializable
data class Local (
    var peers  : List<Peer>,
    var chains : List<Chain>
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
    println("will create ${fsRoot!! + "/" + "local.json"}")
    File(fsRoot!! + "/" + "local.json").writeText(json.stringify(Local.serializer(), this))
}

@Synchronized
fun Local.bg_peersAdd (name: String) : Wait {
    this.peers += Peer(name)
    this.save()
    return this.bg_reloadPeers()
}

@Synchronized
fun Local.peersRem (host: String) {
    this.peers = this.peers.filter { it.name != host }
    this.save()
}

@Synchronized
fun Local.bg_reloadAll (): Wait {
    val w1 = this.bg_reloadChains()
    val w2 = this.bg_reloadPeers()
    return { w1() ; w2() }
}

// When to call:
// x restart / periodically (update ping)
// - enter "Peers" fragment (update ping)
// x add Peer -> call sync
// x rem Peer

@Synchronized
fun Local.bg_reloadPeers () : Wait {
    var f : Wait = {}
    for (i in 0 until this.peers.size) {
        val host = this.peers[i].name + ":8330"
        val t = thread {
            val ms = main_(arrayOf("peer", host, "ping")).let {
                if (!it.first) "down" else it.second!!+"ms"
            }
            //println(">>> $i // $ms // ${this.hosts[i]}")
            val chains = main_(arrayOf("peer", host, "chains")).let {
                if (!it.first) emptyList() else it.second!!.split(' ')
            }
            synchronized (this) {
                this.peers[i].ping = ms
                this.peers[i].chains = chains
                this.save()
            }
        }
        val f_ = f
        f = { t.join() ; f_() }
    }
    return f
}

// When to call:
// x add chain -> call sync
// x rem chain
// x sync
// x listen

@Synchronized
fun Local.bg_reloadChains () : Wait {
    val t = thread {
        val names = main_(arrayOf("chains","list")).let {
            if (!it.first) emptyList() else it.second!!.split(' ')
        }
        val chains = names.map {
            val heads  = main_(arrayOf("chain",it,"heads","all")).second!!.split(' ')
            val gen    = main_(arrayOf("chain",it,"genesis")).second!!
            val blocks = main_(arrayOf("chain",it,"traverse","all",gen)).let {
                if (!it.first) emptyList() else it.second!!.split(' ')
            }
            Chain(it, heads, blocks.reversed().plus(gen))
        }
        synchronized (this) {
            this.chains = chains
            this.save()
        }
    }
    return { t.join() }
}
