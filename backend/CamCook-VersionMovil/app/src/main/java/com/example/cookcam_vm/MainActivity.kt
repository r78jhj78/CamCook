package com.example.cookcam_vm

import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseApp.initializeApp(this)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setContent {
            val navController = rememberNavController()

            NavHost(navController = navController, startDestination = "login") {
                composable("login") {
                    LoginScreen(navController)
                }
                composable("success") {
                    SuccessScreen()
                }
            }
        }
    }

    @Composable
    fun LoginScreen(navController: NavHostController) {
        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var isLoading by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text("Registro / Login", style = MaterialTheme.typography.titleLarge)

            Spacer(modifier = Modifier.height(16.dp))

            TextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            TextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Contraseña") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val trimmedEmail = email.trim()
                    val trimmedPassword = password.trim()

                    if (!Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
                        Toast.makeText(this@MainActivity, "Correo inválido", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    if (trimmedPassword.length < 6) {
                        Toast.makeText(this@MainActivity, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    isLoading = true
                    registerUser(
                        trimmedEmail,
                        trimmedPassword,
                        onSuccess = {
                            navController.navigate("success") {
                                popUpTo("login") { inclusive = true } // Elimina login del back stack
                            }
                        },
                        onFinish = {
                            isLoading = false
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                Text(if (isLoading) "Cargando..." else "Registrar / Login")
            }
        }
    }

    private fun registerUser(
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onFinish: () -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.let {
                        val userData = hashMapOf(
                            "email" to email,
                            "password" to password, // ⚠️ Solo para pruebas
                            "role" to "usuario"
                        )
                        db.collection("users").document(it.uid)
                            .set(userData)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Registro exitoso como usuario", Toast.LENGTH_SHORT).show()
                                onSuccess()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Error al guardar datos: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                            .addOnCompleteListener {
                                onFinish()
                            }
                    }
                } else {
                    val errorMessage = task.exception?.message ?: "Error desconocido"
                    if (errorMessage.contains("The email address is already in use")) {
                        loginUser(email, password, onSuccess, onFinish)
                    } else {
                        Toast.makeText(this, "Error al registrar: $errorMessage", Toast.LENGTH_SHORT).show()
                        onFinish()
                    }
                }
            }
    }

    private fun loginUser(
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onFinish: () -> Unit
    ) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Login exitoso", Toast.LENGTH_SHORT).show()
                    onSuccess()
                } else {
                    val errorMessage = task.exception?.message ?: "Error desconocido"
                    Toast.makeText(this, "Error al iniciar sesión: $errorMessage", Toast.LENGTH_SHORT).show()
                }
                onFinish()
            }
    }

    @Composable
    fun SuccessScreen() {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text("¡Bienvenido!", style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Login exitoso.", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
