package com.codewithchandra.grocent.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codewithchandra.grocent.ui.theme.*
import com.codewithchandra.grocent.util.AddressSuggestion

@Composable
fun SuggestionItem(
    suggestion: AddressSuggestion,
    onClick: () -> Unit
) {
    Column {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = PrimaryGreen,
                    modifier = Modifier.size(24.dp)
                )
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = suggestion.primaryText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = TextBlack
                    )
                    if (suggestion.secondaryText.isNotEmpty()) {
                        Text(
                            text = suggestion.secondaryText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextGray
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = TextGray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Divider()
    }
}
