package com.crosales.firebaseapp

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore

enum class ProviderType{
    BASIC,
    GOOGLE
}

class HomeActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()

    private lateinit var emailTextView: TextView
    private lateinit var providerTextView: TextView
    private lateinit var addressTextView: EditText
    private lateinit var phoneTextView: EditText
    private lateinit var logOutButton: Button
    private lateinit var errorButton: Button
    private lateinit var saveButton: Button
    private lateinit var getButton: Button
    private lateinit var deleteButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        emailTextView = findViewById(R.id.emailTextView)
        providerTextView = findViewById(R.id.providerTextView)
        addressTextView = findViewById(R.id.addressTextView)
        phoneTextView = findViewById(R.id.phoneTextView)
        logOutButton = findViewById(R.id.logOutButton)
        errorButton = findViewById(R.id.errorButton)
        saveButton = findViewById(R.id.saveButton)
        getButton = findViewById(R.id.getButton)
        deleteButton = findViewById(R.id.deleteButton)


        // getExtras
        val bundle:Bundle? = intent.extras
        val email:String? = bundle?.getString("email")
        val provider:String? = bundle?.getString("provider")

        // Setup
        setUp(email ?: "", provider ?: "")

        //Guardado de datos - SharedPref
        val prefs = getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE).edit()
        prefs.putString("email", email)
        prefs.putString("provider", provider)
        prefs.apply()
    }

    private fun setUp(email: String, provider: String) {
        title = "Inicio"

        emailTextView.text = email
        providerTextView.text = provider

        logOutButton.setOnClickListener {
            //Borrado de datos
            val prefs = getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE).edit()
            prefs.clear()
            prefs.apply()

            FirebaseAuth.getInstance().signOut()
            onBackPressed()
        }

        errorButton.setOnClickListener {
            // Envio del ID de usuario
            FirebaseCrashlytics.getInstance().setUserId(email)
            // Envio de claves personalizadas
            FirebaseCrashlytics.getInstance().setCustomKey("provider", provider)

            // Envio del log de errores
            FirebaseCrashlytics.getInstance().log("Se ha pulsado el boton FORZAR ERROR")

            // Forzar error con Firebase Crashlytics
            throw RuntimeException("Forzado de error")
        }

        saveButton.setOnClickListener {
            db.collection("users").document(email).set(
                hashMapOf("provider" to provider, "address" to addressTextView.text.toString().trim(),
                    "phone" to phoneTextView.text.toString().trim())
            ).addOnSuccessListener {
                Log.d("TAG-OK", "Datos guardados correctamente para el correo electrónico: $email")
            }.addOnFailureListener { exception ->
                Log.w("TAG-ERR", "Error al guardar datos para el correo electrónico: $email", exception)
            }
        }

        getButton.setOnClickListener {
            // CHA: Tambien funciona con este codigo:
            /*db.collection("users").document(email).get().addOnCompleteListener {
                addressTextView.setText(it.result.getString("address"))
                phoneTextView.setText(it.result.getString("phone"))

            }*/

            db.collection("users").document(email).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        Log.d("TAG-OK", "Documento encontrado: ${document.id} => ${document.data}")
                        // Aquí puedes acceder a los datos del documento utilizando document.data
                        addressTextView.setText(document.data?.get("address").toString())
                        phoneTextView.setText(document.data?.get("phone").toString())
                    } else {
                        Log.d("TAG-NULL", "No se encontró ningún documento con el correo electrónico especificado")
                    }
                }
                .addOnFailureListener { exception ->
                    Log.d("TAG-ERR", "Error al obtener documento:", exception)
                }
        }

        deleteButton.setOnClickListener {
            db.collection("users").document(email).delete()
        }

    }
}