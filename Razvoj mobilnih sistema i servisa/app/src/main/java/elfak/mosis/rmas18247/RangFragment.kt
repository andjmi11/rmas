package elfak.mosis.rmas18247

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*

class RangFragment : Fragment() {

    private lateinit var dbRef: DatabaseReference
    private lateinit var userRecyclerView: RecyclerView
    private lateinit var userArray: ArrayList<UsersList>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_place, container, false)

        userRecyclerView = view.findViewById(R.id.placesList)
        userRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        userRecyclerView.setHasFixedSize(true)

        userArray = arrayListOf<UsersList>()
        getUsersData()

        return view
    }


    private fun getUsersData() {
        dbRef = FirebaseDatabase.getInstance().getReference("users")
        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    for (userSnapshot in snapshot.children) {
                        val user = userSnapshot.getValue(UsersList::class.java)
                        if (user != null) {
                            userArray.add(
                                UsersList(user.email, user.name, user.surname, user.points)
                            )
                            userRecyclerView.adapter =
                                UsersAdapter(userArray)
                        }
                        userArray.sortByDescending { it.points }
                        userRecyclerView.adapter =
                            UsersAdapter(userArray)
                        userRecyclerView.adapter?.notifyDataSetChanged()
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }

        })


    }
}
