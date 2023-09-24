package elfak.mosis.rmas18247

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ReviewsAdapter(private val reviewList: List<ReviewsList>) : RecyclerView.Adapter<ReviewsAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewsAdapter.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.review_item, parent, false)
        return ReviewsAdapter.ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReviewsAdapter.ViewHolder, position: Int) {
        val currentItem = reviewList [position]
        holder.korisnik.text=currentItem.korisnikid
        holder.ocena.text=currentItem.ocena.toString()
        holder.opis.text=currentItem.opis    }

    override fun getItemCount(): Int {
        return reviewList.size
    }


    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val korisnik : TextView = itemView.findViewById(R.id.korisnik)
        val ocena: TextView = itemView.findViewById(R.id.ocena)
        val opis: TextView = itemView.findViewById(R.id.opis)

    }
}