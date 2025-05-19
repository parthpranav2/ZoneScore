package com.example.zonescore

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.zonescore.databinding.ActivitySignInBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlin.toString

class SignInActivity : AppCompatActivity() {
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 1001

    private var isPasswordVisible = false

    private lateinit var binding: ActivitySignInBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sign_in)

        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.btnpasswordvisibility.setOnClickListener {
            val currentTypeface = binding.txtPass.typeface
            isPasswordVisible = !isPasswordVisible

            if (isPasswordVisible) {
                binding.btnpasswordvisibility.setImageResource(R.drawable.closeeye)
                binding.txtPass.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            } else {
                binding.btnpasswordvisibility.setImageResource(R.drawable.openeye)
                binding.txtPass.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }

            binding.txtPass.typeface = currentTypeface

            binding.txtPass.setSelection(binding.txtPass.text?.length ?: 0)
        }

        binding.txtSignup.setOnClickListener {
            redirectToActivity(CreateAccountActivity::class.java)
        }

        binding.btnsignin.setOnClickListener {
            if (binding.txtEmail.text.toString().isNotEmpty() &&
                binding.txtPass.text.toString().isNotEmpty()
            ) {
                VerifyUser(binding.txtEmail.text.toString(),false)
            } else {
                Toast.makeText(this, "Some of the fields are empty", Toast.LENGTH_LONG).show()
            }
        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))  // From Firebase console
            .requestEmail()
            .build()


        googleSignInClient = GoogleSignIn.getClient(this, gso)

        binding.btnGoogle.setOnClickListener {

            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)

        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)

                // ✅ Set email in the EditText
                VerifyUser(account.email.toString(),true)

                // Sign in to Firebase
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Log.w("GoogleSignIn", "Google sign in failed", e)
                Toast.makeText(this, "Google Sign-In failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        FirebaseAuth.getInstance().signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = FirebaseAuth.getInstance().currentUser
                    Log.d("FirebaseAuth", "Signed in as: ${user?.email}")
                    // You can redirect or update UI here
                } else {
                    Log.w("FirebaseAuth", "signInWithCredential failed", task.exception)
                }
            }
    }

    private fun VerifyUser(email : String,googleAuthSignin: Boolean = false) {
        val password = binding.txtPass.text.toString()
        val sanitizedEmail = email.replace(".", ",")

        val dbRef = FirebaseDatabase.getInstance().getReference("User").child(sanitizedEmail)
        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    binding.txtAccountAlert.visibility=View.GONE
                    if(!googleAuthSignin){
                        val storedPassword = snapshot.child("usPass").getValue(String::class.java)
                        if (storedPassword != null && storedPassword == password) {
                            FetchGlobalDetails(sanitizedEmail)
                        } else {
                            Toast.makeText(
                                this@SignInActivity,
                                "Incorrect Credentials",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }else{
                        FetchGlobalDetails(sanitizedEmail)
                    }

                } else {
                    binding.txtAccountAlert.visibility=View.VISIBLE
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@SignInActivity,
                    "Database error: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }

    private fun FetchGlobalDetails(Email: String) {
        val dbRef = FirebaseDatabase.getInstance().getReference("User").child(Email)
        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {
                // Not implemented
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    GlobalClass.Email = Email
                    GlobalClass.Name = snapshot.child("usName").getValue(String::class.java)

                    val editor = getSharedPreferences("MY_SETTING", MODE_PRIVATE).edit()
                    editor.putString("usEmail", Email)
                    editor.apply()

                    redirectToActivity(DashboardActivity::class.java)
                }
            }
        })
    }

    private fun redirectToActivity(activityClass: Class<*>) {
        val intent = Intent(this, activityClass)
        startActivity(intent)
        finish()
    }
}