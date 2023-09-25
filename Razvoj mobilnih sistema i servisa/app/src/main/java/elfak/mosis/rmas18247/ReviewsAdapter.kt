package elfak.mosis.rmas18247

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ReviewsAdapter(private val reviewList: List<ReviewsList>,
    private val currentUserUid: String, private val deleteReviewListener: (String) -> Unit)
    : RecyclerView.Adapter<ReviewsAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewsAdapter.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.review_item, parent, false)
        return ReviewsAdapter.ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReviewsAdapter.ViewHolder, position: Int) {
        val currentItem = reviewList [position]
        holder.korisnik.text=currentItem.korisnikid
        holder.ocena.text=currentItem.ocena.toString()
        holder.opis.text=currentItem.opis

        Log.d("review", "Vrednost korisnikId ${currentItem.korisnikid}" +
                ", vrednost prosledjenog id $currentUserUid")


        if (currentItem.korisnikid == currentUserUid) {
            holder.deleteButton.visibility = View.VISIBLE
            holder.likeButton.visibility = View.GONE
            holder.deleteButton.setOnClickListener {
                // Pozivanje funkcije za brisanje recenzije sa odgovarajućim ID-om recenzije
                deleteReviewListener(currentItem.pid)
            }
        } else {
            holder.deleteButton.visibility = View.GONE
            holder.likeButton.visibility = View.VISIBLE
        }
    }

    override fun getItemCount(): Int {
        return reviewList.size
    }


    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val korisnik : TextView = itemView.findViewById(R.id.korisnik)
        val ocena: TextView = itemView.findViewById(R.id.ocena)
        val opis: TextView = itemView.findViewById(R.id.opis)
        val deleteButton: Button = itemView.findViewById(R.id.deleteButton)
        val likeButton:ImageView = itemView.findViewById(R.id.likeImg)
    }
}