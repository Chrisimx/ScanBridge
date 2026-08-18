package io.github.chrisimx.scanbridge.uicomponents

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.chrisimx.scanbridge.startupmessages.ShownStartupMessagesRepository
import io.github.chrisimx.scanbridge.startupmessages.StartupMessage
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import scanbridge.composeui.generated.resources.Res
import scanbridge.composeui.generated.resources.okay_text
import scanbridge.composeui.generated.resources.thank_message_play_one
import scanbridge.composeui.generated.resources.thank_message_play_one_title

@Composable
fun StartupMessagesDisplay() {
    val coroutineScope = rememberCoroutineScope()
    val shownMessagesRepository = koinInject<ShownStartupMessagesRepository>()

    val markThanksMessageAsRead = {
        coroutineScope.launch {
            shownMessagesRepository.setShown(StartupMessage.THANKS_FOR_PURCHASE, true)
        }
    }

    val thanksForPurchaseAlreadyShown by shownMessagesRepository
        .getWasShownFlow(StartupMessage.THANKS_FOR_PURCHASE)
        .collectAsState(true)

    if (!thanksForPurchaseAlreadyShown) {
        AlertDialog(
            {
                markThanksMessageAsRead()
            },
            {
                TextButton(
                    onClick = {
                        markThanksMessageAsRead()
                    }
                ) {
                    Text(stringResource(Res.string.okay_text))
                }
            },
            icon = {
                Icon(
                    Icons.Rounded.Favorite,
                    "Thank you",
                    Modifier.size(60.dp),
                    tint = Color.Red
                )
            },
            title = {
                Text(
                    stringResource(Res.string.thank_message_play_one_title),
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    stringResource(Res.string.thank_message_play_one)
                )
            }
        )
    }
}
