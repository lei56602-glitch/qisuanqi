package com.example.ui.components

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalCalculatorPalette
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.viewmodel.CalculationRecord

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryBottomSheet(
  history: List<CalculationRecord>,
  onSelectRecord: (CalculationRecord, Boolean) -> Unit,
  onClearHistory: () -> Unit,
  onDismiss: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val palette = LocalCalculatorPalette.current

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = palette.displaySurface,
    contentColor = MaterialTheme.colorScheme.onSurface,
  ) {
    HistoryListContent(
      history = history,
      onSelectRecord = { record, useResultOnly ->
        onSelectRecord(record, useResultOnly)
        onDismiss()
      },
      onClearHistory = onClearHistory,
      modifier = Modifier.fillMaxWidth().height(440.dp),
    )
  }
}

@Composable
fun HistoryListContent(
  history: List<CalculationRecord>,
  onSelectRecord: (CalculationRecord, Boolean) -> Unit,
  onClearHistory: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val palette = LocalCalculatorPalette.current

  Column(modifier = modifier.padding(horizontal = 20.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Column {
        Text(
          text = stringResource(R.string.history_title),
          style = MaterialTheme.typography.headlineSmall,
          color = MaterialTheme.colorScheme.onSurface,
        )
        if (history.isNotEmpty()) {
          Text(
            text = stringResource(R.string.history_tap_hint),
            style = MaterialTheme.typography.labelMedium,
            color = palette.secondaryText,
          )
        }
      }

      if (history.isNotEmpty()) {
        TextButton(
          onClick = onClearHistory,
          modifier = Modifier.testTag("clear_history_button"),
        ) {
          Icon(
            imageVector = Icons.Rounded.DeleteOutline,
            contentDescription = stringResource(R.string.cd_clear_history),
            tint = palette.clearKeyText,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = stringResource(R.string.history_clear),
            color = palette.clearKeyText,
            style = MaterialTheme.typography.labelLarge,
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))
    HorizontalDivider(color = palette.divider)

    if (history.isEmpty()) {
      Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center,
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center,
        ) {
          Surface(
            shape = RoundedCornerShape(20.dp),
            color = palette.utilityKey,
            modifier = Modifier.size(64.dp),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Rounded.Calculate,
                contentDescription = null,
                tint = palette.operatorKeyText,
                modifier = Modifier.size(32.dp),
              )
            }
          }
          Spacer(modifier = Modifier.height(16.dp))
          Text(
            text = stringResource(R.string.history_empty_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = stringResource(R.string.history_empty_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = palette.secondaryText,
            textAlign = TextAlign.Center,
          )
        }
      }
    } else {
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        items(items = history, key = { it.id }) { record ->
          HistoryRecordCard(
            record = record,
            onClickExpression = { onSelectRecord(record, false) },
            onClickResult = { onSelectRecord(record, true) },
          )
        }
      }
    }
  }
}

@Composable
private fun HistoryRecordCard(
  record: CalculationRecord,
  onClickExpression: () -> Unit,
  onClickResult: () -> Unit,
) {
  val palette = LocalCalculatorPalette.current
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = palette.numberKey,
    modifier = Modifier.fillMaxWidth().testTag("history_item_${record.id}"),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
      horizontalAlignment = Alignment.End,
    ) {
      Text(
        text = record.formattedExpression,
        fontFamily = JetBrainsMonoFontFamily,
        fontSize = 15.sp,
        color = palette.secondaryText,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.End,
        modifier =
          Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClickExpression)
            .padding(vertical = 4.dp),
      )

      Spacer(modifier = Modifier.height(4.dp))

      Surface(
        onClick = onClickResult,
        shape = RoundedCornerShape(10.dp),
        color = palette.operatorKey.copy(alpha = 0.7f),
      ) {
        Text(
          text = "= ${record.resultDisplay}",
          fontFamily = SpaceGroteskFontFamily,
          fontWeight = FontWeight.SemiBold,
          fontSize = 22.sp,
          color = palette.operatorKeyText,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
      }
    }
  }
}
