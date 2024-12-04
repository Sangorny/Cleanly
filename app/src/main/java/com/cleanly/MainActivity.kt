package com.cleanly

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.FirebaseApp



class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseApp.initializeApp(this)

        val account: GoogleSignInAccount? = GoogleSignIn.getLastSignedInAccount(this)
        val photoUrl = account?.photoUrl

        val intent = Intent(this, LogActivity::class.java)
        startActivity(intent)
        finish()
    }
}
