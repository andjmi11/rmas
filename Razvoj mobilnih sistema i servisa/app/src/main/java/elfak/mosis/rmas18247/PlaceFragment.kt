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

class PlaceFragment : Fragment() {

    private lateinit var dbRef: DatabaseReference
    private lateinit var placeRecyclerView: RecyclerView
    private lateinit var placeArray: ArrayList<PlacesList>


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_place, container, false)

        placeRecyclerView = view.findViewById(R.id.placesList)
        placeRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        placeRecyclerView.setHasFixedSize(true)

        placeArray = arrayListOf<PlacesList>()
        getPlacesData()

        return view

    }

    private fun getPlacesData() {
        dbRef = FirebaseDatabase.getInstance().getReference("places")
        dbRef.addValueEventListener(object:ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if(snapshot.exists()){
                    val userRef = FirebaseDatabase.getInstance().getReference("users")
                    for(placeSnapshot in snapshot.children){
                        val place = placeSnapshot.getValue(PlacesList::class.java)
                       if(place!= null){
                            val kreatorID = place.kreatorID
                            userRef.child(kreatorID).addListenerForSingleValueEvent(object: ValueEventListener{
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    if(snapshot.exists()){
                                        val user = snapshot.getValue(Users::class.java)
                                        if(user != null){
                                            placeArray.add(PlacesList(place.naslov.toString(), " ", place.mesto.toString(), user.name +  " " + user.surname, place.dateCreated))
                                            placeRecyclerView.adapter=PlacesAdapter(placeArray, requireActivity() as AppCompatActivity)
                                        }
                                    }
                                }

                                override fun onCancelled(error: DatabaseError) {

                                }

                            })

                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }

        })
    }
}