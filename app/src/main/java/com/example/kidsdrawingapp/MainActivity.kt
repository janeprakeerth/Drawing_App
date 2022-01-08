package com.example.kidsdrawingapp

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.app.Instrumentation
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Message
import android.provider.MediaStore
import android.provider.MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception
import kotlin.coroutines.cancellation.CancellationException

class MainActivity : AppCompatActivity() {
    private var drawingView:DrawingView?=null
    private var mImageButtonCurrentPaint:ImageButton?=null
    val requestPermission: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){
            permissions ->
            permissions.entries.forEach {
                val permissionName = it.key
                val isGranted = it.value
                if(isGranted){
                    Toast.makeText(this,"Permission Granted now you can read the storage files",Toast.LENGTH_LONG).show()
                }else{
                    if(permissionName== Manifest.permission.READ_EXTERNAL_STORAGE){
                         Toast.makeText(this,"Oops you just denied the permission",Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        drawingView = findViewById(R.id.drawing_view)
        drawingView?.setSizeForBrush(20.toFloat())
        val ib_brush:ImageButton=findViewById(R.id.ib_brush)
        val ib_gallery:ImageButton=findViewById(R.id.ib_gallery)
        val ib_undo:ImageButton=findViewById(R.id.ib_undo)
        val ib_save:ImageButton=findViewById(R.id.ib_save)
        val linearLayoutPaintColors =findViewById<LinearLayout>(R.id.ll_paint_colors)
        mImageButtonCurrentPaint = linearLayoutPaintColors[1] as ImageButton
        mImageButtonCurrentPaint!!.setImageDrawable(
            ContextCompat.getDrawable(this,R.drawable.pallet_pressed)
        )
        ib_brush.setOnClickListener {
            showBrushSizeChooserDialog()
        }
        ib_gallery.setOnClickListener {
            if(isReadStorageAllowed()){
                val pickPhotoIntent = Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                startActivityForResult(pickPhotoIntent, GALLERY)
            }
            requestStoragePermission()
        }
        ib_undo.setOnClickListener {
            drawingView?.onClickUndo()
        }
        ib_save.setOnClickListener {
            if(isReadStorageAllowed()){
                val fl_drawing_view_container:FrameLayout=findViewById(R.id.fl_drawing_view_container)
                BitmapAsyncTask(getBitmapFromView(fl_drawing_view_container)).execute()
            }else{
                requestStoragePermission()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode== Activity.RESULT_OK){
             if(requestCode== GALLERY){
                 try{
                     if(data!!.data!=null){
                         val iv_background:ImageView=findViewById(R.id.iv_background)
                         iv_background.visibility=View.VISIBLE
                         iv_background.setImageURI(data.data)
                     }else{
                         Toast.makeText(this,"Error in parsing the image or its corrupted,",Toast.LENGTH_LONG).show()
                     }
                 }catch (e:Exception){
                     e.printStackTrace()
                 }
             }
        }
    }

    private fun isReadStorageAllowed(): Boolean {
             val result = ContextCompat.checkSelfPermission(this,Manifest.permission.READ_EXTERNAL_STORAGE)
             return result==PackageManager.PERMISSION_GRANTED
    }
    private fun getBitmapFromView(view:View):Bitmap{
        val returnedBitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas =    Canvas(returnedBitmap)
        val bgDrawable = view.background
        if(bgDrawable!=null){
             bgDrawable.draw(canvas)
        }else{
            canvas.drawColor(Color.WHITE)
        }
        view.draw(canvas)
        return returnedBitmap
    }
    private inner class BitmapAsyncTask(val mBitmap: Bitmap): AsyncTask<Any, Void, String>(){

        override fun doInBackground(vararg params: Any?): String {
            var result = ""
            if(mBitmap!=null){
                try{
                    val bytes = ByteArrayOutputStream()
                    mBitmap.compress(Bitmap.CompressFormat.PNG,90,bytes)
                    val f = File(externalCacheDir!!.absoluteFile.toString()+ File.separator +"KidsDrawingApp_"+System.currentTimeMillis()/1000 + ".png")
                    val fos = FileOutputStream(f)
                    fos.write(bytes.toByteArray())
                    fos.close()
                    result =f.absolutePath
                }catch (e:Exception){
                    result = ""
                    e.printStackTrace()
                }
            }
            return result
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            if(!result?.isEmpty()!!){
                Toast.makeText(this@MainActivity,"File saved successfully:$result",Toast.LENGTH_SHORT).show()
            }else{
                Toast.makeText(this@MainActivity,"Something went wrong while saving the file",Toast.LENGTH_SHORT).show()
            }
            MediaScannerConnection.scanFile(this@MainActivity, arrayOf(result),null){
                path,uri-> val shareIntent = Intent()
                shareIntent.action = Intent.ACTION_SEND
                shareIntent.putExtra(Intent.EXTRA_STREAM,uri)
                shareIntent.type = "image/png"
                startActivity(Intent.createChooser(shareIntent,"Share"))
            }
        }

    }
    companion object{
        private const val STORAGE_PERMISSION_CODE = 1
        private const val GALLERY = 2
    }

    private fun requestStoragePermission() {
        if(ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.READ_EXTERNAL_STORAGE)){
            showRationaleDialog("Kids Drawing App","Kids Drawing App"+"needs access your external storage")
        }else{
            requestPermission.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
        }
    }

    private fun showBrushSizeChooserDialog(){
        val brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.dialogue_brush_size)
        brushDialog.setTitle("Brush Size :")
        brushDialog.show()
        val smallBtn :ImageButton = brushDialog.findViewById(R.id.ib_small_brush)
        val mediumBtn :ImageButton = brushDialog.findViewById(R.id.ib_medium_brush)
        val largeBtn :ImageButton = brushDialog.findViewById(R.id.ib_large_brush)

        smallBtn.setOnClickListener {
            drawingView?.setSizeForBrush(10.toFloat())
            brushDialog.dismiss()
        }
        mediumBtn.setOnClickListener {
            drawingView?.setSizeForBrush(20.toFloat())
            brushDialog.dismiss()
        }
        largeBtn.setOnClickListener {
            drawingView?.setSizeForBrush(30.toFloat())
            brushDialog.dismiss()
        }

    }
    fun paintClicked(view: View){
        if(view!=mImageButtonCurrentPaint){
            val imageButton = view as ImageButton
            val colorTag = imageButton.tag.toString()
            drawingView?.setColor(colorTag)
            imageButton.setImageDrawable(ContextCompat.getDrawable(this,R.drawable.pallet_pressed))
            mImageButtonCurrentPaint?.setImageDrawable((ContextCompat.getDrawable(this,R.drawable.pallet_normal)))
            mImageButtonCurrentPaint = view
        }
    }
    private fun showRationaleDialog (title:String,message: String){
        val builder : AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton("cancel"){ dialog, _->
                dialog.dismiss()
            }
        builder.create().show()
    }
}