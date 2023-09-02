package elfak.mosis.rmas18247

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.WindowManager.BadTokenException
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import coil.load
import coil.transform.CircleCropTransformation
import com.google.android.gms.cast.framework.media.ImagePicker
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.karumi.dexter.listener.single.PermissionListener
import elfak.mosis.rmas18247.databinding.ActivitySignupBinding
import java.io.File
import java.io.FileOutputStream

class SignupActivity : AppCompatActivity() {


    private lateinit var binding: ActivitySignupBinding

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firebaseRef: DatabaseReference
    private lateinit var storageRef:  StorageReference
    private lateinit var selectedImageUri: Uri


    private val CAMERA_REQUEST_CODE = 1
    private val GALLERY_REQUEST_CODE = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        firebaseRef  = FirebaseDatabase.getInstance().getReference("users")
        storageRef = FirebaseStorage.getInstance().getReference("profileImages")


        binding.btnCamera.setOnClickListener{
            cameraCheckPermission()
        }
        binding.btnGallery.setOnClickListener{
            galleryCheckPermission()
        }

        //kada kliknem na sliku
        binding.profileImg.setOnClickListener{
            val pictureDialog = AlertDialog.Builder(this)
            pictureDialog.setTitle("Izaberi nacin dodavanja slike")
            val pictureDialogItem = arrayOf("Izaveri iz galerije", "Otvori kameru")
            pictureDialog.setItems(pictureDialogItem){dialog, which ->
                when(which){
                    0->gallery()
                    1-> camera()
                }
            }

            pictureDialog.show()
        }




        binding.signupButton.setOnClickListener {
            val email = binding.signupEmail.text.toString()
            val name = binding.signupName.text.toString()
            val surname = binding.signupSurname.text.toString()
            val phone = binding.signupPhone.text.toString()
            val password = binding.signupPassword.text.toString()
            val confirmPassword = binding.signupPasswordConfirm.text.toString()

            if (email.isNotEmpty() && name.isNotEmpty() && surname.isNotEmpty() &&
                phone.isNotEmpty() && password.isNotEmpty() && confirmPassword.isNotEmpty()
            ) {

                if (password == confirmPassword) {
                    firebaseAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                saveData()
                                val intent = Intent(this, LoginActivity::class.java)
                                startActivity(intent)

                            } else {
                                Toast.makeText(this, task.exception.toString(), Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }
                } else {
                    Toast.makeText(this, "Lozinke se ne poklapaju!", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Morate popuniti sva polja!", Toast.LENGTH_SHORT).show()
            }
        }

        binding.loginRedirectText.setOnClickListener {
            val loginIntent = Intent(this, LoginActivity::class.java)
            startActivity(loginIntent)
        }
    }

    private fun saveData() {
        val email = binding.signupEmail.text.toString()
        val name = binding.signupName.text.toString()
        val surname = binding.signupSurname.text.toString()
        val phone = binding.signupPhone.text.toString()

        val uid = firebaseAuth.currentUser?.uid
        val user = Users(email,name,surname, phone, selectedImageUri.toString())

        if(uid != null){
            firebaseRef.child(uid).setValue(user).addOnCompleteListener{
                if(it.isSuccessful){
                    uploadProfilePhoto()
                }
                else{
                    Toast.makeText(this, "Greška sa uploadovanjem slike!", Toast.LENGTH_SHORT).show()
                }
            }
        }


    }

    private fun uploadProfilePhoto() {
        storageRef = FirebaseStorage.getInstance().getReference("Users/"+firebaseAuth.currentUser?.uid)
        storageRef.putFile(selectedImageUri).addOnSuccessListener {
            Toast.makeText(this, "Uspesno ubacena slika!", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener{
            Toast.makeText(this, "Neuspesno dodavanje slike", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveBitmapToFile(bitmap: Bitmap): Uri {
        val imagesDir = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "profile_image_${System.currentTimeMillis()}.jpg")
        if (!imagesDir.exists()) {
            imagesDir.mkdirs()
        }

        val imageFile = File(imagesDir, "profile_image.jpg")
        val outputStream = FileOutputStream(imageFile)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
        outputStream.close()

        return Uri.fromFile(imageFile)
    }

    private fun galleryCheckPermission() {
        val permissionListener = object : PermissionListener {
            override fun onPermissionGranted(response: PermissionGrantedResponse?) {
                gallery()
            }

            override fun onPermissionDenied(response: PermissionDeniedResponse?) {
                Toast.makeText(
                    this@SignupActivity,
                    "Odbili ste dozvolu da izaberete sliku",
                    Toast.LENGTH_SHORT
                ).show()
                showRotationalDialogPermission()
            }

            override fun onPermissionRationaleShouldBeShown(
                permission: PermissionRequest?,
                token: PermissionToken?
            ) {
                showRotationalDialogPermission()
            }
        }

        Dexter.withContext(this)
            .withPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
            .withListener(permissionListener)
            .onSameThread()
            .check()
    }

    private fun gallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type="image/*"
        startActivityForResult(intent, GALLERY_REQUEST_CODE)
    }

    private fun cameraCheckPermission() {
        Dexter.withContext(this)
            .withPermissions(android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.CAMERA).withListener(
                object  : MultiplePermissionsListener{
                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    report?.let {
                        if(report.areAllPermissionsGranted()){
                            camera()
                        }
                    }
                    }

                    override fun onPermissionRationaleShouldBeShown(
                        p0: MutableList<PermissionRequest>?,
                        p1: PermissionToken?
                    ) {
                        showRotationalDialogPermission()
                    }
                }
                ).onSameThread().check()
    }

    private fun showRotationalDialogPermission() {
        AlertDialog.Builder(this)
            .setMessage("Isklučene su vam dozvole koje su obavezne. Možete ih uključiti u svojim podešavanjima.")
            .setPositiveButton("Idi u podešavanja"){_,_->
                try{
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data=uri
                    startActivity(intent)
                }catch(e:ActivityNotFoundException){
                    e.printStackTrace()
                }
            }
            .setNegativeButton("Poništi"){dialog,_->
                dialog.dismiss()
            }.show()
    }

    private fun camera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(intent, CAMERA_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(resultCode == Activity.RESULT_OK){
            when(requestCode){
                CAMERA_REQUEST_CODE->{
                    val bitmap = data?.extras?.get("data") as Bitmap
                    val imageUri = saveBitmapToFile(bitmap)
                    binding.profileImg.load(bitmap){
                        crossfade(true)
                        crossfade(1000)
                        transformations(CircleCropTransformation())
                    }
                    selectedImageUri=imageUri
                }

                GALLERY_REQUEST_CODE->{
                    binding.profileImg.load(data?.data){
                        crossfade(true)
                        crossfade(1000)
                        transformations(CircleCropTransformation())
                    }
                    selectedImageUri = data?.data!!
                }
            }
        }
    }

}

