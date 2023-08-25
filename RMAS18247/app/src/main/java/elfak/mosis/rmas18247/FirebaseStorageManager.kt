package elfak.mosis.rmas18247

import android.app.ProgressDialog
import android.net.Uri
import android.util.Log
import com.google.firebase.storage.FirebaseStorage

class FirebaseStorageManager {
    private val TAG = "FirebaseStorageManager"

    private val mStorageRef = FirebaseStorage.getInstance().reference
    private lateinit var mProgressDialog: ProgressDialog
    fun uploadImage(mContext: android.content.Context, imageURI: Uri){
        mProgressDialog = ProgressDialog(mContext)
        mProgressDialog.setMessage("Please wait image being uploading...")
        val uploadTask = mStorageRef.child("user/profilePic.png").putFile(imageURI)
        uploadTask.addOnSuccessListener{
            Log.e(TAG, "Image upload successfully!")
        }.addOnFailureListener{
            Log.e(TAG, "Image upload failed ${it.printStackTrace()}")
        }
    }
}