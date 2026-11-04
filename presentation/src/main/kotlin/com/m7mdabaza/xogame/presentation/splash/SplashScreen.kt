package com.m7mdabaza.xogame.presentation.splash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.m7mdabaza.xogame.presentation.R
import com.m7mdabaza.xogame.presentation.components.BrandBackground
import com.m7mdabaza.xogame.presentation.components.XoLogo
import com.m7mdabaza.xogame.presentation.theme.XoTheme

/**
 * The launch screen: the brand logo and tagline on the Home background. It
 * follows the theme (light/dark); the activity in :app runs the consent flow
 * while it shows.
 */
@Composable
fun SplashContent() {
    Box(Modifier.fillMaxSize()) {
        BrandBackground()
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            XoLogo(markSize = 120.dp)
            Spacer(Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.home_tagline),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            // Lifts the logo a little above the centre, like on Home.
            Spacer(Modifier.height(120.dp))
        }
    }
}

@Preview(showSystemUi = true)
@Composable
private fun SplashPreview() {
    XoTheme { Surface { SplashContent() } }
}

@Preview(showSystemUi = true)
@Composable
private fun SplashDarkPreview() {
    XoTheme(darkTheme = true) { Surface { SplashContent() } }
}
