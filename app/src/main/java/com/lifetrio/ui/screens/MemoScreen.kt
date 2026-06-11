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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.lifetrio.ui.components.EmptyState
import com.lifetrio.ui.components.FieldLabel
import com.lifetrio.ui.components.FilterPill
import com.lifetrio.ui.components.PillSearchField
import com.lifetrio.ui.components.PrimaryButton
import com.lifetrio.ui.components.ScreenHeader
import com.lifetrio.ui.components.SoftChip
import com.lifetrio.ui.components.UnderlineField
import com.lifetrio.ui.theme.AppColors
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
    val memos by container.memoRepository.search(query).collectAsState(initial = emptyList())

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

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        AppPage(modifier = Modifier.padding(padding)) {
            item { ScreenHeader("备忘", "捕捉灵感・管理日常") }
            if (selectedMemo == null) {
                item { PillSearchField(query, { query = it }, "搜索标题、正文或标签") }
                item {
                    MemoEditorCard(
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
                                return@MemoEditorCard
                            }
                            runCatching { speechLauncher.launch(intent) }
                                .onFailure {
                                    scope.launch { snackbarHostState.showSnackbar("当前设备不可用语音识别") }
                                }
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
                                editingId = null
                                title = ""
                                body = ""
                                tags = ""
                                pinned = false
                                imageUris = emptyList()
                                snackbarHostState.showSnackbar("已保存备忘")
                            }
                        }
                    )
                }
                if (memos.isEmpty()) {
                    item { EmptyState("暂无备忘", "先写下一条随手记", "📝") }
                }
                items(memos, key = { "memo-${it.id}" }) { memo ->
                    MemoCard(
                        memo = memo,
                        onOpen = { selectedMemo = memo },
                        onEdit = {
                            selectedMemo = null
                            editingId = memo.id
                            title = memo.title
                            body = memo.body
                            pinned = memo.isPinned
                            imageUris = memo.imageUris.split("|").filter { it.isNotBlank() }
                            scope.launch { tags = container.memoRepository.tagsForMemo(memo.id).joinToString(",") { it.name } }
                        },
                        onDelete = {
                            scope.launch {
                                container.memoRepository.deleteMemo(memo.id)
                                if (selectedMemo?.id == memo.id) selectedMemo = null
                                if (editingId == memo.id) {
                                    editingId = null
                                    title = ""
                                    body = ""
                                    tags = ""
                                    pinned = false
                                    imageUris = emptyList()
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
                            onEdit = {
                                selectedMemo = null
                                editingId = memo.id
                                title = memo.title
                                body = memo.body
                                pinned = memo.isPinned
                                imageUris = memo.imageUris.split("|").filter { it.isNotBlank() }
                                scope.launch { tags = container.memoRepository.tagsForMemo(memo.id).joinToString(",") { it.name } }
                            },
                            onDelete = {
                                scope.launch {
                                    container.memoRepository.deleteMemo(memo.id)
                                    selectedMemo = null
                                    if (editingId == memo.id) {
                                        editingId = null
                                        title = ""
                                        body = ""
                                        tags = ""
                                        pinned = false
                                        imageUris = emptyList()
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
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MemoEditorCard(
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
    onSave: () -> Unit
) {
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            FieldLabel("📌", "标题")
            UnderlineField(title, onTitle, "咖啡馆备忘")
            FieldLabel("✍️", "正文")
            UnderlineField(body, onBody, "随手记", modifier = Modifier.height(110.dp), singleLine = false)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onBold) { Text("B", fontWeight = FontWeight.Bold) }
                IconButton(onClick = onList) { Icon(Icons.AutoMirrored.Filled.FormatListBulleted, contentDescription = "列表") }
                IconButton(onClick = onTodo) { Icon(Icons.Default.CheckBox, contentDescription = "待办") }
                IconButton(onClick = onVoice) { Icon(Icons.Default.Mic, contentDescription = "语音") }
                IconButton(onClick = { onPinned(!pinned) }) {
                    Icon(Icons.Default.PushPin, contentDescription = "置顶", tint = if (pinned) AppColors.Blue else AppColors.Muted)
                }
            }
            FieldLabel("🏷️", "标签（点击 + 添加）")
            TagEditor(tags, onTags)
            FieldLabel("🖼️", "图片附件")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                DashedUploadBox(
                    "+ 添加图片",
                    if (imageUris.isEmpty()) "🖼️" else "${imageUris.size} 张",
                    onPickImage,
                    modifier = Modifier.weight(1f)
                )
                DashedUploadBox(
                    "拍照",
                    "📷",
                    onTakePhoto,
                    modifier = Modifier.weight(1f)
                )
            }
            if (imageUris.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    imageUris.forEach { path ->
                        RemovableMemoImagePreview(path = path, onRemove = { onRemoveImage(path) })
                    }
                }
            }
            PrimaryButton("💾 保存笔记", onSave, enabled = title.isNotBlank() || body.isNotBlank())
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagEditor(value: String, onValueChange: (String) -> Unit) {
    val tags = value.split(",", "，").map { it.trim() }.filter { it.isNotBlank() }.distinct()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            tags.forEach { tag -> SoftChip(tag) }
        }
        UnderlineField(value, onValueChange, "生活，账单")
    }
}

@Composable
private fun MemoCard(memo: MemoEntity, onOpen: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit, onToPlan: () -> Unit) {
    AppCard(onClick = onOpen) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (memo.isPinned) Icon(Icons.Default.Star, contentDescription = "置顶", tint = AppColors.Yellow, modifier = Modifier.size(18.dp))
                Text(memo.title, color = AppColors.Text, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            }
            Text(memo.body, maxLines = 4, overflow = TextOverflow.Ellipsis, color = AppColors.Text)
            if (memo.imageUris.isNotBlank()) Text("含 ${memo.imageUris.split("|").count { it.isNotBlank() }} 张图片", color = AppColors.Muted)
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onEdit) { Text("编辑") }
                TextButton(onClick = onDelete) { Text("删除", color = AppColors.Red) }
                TextButton(onClick = onToPlan) { Text("转计划") }
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
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onBack) { Text("返回") }
                Spacer(Modifier.width(6.dp))
                Text("备忘详情", color = AppColors.Text, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                if (memo.isPinned) Icon(Icons.Default.Star, contentDescription = "置顶", tint = AppColors.Yellow, modifier = Modifier.size(20.dp))
            }
            Text(memo.title.ifBlank { "未命名备忘" }, color = AppColors.Text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            if (memo.body.isBlank()) {
                Text("暂无正文", color = AppColors.Muted)
            } else {
                Text(memo.body, color = AppColors.Text)
            }
            Text("更新于 ${memo.updatedAt.toString().replace("T", " ").substringBefore(".")}", color = AppColors.Muted)
            FieldLabel("🖼️", "图片附件")
            if (images.isEmpty()) {
                Text("暂无图片", color = AppColors.Muted)
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    images.forEach { path -> RemovableMemoImagePreview(path = path, onRemove = { onRemoveImage(path) }) }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                FilterPill("编辑", false, onEdit)
                FilterPill("转计划", false, onToPlan)
                TextButton(onClick = onDelete) { Text("删除", color = AppColors.Red) }
            }
        }
    }
}

@Composable
private fun RemovableMemoImagePreview(path: String, onRemove: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth()) {
        MemoImagePreview(path)
        IconButton(
            onClick = onRemove,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .background(AppColors.Surface, CircleShape)
                .size(34.dp)
        ) {
            Icon(Icons.Default.Close, contentDescription = "删除图片", tint = AppColors.Red, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun MemoImagePreview(path: String) {
    val bitmap = remember(path) { BitmapFactory.decodeFile(path)?.asImageBitmap() }
    if (bitmap == null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
                .background(AppColors.BlueSoft, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("图片无法读取", color = AppColors.Muted)
        }
    } else {
        Image(
            bitmap = bitmap,
            contentDescription = "备忘图片",
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
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
