package com.sap.codelab.view.home

import android.Manifest
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.withResumed
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.sap.codelab.R
import com.sap.codelab.databinding.ActivityHomeBinding
import com.sap.codelab.model.GeofenceRegisterResult
import com.sap.codelab.model.Memo
import com.sap.codelab.utils.hasBackgroundLocationPermission
import com.sap.codelab.utils.hasNotificationPermission
import com.sap.codelab.view.create.CreateMemo
import com.sap.codelab.view.detail.ViewMemo
import com.sap.codelab.work.GeofenceRegisterWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The main activity of the app. Shows a list of recorded memos and lets the user add new memos.
 */
internal class Home : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var model: HomeViewModel

    private lateinit var prefs: SharedPreferences

    private var canRequestLocationPermission = false
    private var shouldShowMemoLimitMessage = true

    private val foregroundLocationLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
        ::onLocationPermissionsResult,
    )

    private val backgroundLocationLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
        ::onBackgroundLocationPermissionResult,
    )

    private val notificationLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            // Geofencing won't be able to serve the events without notifications
        }

    private val createMemoLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
        ::onCreateMemoResult,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        model = ViewModelProvider(this, HomeViewModel.Factory)[HomeViewModel::class.java]

        // Setup the adapter and the recycler view
        val adapter = initializeAdapter()
        setupRecyclerView(adapter)

        binding.fab.setOnClickListener {
            // Handles clicks on the FAB button > creates a new Memo
            val intent = Intent(this@Home, CreateMemo::class.java)
            createMemoLauncher.launch(intent)
        }

        lifecycleScope.launch {
            launch {
                prefs = withContext(Dispatchers.IO) {
                    getPreferences(MODE_PRIVATE)
                }
                canRequestLocationPermission = prefs.getBoolean(PERMISSION_KEY, true)
                shouldShowMemoLimitMessage = prefs.getBoolean(MEMO_LIMIT_KEY, true)

                if (savedInstanceState == null && canRequestLocationPermission) {
                    requestLocationPermissions()
                }
            }

            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    model.isShowAll.collect {
                        invalidateOptionsMenu()
                    }
                }
                launch {
                    model.memos.collect { memos ->
                        adapter.setItems(memos)
                    }
                }
            }
        }
    }

    /**
     * Initializes the adapter and sets the needed callbacks.
     */
    private fun initializeAdapter() = MemoAdapter(
        mutableListOf(),
        { view ->
            // Implementation for when the user selects a row to show the detail view
            showMemo((view.tag as Memo).id)
        },
        { checkbox, isChecked ->
            // Implementation for when the user marks a memo as completed
            model.updateMemo(checkbox.tag as Memo, isChecked)
        },
    )

    /**
     * Opens the Memo detail view for the given memoId.
     *
     * @param memoId    - the id of the memo to be shown.
     */
    private fun showMemo(memoId: Long) {
        val intent = Intent(this@Home, ViewMemo::class.java)
        intent.putExtra(ViewMemo.BUNDLE_MEMO_ID, memoId)
        startActivity(intent)
    }

    /**
     * Initializes the recycler view to display the list of memos.
     */
    private fun setupRecyclerView(adapter: MemoAdapter) {
        binding.contentHome.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@Home, LinearLayoutManager.VERTICAL, false)
            this.adapter = adapter
            addItemDecoration(
                DividerItemDecoration(
                    this@Home,
                    (layoutManager as LinearLayoutManager).orientation,
                ),
            )
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_home, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val isShowAll = model.isShowAll.value
        menu.findItem(R.id.action_show_all).isVisible = !isShowAll
        menu.findItem(R.id.action_show_open).isVisible = isShowAll
        return super.onPrepareOptionsMenu(menu)
    }

    /**
     * Handles actionbar interactions.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_show_all -> {
                model.loadAllMemos()
                true
            }

            R.id.action_show_open -> {
                model.loadOpenMemos()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun onCreateMemoResult(result: ActivityResult) {
        when (result.resultCode) {
            GeofenceRegisterResult.MissingPermission.resultCode ->
                if (canRequestLocationPermission) {
                    showErrorDialog(
                        title = R.string.location_permissions,
                        message = R.string.missing_permissions,
                        positiveButton = R.string.button_grant,
                        onPositiveButtonClick = { dialog ->
                            requestLocationPermissions()
                            dialog.dismiss()
                        },
                        negativeButton = R.string.button_dismiss,
                        onNegativeButtonClick = { dialog ->
                            canRequestLocationPermission = false

                            prefs.edit {
                                putBoolean(PERMISSION_KEY, false)
                            }
                            dialog.dismiss()
                        },
                    )
                }

            GeofenceRegisterResult.LimitReached.resultCode -> if (shouldShowMemoLimitMessage) {
                showErrorDialog(
                    title = R.string.memo_geofences_limit,
                    message = R.string.memo_geofences_limit_description,
                    positiveButton = android.R.string.ok,
                    onPositiveButtonClick = { dialog ->
                        prefs.edit { putBoolean(MEMO_LIMIT_KEY, false) }
                        dialog.dismiss()
                    },
                )
            }
        }
    }

    private fun requestLocationPermissions() {
        foregroundLocationLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
            ),
        )
    }

    private fun onLocationPermissionsResult(result: Map<String, Boolean>) {
        val fineLocationPermissionGranted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val backgroundLocationPermissionGranted = hasBackgroundLocationPermission()

        when {
            fineLocationPermissionGranted && backgroundLocationPermissionGranted ->
                requestNotificationsPermission()

            fineLocationPermissionGranted -> requestBackgroundLocationPermission()

            // Schedule a periodic check for background location permission grant
            else -> GeofenceRegisterWorker.schedule(this)
        }
    }

    private fun requestBackgroundLocationPermission() {
        if (!hasBackgroundLocationPermission()) {
            backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
    }

    private fun onBackgroundLocationPermissionResult(granted: Boolean) {
        if (granted) {
            // Schedule geofence registration of all open memos
            GeofenceRegisterWorker.schedule(this)
            requestNotificationsPermission()
        }
    }

    private fun requestNotificationsPermission() {
        if (!hasNotificationPermission()) {
            lifecycleScope.launch {
                withResumed {
                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    private fun showErrorDialog(
        @StringRes title: Int,
        @StringRes message: Int,
        @StringRes positiveButton: Int,
        onPositiveButtonClick: (DialogInterface) -> Unit,
        @StringRes negativeButton: Int? = null,
        onNegativeButtonClick: ((DialogInterface) -> Unit)? = null,
    ) {
        val builder = AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(positiveButton) { dialog, _ ->
                onPositiveButtonClick(dialog)
            }

        if (negativeButton != null && onNegativeButtonClick != null) {
            builder.setNegativeButton(negativeButton) { dialog, _ ->
                onNegativeButtonClick(dialog)
            }
        }

        builder.show()
    }
}

private const val PERMISSION_KEY = "SAP PERMISSION_KEY"
private const val MEMO_LIMIT_KEY = "SAP MEMO_LIMIT_KEY"
