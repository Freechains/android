package org.freechains.android

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import java.io.File
import kotlin.concurrent.thread
import org.freechains.common.main
import org.freechains.platform.fsRoot

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
            main(arrayOf("chain","create","/0"))
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}
