package com.example.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NetworkMonitor(context: Context) {

    private val connectivityManager =
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private val _isOnline = MutableStateFlow(checkOnlineNow())
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _isOnline.value = checkOnlineNow()
        }

        override fun onLost(network: Network) {
            _isOnline.value = checkOnlineNow()
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            _isOnline.value = checkOnlineNow()
        }
    }

    init {
        runCatching {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager?.registerNetworkCallback(request, callback)
        }
    }

    fun isCurrentlyOnline(): Boolean {
        val online = checkOnlineNow()
        _isOnline.value = online
        return online
    }

    private fun checkOnlineNow(): Boolean {
        val cm = connectivityManager ?: return false
        val active = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(active) ?: return false
        val hasInternet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        val notCaptive = !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL)
        if (!hasInternet || !notCaptive) return false
        if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) return true
        val hasKnownTransport = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
            caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
        if (hasKnownTransport) return true
        @Suppress("DEPRECATION")
        return cm.activeNetworkInfo?.isConnected == true
    }
}
