package org.freechains.android

import kotlinx.serialization.Serializable
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import org.freechains.common.HKey
import org.freechains.common.main_
import org.freechains.common.main__
import org.freechains.platform.fsRoot
import java.io.File
import kotlin.concurrent.thread

typealias Wait = (() -> Unit)

fun String.block2id () : String {
    return this.take(5) + "..." + this.takeLast(3)
}

fun String.pub2id () : String {
    return this.take(3) + "..." + this.takeLast(3)
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
data class Id (
    var nick : String,
    //var desc : String,
    var pub  : HKey
)

@Serializable
data class Local (
    var peers  : List<Peer>,
    var chains : List<Chain>,
    var ids    : List<Id>,
    var cts    : List<Id>
)

var LOCAL: Local? = null

fun Local_load () {
    val file = File(fsRoot!! + "/" + "local.json")
    if (!file.exists()) {
        LOCAL = Local(emptyList(), emptyList(), emptyList(), emptyList())
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
    //println("will create ${fsRoot!! + "/" + "local.json"}")
    File(fsRoot!! + "/" + "local.json").writeText(json.stringify(Local.serializer(), this))
}

@Synchronized
fun Local.write (f: (Local)->Unit) {
    f(this)
    this.save()
}

@Synchronized
fun <T> Local.read (f: (Local)->T): T {
    return f(this)
}

////////////////////////////////////////

// When to call:
// x add chain -> call sync
// x rem chain
// x sync
// x listen

@Synchronized
fun Local.bg_reloadChains () : Wait {
    val t = thread {
        val names = main__(arrayOf("chains","list")).let {
            if (it.isEmpty()) emptyList() else it.split(' ')
        }
        val chains = names.map {
            val heads  = main__(arrayOf("chain",it,"heads","all")).split(' ')
            val gen    = main__(arrayOf("chain",it,"genesis"))
            val blocks = main__(arrayOf("chain",it,"traverse","all",gen)).let {
                if (it.isEmpty()) emptyList() else it.split(' ')
            }
            Chain(it, heads, blocks.reversed().plus(gen))
        }
        this.write {
            this.chains = chains
        }
    }
    return { t.join() }
}

////////////////////////////////////////

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
            val chains = main__(arrayOf("peer", host, "chains")).let {
                if (it.isEmpty()) emptyList() else it.split(' ')
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

////////////////////////////////////////

@Synchronized
fun Local.bg_idsAdd (nick: String, passphrase: String) : Wait {
    val t = thread {
        val pub = main__(arrayOf("crypto", "create", "pubpvt", passphrase)).split(' ')[0]
        synchronized (this) {
            if (this.ids.none { it.nick==nick || it.pub==pub }) {
                this.ids += Id(nick, pub)
                this.save()
            }
        }
    }
    return { t.join() }
}

@Synchronized
fun Local.idsRem (nick: String) {
    this.ids = this.ids.filter { it.nick != nick }
    this.save()
}

////////////////////////////////////////

@Synchronized
fun Local.ctsAdd (nick: String, pub: HKey) : Boolean {
    if (this.cts.none { it.nick==nick || it.pub==pub }) {
        this.cts += Id(nick, pub)
        this.save()
        return true
    } else {
        return false
    }
}

@Synchronized
fun Local.ctsRem (nick: String) {
    this.ids = this.ids.filter { it.nick != nick }
    this.save()
}
