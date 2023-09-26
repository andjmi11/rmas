package elfak.mosis.rmas18247

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import coil.load
import coil.transform.CircleCropTransformation
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*


class ProfileFragment : Fragment() {

    private lateinit var firebaseAuth:FirebaseAuth
    private  lateinit var databaseRef:DatabaseReference

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        firebaseAuth = FirebaseAuth.getInstance()
        databaseRef = FirebaseDatabase.getInstance().reference.child("users")

        val emailTextView = view.findViewById<TextView>(R.id.emailTextView)
        val nameTextView = view.findViewById<TextView>(R.id.nameTextView)
        val surnameTextView = view.findViewById<TextView>(R.id.surnameTextView)
        val phoneTextView = view.findViewById<TextView>(R.id.phoneTextView)
        val pointsTextView = view.findViewById<TextView>(R.id.pointsTextView)
        val profileImageView = view.findViewById<ImageView>(R.id.profileImageView)
        val buttonChangePassword = view.findViewById<Button>(R.id.buttonChangePass)

        buttonChangePassword.setOnClickListener{
            val inflater = layoutInflater
            val dialogView = inflater.inflate(R.layout.dialog_changepassword, null)

            val alertDialogBuilder = AlertDialog.Builder(requireContext())
            alertDialogBuilder.setView(dialogView)
            val alertDialog = alertDialogBuilder.create()

            val oldPasswordEditText = dialogView.findViewById<EditText>(R.id.oldPasswordEditText)
            val newPasswordEditText = dialogView.findViewById<EditText>(R.id.newPasswordEditText)
            val changePasswordButton = dialogView.findViewById<Button>(R.id.changePasswordButton)

            val user = FirebaseAuth.getInstance().currentUser

            changePasswordButton.setOnClickListener {
                val oldPassword = oldPasswordEditText.text.toString()
                val newPassword = newPasswordEditText.text.toString()

                // Provera da li su unete stare i nove lozinke
                if (oldPassword.isNotEmpty() && newPassword.isNotEmpty()) {
                    val credential = EmailAuthProvider.getCredential(user?.email.toString(), oldPassword)

                    // Ponovna autentikacija sa starom lozinkom
                    user?.reauthenticate(credential)
                        ?.addOnCompleteListener { authTask ->
                            if (authTask.isSuccessful) {
                                // Ponovna autentikacija uspešna, sada možete promeniti lozinku
                                user.updatePassword(newPassword)
                                    .addOnCompleteListener { updateTask ->
                                        if (updateTask.isSuccessful) {
                                            // Uspešno promenjena lozinka
                                            Toast.makeText(requireContext(), "Lozinka uspešno promenjena.", Toast.LENGTH_SHORT).show()
                                            // Možete korisnika odjaviti i preusmeriti ga na ekran za prijavu
                                           // FirebaseAuth.getInstance().signOut()
                                            //startActivity(Intent(requireContext(), LoginActivity::class.java))

                                        } else {
                                            // Greška prilikom promene lozinke
                                            Toast.makeText(requireContext(), "Greška prilikom promene lozinke.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                            } else {
                                // Greška pri ponovnoj autentikaciji
                                Toast.makeText(requireContext(), "Netačna stara lozinka.", Toast.LENGTH_SHORT).show()
                            }
                        }
                } else {
                    // Poruka korisniku da unese obe lozinke
                    Toast.makeText(requireContext(), "Unesite staru i novu lozinku.", Toast.LENGTH_SHORT).show()
                }
            }


            alertDialog.show()

        }
        val currentUser = firebaseAuth.currentUser
        val uid = currentUser?.uid

        if (uid != null) {
            databaseRef.child(uid).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val user = snapshot.getValue(Users::class.java)

                        // Sada možete postaviti podatke u TextView-ove i ImageView
                        if (user != null) {
                            emailTextView.text = "Email: ${user.email}"
                            nameTextView.text = "Ime: ${user.name}"
                            surnameTextView.text = "Prezime: ${user.surname}"
                            phoneTextView.text = "Telefon: ${user.phone}"
                            pointsTextView.text = "Broj poena: ${user.points}"
                            val profileImageUriString = user.imgUrl

                            val profileImageUri = Uri.parse(profileImageUriString)

                            Log.d("profil", "Vrednost urija $profileImageUri")

                            profileImageView.load(profileImageUri) {
                                crossfade(true)
                                crossfade(1000)
                                transformations(CircleCropTransformation())
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Tretirajte greške ako je potrebno
                }
            })
        }
        return view
    }

}