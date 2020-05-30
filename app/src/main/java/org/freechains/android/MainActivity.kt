package org.freechains.android

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import kotlin.concurrent.thread
import org.freechains.common.main
import org.freechains.platform.fsRoot

const val EXTRA_MESSAGE = "org.freechains.android.MESSAGE"

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
/*
        //File("xxx.txt").writeText("testando 123\n")
        //File("/chico/xxx.txt").writeText("testando 123\n")
        //File("chico/xxx.txt").writeText("testando 123\n")
import java.io.File
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.kotlin.toObservable

        File(fsRoot!!, "test.txt").bufferedWriter().use {
            it.write("teste 1")
            it.newLine()
        }

        File(fsRoot!!, "test.txt").bufferedReader().use {
            println(it.readText())
        }

        val list = listOf("Alpha", "Beta", "Gamma", "Delta", "Epsilon")

        list.toObservable() // extension function for Iterables
            .filter { it.length >= 5 }
            .subscribeBy(  // named arguments for lambda Subscribers
                onNext = { println(it) },
                onError =  { it.printStackTrace() },
                onComplete = { println("Done!") }
            )
*/

        fsRoot = applicationContext.filesDir.toString()
        println(fsRoot)
        main(arrayOf("host","create","/data/"))
        thread {
            main(arrayOf("host","start","/data/"))
        }
        thread {
            Thread.sleep(100)
            main(arrayOf("chains","join","/"))
            main(arrayOf("chains","join","/mail"))
        }
    }

    fun onClick_Hosts (view: View) {
        startActivity (
            Intent(this, HostsActivity::class.java)
        )
    }

    fun onClick_Chains(view: View) {
        startActivity (
            Intent(this, ChainsActivity::class.java)
        )
    }
}
