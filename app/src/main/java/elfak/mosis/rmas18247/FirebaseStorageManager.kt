package elfak.mosis.rmas18247

import android.app.ProgressDialog
import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.firebase.storage.FirebaseStorage

class FirebaseStorageManager {
    private val TAG = "FirebaseStorageManager"

    private val mStorageRef = FirebaseStorage.getInstance().reference
    private lateinit var mProgressDialog: ProgressDialog

    fun uploadImage(mContext: Context, imageURI: Uri){
        mProgressDialog = ProgressDialog(mContext)
        mProgressDialog.setMessage("Molimo sačekajte, slika se učitava...")
        val uploadTask = mStorageRef.child("users/profilePic.png").putFile(imageURI)
        uploadTask.addOnSuccessListener {
            Log.e(TAG,"Slika je upload-ovana uspešno!")
        }.addOnFailureListener{
            Log.e(TAG, "Neuspešno upload-ovanje slike!")
        }
    }
}