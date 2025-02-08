package nl.tudelft.trustchain.smartphonedemocracy.ui.compose

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

// TODO: Implement proper item sorting and filtering in a ViewModel, this might be removed

enum class SortState {
    NONE,
    VOTES_ASC,
    VOTES_DESC,
    DEADLINE_ASC,
    DEADLINE_DESC
}

enum class FilterState {
    NONE,
    VOTING,
    FINISHED
}

@Composable
fun SortItem(
    text: String,
    icon: ImageVector,
    sortState: SortState,
    targetState: SortState,
    sortOrder: Boolean,
    onStateChange: (SortState) -> Unit
) {
    DropdownMenuItem(onClick = { onStateChange(targetState) }, text = {
        Row {
            Icon(imageVector = icon, contentDescription = text)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = text)
            Spacer(modifier = Modifier.weight(1f))
            if (sortState == targetState) {
                Icon(
                    imageVector = if (sortOrder) {
                        Icons.Filled.ArrowUpward
                    } else {
                        Icons.Filled.ArrowDownward
                    },
                    contentDescription = "Sort Order"
                )
            }
        }
    })
}

@Composable
fun FilterItem(
    text: String,
    filterState: FilterState,
    targetState: FilterState,
    onStateChange: (FilterState) -> Unit
) {
    DropdownMenuItem(onClick = { onStateChange(targetState) }, text = {
        Row {
            Text(text = text)
            Spacer(modifier = Modifier.weight(1f))
            if (filterState == targetState) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Filter Selected"
                )
            }
        }
    })
}

@Preview
@Composable
private fun DropdownPreview() {
    Row {
        SortItem("", Icons.Filled.ArrowUpward, SortState.NONE, SortState.VOTES_ASC, true) {}
        Spacer(modifier = Modifier.width(8.dp))
        SortItem("", Icons.Filled.ArrowDownward, SortState.NONE, SortState.VOTES_DESC, false) {}
        Spacer(modifier = Modifier.width(8.dp))
        FilterItem("", FilterState.NONE, FilterState.VOTING) {}
    }
}
