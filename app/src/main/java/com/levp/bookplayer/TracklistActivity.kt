package com.levp.bookplayer

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.database.Cursor
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.BaseColumns
import android.provider.MediaStore
import android.provider.MediaStore.Audio.*
import android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_tracklist.*
import java.io.File


class TracklistActivity : AppCompatActivity() {
    lateinit var trackList : ArrayList<TrackSupport.Track>
    lateinit var audioList : ArrayList<Audio>
    private val MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 9834

    private var player: MediaPlayerService? = null
    var serviceBound = false

    lateinit var adapter:TracklistAdapter

    private val allShownAudioPath: ArrayList<File> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tracklist)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

                // Should we show an explanation?
                if (shouldShowRequestPermissionRationale(READ_EXTERNAL_STORAGE)
                ) {
                    // Explain to the user why we need to read the contacts
                }

                requestPermissions(
                        arrayOf(READ_EXTERNAL_STORAGE),
                        MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE
                )

                // MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE is an
                // app-defined int constant that should be quite unique

                return;
            }
            loadAudio()
        }

        val tracklistAdapter = TracklistAdapter(trackList)

        tracklist_holder.apply {
            layoutManager = LinearLayoutManager(applicationContext)
            // set adapter
            adapter = tracklistAdapter

            // Touch handling
            tracklist_holder.addOnItemTouchListener(RecyclerTouchListener(
                    applicationContext,
                    tracklist_holder,
                    object : RecyclerTouchListener.ClickListener {
                        override fun onClick(view: View?, position: Int) {
                            val path = trackList.get(position).dataUri.toString()
                            playAudio(path)

                        }

                        override fun onLongClick(
                                view: View,
                                recyclerView: RecyclerView,
                                position: Int
                        ) {


                        }
                    }
            ))

        }



    }

    private fun loadAudio() {
        trackList = ArrayList()
        val contentResolver = contentResolver
        val uri: Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0"
        val sortOrder = MediaStore.Audio.Media.TITLE + " ASC"
        val cursor: Cursor? = contentResolver.query(uri, null, selection, null, sortOrder)
        val toShow = cursor?.count
        Toast.makeText(
                applicationContext,
                "$toShow are loaded",
                Toast.LENGTH_SHORT
        ).show()
        if (cursor != null && cursor.count > 0) {

            audioList = ArrayList()
            while (cursor.moveToNext()) {
                val _id = cursor.getLong(cursor.getColumnIndexOrThrow(Media._ID))
                //val volumeName = cursor.getString(cursor.getColumnIndex(Media.VOLUME_NAME))

                val title: String = cursor.getString(cursor.getColumnIndex(Media.TITLE))
                val album: String = cursor.getString(cursor.getColumnIndex(Media.ALBUM))
                val artist: String = cursor.getString(cursor.getColumnIndex(Media.ARTIST))
                val duration = cursor.getLong(cursor.getColumnIndex(Media.DURATION))
                //val art = cursor.getString(cursor.getColumnIndex())

                val albumId = cursor.getString(cursor.getColumnIndex(Media.ALBUM_ID))
                val ImageUrl: Uri = getAlbumUri(applicationContext, _id.toString())!!

                lateinit var track: TrackSupport.Track
                //ёval dataUri = Media.getContentUri(volumeName)
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
                    val data = cursor.getString(cursor.getColumnIndex(Media.DATA))
                    //val albumArt: String = cursor.getString(cursor.getColumnIndex(Albums.ALBUM_ART))
                    track = TrackSupport.Track(data, title, album, artist)
                    track.imageUri = ImageUrl
                }
                else
                {

                }
                // Save to audioList
                //audioList.add(Audio(data, title, album, artist))


                track.duration = duration
                track.context = applicationContext
                trackList.add(track)

            }
        }
        cursor?.close()
    }
    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        savedInstanceState.putBoolean("ServiceState", serviceBound)
        super.onSaveInstanceState(savedInstanceState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        serviceBound = savedInstanceState.getBoolean("ServiceState")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (serviceBound) {
            unbindService(serviceConnection)
            //service is active
            player!!.stopSelf()
        }
    }
    private fun gotoPlayer(switch: Boolean)
    {
        intent = Intent(this, PlayerActivity::class.java)
        intent.putExtra("switch", switch)
        startActivity(intent)
    }
    private fun gotoTracklist()
    {
        intent = Intent(this, TracklistActivity::class.java)
        startActivity(intent)
    }

    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            val binder = service as MediaPlayerService.LocalBinder
            player = binder.getService()
            serviceBound = true
            Toast.makeText(this@TracklistActivity, "Service Bound", Toast.LENGTH_SHORT).show()
        }

        override fun onServiceDisconnected(name: ComponentName) {
            serviceBound = false
        }
    }
    private fun playAudio(media: String) {
        //Check is service is active
        if (!serviceBound) {
            val playerIntent = Intent(this, MediaPlayerService::class.java)
            playerIntent.putExtra("media", media)
            startService(playerIntent)
            bindService(playerIntent, serviceConnection, Context.BIND_AUTO_CREATE)
        } else {
            //TODO ы
            //Service is active
            //Send media with BroadcastReceiver
        }
    }
    fun getAlbumUri(mContext: Context, album_id: String) : Uri?
    {
        if (mContext != null) {
            val sArtworkUri = Uri.parse("content://media/external/audio/albumart")
            return Uri.withAppendedPath(sArtworkUri, java.lang.String.valueOf(album_id))
        }
        return null
    }

}
