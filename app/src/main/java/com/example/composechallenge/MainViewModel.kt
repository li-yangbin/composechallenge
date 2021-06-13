package com.example.composechallenge

import android.annotation.SuppressLint
import android.app.Application
import android.content.ContentResolver
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import android.telecom.TelecomManager
import android.text.TextUtils
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.core.database.getStringOrNull
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.CONFLATED
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val resolver : ContentResolver get() = getApplication<Application>().contentResolver
    private val _permissionGranted = MutableLiveData(false)
    val permissionGranted : LiveData<Boolean> = _permissionGranted

    private var permissionRequestLauncher : ActivityResultLauncher<Array<String>>? = null

    private val searchInputChannel: Channel<String?> = Channel(capacity = CONFLATED)
    private val searchInputFlow = searchInputChannel.consumeAsFlow().debounce(1000)
        .onStart { emit("") }

    @ExperimentalCoroutinesApi
    val contactData by lazy {
        callbackFlow<Unit> {
            Log.i("leeyb", "callbackFlow collected")
            val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    Log.i("leeyb", "Contact db change");
                    offer(Unit)
                }
            }

            resolver.registerContentObserver(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                false, observer)

            awaitClose {
                Log.i("leeyb", "callbackFlow disposed")
                resolver.unregisterContentObserver(observer)
            }
        }.onStart<Unit> {
            emit(Unit)
        }.map {
            loadContact()
        }.combine(searchInputFlow) { list, search ->
            if (TextUtils.isEmpty(search)) {
                list
            } else {
                val searchedList = mutableListOf<ContactInfo>()
                list.forEach {
                    if (it.name.contains(search!!, ignoreCase = true)) {
                        searchedList += it
                    }
                }
                searchedList
            }
        }.shareIn(viewModelScope, SharingStarted.Lazily, replay = 1)
    }

    private suspend fun loadContact() = withContext(Dispatchers.IO) {
        val contactList = mutableListOf<ContactInfo>()
        resolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            null, null, null, null)?.use {
            if (it.count > 0) {
                it.moveToFirst()
                do {
                    val phone = it.getStringOrNull(it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER))
                    val name = it.getStringOrNull(it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
                    if (!TextUtils.isEmpty(name)) {
                        contactList += ContactInfo(name!!, phone)
                    }
                } while (it.moveToNext())
            }

            contactList += ContactInfo("Simon", "23465123")
            contactList += ContactInfo("Amber", "356345623")
            contactList += ContactInfo("Sharon", "3453453")
            contactList += ContactInfo("Tom", "682649834")
            contactList += ContactInfo("Cris", "348761")
            contactList += ContactInfo("Anna", "3676174")
            contactList += ContactInfo("Will", "34548772")
            contactList += ContactInfo("Harry", "65473")
            contactList += ContactInfo("Peter", "456773")
            contactList += ContactInfo("Zoe", "788572434")

        }
        contactList
    }

    @ExperimentalAnimationApi
    fun requestContactPermission(activity: MainActivity) {
        if (activity.checkSelfPermission(android.Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
            && activity.checkSelfPermission(android.Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            _permissionGranted.value = true
        } else {
            _permissionGranted.value = false
            if (permissionRequestLauncher == null) {
                permissionRequestLauncher = activity
                    .registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
                        it.forEach { entry ->
                            if (!entry.value) {
                                _permissionGranted.value = false
                                return@registerForActivityResult
                            }
                        }
                    _permissionGranted.value = true
                }
            }
            permissionRequestLauncher!!.launch(arrayOf(android.Manifest.permission.READ_CONTACTS,
                android.Manifest.permission.CALL_PHONE))
        }
    }

    fun querySearch(text: String?) {
        searchInputChannel.offer(text)
    }

    @SuppressLint("MissingPermission")
    fun dialContact(contactInfo: ContactInfo) {
        val telecomManager = getApplication<Application>().getSystemService(TelecomManager::class.java)
        telecomManager.placeCall(Uri.parse("tel:" + contactInfo.number!!), null)
    }
}