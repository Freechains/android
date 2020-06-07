package org.freechains.android

import kotlinx.serialization.Serializable
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import org.freechains.common.HKey
import org.freechains.platform.fsRoot
import java.io.File

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

val Local_cbs: MutableSet<()->Unit> = mutableSetOf()

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
fun <T> Local.read (f: (Local)->T): T {
    return f(this)
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
        Local_cbs.forEach { it() }
    }
    return ret
}
