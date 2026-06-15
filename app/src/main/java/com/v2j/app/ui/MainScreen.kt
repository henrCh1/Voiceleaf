package com.v2j.app.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.v2j.app.BuildConfig
import com.v2j.app.MainViewModel
import com.v2j.app.SyncUi
import com.v2j.app.data.Entry
import com.v2j.app.data.PolishStatus
import com.v2j.app.sync.WeekMath

@Composable
fun MainScreen(
    vm: MainViewModel,
    onOpenSettings: () -> Unit,
    onOpenHistory: () -> Unit,
) {
    val entries by vm.entries.collectAsState()
    val draft by vm.draft.collectAsState()
    val sync by vm.sync.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var editing by remember { mutableStateOf<Entry?>(null) }

    LaunchedEffect(sync) {
        val current = sync
        if (current is SyncUi.Done) {
            snackbar.showSnackbar(current.message)
            vm.onSyncMessageShown()
        }
    }

    val weekRange = remember {
        val today = WeekMath.localDate(System.currentTimeMillis())
        "${WeekMath.mmDashDd(WeekMath.weekStartDate(today))} – ${WeekMath.mmDashDd(WeekMath.weekEndDate(today))}"
    }

    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    val enter by animateFloatAsState(if (appeared) 1f else 0f, tween(460), label = "enter")

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = {
            SnackbarHost(snackbar) { data ->
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    shape = MaterialTheme.shapes.medium,
                    containerColor = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                ) { Text(data.visuals.message, style = MaterialTheme.typography.bodyMedium) }
            }
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .graphicsLayer {
                    alpha = enter
                    translationY = (1f - enter) * 44f
                },
        ) {
            Header(weekRange = weekRange, onOpenSettings = onOpenSettings, onOpenHistory = onOpenHistory)

            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                Spacer(Modifier.height(4.dp))
                WritingCard(
                    draft = draft,
                    onChange = vm::onDraftChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
                Spacer(Modifier.height(12.dp))
                ActionRow(
                    canFinish = draft.isNotBlank(),
                    syncing = sync is SyncUi.Running,
                    onFinish = vm::onFinish,
                    onSync = vm::onSync,
                )
                Spacer(Modifier.height(20.dp))
                SectionLabel(count = entries.size)
                Spacer(Modifier.height(10.dp))
                if (entries.isEmpty()) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .weight(1.2f),
                        contentAlignment = Alignment.Center,
                    ) { EmptyState() }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1.2f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 24.dp),
                    ) {
                        items(entries, key = { it.id }) { entry ->
                            EntryCard(
                                entry = entry,
                                onEdit = { editing = entry },
                                onDelete = { vm.onDelete(entry) },
                                onRetry = { vm.onRetry(entry) },
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }
                }
            }
        }
    }

    editing?.let { e ->
        EntryEditDialog(
            initialText = (e.polishedText ?: e.rawText).trim(),
            onDismiss = { editing = null },
            onSave = { vm.onEdit(e, it); editing = null },
        )
    }
}

@Composable
private fun Header(weekRange: String, onOpenSettings: () -> Unit, onOpenHistory: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 14.dp, top = 16.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LeafMark(Modifier.size(30.dp))
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "Voiceleaf",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "v${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 3.dp),
                )
            }
            Text(
                text = "本周 · $weekRange",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        HeaderIconButton(icon = Icons.Rounded.History, desc = "历史记录", onClick = onOpenHistory)
        Spacer(Modifier.width(8.dp))
        HeaderIconButton(icon = Icons.Rounded.Settings, desc = "设置", onClick = onOpenSettings)
    }
}

@Composable
private fun HeaderIconButton(icon: ImageVector, desc: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.size(42.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                icon,
                contentDescription = desc,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(21.dp),
            )
        }
    }
}

@Composable
private fun WritingCard(draft: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
    ) {
        Column(Modifier.fillMaxSize().padding(18.dp)) {
            Box(Modifier.fillMaxWidth().weight(1f)) {
                if (draft.isEmpty()) {
                    Text(
                        text = "此刻在想什么？说出来或写下来，按「完成」我帮你润成书面的。",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                BasicTextField(
                    value = draft,
                    onValueChange = onChange,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.Mic,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(15.dp),
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text = "键盘上点麦克风即可口述",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ActionRow(
    canFinish: Boolean,
    syncing: Boolean,
    onFinish: () -> Unit,
    onSync: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(
            onClick = onFinish,
            enabled = canFinish,
            shape = CircleShape,
            modifier = Modifier
                .weight(1f)
                .height(54.dp),
        ) {
            Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("完成", style = MaterialTheme.typography.labelLarge)
        }
        FilledTonalButton(
            onClick = onSync,
            enabled = !syncing,
            shape = CircleShape,
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ),
            modifier = Modifier
                .weight(1f)
                .height(54.dp),
        ) {
            if (syncing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            } else {
                Icon(Icons.Rounded.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("同步", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun SectionLabel(count: Int) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "本周记录",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
            Text(
                "$count 条",
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun EntryCard(
    entry: Entry,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = statusColor(entry.polishStatus)
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
    ) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            Box(
                Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(accent.copy(alpha = 0.85f)),
            )
            Column(Modifier.weight(1f).padding(start = 14.dp, end = 8.dp, top = 12.dp, bottom = 12.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            WeekMath.hhmm(entry.createdAt),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(8.dp))
                        StatusPill(entry.polishStatus)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (entry.polishStatus != PolishStatus.POLISHING) {
                            IconButton(onClick = onEdit, modifier = Modifier.size(30.dp)) {
                                Icon(
                                    Icons.Rounded.Edit,
                                    contentDescription = "编辑",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(17.dp),
                                )
                            }
                        }
                        IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                            Icon(
                                Icons.Rounded.DeleteOutline,
                                contentDescription = "删除",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = (entry.polishedText ?: entry.rawText).trim(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.animateContentSize(),
                )
                if (entry.polishStatus == PolishStatus.FAILED) {
                    Spacer(Modifier.height(10.dp))
                    RetryChip(onRetry)
                }
            }
        }
    }
}

@Composable
private fun RetryChip(onRetry: () -> Unit) {
    Surface(
        onClick = onRetry,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.errorContainer,
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Rounded.Refresh,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(15.dp),
            )
            Spacer(Modifier.width(5.dp))
            Text(
                "重试润色",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}

@Composable
private fun StatusPill(status: PolishStatus) {
    val color = statusColor(status)
    val label = when (status) {
        PolishStatus.POLISHED -> "已润色"
        PolishStatus.POLISHING -> "润色中"
        PolishStatus.RAW -> "待润色"
        PolishStatus.FAILED -> "润色失败"
    }
    Surface(shape = CircleShape, color = color.copy(alpha = 0.14f)) {
        Row(
            Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color),
            )
            Spacer(Modifier.width(5.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun statusColor(status: PolishStatus): Color = when (status) {
    PolishStatus.POLISHED -> MaterialTheme.colorScheme.primary
    PolishStatus.POLISHING -> MaterialTheme.colorScheme.tertiary
    PolishStatus.RAW -> MaterialTheme.colorScheme.onSurfaceVariant
    PolishStatus.FAILED -> MaterialTheme.colorScheme.error
}

@Composable
private fun EmptyState() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(24.dp),
    ) {
        Box(Modifier.alpha(0.35f)) { LeafMark(Modifier.size(58.dp)) }
        Spacer(Modifier.height(16.dp))
        Text(
            "本周还没有记录",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "写下或说出此刻的想法，按「完成」即可。\n攒一周，点「同步」就进 Obsidian。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun LeafMark(modifier: Modifier = Modifier) {
    val leaf = MaterialTheme.colorScheme.primary
    val vein = MaterialTheme.colorScheme.onPrimary
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.5f, 0f)
            cubicTo(w * 1.0f, h * 0.30f, w * 0.86f, h * 0.82f, w * 0.5f, h)
            cubicTo(w * 0.14f, h * 0.82f, w * 0.0f, h * 0.30f, w * 0.5f, 0f)
            close()
        }
        drawPath(path, leaf)
        // Midrib
        drawLine(
            color = vein.copy(alpha = 0.9f),
            start = Offset(w * 0.5f, h * 0.14f),
            end = Offset(w * 0.5f, h * 0.9f),
            strokeWidth = w * 0.06f,
            cap = StrokeCap.Round,
        )
        // Side veins — match the launcher logo: one up-right (upper), one up-left (lower).
        drawLine(
            color = vein.copy(alpha = 0.75f),
            start = Offset(w * 0.5f, h * 0.46f),
            end = Offset(w * 0.66f, h * 0.37f),
            strokeWidth = w * 0.045f,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = vein.copy(alpha = 0.75f),
            start = Offset(w * 0.5f, h * 0.58f),
            end = Offset(w * 0.34f, h * 0.49f),
            strokeWidth = w * 0.045f,
            cap = StrokeCap.Round,
        )
    }
}
