package elfak.mosis.rmas18247

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView

class PlacesAdapter(private val placeList: List<PlacesList>, private val activity: AppCompatActivity) : RecyclerView.Adapter<PlacesAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val naslov : TextView = itemView.findViewById(R.id.naslov)
        val mesto :TextView=itemView.findViewById(R.id.mesto)
        val kreator: TextView=itemView.findViewById(R.id.kreator)
        val datum: TextView = itemView.findViewById(R.id.datumKreiranja)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.place_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
       val currentItem = placeList [position]
        holder.naslov.text=currentItem.naslov
        holder.mesto.text=currentItem.mesto
        holder.kreator.text=currentItem.kreatorID
        holder.datum.text=currentItem.dateCreated


    }

    override fun getItemCount(): Int {
        return placeList.size
    }
}
