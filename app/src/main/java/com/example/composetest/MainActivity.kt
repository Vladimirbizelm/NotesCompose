package com.example.composetest

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.composetest.ui.theme.ComposeTestTheme
import kotlinx.coroutines.launch

val Context.datastore by preferencesDataStore(name = "notes")
val notes = mutableMapOf<String, String>()

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        lifecycleScope.launch {
            DataStore.getNotes(this@MainActivity).collect { loadedNotes ->
                notes.clear()
                notes.putAll(loadedNotes)
            }
        }

        setContent {
            ComposeTestTheme {
                Navigation(notes)
            }
        }
    }
}

@Composable
fun Navigation(notes: Map<String, String>) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "main_page") {
        composable("main_page") { MainPageScreen(navController, notes) }
        composable("search") { SearchScreen(navController, notes) }
        composable("create") { CreateScreen(navController) }
        composable("note_details/{noteKey}") { backStackEntry ->
            val noteKey = backStackEntry.arguments?.getString("noteKey") ?: ""
            NoteDetailsScreen(navController, noteKey, notes[noteKey] ?: "")
        }
    }
}

@Composable
fun MainPageScreen(navController: NavHostController, notes: Map<String, String>) {
    val scope = rememberCoroutineScope()
    val snackbarHost: SnackbarHostState = remember { SnackbarHostState() }
    ContentScaffold(
        title = "Notes",
        navController = navController,
        actions = {
            IconButton(onClick = { navController.navigate("search") }) {
                Icon(Icons.Filled.Search, contentDescription = "Search")
            }
        },
        floatingActionButton = {
            LargeFloatingActionButton(
                onClick = {
                    Log.d("tag", "pressed")
                    scope.launch {
                        snackbarHost.showSnackbar(
                            message = "Add button pressed",
                            withDismissAction = true
                        )
                    }
                },

                shape = RoundedCornerShape(20.dp),
                containerColor = Color.Gray,
                modifier = Modifier
                    .width(70.dp)
                    .height(70.dp)
                    .border(2.dp, Color.Black, RoundedCornerShape(20.dp))
            ) {
                Icon(Icons.Filled.Create, contentDescription = "Create new note")
            }
            SnackbarHost(hostState = snackbarHost, snackbar = { Snackbar(snackbarData = it) })
        }
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.padding(innerPadding)) {
            items(notes.toList().reversed()) { (key, value) ->
                NoteItem(noteKey = key, noteValue = value, navController)
            }
        }
    }
}

@Composable
fun SearchScreen(navController: NavHostController, notes: Map<String, String>) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredNotes = remember(key1 = searchQuery, key2 = notes) {
        notes.filter {
            it.key.contains(searchQuery, ignoreCase = true) || it.value.contains(
                searchQuery,
                ignoreCase = true
            )
        }
    }
    ContentScaffold(
        title = "Search",
        navController = navController,
        actions = {
            IconButton(onClick = { navController.navigate("main_page") }) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
            LazyColumn(modifier = Modifier.padding(4.dp)) {
                items(filteredNotes.toList()){ (key, value) ->
                    NoteItem(noteKey = key, noteValue = value, navController)
                }
            }
        }
    }
}

@Composable
fun CreateScreen(navController: NavHostController) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    ContentScaffold(
        title = "Create Note",
        navController = navController,
        actions = {
            Button(onClick = {
                if (description.isNotEmpty()) {
                    val newTitle = if (title.isNotEmpty()) title else description.split(" ").first()
                    notes[newTitle] = description
                    coroutineScope.launch {
                        DataStore.saveNotes(context, notes)
                    }
                    navController.navigate("main_page")
                }
            }) {
                Text("Save")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            TextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                maxLines = 1,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            TextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 16.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentScaffold(
    title: String,
    navController: NavHostController,
    actions: @Composable (() -> Unit)? = null,
    floatingActionButton: @Composable (() -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                actions = { actions?.invoke() }
            )
        },
        floatingActionButton = { floatingActionButton?.invoke() },
        content = content
    )
}


@Composable
fun NoteItem(noteKey: String, noteValue: String, navController: NavHostController) {
    val context = LocalContext.current
    val isChecked =
        remember { mutableStateOf(false) } //or use by remember to have access to the value without calling . value
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(
            checked = isChecked.value, onCheckedChange = { isChecked.value = !isChecked.value },
            modifier = Modifier
                .padding(2.dp)
                .defaultMinSize(5.dp, 5.dp)
                .background(Color.Gray, shape = RoundedCornerShape(4.dp))
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .background(color = Color.Gray, shape = RoundedCornerShape(16.dp))
                .padding(8.dp)
                .clickable {
                    Toast.makeText(
                        context,
                        "Clicked on: $noteKey",
                        Toast.LENGTH_SHORT
                    ).show()
                    navController.navigate("note_details/$noteKey")
                }
        ) {
            Text(
                noteKey,
                modifier = Modifier.padding(bottom = 4.dp),
                maxLines = 1
            )
            Text(
                noteValue,
                modifier = Modifier.padding(bottom = 4.dp),
                maxLines = 1
            )
        }
    }

}

@Composable
fun NoteDetailsScreen(navController: NavHostController, noteKey: String, noteValue: String) {
    ContentScaffold(
        title = "Note Details",
        navController = navController,
        actions = {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text("Title: $noteKey", style = MaterialTheme.typography.bodyLarge)
            Text(
                "Description: $noteValue",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
@Preview
fun NotesScreenPreview() {
    ComposeTestTheme {

    }
}
