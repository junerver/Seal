package com.junkfood.seal.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.junkfood.seal.R
import com.junkfood.seal.ui.theme.harmonizeWithPrimary

enum class DownloadTaskItemStatus(
    val statusLabelId: Int, val primaryButtonIcon: ImageVector, val primaryOperationDescId: Int
) {
    ENQUEUED(
        R.string.status_enqueued,
        Icons.Rounded.Cancel,
        R.string.cancel
    ),
    COMPLETED(
        R.string.status_completed,
        Icons.Rounded.PlayArrow,
        R.string.open_file
    ),
    DOWNLOADING(
        R.string.status_downloading,
        Icons.Rounded.Stop,
        R.string.cancel
    ),
    CANCELED(
        R.string.status_canceled,
        Icons.Outlined.RestartAlt,
        R.string.restart
    ),
    FETCHING_INFO(
        R.string.status_fetching_video_info,
        Icons.Rounded.Stop,
        R.string.cancel
    ),
    ERROR(R.string.status_error, Icons.Outlined.RestartAlt, R.string.restart)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadTaskItem(
    modifier: Modifier = Modifier.padding(12.dp),
    imageModel: Any = R.drawable.ic_launcher_foreground,
    title: String = "sample title ".repeat(5),
    author: String = "author sample ".repeat(5),
    status: DownloadTaskItemStatus = DownloadTaskItemStatus.ENQUEUED,
    expanded: Boolean = false,
    progress: Float = -1f,
    progressText: String = "This is a sample of very long progress text. ".repeat(3),
    errorText: String = "This is a sample of very long error text. ".repeat(3)
) {
    var isExpanded by remember { mutableStateOf(expanded) }
    var isMenuExpanded by remember { mutableStateOf(false) }
    ElevatedCard(modifier = modifier, onClick = { isExpanded = !isExpanded }) {
        Column() {
            Box() {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                ) {
                    /*AsyncImage(
                        modifier = Modifier
                            .fillMaxWidth(0.45f)
                            .aspectRatio(16f / 9f, matchHeightConstraintsFirst = true),
                        model = imageModel,
                        contentDescription = stringResource(R.string.thumbnail),
                        contentScale = ContentScale.Crop,
                    )*/
                    Image(
                        painter = painterResource(id = R.drawable.sample),
                        modifier = Modifier
                            .padding(12.dp)
                            .weight(1f)
                            .clip(MaterialTheme.shapes.small)
                            .aspectRatio(16f / 10f, matchHeightConstraintsFirst = true),
                        contentDescription = stringResource(R.string.thumbnail),
                        contentScale = ContentScale.Crop,
                    )
                    Column(
                        modifier = Modifier
                            .padding(vertical = 12.dp)
                            .padding(end = 12.dp)
                            .weight(1f)
                            .fillMaxHeight(), verticalArrangement = Arrangement.Top
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            modifier = Modifier.padding(top = 3.dp),
                            text = author,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = stringResource(id = status.statusLabelId),
                            style = MaterialTheme.typography.labelMedium,
                            color = with(MaterialTheme.colorScheme)
                            { if (status != DownloadTaskItemStatus.ERROR) primary else error.harmonizeWithPrimary() }
                        )
                    }
                }
                val animatedDegree =
                    animateFloatAsState(targetValue = if (isExpanded) 0f else -180f)
                FilledTonalIconButton(modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
                    .size(24.dp),
                    onClick = { isExpanded = !isExpanded }) {
                    Icon(
                        Icons.Outlined.ExpandLess,
                        null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.rotate(animatedDegree.value)
                    )
                }
            }
            AnimatedVisibility(visible = isExpanded) {
                Divider(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .height(1.dp)
                        .clip(MaterialTheme.shapes.small)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max)
                        .padding(horizontal = 12.dp)
                        .padding(top = 4.dp, bottom = 4.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(4f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (status != DownloadTaskItemStatus.ERROR) progressText else errorText,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall,
                            color = with(MaterialTheme.colorScheme)
                            { if (status != DownloadTaskItemStatus.ERROR) onSurfaceVariant else error.harmonizeWithPrimary() },
                        )
                    }
                    val containerColor = MaterialTheme.colorScheme.secondaryContainer
//                    val contentColor = contentColorFor(backgroundColor = containerColor)
                    val contentColor =
                        MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(2f)
                            .wrapContentSize(Alignment.TopEnd)
                    ) {
                        Row(modifier = Modifier, horizontalArrangement = Arrangement.End) {
                            FilledIconButton(
                                onClick = { }, colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = containerColor, contentColor = contentColor
                                )
                            ) {
                                Icon(
                                    status.primaryButtonIcon,
                                    stringResource(id = status.primaryOperationDescId),
                                )
                            }
                            FilledTonalIconButton(
                                onClick = { isMenuExpanded = true },
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = containerColor, contentColor = contentColor
                                )
                            ) { Icon(Icons.Outlined.MoreHoriz, null) }
                            DropdownMenu(
                                expanded = isMenuExpanded,
                                onDismissRequest = { isMenuExpanded = false }) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.copy_link)) },
                                    onClick = { },
                                    leadingIcon = { Icon(Icons.Outlined.Link, null) })
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.copy_error_report)) },
                                    onClick = { },
                                    leadingIcon = { Icon(Icons.Outlined.BugReport, null) })
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.remove)) },
                                    onClick = { },
                                    leadingIcon = { Icon(Icons.Outlined.Delete, null) })
                            }
                        }
                    }
                }
            }
            if (progress < 0) LinearProgressIndicator(
                Modifier
                    .fillMaxWidth()
                    .padding()
            )
            else LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(), progress = progress
            )
        }
    }
}

@Composable
@Preview
fun CardPreview() {
    Column() {
        DownloadTaskItem(expanded = true, status = DownloadTaskItemStatus.FETCHING_INFO)
        DownloadTaskItem(expanded = true, progress = 1f, status = DownloadTaskItemStatus.COMPLETED)
        DownloadTaskItem(expanded = true, progress = 0f, status = DownloadTaskItemStatus.CANCELED)
        DownloadTaskItem(expanded = true, progress = 0f, status = DownloadTaskItemStatus.ERROR)
    }

}