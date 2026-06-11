package com.lifetrio.ui.screens

import android.app.KeyguardManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.lifetrio.core.data.AppContainer
import com.lifetrio.password.PasswordRecord
import com.lifetrio.ui.components.AppCard
import com.lifetrio.ui.components.AppPage
import com.lifetrio.ui.components.EmptyState
import com.lifetrio.ui.components.FieldLabel
import com.lifetrio.ui.components.FilterPill
import com.lifetrio.ui.components.PillSearchField
import com.lifetrio.ui.components.PrimaryButton
import com.lifetrio.ui.components.ScreenHeader
import com.lifetrio.ui.components.SectionTitle
import com.lifetrio.ui.components.SoftChip
import com.lifetrio.ui.components.UnderlineField
import com.lifetrio.ui.theme.AppColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PasswordScreen(container: AppContainer) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current
    val repository = container.passwordVaultRepository
    val isUnlocked by repository.isUnlocked.collectAsState()
    val records by repository.records.collectAsState()
    var query by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<PasswordRecord?>(null) }
    var selected by remember { mutableStateOf<PasswordRecord?>(null) }

    DisposableEffect(lifecycleOwner, repository) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                repository.lock()
                editing = null
                selected = null
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            repository.lock()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        if (!isUnlocked) {
            LockedPasswordScreen(
                modifier = Modifier.padding(padding),
                onUnlock = {
                    if (activity == null) {
                        scope.launch { snackbarHostState.showSnackbar("无法启动本机验证") }
                    } else {
                        requestPasswordVaultAuth(
                            activity,
                            onSuccess = {
                                scope.launch {
                                    runCatching { repository.unlock() }
                                        .onFailure { snackbarHostState.showSnackbar("保险库无法解密") }
                                }
                            },
                            onError = { scope.launch { snackbarHostState.showSnackbar(it) } }
                        )
                    }
                }
            )
        } else {
            PasswordVaultContent(
                modifier = Modifier.padding(padding),
                records = records.filter { it.matches(query) },
                query = query,
                onQuery = { query = it },
                editing = editing,
                selected = selected,
                onNew = {
                    selected = null
                    editing = PasswordRecord(name = "", account = "", secret = "", target = "")
                },
                onEdit = {
                    selected = null
                    editing = it
                },
                onSelect = {
                    editing = null
                    selected = it
                },
                onCancelEdit = { editing = null },
                onSave = {
                    scope.launch {
                        repository.save(it)
                        editing = null
                        snackbarHostState.showSnackbar("已保存")
                    }
                },
                onDelete = {
                    scope.launch {
                        repository.delete(it.id)
                        selected = null
                        editing = null
                        snackbarHostState.showSnackbar("已删除")
                    }
                },
                onCopyAccount = {
                    copyToClipboard(context, "life-trio 账号", it)
                    scope.launch { snackbarHostState.showSnackbar("账号已复制") }
                },
                onCopySecret = {
                    copyToClipboard(context, "life-trio 密码", it)
                    scope.launch {
                        snackbarHostState.showSnackbar("密码已复制，30 秒后清空剪贴板")
                        delay(30_000)
                        clearClipboardIfValueMatches(context, it)
                    }
                }
            )
        }
    }
}

@Composable
private fun LockedPasswordScreen(modifier: Modifier = Modifier, onUnlock: () -> Unit) {
    Box(modifier = modifier.background(AppColors.Background)) {
        AppPage {
            item { ScreenHeader("密码", "本机验证后访问保险库") }
            item {
                AppCard {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Box(Modifier.size(62.dp).background(AppColors.BlueSoft, CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Password, contentDescription = null, tint = AppColors.Blue)
                        }
                        Text("需要验证本机密码或指纹", color = AppColors.Text, fontWeight = FontWeight.Bold)
                        Text("密码保险库只在验证通过后解密，离开页面或退到后台会立即锁定。", color = AppColors.Muted)
                        PrimaryButton("解锁密码管理", onUnlock)
                    }
                }
            }
        }
    }
}

@Composable
private fun PasswordVaultContent(
    modifier: Modifier,
    records: List<PasswordRecord>,
    query: String,
    onQuery: (String) -> Unit,
    editing: PasswordRecord?,
    selected: PasswordRecord?,
    onNew: () -> Unit,
    onEdit: (PasswordRecord) -> Unit,
    onSelect: (PasswordRecord) -> Unit,
    onCancelEdit: () -> Unit,
    onSave: (PasswordRecord) -> Unit,
    onDelete: (PasswordRecord) -> Unit,
    onCopyAccount: (String) -> Unit,
    onCopySecret: (String) -> Unit
) {
    AppPage(modifier = modifier) {
        item {
            ScreenHeader("密码", "账号与密钥保险库") {
                FilterPill("+ 新增", false, onNew)
            }
        }
        item { PillSearchField(query, onQuery, "搜索名称、账号、网站或应用") }
        if (editing != null) {
            item(key = "password-editor-${editing.id}") { PasswordEditor(editing, onSave, onCancelEdit) }
        }
        if (selected != null) {
            item(key = "password-detail-${selected.id}") {
                PasswordDetail(
                    record = selected,
                    onEdit = { onEdit(selected) },
                    onDelete = { onDelete(selected) },
                    onCopyAccount = { onCopyAccount(selected.account) },
                    onCopySecret = { onCopySecret(selected.secret) }
                )
            }
        }
        item { SectionTitle("全部密码") }
        if (records.isEmpty()) {
            item { EmptyState("暂无密码", "新增一个账号密码后会在这里显示", "🔐") }
        }
        items(records, key = { "password-${it.id}" }) { PasswordRow(it) { onSelect(it) } }
    }
}

@Composable
private fun PasswordEditor(record: PasswordRecord, onSave: (PasswordRecord) -> Unit, onCancel: () -> Unit) {
    var name by remember(record.id) { mutableStateOf(record.name) }
    var account by remember(record.id) { mutableStateOf(record.account) }
    var secret by remember(record.id) { mutableStateOf(record.secret) }
    var target by remember(record.id) { mutableStateOf(record.target) }
    var note by remember(record.id) { mutableStateOf(record.note) }
    var showSecret by remember(record.id) { mutableStateOf(false) }
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            FieldLabel("🔐", if (record.name.isBlank()) "新增密码" else "编辑密码")
            UnderlineField(name, { name = it }, "名称")
            UnderlineField(account, { account = it }, "账号")
            UnderlineField(
                secret,
                { secret = it },
                "密码",
                visualTransformation = if (showSecret) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showSecret = !showSecret }) {
                        Icon(if (showSecret) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = "显示或隐藏密码")
                    }
                }
            )
            UnderlineField(target, { target = it }, "网站或应用")
            UnderlineField(note, { note = it }, "备注")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PrimaryButton("保存", {
                    onSave(record.copy(name = name.trim(), account = account.trim(), secret = secret, target = target.trim(), note = note.trim()))
                }, modifier = Modifier.weight(1f), enabled = name.isNotBlank() && secret.isNotBlank())
                TextButton(onClick = onCancel) { Text("取消") }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PasswordDetail(record: PasswordRecord, onEdit: () -> Unit, onDelete: () -> Unit, onCopyAccount: () -> Unit, onCopySecret: () -> Unit) {
    var showSecret by remember(record.id) { mutableStateOf(false) }
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(record.name, color = AppColors.Text, fontWeight = FontWeight.Bold)
            Text("账号：${record.account.ifBlank { "未填写" }}", color = AppColors.Text)
            Text("网站/应用：${record.target.ifBlank { "未填写" }}", color = AppColors.Muted)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("密码：${if (showSecret) record.secret else "••••••••"}", color = AppColors.Text, modifier = Modifier.weight(1f))
                IconButton(onClick = { showSecret = !showSecret }) {
                    Icon(if (showSecret) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = "显示或隐藏密码")
                }
            }
            if (record.note.isNotBlank()) Text(record.note, color = AppColors.Muted)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterPill("复制账号", false, onCopyAccount)
                FilterPill("复制密码", false, onCopySecret)
                TextButton(onClick = onEdit) { Text("编辑") }
                TextButton(onClick = onDelete) { Text("删除", color = AppColors.Red) }
            }
        }
    }
}

@Composable
private fun PasswordRow(record: PasswordRecord, onClick: () -> Unit) {
    AppCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Password, contentDescription = null, tint = AppColors.Blue)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(record.name, color = AppColors.Text, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(record.account, color = AppColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (record.target.isNotBlank()) SoftChip(record.target)
        }
    }
}

@Suppress("DEPRECATION")
private fun requestPasswordVaultAuth(activity: FragmentActivity, onSuccess: () -> Unit, onError: (String) -> Unit) {
    val manager = BiometricManager.from(activity)
    val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        when (manager.canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> Unit
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                onError("请先在系统设置中录入指纹或设置锁屏密码")
                return
            }
            else -> {
                onError("当前设备不可用本机验证")
                return
            }
        }
    } else {
        val keyguard = activity.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        val hasBiometric = manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
        if (!hasBiometric && !keyguard.isDeviceSecure) {
            onError("请先在系统设置中录入指纹或设置锁屏密码")
            return
        }
    }
    val prompt = BiometricPrompt(
        activity,
        ContextCompat.getMainExecutor(activity),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onSuccess()
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) = onError(errString.toString().ifBlank { "验证已取消" })
            override fun onAuthenticationFailed() = onError("验证失败，请重试")
        }
    )
    val info = BiometricPrompt.PromptInfo.Builder()
        .setTitle("解锁密码管理")
        .setSubtitle("使用本机密码或指纹验证")
        .apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                setAllowedAuthenticators(authenticators)
            } else {
                setDeviceCredentialAllowed(true)
            }
        }
        .build()
    prompt.authenticate(info)
}

private fun copyToClipboard(context: Context, label: String, value: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, value))
}

private fun clearClipboardIfValueMatches(context: Context, value: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val current = clipboard.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString()
    if (current == value) clipboard.setPrimaryClip(ClipData.newPlainText("life-trio", ""))
}
