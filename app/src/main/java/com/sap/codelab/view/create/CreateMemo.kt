package com.sap.codelab.view.create

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.annotation.StringRes
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
import com.google.android.material.snackbar.Snackbar
import com.sap.codelab.R
import com.sap.codelab.databinding.ActivityCreateMemoBinding
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

/**
 * Activity that allows a user to create a new Memo.
 */
internal class CreateMemo : AppCompatActivity() {

    private lateinit var binding: ActivityCreateMemoBinding
    private lateinit var model: CreateMemoViewModel

    /**
     * Holds the currently selected location on the map; or `null` if no location is selected.
     */
    private var marker: Marker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateMemoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        model = ViewModelProvider(
            owner = this,
            factory = CreateMemoViewModel.Factory,
        )[CreateMemoViewModel::class.java]

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment

        mapFragment.getMapAsync { map ->
            map.setOnMapClickListener { position -> onMapClick(map, position) }

            if (savedInstanceState != null &&
                savedInstanceState.containsKey(MARKER_LAT_BUNDLE_KEY) &&
                savedInstanceState.containsKey(MARKER_LNG_BUNDLE_KEY)
            ) {
                val position = LatLng(
                    savedInstanceState.getDouble(MARKER_LAT_BUNDLE_KEY),
                    savedInstanceState.getDouble(MARKER_LNG_BUNDLE_KEY),
                )
                val options = MarkerOptions().position(position)
                marker = map.addMarker(options)
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    model.memoRegisterResult.filterNotNull().collect { result ->
                        setResult(result.resultCode)
                        finish()
                    }
                }
                launch {
                    model.memoStoreFailed.collect { failed ->
                        if (failed) {
                            showError(R.string.memo_save_failed, Snackbar.LENGTH_INDEFINITE) {
                                saveMemo()
                            }
                            model.clearErrorMessage()
                        }
                    }
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        marker?.let { currentMarker ->
            outState.putDouble(MARKER_LAT_BUNDLE_KEY, currentMarker.position.latitude)
            outState.putDouble(MARKER_LNG_BUNDLE_KEY, currentMarker.position.longitude)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_create_memo, menu)
        return true
    }

    /**
     * Handles actionbar interactions.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_save -> {
                saveMemo()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * Adds a new, or updates the current marker on the map when the user picks a location.
     */
    private fun onMapClick(map: GoogleMap, newPosition: LatLng) {
        val currentMarker = marker

        if (currentMarker == null) {
            val markerOptions = MarkerOptions().position(newPosition)
            marker = map.addMarker(markerOptions)
        } else {
            currentMarker.position = newPosition
        }

        map.animateCamera(CameraUpdateFactory.newLatLng(newPosition))
    }

    /**
     * Saves the memo if the input is valid; otherwise shows the corresponding error messages.
     */
    private fun saveMemo() {
        binding.contentCreateMemo.run {
            model.updateMemo(
                title = memoTitle.text.toString(),
                description = memoDescription.text.toString(),
                latitude = marker?.position?.latitude ?: CreateMemoViewModel.NO_LOCATION,
                longitude = marker?.position?.longitude ?: CreateMemoViewModel.NO_LOCATION,
            )
            if (model.isMemoValid()) {
                model.saveMemo()
            } else {
                memoTitleContainer.error = getErrorMessage(
                    model.hasTitleError(),
                    R.string.memo_title_empty_error,
                )
                memoDescription.error = getErrorMessage(
                    model.hasTextError(),
                    R.string.memo_text_empty_error,
                )

                if (model.hasLocationError()) {
                    showError(R.string.memo_location_empty_error, Snackbar.LENGTH_SHORT)
                }
            }
        }
    }

    /**
     * Shows a snackbar containing the specified [resId] for the specified [duration].
     *
     * If [onRetry] is specified, an action button will appear next to the message
     * for retrying the failed action.
     */
    private fun showError(@StringRes resId: Int, duration: Int, onRetry: (() -> Unit)? = null) {
        val builder = Snackbar.make(binding.root, resId, duration)

        if (onRetry != null) {
            builder.setAction(R.string.button_retry) {
                onRetry()
            }
        }

        builder.show()
    }

    /**
     * Returns the error message if there is an error, or `null` otherwise.
     *
     * @param hasError          - whether there is an error.
     * @param errorMessageResId - the resource id of the error message to show.
     * @return the error message if there is an error, or `null` otherwise.
     */
    private fun getErrorMessage(hasError: Boolean, @StringRes errorMessageResId: Int): String? {
        return if (hasError) {
            getString(errorMessageResId)
        } else {
            null
        }
    }
}

private const val MARKER_LAT_BUNDLE_KEY = "MapMarkerLatitudeKey"
private const val MARKER_LNG_BUNDLE_KEY = "MapMarkerLongitudeKey"
