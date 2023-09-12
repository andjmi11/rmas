package elfak.mosis.rmas18247

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.widget.Toolbar
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import coil.load
import coil.transform.CircleCropTransformation
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import elfak.mosis.rmas18247.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
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

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        val navigationView = findViewById<NavigationView>(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener(this)

        val toggle = ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.open_nav, R.string.close_nav)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        if(savedInstanceState == null){
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, MapFragment()).commit()
            navigationView.setCheckedItem(R.id.nav_logout) //ovde mi treba nav_map!!!

        }
        firebaseAuth = FirebaseAuth.getInstance()

        getData()

    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.nav_logout -> supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, MapFragment()).commit()
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onBackPressed() {
        if(drawerLayout.isDrawerOpen(GravityCompat.START)){
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            onBackPressedDispatcher.onBackPressed()
        }
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
                                if(profileImageUri == null){
                                    val message ="Nema uri slike"
                                    showErrorMessage(message)
                                }
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
                        when (error.code) {
                            DatabaseError.PERMISSION_DENIED -> {
                                // Korisnik nema dozvolu za pristup podacima
                                val message = "Nemate dozvolu za pristup podacima. Molimo, kontaktirajte podršku."
                                showErrorMessage(message)
                            }
                            DatabaseError.NETWORK_ERROR -> {
                                // Problemi sa internet vezom prilikom dohvatanja podataka
                                val message = "Problemi sa internet vezom. Proverite svoju internet konekciju i pokušajte ponovo."
                                showErrorMessage(message)
                            }
                            else -> {
                                // Nepoznata greška
                                val message = "Došlo je do nepoznate greške. Pokušajte ponovo kasnije."
                                showErrorMessage(message)
                            }
                        }
                    }


                })
        }else{
            Toast.makeText(this, "Current user je null", Toast.LENGTH_SHORT).show()
        }

    }

    private fun showErrorMessage(message: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Greška")
        builder.setMessage(message)
        builder.setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
        val dialog = builder.create()
        dialog.show()
    }

   /* fun clickMenu(view: View) {
        openDrawer(drawerLayout)
    }

    private fun openDrawer(drawerLayout: DrawerLayout) {
        drawerLayout.openDrawer(GravityCompat.START)
    }*/

   /* fun logout(view: View) {
        logoutMenu(this@MainActivity)
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
    }*/
}


