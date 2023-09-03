package elfak.mosis.rmas18247

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import coil.load
import coil.transform.CircleCropTransformation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import elfak.mosis.rmas18247.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var drawerLayout: DrawerLayout

    private  var  firebaseDatabase: FirebaseDatabase?= null
    private var databaseReference: DatabaseReference?= null
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        drawerLayout = findViewById(R.id.drawer_menu)

        firebaseAuth = FirebaseAuth.getInstance()

        getData()
    }

    private fun getData() {
        val currentUser = firebaseAuth.currentUser

        if(currentUser != null) {
            val uid = currentUser.uid
            //prijavljen korisnik

            FirebaseDatabase.getInstance().getReference("users")?.child(uid)
                ?.addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val user = snapshot.getValue(Users::class.java)
                            if (user != null) {
                                val email = user.email
                                val name = user.name
                                val surname = user.surname
                                val phone = user.phone
                                val profileImageUriString = user.imgUrl

                                val profileImageUri = Uri.parse(profileImageUriString)

                                val emailTextView = findViewById<TextView>(R.id.emailTextView)
                                val nameTextView = findViewById<TextView>(R.id.nameTextView)
                                val profileImageView = findViewById<ImageView>(R.id.profileImageView)


                                emailTextView.text = "Email: $email"
                                nameTextView.text = "Name: $name"
                                profileImageView.load(profileImageUri) {
                                    crossfade(true)
                                    crossfade(1000)
                                    transformations(CircleCropTransformation())
                                }




                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        //greska pri dohvatanju podataka
                    }

                })
        }else{
            //korisnik nije prijavljen
        }

    }

    fun clickMenu(view: View) {
        openDrawer(drawerLayout)
    }

    private fun openDrawer(drawerLayout: DrawerLayout) {
        drawerLayout.openDrawer(GravityCompat.START)
    }

    fun logout(view: View) {
        logoutMenu(this@MainActivity)
    }

    fun callMap(view: View) {
        //
    }

    private fun logoutMenu(mainActivity: MainActivity) {
        val builder = AlertDialog.Builder(mainActivity)

        builder.setTitle("Odjava")

        builder.setMessage("Da li ste sigurni da želite da se odjavite?")

        builder.setPositiveButton("Yes") { dialogInterface, _ ->

            val intent = Intent(mainActivity, LoginActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            mainActivity.startActivity(intent)
            mainActivity.finish()
        }

        builder.setNegativeButton("No") { dialogInterface, _ ->
            dialogInterface.dismiss()
        }

        val dialog = builder.create()

        dialog.show()
    }
}


