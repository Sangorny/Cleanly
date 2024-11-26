package com.cleanly

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.FirebaseApp
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.cleanly.WelcomeActivity.WelcomeBarra
import com.cleanly.ZonaActivity.ZonasActivity


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
