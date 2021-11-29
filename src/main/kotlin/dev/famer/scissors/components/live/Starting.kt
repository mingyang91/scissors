import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.rememberDialogState
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("Initialization")

@Composable
@Preview
fun Starting() {
    val dialogState = rememberDialogState(size = DpSize(400.dp, 430.dp))
    Dialog(
        onCloseRequest = {},
        resizable = false,
        title = "模型启动中，请稍候......",
        state = dialogState
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(12.dp).weight(1f),
                    strokeWidth = 36.dp
                )
            }
        }
    }
}
