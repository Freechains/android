package org.freechains.android

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.EditText
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

        fsRoot = applicationContext.filesDir.toString()
        println(fsRoot)

        File(fsRoot!!, "test.txt").bufferedWriter().use {
            it.write("teste 1")
            it.newLine()
        }

        File(fsRoot!!, "test.txt").bufferedReader().use {
            println(it.readText())
        }

        main(arrayOf("host","create","/tests/"))
        thread {
            main(arrayOf("host","start","/tests/"))
        }
        thread {
            Thread.sleep(100)
            main(arrayOf("chain","join","/"))
        }

        val list = listOf("Alpha", "Beta", "Gamma", "Delta", "Epsilon")

        list.toObservable() // extension function for Iterables
            .filter { it.length >= 5 }
            .subscribeBy(  // named arguments for lambda Subscribers
                onNext = { println(it) },
                onError =  { it.printStackTrace() },
                onComplete = { println("Done!") }
            )

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun sendMessage(view: View) {
        val editText = findViewById<EditText>(R.id.editText)
        val message = editText.text.toString()
        val intent = Intent(this, DisplayMessageActivity::class.java).apply {
            putExtra(EXTRA_MESSAGE, message)
        }
        startActivity(intent)
    }

}
