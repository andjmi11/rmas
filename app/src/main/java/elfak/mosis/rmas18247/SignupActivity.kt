package elfak.mosis.rmas18247

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import elfak.mosis.rmas18247.databinding.ActivitySignupBinding

class SignupActivity : AppCompatActivity() {


    private lateinit var binding: ActivitySignupBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var usersRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        val database = FirebaseDatabase.getInstance()
        usersRef = database.reference.child("users")

        binding.signupButton.setOnClickListener{
            val email = binding.signupEmail.text.toString()
            val name = binding.signupName.text.toString()
            val surname = binding.signupSurname.text.toString()
            val phone = binding.signupPhone.text.toString()
            val password = binding.signupPassword.text.toString()
            val confirmPassword = binding.signupPasswordConfirm.text.toString()

            if(email.isNotEmpty() && name.isNotEmpty() && surname.isNotEmpty() &&
                phone.isNotEmpty() && password.isNotEmpty() && confirmPassword.isNotEmpty()){

                if(password==confirmPassword){
                    firebaseAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                        if(task.isSuccessful){
                            val user = firebaseAuth.currentUser
                            val userId = user?.uid ?: ""

                            val userData = HashMap<String, Any>()
                            userData["email"] = email
                            userData["name"] = name
                            userData["surname"] = surname
                            userData["phone"] = phone

                            usersRef.child(userId).setValue(userData).addOnCompleteListener { databaseTask ->
                                if (databaseTask.isSuccessful) {
                                    val intent = Intent(this, LoginActivity::class.java)
                                    startActivity(intent)
                                } else {
                                    Toast.makeText(this, "Greška pri dodavanju podataka.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            Toast.makeText(this, task.exception.toString(), Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(this, "Lozinke se ne poklapaju!", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Morate popuniti sva polja!", Toast.LENGTH_SHORT).show()
            }
        }

        binding.loginRedirectText.setOnClickListener{
            val loginIntent = Intent(this, LoginActivity::class.java)
            startActivity(loginIntent)
        }
    }
}