package com.example.composechallenge

import android.app.Application
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.composechallenge.ui.theme.ComposeChallengeTheme
import java.util.*

@ExperimentalAnimationApi
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val viewModel = ViewModelProvider(this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application))
            .get(MainViewModel::class.java)

        viewModel.requestContactPermission(this)

        setContent {
            ComposeChallengeTheme {
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colors.background) {
                    ContactSearchHoisted {
                        viewModel.requestContactPermission(this@MainActivity)
                    }
                }
            }
        }
    }
}

@ExperimentalAnimationApi
@Composable
fun ContactSearchHoisted(request: () -> Unit) {
    val appContext = LocalContext.current.applicationContext as Application
    val mainViewModel = viewModel<MainViewModel>(
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(appContext))
    val permissionGrantedHoist by mainViewModel.permissionGranted.observeAsState(false)
    if (permissionGrantedHoist) {
        val contactList by mainViewModel.contactData.collectAsState(initial = mutableListOf())
        ContactSearch(contactList, onClick = {
            mainViewModel.dialContact(it)
        }) {
            mainViewModel.querySearch(it)
        }
    } else {
        Box(modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center) {
            TextButton(onClick = request) {
                Text(text = stringResource(id = R.string.request_permission))
            }
        }
    }
}

@ExperimentalAnimationApi
@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    ComposeChallengeTheme {
        ContactSearch(mutableListOf(ContactInfo("Lily", "12345"), ContactInfo("Sam", "3214")))
    }
}

@ExperimentalAnimationApi
@Composable
fun ContactSearch(contactList: List<ContactInfo>,
                  onClick: ((ContactInfo) -> Unit)? = null,
                  onSearch: ((String) -> Unit)? = null) {
    Column(
        modifier = Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        var text by rememberSaveable {
            mutableStateOf("")
        }
        val inputCallback: (String) -> Unit = { input: String ->
            text = input
            onSearch?.invoke(input)
        }
        TextField(
            value = text,
            onValueChange = inputCallback,
            modifier = Modifier.fillMaxWidth(),
            label = {
            Text(text = "Search for Contact")
        })
        AnimatedVisibility(
            visible = contactList.isNotEmpty(),
            enter = fadeIn(),
            exit = fadeOut()) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(contactList) { info ->
                    ContactItem(keyword = text, contact = info, onClick = onClick)
                }
            }
        }
        AnimatedVisibility(
            visible = contactList.isEmpty(),
            enter = fadeIn(),
            exit = fadeOut()) {
            Box(modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center) {
                Text(text = "Empty")
            }
        }
    }
}

@Composable
fun ContactItem(keyword: String?, contact: ContactInfo, onClick: ((ContactInfo) -> Unit)?) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(all = 8.dp)
            .size(48.dp),
        elevation = 4.dp) {
        Row(
            modifier = Modifier
                .clickable(onClick = { onClick?.invoke(contact) })
                .padding(start = 12.dp),
            verticalAlignment = Alignment.CenterVertically) {

            val text = buildAnnotatedString {
                var rawString = contact.name
                val tagStyle = MaterialTheme.typography.subtitle1.toSpanStyle().copy(
                    background = MaterialTheme.colors.primary.copy(alpha = 0.1f)
                )
                do {
                    val nextIndex = if (keyword?.isNotEmpty()!!) rawString.indexOf(keyword, ignoreCase = true) else -1
                    rawString = if (nextIndex > -1) {
                        append(rawString.substring(0, nextIndex))
                        val endIndex = nextIndex + keyword.length
                        withStyle(tagStyle) {
                            append(rawString.substring(nextIndex, endIndex))
                        }
                        rawString.substring(endIndex)
                    } else {
                        append(rawString)
                        ""
                    }
                } while (rawString.isNotEmpty())
            }

            Text(
                text = text,
                color = MaterialTheme.colors.primary,
                style = MaterialTheme.typography.subtitle1.copy(
                    textAlign = TextAlign.Left
                ),)
        }
    }
}