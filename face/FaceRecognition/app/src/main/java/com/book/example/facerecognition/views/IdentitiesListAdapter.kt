package com.book.example.facerecognition.views

import android.content.Context
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import com.book.example.facerecognition.R
import com.book.example.facerecognition.databinding.IdentityRowItemBinding
import com.book.example.facerecognition.model.IdentityDistance

class IdentitiesListAdapter(context: Context) :
    BaseAdapter() {

    var identities: List<Identity> = emptyList()
    set(value) {
        field = value
        notifyDataSetChanged()
    }

    private val inflater = LayoutInflater.from(context);

    override fun getCount(): Int = identities.size
    override fun getItem(position: Int): Any = identities[position]
    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?,
                         parent: ViewGroup): View {
        val binding = if (convertView != null) {
            IdentityRowItemBinding.bind(convertView)
        } else {
            IdentityRowItemBinding.inflate(inflater, parent, false)
        }
        binding.imgIdentityFace.setImageBitmap(identities[position].face)
        binding.txtIdentityName.text = identities[position].name
        binding.txtIdentityDistance.text = identities[position].distance
        return binding.root
    }
}

data class Identity(
    val face: Bitmap,
    private val identityDistance: IdentityDistance?
) {
    val name = identityDistance?.name ?: ""
    val distance = identityDistance?.distance?.toString() ?: ""
}