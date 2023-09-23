package elfak.mosis.rmas18247

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.w3c.dom.Text

class ReviewsAdapter(private val reviews: List<ReviewPlaces>) : RecyclerView.Adapter<ReviewsAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userNameTextView: TextView = itemView.findViewById(R.id.user_name_text)
        val userSurnameTextView: TextView = itemView.findViewById(R.id.user_surname_text)
       // val descriptionTextView: TextView = itemView.findViewById(R.id.description_text)
       // val ratingBar: RatingBar = itemView.findViewById(R.id.rating_bar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_review, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val review = reviews[position]
        holder.userNameTextView.text = review.userName
        holder.userSurnameTextView.text = review.userSurname
      //  holder.descriptionTextView.text = review.description
       // holder.ratingBar.rating = review.rating
    }

    override fun getItemCount(): Int {
        return reviews.size
    }
}
