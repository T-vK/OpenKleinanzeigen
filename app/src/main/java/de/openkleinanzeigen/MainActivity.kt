package de.openkleinanzeigen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import de.openkleinanzeigen.ui.OpenKleinanzeigenAppUi
import de.openkleinanzeigen.ui.theme.OpenKleinanzeigenTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repos = (application as OpenKleinanzeigenApp).repos
        setContent {
            OpenKleinanzeigenTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    OpenKleinanzeigenAppUi(repos)
                }
            }
        }
    }
}
