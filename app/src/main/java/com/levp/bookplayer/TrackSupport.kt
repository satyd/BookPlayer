package com.levp.bookplayer

import android.content.Context
import android.net.Uri
import kotlin.properties.Delegates

class TrackSupport{
    class Track(var dataUri:String, var name : String,var album : String, var artist : String){
        var duration by Delegates.notNull<Long>()
        lateinit var imageUri : Uri
        lateinit var context: Context

    }

}