package com.sap.codelab.view.detail

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.sap.codelab.R
import com.sap.codelab.databinding.ActivityViewMemoBinding
import com.sap.codelab.model.Memo
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Activity that allows a user to see the details of a memo.
 */
internal class ViewMemo : AppCompatActivity() {

    private lateinit var binding: ActivityViewMemoBinding

    private var marker: Marker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewMemoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.contentCreateMemo.memoTitle.isEnabled = false
        binding.contentCreateMemo.memoDescription.isEnabled = false
        val model = ViewModelProvider(
            owner = this,
            factory = ViewMemoViewModel.Factory,
        )[ViewMemoViewModel::class.java]
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment

        // Observe the memo state flow for changes
        lifecycleScope.launch {
            val googleMapDeferred = async {
                mapFragment.awaitGoogleMap()
            }

            repeatOnLifecycle(Lifecycle.State.STARTED) {
                val memoFlow = model.memo.filterNotNull()

                // Update the UI text fields without waiting for the GoogleMap object to be obtained
                launch {
                    memoFlow.collect(::updateTextFields)
                }

                // Since the GoogleMap object is asynchronously initialized,
                // run a second collector to update the map when we have the object
                launch {
                    val googleMap = googleMapDeferred.await()
                    memoFlow.collect { memo -> updateMap(googleMap, memo) }
                }
            }
        }
    }

    /**
     * Returns this fragment's [GoogleMap] instance. Suspends the calling coroutine
     * while waiting to obtain the instance.
     */
    private suspend fun SupportMapFragment.awaitGoogleMap(): GoogleMap =
        suspendCancellableCoroutine { continuation ->
            getMapAsync { map ->
                continuation.resume(map)
            }
        }

    /**
     * Updates the text fields of the UI with the given memo details.
     *
     * @param memo - the memo whose details are to be displayed.
     */
    private fun updateTextFields(memo: Memo) {
        binding.contentCreateMemo.run {
            memoTitle.setText(memo.title)
            memoDescription.setText(memo.description)
        }
    }

    /**
     * Updates the specified [map] with the details of the specified [memo].
     */
    private fun updateMap(map: GoogleMap, memo: Memo) {
        val currentMarker = marker
        val position = LatLng(memo.reminderLatitude, memo.reminderLongitude)

        if (currentMarker == null) {
            val options = MarkerOptions().position(position)
            marker = map.addMarker(options)
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(position, MAP_ZOOM))
        } else {
            currentMarker.position = position
        }
    }

    companion object {
        internal const val BUNDLE_MEMO_ID: String = "memoId"
    }
}

private const val MAP_ZOOM = 15f
