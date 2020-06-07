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
    val name   : String,
    var heads  : List<String>,
    var blocks : List<String>
)

@Serializable
data class Id (
    val nick : String,
    //var desc : String,
    val pub  : HKey
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

fun <T> Local.write (f: (Local)->T): T {
    var ret: T? = null
    this.write_tst { ret=f(this) ; true }
    return ret!!
}

@Synchronized
fun Local.write_tst (f: (Local)->Boolean): Boolean {
    val ret = f(this)
    if (ret) {
        this.save()
    }
    return ret
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
            it.chains = chains
        }
    }
    return { t.join() }
}

////////////////////////////////////////

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
            this.write {
                it.peers[i].ping = ms
                it.peers[i].chains = chains
            }
        }
        val f_ = f
        f = { t.join() ; f_() }
    }
    return f
}
