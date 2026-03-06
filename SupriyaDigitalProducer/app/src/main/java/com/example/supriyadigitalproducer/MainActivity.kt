package com.example.supriyadigitalproducer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.supriyadigitalproducer.ui.theme.ProducerAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ProducerAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ProducerApp()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProducerApp() {
    var producerName by remember { mutableStateOf("") }
    var producerList by remember { mutableStateOf(listOf<String>()) }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text("Supriya Digital Producer") },
                colors = TopAppBarDefaults.smallTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        content = { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedTextField(
                    value = producerName,
                    onValueChange = { producerName = it },
                    label = { Text("Enter Producer Name") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Text
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        if (producerName.isNotBlank()) {
                            producerList = producerList + producerName
                            producerName = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Add Producer")
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (producerList.isEmpty()) {
                    Text("No producers added yet.")
                } else {
                    producerList.forEachIndexed { index, name ->
                        ProducerItem(
                            name = name,
                            onRemove = {
                                producerList = producerList.filterIndexed { i, _ -> i != index }
                            }
                        )
                    }
                }
            }
        }
    )
}

@Composable
fun ProducerItem(name: String, onRemove: () -> Unit) {
    Card(
        modifier = Modifier
            .padding(vertical = 4.dp)
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = name, style = MaterialTheme.typography.bodyLarge)
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remove Producer"
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ProducerAppPreview() {
    ProducerAppTheme {
        ProducerApp()
    }
}
