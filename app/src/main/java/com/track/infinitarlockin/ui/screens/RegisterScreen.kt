package com.track.infinitarlockin.ui.screens

import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.track.infinitarlockin.data.remote.RetrofitClient
import com.track.infinitarlockin.data.remote.dto.RegisterRequest
import com.track.infinitarlockin.ui.navigation.Screen
import com.track.infinitarlockin.ui.viewmodels.MainViewModel
import kotlinx.coroutines.launch

@Composable
fun RegisterScreen(navController: NavController, mainViewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Welcome! Please register your device.")
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Your Name") },
            singleLine = true
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                if (name.isBlank()) {
                    error = "Name cannot be empty."
                    return@Button
                }
                isLoading = true
                coroutineScope.launch {
                    val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
                    try {
                        val request = RegisterRequest(name = name, deviceId = deviceId)
                        val response = RetrofitClient.instance.registerEmployee(request)
                        if (response.success) {
                            mainViewModel.checkDeviceRegistration(context)
                            // The navigation will be handled by the observer in MainActivity
                        } else {
                            error = "Registration failed. Please try again."
                            isLoading = false
                        }
                    } catch (e: Exception) {
                        error = "Error: ${e.message}"
                        isLoading = false
                    }
                }
            },
            enabled = !isLoading
        ) {
            Text(if (isLoading) "Registering..." else "Register")
        }
        error?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = it, color = androidx.compose.ui.graphics.Color.Red)
        }
    }
}
