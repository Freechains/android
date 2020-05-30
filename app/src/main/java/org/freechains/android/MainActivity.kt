package org.freechains.android

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import java.io.File
import kotlin.concurrent.thread
import org.freechains.common.main
import org.freechains.platform.fsRoot

import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.kotlin.toObservable

const val EXTRA_MESSAGE = "org.freechains.android.MESSAGE"

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        //File("xxx.txt").writeText("testando 123\n")
        //File("/chico/xxx.txt").writeText("testando 123\n")
        //File("chico/xxx.txt").writeText("testando 123\n")
/*
        fsRoot = applicationContext.filesDir.toString()
        println(fsRoot)

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

        main(arrayOf("host","create","/freechains/"))
        thread {
            main(arrayOf("host","start","/freechains/"))
        }
        thread {
            Thread.sleep(100)
            main(arrayOf("chains","join","/"))
        }
*/
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
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
