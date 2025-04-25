import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.abeernoor.i221122.InviteFriend
import com.abeernoor.i221122.R

class InviteFriendsAdapter(private val friends: List<InviteFriend>) :
    RecyclerView.Adapter<InviteFriendsAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImage: ImageView = itemView.findViewById(R.id.inviteProfileImage)
        val name: TextView = itemView.findViewById(R.id.inviteFriendName)
        val inviteButton: Button = itemView.findViewById(R.id.inviteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_invite_friend, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val friend = friends[position]
        holder.name.text = friend.name
        holder.profileImage.setImageResource(friend.profileImage)

    }

    override fun getItemCount(): Int = friends.size
}
