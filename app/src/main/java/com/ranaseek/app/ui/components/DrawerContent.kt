package com.ranaseek.app.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ranaseek.app.data.session.StoredSession
import com.ranaseek.app.ui.theme.RsTheme
import java.util.Calendar

private val DrawerWidth = 320.dp

/** 侧边栏：搜索框 + 置顶/今天/更早分组 + 底部用户行（复刻截图 3）。 */
@Composable
fun DrawerContent(
    sessions: List<StoredSession>,
    onSelectSession: (String) -> Unit,
    onDeleteSession: (String) -> Unit,
    onDeleteSessions: (List<String>) -> Unit,
    onRenameSession: (String, String) -> Unit,
    onTogglePin: (String) -> Unit,
    onSetPinned: (List<String>, Boolean) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val p = RsTheme
    val scroll = rememberScrollState()

    // 多选模式
    var selectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    // 长按菜单当前锚定的会话
    var menuSession by remember { mutableStateOf<StoredSession?>(null) }
    // 搜索
    var searchQuery by remember { mutableStateOf("") }
    // 重命名
    var renameTarget by remember { mutableStateOf<StoredSession?>(null) }
    var renameText by remember { mutableStateOf("") }
    // 删除确认
    var pendingDeleteIds by remember { mutableStateOf<List<String>?>(null) }

    val searching = searchQuery.isNotBlank()
    val searchHits = remember(searchQuery, sessions) {
        if (!searching) emptyList()
        else searchSessions(sessions, searchQuery)
    }

    val todayStart = rememberTodayStart()
    val pinned = sessions.filter { it.pinned }
    val unpinned = sessions.filterNot { it.pinned }
    val todaySessions = unpinned.filter { it.updatedAt >= todayStart }
    val earlierSessions = unpinned.filter { it.updatedAt < todayStart }

    fun exitSelection() {
        selectionMode = false
        selectedIds = emptySet()
    }

    Column(
        modifier = modifier
            .width(DrawerWidth)
            .fillMaxHeight()
            .background(p.bgSecondary)
            .statusBarsPadding()
            .padding(top = 8.dp)
    ) {
        if (selectionMode) {
            // 多选模式头部：选择对话 + 关闭
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "选择对话",
                    color = p.textPrimary,
                    fontSize = 20.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                )
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(p.surfaceElevated, CircleShape)
                        .clickable { exitSelection() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = "退出多选",
                        modifier = Modifier.size(20.dp),
                        tint = p.textPrimary
                    )
                }
            }
        } else {
            // 搜索框（可输入，KMP 匹配历史消息）
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .height(44.dp)
                    .background(p.surfaceElevated, RoundedCornerShape(22.dp))
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.Search,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = p.textSecondary
                )
                Spacer(Modifier.width(8.dp))
                androidx.compose.foundation.text.BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = p.textPrimary,
                        fontSize = 15.sp
                    ),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(p.brand),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        if (searchQuery.isEmpty()) {
                            Text("搜索对话内容...", color = p.textSecondary, fontSize = 15.sp)
                        }
                        inner()
                    }
                )
                if (searchQuery.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .clickable { searchQuery = "" },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.Close,
                            contentDescription = "清除",
                            modifier = Modifier.size(16.dp),
                            tint = p.textSecondary
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scroll)
        ) {
            if (searching) {
                // 搜索结果（按时间倒序）
                if (searchHits.isEmpty()) {
                    Text(
                        "没有找到相关内容",
                        color = p.textSecondary,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
                    )
                } else {
                    searchHits.forEach { hit ->
                        SearchResultRow(
                            hit = hit,
                            onClick = {
                                searchQuery = ""
                                onSelectSession(hit.session.id)
                            }
                        )
                    }
                }
            } else if (sessions.isEmpty()) {
                SectionHeader(title = "置顶", showSort = true, onSortClick = {
                    selectionMode = true
                    selectedIds = emptySet()
                })
                DrawerItem(text = "还没有对话", muted = true, selectionMode = false)
            } else {
                SectionHeader(title = "置顶", showSort = true, onSortClick = {
                    selectionMode = true
                    selectedIds = emptySet()
                })
                pinned.forEach { s ->
                    SessionRow(
                        session = s,
                        selectionMode = selectionMode,
                        selected = s.id in selectedIds,
                        menuOpen = menuSession?.id == s.id,
                        onMenuDismiss = { menuSession = null },
                        onOpenMenu = { menuSession = s },
                        onClick = {
                            if (selectionMode) {
                                selectedIds = if (s.id in selectedIds) selectedIds - s.id else selectedIds + s.id
                            } else {
                                onSelectSession(s.id)
                            }
                        },
                        onRename = {
                            renameText = s.title
                            renameTarget = s
                            menuSession = null
                        },
                        onTogglePin = {
                            onTogglePin(s.id)
                            menuSession = null
                        },
                        onMultiSelect = {
                            selectionMode = true
                            selectedIds = setOf(s.id)
                            menuSession = null
                        },
                        onDelete = {
                            pendingDeleteIds = listOf(s.id)
                            menuSession = null
                        }
                    )
                }

                if (todaySessions.isNotEmpty()) {
                    SectionHeader(title = "今天", showSort = false)
                    todaySessions.forEach { s ->
                        SessionRow(
                            session = s,
                            selectionMode = selectionMode,
                            selected = s.id in selectedIds,
                            menuOpen = menuSession?.id == s.id,
                            onMenuDismiss = { menuSession = null },
                            onOpenMenu = { menuSession = s },
                            onClick = {
                                if (selectionMode) {
                                    selectedIds = if (s.id in selectedIds) selectedIds - s.id else selectedIds + s.id
                                } else {
                                    onSelectSession(s.id)
                                }
                            },
                            onRename = {
                                renameText = s.title
                                renameTarget = s
                                menuSession = null
                            },
                            onTogglePin = {
                                onTogglePin(s.id)
                                menuSession = null
                            },
                            onMultiSelect = {
                                selectionMode = true
                                selectedIds = setOf(s.id)
                                menuSession = null
                            },
                            onDelete = {
                                pendingDeleteIds = listOf(s.id)
                                menuSession = null
                            }
                        )
                    }
                }

                if (earlierSessions.isNotEmpty()) {
                    SectionHeader(title = "更早", showSort = false)
                    earlierSessions.forEach { s ->
                        SessionRow(
                            session = s,
                            selectionMode = selectionMode,
                            selected = s.id in selectedIds,
                            menuOpen = menuSession?.id == s.id,
                            onMenuDismiss = { menuSession = null },
                            onOpenMenu = { menuSession = s },
                            onClick = {
                                if (selectionMode) {
                                    selectedIds = if (s.id in selectedIds) selectedIds - s.id else selectedIds + s.id
                                } else {
                                    onSelectSession(s.id)
                                }
                            },
                            onRename = {
                                renameText = s.title
                                renameTarget = s
                                menuSession = null
                            },
                            onTogglePin = {
                                onTogglePin(s.id)
                                menuSession = null
                            },
                            onMultiSelect = {
                                selectionMode = true
                                selectedIds = setOf(s.id)
                                menuSession = null
                            },
                            onDelete = {
                                pendingDeleteIds = listOf(s.id)
                                menuSession = null
                            }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        if (selectionMode) {
            // 多选底部操作栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SelectionAction(
                    icon = Icons.Outlined.PushPin,
                    label = "置顶",
                    enabled = selectedIds.isNotEmpty(),
                    onClick = {
                        onSetPinned(selectedIds.toList(), true)
                        exitSelection()
                    }
                )
                Spacer(Modifier.width(24.dp))
                SelectionAction(
                    icon = Icons.Outlined.Delete,
                    label = "删除",
                    enabled = selectedIds.isNotEmpty(),
                    danger = true,
                    onClick = {
                        pendingDeleteIds = selectedIds.toList()
                    }
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenSettings)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(p.surface, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    AppAvatar(size = 34.dp)
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    "RanaSeek",
                    color = p.textPrimary,
                    fontSize = 17.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.weight(1f))
                Icon(
                    Icons.Outlined.MoreHoriz,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = p.textSecondary
                )
            }
        }
        Spacer(Modifier.height(8.dp))
    }

    // 重命名对话框
    val renaming = renameTarget
    if (renaming != null) {
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            containerColor = p.surfaceElevated,
            title = { Text("重命名", color = p.textPrimary) },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onRenameSession(renaming.id, renameText)
                    renameTarget = null
                }) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) {
                    Text("取消")
                }
            }
        )
    }

    // 删除确认
    val deletingIds = pendingDeleteIds
    if (deletingIds != null) {
        val deletingSessions = sessions.filter { it.id in deletingIds }
        AlertDialog(
            onDismissRequest = { pendingDeleteIds = null },
            containerColor = p.surfaceElevated,
            title = { Text("删除对话", color = p.textPrimary) },
            text = {
                Text(
                    if (deletingSessions.size == 1) {
                        "「${deletingSessions.first().title}」会从本机删除，不能恢复哦。"
                    } else {
                        "${deletingSessions.size} 个对话会从本机删除，不能恢复哦。"
                    },
                    color = p.textSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (deletingSessions.size == 1) {
                        onDeleteSession(deletingSessions.first().id)
                    } else {
                        onDeleteSessions(deletingSessions.map { it.id })
                    }
                    pendingDeleteIds = null
                    exitSelection()
                }) {
                    Text("删除", color = p.danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteIds = null }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun SelectionAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean,
    danger: Boolean = false,
    onClick: () -> Unit
) {
    val p = RsTheme
    Row(
        modifier = Modifier
            .then(
                if (enabled) Modifier.clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick)
                else Modifier
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = when {
                danger && enabled -> p.danger
                enabled -> p.textPrimary
                else -> p.textSecondary.copy(alpha = 0.5f)
            }
        )
        Spacer(Modifier.width(6.dp))
        Text(
            label,
            color = when {
                danger && enabled -> p.danger
                enabled -> p.textPrimary
                else -> p.textSecondary.copy(alpha = 0.5f)
            },
            fontSize = 15.sp
        )
    }
}

@Composable
private fun rememberTodayStart(): Long {    return remember {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.timeInMillis
    }
}

@Composable
private fun SectionHeader(
    title: String,
    showSort: Boolean,
    onSortClick: (() -> Unit)? = null
) {
    val p = RsTheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = p.textSecondary, fontSize = 13.sp)
        Spacer(Modifier.weight(1f))
        if (showSort) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .then(
                        if (onSortClick != null) {
                            Modifier
                                .clip(CircleShape)
                                .clickable(onClick = onSortClick)
                        } else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Tune,
                    contentDescription = "多选",
                    modifier = Modifier.size(19.dp),
                    tint = p.textSecondary
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SessionRow(
    session: StoredSession,
    selectionMode: Boolean,
    selected: Boolean,
    menuOpen: Boolean,
    onMenuDismiss: () -> Unit,
    onOpenMenu: () -> Unit,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onTogglePin: () -> Unit,
    onMultiSelect: () -> Unit,
    onDelete: () -> Unit
) {
    val p = RsTheme
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { if (!selectionMode) onOpenMenu() }
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectionMode) {
                // 多选圆点
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .then(
                            if (selected) {
                                Modifier.background(p.brand)
                            } else {
                                Modifier.border(1.5.dp, p.textSecondary, CircleShape)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (selected) {
                        Icon(
                            Icons.Outlined.Check,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = androidx.compose.ui.graphics.Color.White
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
            }
            Text(
                session.title,
                color = if (selectionMode && !selected) p.textPrimary.copy(alpha = 0.8f) else p.textPrimary,
                fontSize = 17.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // 长按浮动菜单（重命名 / 置顶 / 多选 / 删除）
        DropdownMenu(
            expanded = menuOpen,
            onDismissRequest = onMenuDismiss,
            modifier = Modifier.width(248.dp),
            shape = RoundedCornerShape(16.dp),
            containerColor = p.surfaceElevated,
            tonalElevation = 0.dp
        ) {
            Column(Modifier.padding(vertical = 8.dp)) {
                DrawerMenuEntry(Icons.Outlined.Edit, "重命名", onClick = onRename)
                DrawerMenuEntry(
                    Icons.Outlined.PushPin,
                    if (session.pinned) "取消置顶" else "置顶",
                    onClick = onTogglePin
                )
                DrawerMenuEntry(Icons.Outlined.Tune, "多选", onClick = onMultiSelect)
                DrawerMenuEntry(Icons.Outlined.Delete, "删除", danger = true, onClick = onDelete)
            }
        }
    }
}

@Composable
private fun DrawerMenuEntry(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    danger: Boolean = false,
    onClick: () -> Unit
) {
    val p = RsTheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = if (danger) p.danger else p.textPrimary
        )
        Spacer(Modifier.width(16.dp))
        Text(
            label,
            color = if (danger) p.danger else p.textPrimary,
            fontSize = 17.sp
        )
    }
}

@Composable
private fun DrawerItem(
    text: String,
    muted: Boolean = false,
    selectionMode: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val p = RsTheme
    Text(
        text = text,
        color = if (muted) p.textPlaceholder else p.textPrimary,
        fontSize = 17.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else Modifier
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

// ---------- 搜索 ----------

/** 单条搜索命中：所属会话 + 展示片段 + 时间。 */
private data class SearchHit(
    val session: StoredSession,
    val snippet: String,
    val time: Long
)

/** KMP 子串匹配（忽略大小写）。 */
private fun kmpContains(text: String, pattern: String): Boolean {
    val t = text.lowercase()
    val p = pattern.lowercase()
    if (p.isEmpty()) return true
    if (t.length < p.length) return false

    val lps = IntArray(p.length)
    var j = 0
    for (i in 1 until p.length) {
        while (j > 0 && p[i] != p[j]) j = lps[j - 1]
        if (p[i] == p[j]) j++
        lps[i] = j
    }

    j = 0
    for (ch in t) {
        while (j > 0 && ch != p[j]) j = lps[j - 1]
        if (ch == p[j]) j++
        if (j == p.length) return true
    }
    return false
}

/** 搜索所有会话：标题或消息文本命中即列出一条，按会话时间倒序。 */
private fun searchSessions(sessions: List<StoredSession>, query: String): List<SearchHit> {
    val q = query.trim()
    if (q.isEmpty()) return emptyList()
    val hits = mutableListOf<SearchHit>()
    sessions.forEach { s ->
        if (kmpContains(s.title, q)) {
            hits += SearchHit(s, s.title, s.updatedAt)
        }
        s.messages.forEach { m ->
            if (m.text.isNotBlank() && kmpContains(m.text, q)) {
                hits += SearchHit(s, snippetOf(m.text, q), s.updatedAt)
            }
        }
    }
    return hits.sortedByDescending { it.time }
}

/** 截取命中位置附近的片段，前后加省略号。 */
private fun snippetOf(text: String, query: String): String {
    val idx = text.indexOf(query, ignoreCase = true)
    return if (idx < 0) {
        text.take(60).replace("\n", " ")
    } else {
        val start = (idx - 24).coerceAtLeast(0)
        val end = (idx + query.length + 40).coerceAtMost(text.length)
        buildString {
            if (start > 0) append("…")
            append(text.substring(start, end).replace("\n", " "))
            if (end < text.length) append("…")
        }
    }
}

private fun formatResultTime(ts: Long): String {
    val now = Calendar.getInstance()
    val cal = Calendar.getInstance().apply { timeInMillis = ts }
    return when {
        cal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
            cal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR) ->
            String.format("%d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
        cal.get(Calendar.YEAR) == now.get(Calendar.YEAR) ->
            "${cal.get(Calendar.MONTH) + 1}月${cal.get(Calendar.DAY_OF_MONTH)}日"
        else ->
            "${cal.get(Calendar.YEAR)}年${cal.get(Calendar.MONTH) + 1}月${cal.get(Calendar.DAY_OF_MONTH)}日"
    }
}

@Composable
private fun SearchResultRow(hit: SearchHit, onClick: () -> Unit) {
    val p = RsTheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(p.surfaceElevated, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.ChatBubbleOutline,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = p.textSecondary
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    hit.session.title,
                    color = p.textPrimary,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    formatResultTime(hit.time),
                    color = p.textSecondary,
                    fontSize = 12.sp
                )
            }
            if (hit.snippet != hit.session.title) {
                Text(
                    hit.snippet,
                    color = p.textSecondary,
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}
