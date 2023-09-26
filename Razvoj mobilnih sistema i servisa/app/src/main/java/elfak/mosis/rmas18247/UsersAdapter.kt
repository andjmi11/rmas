package elfak.mosis.rmas18247

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.w3c.dom.Text

class UsersAdapter(private val userList: List<UsersList>):
    RecyclerView.Adapter<UsersAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ime : TextView = itemView.findViewById(R.id.ime)
        val prezime: TextView = itemView.findViewById(R.id.prezime)
        val email : TextView = itemView.findViewById(R.id.email)
        val poeni: TextView = itemView.findViewById(R.id.poeni)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.user_item, parent, false)
        return UsersAdapter.ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return userList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentItem = userList [position]
        holder.ime.text=currentItem.name
        holder.prezime.text=currentItem.surname
        holder.email.text=currentItem.email
        holder.poeni.text= currentItem.points.toString()

    }



}