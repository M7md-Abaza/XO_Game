package com.m7mdabaza.xogame.presentation.achievements

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.m7mdabaza.xogame.domain.model.Achievement
import com.m7mdabaza.xogame.domain.model.AchievementStatus
import com.m7mdabaza.xogame.domain.usecase.ObserveAchievementsUseCase
import com.m7mdabaza.xogame.presentation.R
import com.m7mdabaza.xogame.presentation.components.ScreenTopBar
import com.m7mdabaza.xogame.presentation.components.formatMillisDate
import com.m7mdabaza.xogame.presentation.theme.XoTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@StringRes
fun Achievement.titleRes(): Int = when (this) {
    Achievement.FIRST_WIN -> R.string.achievement_first_win
    Achievement.BEAT_MEDIUM -> R.string.achievement_beat_medium
    Achievement.BEAT_HARD -> R.string.achievement_beat_hard
    Achievement.WIN_STREAK_5 -> R.string.achievement_streak_5
    Achievement.GAMES_100 -> R.string.achievement_games_100
    Achievement.TWO_PLAYER_GAME -> R.string.achievement_two_player
    Achievement.FIRST_CHALLENGE -> R.string.achievement_first_challenge
    Achievement.CHALLENGE_STREAK_7 -> R.string.achievement_challenge_streak_7
}

@StringRes
fun Achievement.descriptionRes(): Int = when (this) {
    Achievement.FIRST_WIN -> R.string.achievement_first_win_desc
    Achievement.BEAT_MEDIUM -> R.string.achievement_beat_medium_desc
    Achievement.BEAT_HARD -> R.string.achievement_beat_hard_desc
    Achievement.WIN_STREAK_5 -> R.string.achievement_streak_5_desc
    Achievement.GAMES_100 -> R.string.achievement_games_100_desc
    Achievement.TWO_PLAYER_GAME -> R.string.achievement_two_player_desc
    Achievement.FIRST_CHALLENGE -> R.string.achievement_first_challenge_desc
    Achievement.CHALLENGE_STREAK_7 -> R.string.achievement_challenge_streak_7_desc
}

@HiltViewModel
class AchievementsViewModel @Inject constructor(
    observeAchievements: ObserveAchievementsUseCase,
) : ViewModel() {
    val achievements: StateFlow<List<AchievementStatus>> = observeAchievements()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

/** Stateful entry point used by the navigation graph. */
@Composable
fun AchievementsRoute(
    onBack: () -> Unit,
    viewModel: AchievementsViewModel = hiltViewModel(),
) {
    val achievements by viewModel.achievements.collectAsStateWithLifecycle()
    AchievementsScreen(achievements, onBack)
}

@Composable
fun AchievementsScreen(achievements: List<AchievementStatus>, onBack: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding(),
    ) {
        ScreenTopBar(stringResource(R.string.achievements_title), onBack = onBack)
        LazyColumn(
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    stringResource(R.string.achievements_summary, achievements.count { it.isUnlocked }, achievements.size),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(achievements, key = { it.achievement.name }) { AchievementCard(it) }
        }
    }
}

@Composable
private fun AchievementCard(status: AchievementStatus) {
    val achievement = status.achievement
    val title = stringResource(achievement.titleRes())
    val description = stringResource(achievement.descriptionRes())
    val unlockedAt = status.unlockedAtMillis
    val detail = when {
        unlockedAt != null -> stringResource(R.string.achievements_unlocked_on, formatMillisDate(unlockedAt))
        achievement.target > 1 -> stringResource(R.string.achievements_progress, status.progress, achievement.target)
        else -> stringResource(R.string.achievements_locked)
    }
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (status.isUnlocked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        // One screen-reader item: "First win. Win a game against the phone. Locked".
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = "$title. $description. $detail" },
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (status.isUnlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(if (status.isUnlocked) R.drawable.ic_trophy else R.drawable.ic_lock),
                    contentDescription = null,
                    tint = if (status.isUnlocked) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                if (!status.isUnlocked && achievement.target > 1) {
                    LinearProgressIndicator(
                        progress = { status.progress.toFloat() / achievement.target },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                    )
                }
                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Preview(showSystemUi = true)
@Composable
private fun AchievementsScreenPreview() {
    XoTheme {
        AchievementsScreen(
            Achievement.entries.mapIndexed { i, a -> AchievementStatus(a, if (i == 4) 37 else 0, if (i < 2) 1_700_000_000_000 else null) },
            onBack = {},
        )
    }
}
