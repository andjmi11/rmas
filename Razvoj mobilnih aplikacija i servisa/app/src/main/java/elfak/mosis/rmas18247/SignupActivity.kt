package elfak.mosis.rmas18247

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.view.WindowManager.BadTokenException
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import coil.load
import com.google.android.gms.cast.framework.media.ImagePicker
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import elfak.mosis.rmas18247.databinding.ActivitySignupBinding

class SignupActivity : AppCompatActivity() {


    private lateinit var binding: ActivitySignupBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var usersRef: DatabaseReference

    private val CAMERA_REQUEST_CODE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        val database = FirebaseDatabase.getInstance()
        usersRef = database.reference.child("users")

        binding.btnCamera.setOnClickListener{
            cameraCheckPermission()
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
                                val user = firebaseAuth.currentUser
                                val userId = user?.uid ?: ""

                                val userData = HashMap<String, Any>()
                                userData["email"] = email
                                userData["name"] = name
                                userData["surname"] = surname
                                userData["phone"] = phone

                                usersRef.child(userId).setValue(userData)
                                    .addOnCompleteListener { databaseTask ->
                                        if (databaseTask.isSuccessful) {
                                            val intent = Intent(this, LoginActivity::class.java)
                                            startActivity(intent)
                                        } else {
                                            Toast.makeText(
                                                this,
                                                "Greška pri dodavanju podataka.",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
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

                    binding.profileImg.load(bitmap)
                }
            }
        }
    }
}

