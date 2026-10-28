package com.phoenix.qiur.utils

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.wifi.WifiNetworkSuggestion
import android.os.Build
import android.provider.Settings

fun connectWifi(
    wifi: CodeContent.Wifi,
    wifiLauncher: (Intent) -> Unit,
    openWifiSetting: () -> Unit,
) {
    val canSuggest = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
            (wifi.security == CodeContent.WifiSecurity.NONE ||
                    (wifi.security == CodeContent.WifiSecurity.WPA && !wifi.password.isNullOrEmpty()))
    if (canSuggest) {
        val builder = WifiNetworkSuggestion.Builder()
            .setSsid(wifi.ssid)
            .setIsHiddenSsid(wifi.hidden)
        if (wifi.security == CodeContent.WifiSecurity.WPA) {
            builder.setWpa2Passphrase(wifi.password!!)
        }
        val intent = Intent(Settings.ACTION_WIFI_ADD_NETWORKS).putParcelableArrayListExtra(
            Settings.EXTRA_WIFI_NETWORK_LIST,
            arrayListOf(builder.build())
        )
        try {
            wifiLauncher(intent)
        } catch (_: ActivityNotFoundException) {
            openWifiSetting()
        }
    } else {
        openWifiSetting()
    }
}