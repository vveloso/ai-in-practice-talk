package com.book.example.photocaption

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.fragment_picture.*
import org.apache.commons.imaging.Imaging
import org.apache.commons.imaging.Imaging.getMetadata
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata
import org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants
import org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants.EXIF_TAG_USER_COMMENT

class PictureFragment : Fragment() {

    val args : PictureFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        inflater.inflate(R.layout.fragment_picture, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val imageUri = Uri.parse(args.filePath)

        requireContext().contentResolver.openInputStream(imageUri)
            .use {
                val metadata =
                    getMetadata(it, "stream") as JpegImageMetadata
                pictureCaption.text = metadata
                    .findEXIFValueWithExactMatch(EXIF_TAG_USER_COMMENT)
                    .stringValue
            }

        Glide.with(this)
            .load(imageUri)
            .into(this.imageView)
    }

}