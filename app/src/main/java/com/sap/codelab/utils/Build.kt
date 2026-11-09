package com.sap.codelab.utils

import android.os.Build

/**
 * @return `true` if the device's SDK version is greater than or equal
 *         to the specified [sdkVersion], `false` otherwise.
 */
fun isSdkAtLeast(sdkVersion: Int) = Build.VERSION.SDK_INT >= sdkVersion