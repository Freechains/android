package org.freechains.android

import android.os.Bundle
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ChainsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chains)

        val vs = arrayListOf<String> (
            "Android", "iPhone", "WindowsMobile",
            "Blackberry", "WebOS", "Ubuntu", "Windows7", "Max OS X",
            "Linux", "OS/2", "Ubuntu", "Windows7", "Max OS X", "Linux",
            "OS/2", "Ubuntu", "Windows7", "Max OS X", "Linux", "OS/2",
            "Android", "iPhone", "WindowsMobile"
        )

        val list = findViewById<ListView>(R.id.list)

        list.setAdapter (
            ArrayAdapter<String>(this,android.R.layout.simple_list_item_1, vs)
        )

        list.setOnItemClickListener(OnItemClickListener { parent, view, position, id ->
            Toast.makeText(
                applicationContext,
                "Click ListItem Number $position", Toast.LENGTH_LONG
            )
                .show()
        })
    }
}

