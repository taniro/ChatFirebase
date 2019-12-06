package tads.eaj.ufrn.chatfirebase


import android.view.View
import android.widget.ImageView
import android.widget.TextView

class MensagemViewHolder  (v: View){

    var photoImageView: ImageView
    var messageTextView:TextView
    var authorTextView:TextView

    init {
        photoImageView = v.findViewById(R.id.photoImageView)
        messageTextView = v.findViewById(R.id.messageTextView)
        authorTextView = v.findViewById(R.id.nameTextView)
    }


}
