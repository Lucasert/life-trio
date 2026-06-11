package com.lifetrio.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.speech.RecognizerIntent
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EventRepeat
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.material.icons.outlined.Title
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.navigation.NavHostController
import com.lifetrio.core.data.AppContainer
import com.lifetrio.core.data.db.entity.CarryStrategy
import com.lifetrio.core.data.db.entity.MemoEntity
import com.lifetrio.core.data.db.entity.PlanRuleType
import com.lifetrio.ui.components.AppCard
import com.lifetrio.ui.components.AppPage
import com.lifetrio.ui.components.DashedUploadBox
import com.lifetrio.ui.components.EditorSheet
import com.lifetrio.ui.components.EmptyState
import com.lifetrio.ui.components.FieldLabel
import com.lifetrio.ui.components.PillSearchField
import com.lifetrio.ui.components.PrimaryButton
import com.lifetrio.ui.components.ScreenHeader
import com.lifetrio.ui.components.SoftChip
import com.lifetrio.ui.components.UnderlineField
import com.lifetrio.ui.theme.Spacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MemoScreen(container: AppContainer, navController: NavHostController, planRoute: String) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var query by remember { mutableStateOf("") }
    var editingId by remember { mutableStateOf<Long?>(null) }
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("") }
    var pinned by remember { mutableStateOf(false) }
    var imageUris by remember { mutableStateOf<List<String>>(emptyList()) }
    var pendingCameraImagePath by remember { mutableStateOf<String?>(null) }
    var selectedMemo by remember { mutableStateOf<MemoEntity?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    val memos by container.memoRepository.search(query).collectAsState(initial = emptyList())

    fun resetEditor() {
        editingId = null
        title = ""
        body = ""
        tags = ""
        pinned = false
        imageUris = emptyList()
    }

    fun openNewEditor() {
        resetEditor()
        showEditor = true
    }

    fun openEditEditor(memo: MemoEntity) {
        selectedMemo = null
        editingId = memo.id
        title = memo.title
        body = memo.body
        pinned = memo.isPinned
        imageUris = memo.imageUris.split("|").filter { it.isNotBlank() }
        scope.launch { tags = container.memoRepository.tagsForMemo(memo.id).joinToString(",") { it.name } }
        showEditor = true
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
        val uri = result.data?.data
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val copyResult = runCatching { copyImageToPrivateStorage(context, uri) }
            copyResult.onSuccess { imageUris = imageUris + it }
            copyResult.onFailure { error ->
                Log.e("LifeTrioMemo", "Image copy failed", error)
                snackbarHostState.showSnackbar("图片添加失败：${error.javaClass.simpleName}")
            }
        }
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
        val path = pendingCameraImagePath
        if (result.resultCode == android.app.Activity.RESULT_OK && path != null) {
            imageUris = imageUris + path
        } else if (path != null) {
            File(path).delete()
        }
        pendingCameraImagePath = null
    }
    val speechLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val text = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty()
        if (text.isNotBlank()) body = "$body$text"
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (selectedMemo == null) {
                FloatingActionButton(onClick = { openNewEditor() }) {
                    Icon(Icons.Default.Add, contentDescription = "新增备忘")
                }
            }
        }
    ) { padding ->
        AppPage(modifier = Modifier.padding(padding)) {
            item { ScreenHeader("备忘", "捕捉灵感・管理日常") }
            if (selectedMemo == null) {
                item { PillSearchField(query, { query = it }, "搜索标题、正文或标签") }
                if (memos.isEmpty()) {
                    item { EmptyState("暂无备忘", "点击右下角按钮写一条随手记", Icons.Outlined.StickyNote2) }
                }
                items(memos, key = { "memo-${it.id}" }) { memo ->
                    MemoCard(
                        memo = memo,
                        onOpen = { selectedMemo = memo },
                        onEdit = { openEditEditor(memo) },
                        onDelete = {
                            scope.launch {
                                container.memoRepository.deleteMemo(memo.id)
                                if (selectedMemo?.id == memo.id) selectedMemo = null
                                if (editingId == memo.id) {
                                    showEditor = false
                                    resetEditor()
                                }
                                snackbarHostState.showSnackbar("已删除备忘")
                            }
                        },
                        onToPlan = {
                            scope.launch {
                                container.planRepository.addPlan(
                                    title = memo.title,
                                    note = memo.body.take(120),
                                    ruleType = PlanRuleType.Daily,
                                    weekdays = emptySet(),
                                    monthDays = emptySet(),
                                    intervalDays = 1,
                                    startDate = LocalDate.now(),
                                    carryStrategy = CarryStrategy.CarryNextDay,
                                    sourceMemoId = memo.id
                                )
                                navController.navigate(planRoute)
                            }
                        }
                    )
                }
            } else {
                selectedMemo?.let { memo ->
                    item(key = "memo-detail-${memo.id}") {
                        MemoDetailCard(
                            memo = memo,
                            onBack = { selectedMemo = null },
                            onEdit = { openEditEditor(memo) },
                            onDelete = {
                                scope.launch {
                                    container.memoRepository.deleteMemo(memo.id)
                                    selectedMemo = null
                                    if (editingId == memo.id) {
                                        showEditor = false
                                        resetEditor()
                                    }
                                    snackbarHostState.showSnackbar("已删除备忘")
                                }
                            },
                            onToPlan = {
                                scope.launch {
                                    container.planRepository.addPlan(
                                        title = memo.title,
                                        note = memo.body.take(120),
                                        ruleType = PlanRuleType.Daily,
                                        weekdays = emptySet(),
                                        monthDays = emptySet(),
                                        intervalDays = 1,
                                        startDate = LocalDate.now(),
                                        carryStrategy = CarryStrategy.CarryNextDay,
                                        sourceMemoId = memo.id
                                    )
                                    selectedMemo = null
                                    navController.navigate(planRoute)
                                }
                            },
                            onRemoveImage = { path ->
                                scope.launch {
                                    val nextImages = memo.imageUris.split("|").filter { it.isNotBlank() && it != path }
                                    val updatedMemo = container.memoRepository.updateMemoImages(memo.id, nextImages)
                                    if (updatedMemo != null) {
                                        selectedMemo = updatedMemo
                                        deleteMemoImageFile(context, path)
                                        snackbarHostState.showSnackbar("已删除图片")
                                    } else {
                                        selectedMemo = null
                                        snackbarHostState.showSnackbar("备忘已不存在")
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showEditor) {
        MemoEditorSheet(
            isEditing = editingId != null,
            title = title,
            body = body,
            tags = tags,
            pinned = pinned,
            imageUris = imageUris,
            onTitle = { title = it },
            onBody = { body = it },
            onTags = { tags = it },
            onPinned = { pinned = it },
            onRemoveImage = { path ->
                imageUris = imageUris.filterNot { it == path }
                deleteMemoImageFile(context, path)
            },
            onBold = { body += "**加粗文本**" },
            onList = { body += "\n- " },
            onTodo = { body += "\n- [ ] " },
            onPickImage = {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "image/*"
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                }
                runCatching { imagePicker.launch(Intent.createChooser(intent, "选择图片")) }
                    .onFailure { error ->
                        Log.e("LifeTrioMemo", "Image picker launch failed", error)
                        scope.launch { snackbarHostState.showSnackbar("图片选择失败：${error.javaClass.simpleName}") }
                    }
            },
            onTakePhoto = {
                val photoFile = createMemoImageFile(context)
                val photoUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    photoFile
                )
                val cameraIntent = createCameraIntent(context, photoUri)
                pendingCameraImagePath = photoFile.absolutePath
                runCatching { cameraLauncher.launch(cameraIntent) }
                    .onFailure {
                        pendingCameraImagePath = null
                        photoFile.delete()
                        scope.launch { snackbarHostState.showSnackbar("当前设备不可用相机") }
                    }
            },
            onVoice = {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.CHINESE.toLanguageTag())
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "说出要记录的内容")
                }
                if (intent.resolveActivity(context.packageManager) == null) {
                    scope.launch { snackbarHostState.showSnackbar("当前设备不可用语音识别") }
                } else {
                    runCatching { speechLauncher.launch(intent) }
                        .onFailure {
                            scope.launch { snackbarHostState.showSnackbar("当前设备不可用语音识别") }
                        }
                }
            },
            onDismiss = {
                showEditor = false
                resetEditor()
            },
            onSave = {
                scope.launch {
                    container.memoRepository.saveMemo(
                        id = editingId,
                        title = title,
                        body = body,
                        tags = tags.split(",", "，"),
                        isPinned = pinned,
                        imageUris = imageUris
                    )
                    showEditor = false
                    resetEditor()
                    snackbarHostState.showSnackbar("已保存备忘")
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MemoEditorSheet(
    isEditing: Boolean,
    title: String,
    body: String,
    tags: String,
    pinned: Boolean,
    imageUris: List<String>,
    onTitle: (String) -> Unit,
    onBody: (String) -> Unit,
    onTags: (String) -> Unit,
    onPinned: (Boolean) -> Unit,
    onRemoveImage: (String) -> Unit,
    onBold: () -> Unit,
    onList: () -> Unit,
    onTodo: () -> Unit,
    onPickImage: () -> Unit,
    onTakePhoto: () -> Unit,
    onVoice: () -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    EditorSheet(title = if (isEditing) "编辑备忘" else "新增备忘", onDismiss = onDismiss) {
        FieldLabel(Icons.Outlined.Title, "标题")
        UnderlineField(title, onTitle, "咖啡馆备忘")
        FieldLabel(Icons.Outlined.Edit, "正文")
        UnderlineField(body, onBody, "随手记", modifier = Modifier.height(110.dp), singleLine = false)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xxs), verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
            IconButton(onClick = onBold) { Text("B", fontWeight = FontWeight.Bold) }
            IconButton(onClick = onList) { Icon(Icons.AutoMirrored.Filled.FormatListBulleted, contentDescription = "列表") }
            IconButton(onClick = onTodo) { Icon(Icons.Default.CheckBox, contentDescription = "待办") }
            IconButton(onClick = onVoice) { Icon(Icons.Default.Mic, contentDescription = "语音") }
            IconButton(onClick = { onPinned(!pinned) }) {
                Icon(Icons.Default.PushPin, contentDescription = "置顶", tint = if (pinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        FieldLabel(Icons.AutoMirrored.Outlined.Label, "标签（逗号分隔）")
        TagEditor(tags, onTags)
        FieldLabel(Icons.Outlined.Image, "图片附件")
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), modifier = Modifier.fillMaxWidth()) {
            DashedUploadBox(
                "添加图片",
                if (imageUris.isEmpty()) "相册" else "${imageUris.size} 张",
                onPickImage,
                modifier = Modifier.weight(1f)
            )
            DashedUploadBox(
                "拍照",
                "相机",
                onTakePhoto,
                modifier = Modifier.weight(1f)
            )
        }
        if (imageUris.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                imageUris.forEach { path ->
                    RemovableMemoImagePreview(path = path, onRemove = { onRemoveImage(path) })
                }
            }
        }
        PrimaryButton("保存笔记", onSave, enabled = title.isNotBlank() || body.isNotBlank())
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagEditor(value: String, onValueChange: (String) -> Unit) {
    val tags = value.split(",", "，").map { it.trim() }.filter { it.isNotBlank() }.distinct()
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs), verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
            tags.forEach { tag -> SoftChip(tag) }
        }
        UnderlineField(value, onValueChange, "生活，账单")
    }
}

@Composable
private fun MemoCard(memo: MemoEntity, onOpen: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit, onToPlan: () -> Unit) {
    AppCard(onClick = onOpen) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (memo.isPinned) Icon(Icons.Default.Star, contentDescription = "置顶", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Text(memo.title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            }
            Text(memo.body, maxLines = 4, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurface)
            if (memo.imageUris.isNotBlank()) Text("含 ${memo.imageUris.split("|").count { it.isNotBlank() }} 张图片", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = onEdit) { Icon(Icons.Outlined.Edit, contentDescription = "编辑") }
                IconButton(onClick = onToPlan) { Icon(Icons.Outlined.EventRepeat, contentDescription = "转计划") }
                IconButton(onClick = onDelete) { Icon(Icons.Outlined.DeleteOutline, contentDescription = "删除", tint = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MemoDetailCard(
    memo: MemoEntity,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToPlan: () -> Unit,
    onRemoveImage: (String) -> Unit
) {
    val images = memo.imageUris.split("|").filter { it.isNotBlank() }
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回") }
                Spacer(Modifier.width(Spacing.xxs))
                Text("备忘详情", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                if (memo.isPinned) Icon(Icons.Default.Star, contentDescription = "置顶", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
            Text(memo.title.ifBlank { "未命名备忘" }, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleLarge)
            if (memo.body.isBlank()) {
                Text("暂无正文", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Text(memo.body, color = MaterialTheme.colorScheme.onSurface)
            }
            Text("更新于 ${memo.updatedAt.toString().replace("T", " ").substringBefore(".")}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            FieldLabel(Icons.Outlined.Image, "图片附件")
            if (images.isEmpty()) {
                Text("暂无图片", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    images.forEach { path -> RemovableMemoImagePreview(path = path, onRemove = { onRemoveImage(path) }) }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onEdit) { Icon(Icons.Outlined.Edit, contentDescription = "编辑") }
                IconButton(onClick = onToPlan) { Icon(Icons.Outlined.EventRepeat, contentDescription = "转计划") }
                IconButton(onClick = onDelete) { Icon(Icons.Outlined.DeleteOutline, contentDescription = "删除", tint = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@Composable
private fun RemovableMemoImagePreview(path: String, onRemove: () -> Unit) {
    Box(modifier = Modifier.size(104.dp)) {
        MemoImagePreview(path)
        IconButton(
            onClick = onRemove,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(Spacing.xxs)
                .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.small)
                .size(28.dp)
        ) {
            Icon(Icons.Default.Close, contentDescription = "删除图片", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun MemoImagePreview(path: String) {
    val bitmap = remember(path) { BitmapFactory.decodeFile(path)?.asImageBitmap() }
    if (bitmap == null) {
        Box(
            modifier = Modifier
                .size(104.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text("无法读取", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
        }
    } else {
        Image(
            bitmap = bitmap,
            contentDescription = "备忘图片",
            modifier = Modifier
                .size(104.dp)
                .clip(MaterialTheme.shapes.medium),
            contentScale = ContentScale.Crop
        )
    }
}

private suspend fun copyImageToPrivateStorage(context: Context, source: Uri): String = withContext(Dispatchers.IO) {
    val file = createMemoImageFile(context)
    context.contentResolver.openInputStream(source)?.use { input ->
        file.outputStream().use { output -> input.copyTo(output) }
    } ?: error("Cannot open image")
    file.absolutePath
}

private fun createCameraIntent(context: Context, output: Uri): Intent =
    Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
        putExtra(MediaStore.EXTRA_OUTPUT, output)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        context.packageManager.queryIntentActivities(this, 0).forEach { resolved ->
            context.grantUriPermission(
                resolved.activityInfo.packageName,
                output,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }
    }

private fun createMemoImageFile(context: Context): File {
    val dir = File(context.filesDir, "memo_images").apply { mkdirs() }
    return File(dir, "${System.currentTimeMillis()}_${System.nanoTime()}.jpg")
}

private fun deleteMemoImageFile(context: Context, path: String) {
    val file = File(path)
    val imageDir = File(context.filesDir, "memo_images")
    if (runCatching { file.canonicalPath.startsWith(imageDir.canonicalPath) }.getOrDefault(false)) {
        runCatching { file.delete() }
    }
}
