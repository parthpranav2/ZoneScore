package com.example.zonescore

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.zonescore.databinding.ActivityCreateAccountBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlin.jvm.java
import kotlin.text.clear
import kotlin.text.split

class CreateAccountActivity : AppCompatActivity() {
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 1001

    private var isPasswordVisible = false
    private var isReEnterPasswordVisible = false

    private var phoneNumber: String? = null
    private var AuthSetEmail: String? = null


    private lateinit var binding: ActivityCreateAccountBinding
    private lateinit var dbRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_create_account)
        dbRef= FirebaseDatabase.getInstance().getReference("User")

        binding = ActivityCreateAccountBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.btnreenterpasswordvisibility.setOnClickListener {
            val currentTypeface = binding.txtReEnterPass.typeface
            isReEnterPasswordVisible = !isReEnterPasswordVisible

            if (isReEnterPasswordVisible) {
                binding.btnreenterpasswordvisibility.setImageResource(R.drawable.closeeye)
                binding.txtReEnterPass.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            } else {
                binding.btnreenterpasswordvisibility.setImageResource(R.drawable.openeye)
                binding.txtReEnterPass.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }

            binding.txtReEnterPass.typeface = currentTypeface

            binding.txtReEnterPass.setSelection(binding.txtReEnterPass.text?.length ?: 0)
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

        binding.btnGoogle.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent

            //binding.btnSignUp.isEnabled=false

            binding.txtPass.setText("ibm4534ujk8")//default pass for a firebase auth generated account
            binding.txtReEnterPass.setText("ibm4534ujk8")//default pass for a firebase auth generated account
            startActivityForResult(signInIntent, RC_SIGN_IN)

            if(binding.txtPhone.text.toString().isEmpty()){
                binding.txtPhoneNoAlert.visibility=View.VISIBLE
            }
        }


        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))  // From Firebase console
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        binding.txtLogIn.setOnClickListener {
            redirectToActivity(SignInActivity::class.java)
        }

        val spinner2 = findViewById<Spinner>(R.id.cmbcountrycode)
        val countryCodes = resources.getStringArray(R.array.country_codes)

        val adapter = object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, countryCodes) {
            override fun getItem(position: Int): String {
                // When the spinner is clicked, we can display the full format
                return super.getItem(position) ?: ""
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                // Customize dropdown items if needed, but use the default
                return super.getDropDownView(position, convertView, parent)
            }

            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)

                // If the item is selected, show only the country code (e.g., +91)
                val selectedItem = countryCodes[position]
                val codeOnly = selectedItem.split(" →")[0] // Extract +91 or other code
                (view as TextView).text = codeOnly // Show just the country code in the spinner
                return view
            }
        }
        spinner2.adapter = adapter


        binding.btnSignUp.setOnClickListener {
            if(binding.txtName.text.toString().isNotEmpty() &&
                binding.txtEmail.text.toString().isNotEmpty() &&
                binding.txtPass.text.toString().isNotEmpty() &&
                binding.txtReEnterPass.text.toString().isNotEmpty() &&
                binding.txtPhone.text.toString().isNotEmpty()){

                if(binding.txtPass.text.toString()==binding.txtReEnterPass.text.toString()){
                    SaveUserData()
                }
                else{
                    Toast.makeText(this,"The password in Re-Enter Password field didn't match with Password",Toast.LENGTH_LONG).show()
                }
            }
            else{
                Toast.makeText(this,"Some of the fields are empty",Toast.LENGTH_LONG).show()
            }

        }

        binding.txtPhone.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
                //
            }

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
                //
            }

            override fun afterTextChanged(s: Editable?) {
                if(binding.txtPhone.text.toString().isEmpty()){
                    binding.txtPhoneNoAlert.visibility=View.VISIBLE
                }else{
                    binding.txtPhoneNoAlert.visibility=View.GONE
                }
            }
        })

        binding.txtEmail.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
                //
            }

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
                //
            }

            override fun afterTextChanged(s: Editable?) {
                //AutoFillToggled()
                VerifyExistance(binding.txtEmail.text.toString())
            }
        })

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)


                ValuesSetter(account.email.toString())

                AuthSetEmail=account.email.toString()

                AutoFillToggled()

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

    private fun ValuesSetter(email: String) {
        val account = GoogleSignIn.getLastSignedInAccount(this)
        if (account != null) {
            // Set name values to EditText fields
            binding.txtName.setText(account.displayName ?: "")

            // Set email
            binding.txtEmail.setText(email)

            // Store phone number if available
            //phoneNumber = account.phoneNumber
            Log.d("PhoneDebug", "Phone Number: $phoneNumber")
        }
    }



    private fun VerifyExistance(email : String){
        val sanitizedEmail = email.replace(".", ",")
        val dbRef = FirebaseDatabase.getInstance().getReference("User").child(sanitizedEmail)
        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    binding.txtEmailAlert.visibility=View.VISIBLE
                    //binding.btnSignUp.isEnabled=false
                }else{
                    binding.txtEmailAlert.visibility=View.GONE
                }
            }

            override fun onCancelled(error: DatabaseError) {
                //
            }
        })
    }

    private fun AutoFillToggled(){
        if(binding.txtEmail.text.toString()==AuthSetEmail){
            binding.txtName.isEnabled=false
            binding.txtPass.isEnabled=false
            binding.txtReEnterPass.isEnabled=false
            binding.btnpasswordvisibility.isEnabled=false
            binding.btnreenterpasswordvisibility.isEnabled=false
        }else{
            binding.txtName.isEnabled=true
            binding.txtPass.isEnabled=true
            binding.txtReEnterPass.isEnabled=true
            binding.btnpasswordvisibility.isEnabled=true
            binding.btnreenterpasswordvisibility.isEnabled=true
        }
    }

    private fun SaveUserData(){
        // Sanitize email to make it a valid key
        val sanitizedEmail = binding.txtEmail.text.toString().replace(".", ",") // Replace '.' with ','
        val dbRef = FirebaseDatabase.getInstance().getReference("User").child(sanitizedEmail)

        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    Toast.makeText(this@CreateAccountActivity, "This email already exists", Toast.LENGTH_LONG).show()
                    binding.txtEmail.text.clear()
                    return
                }else{
                    val user1 = UserModel(binding.txtName.text.toString(),
                        binding.txtEmail.text.toString(),
                        (binding.cmbcountrycode.selectedItem.toString().split(" →")[0] +" "+binding.txtPhone.text.toString()),
                        binding.txtPass.text.toString())

                    GlobalClass.Email=sanitizedEmail
                    GlobalClass.Name=binding.txtName.text.toString()

                    dbRef.setValue(user1)
                        .addOnCompleteListener {
                            val editor=getSharedPreferences("MY_SETTING", MODE_PRIVATE).edit()
                            editor.putString("usEmail",sanitizedEmail)
                            editor.apply()

                            idle()
                            redirectToActivity(DashboardActivity::class.java)
                        }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@CreateAccountActivity,"Error ${error.message}",Toast.LENGTH_LONG).show()
            }

            //intent
        })
    }

    private fun idle(){
        binding.txtName.text.clear()
        binding.txtEmail.text.clear()
        binding.txtPhone.text.clear()
        binding.txtPass.text.clear()
        binding.txtReEnterPass.text.clear()
    }

    private fun redirectToActivity(activityClass: Class<*>) {
        val intent = Intent(this, activityClass)
        startActivity(intent)
        finish()
    }
}