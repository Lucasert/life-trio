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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import com.lifetrio.ui.components.EditorSheet
import com.lifetrio.ui.components.EmptyState
import com.lifetrio.ui.components.PillSearchField
import com.lifetrio.ui.components.PrimaryButton
import com.lifetrio.ui.components.ScreenHeader
import com.lifetrio.ui.components.SectionTitle
import com.lifetrio.ui.components.SoftChip
import com.lifetrio.ui.components.UnderlineField
import com.lifetrio.ui.theme.Spacing
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
    var showEditor by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner, repository) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                repository.lock()
                editing = null
                selected = null
                showEditor = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            repository.lock()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (isUnlocked) {
                FloatingActionButton(onClick = {
                    selected = null
                    editing = PasswordRecord(name = "", account = "", secret = "", target = "")
                    showEditor = true
                }) {
                    Icon(Icons.Default.Add, contentDescription = "新增密码")
                }
            }
        }
    ) { padding ->
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
                selected = selected,
                onSelect = {
                    editing = null
                    showEditor = false
                    selected = it
                },
                onEdit = {
                    selected = null
                    editing = it
                    showEditor = true
                },
                onDelete = {
                    scope.launch {
                        repository.delete(it.id)
                        selected = null
                        editing = null
                        showEditor = false
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

    val editingRecord = editing
    if (showEditor && editingRecord != null) {
        PasswordEditorSheet(
            record = editingRecord,
            onDismiss = {
                showEditor = false
                editing = null
            },
            onSave = {
                scope.launch {
                    repository.save(it)
                    showEditor = false
                    editing = null
                    snackbarHostState.showSnackbar("已保存")
                }
            }
        )
    }
}

@Composable
private fun LockedPasswordScreen(modifier: Modifier = Modifier, onUnlock: () -> Unit) {
    Box(modifier = modifier.background(MaterialTheme.colorScheme.background)) {
        AppPage {
            item { ScreenHeader("密码", "本机验证后访问保险库") }
            item {
                AppCard(hero = true) {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                        Box(Modifier.size(62.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Key, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Text("需要验证本机密码或指纹", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                        Text("密码保险库只在验证通过后解密，离开页面或退到后台会立即锁定。", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    selected: PasswordRecord?,
    onSelect: (PasswordRecord) -> Unit,
    onEdit: (PasswordRecord) -> Unit,
    onDelete: (PasswordRecord) -> Unit,
    onCopyAccount: (String) -> Unit,
    onCopySecret: (String) -> Unit
) {
    AppPage(modifier = modifier) {
        item { ScreenHeader("密码", "账号与密钥保险库") }
        item { PillSearchField(query, onQuery, "搜索名称、账号、网站或应用") }
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
            item { EmptyState("暂无密码", "点击右下角按钮新增账号密码", Icons.Outlined.Key) }
        }
        items(records, key = { "password-${it.id}" }) { PasswordRow(it) { onSelect(it) } }
    }
}

@Composable
private fun PasswordEditorSheet(record: PasswordRecord, onDismiss: () -> Unit, onSave: (PasswordRecord) -> Unit) {
    var name by remember(record.id) { mutableStateOf(record.name) }
    var account by remember(record.id) { mutableStateOf(record.account) }
    var secret by remember(record.id) { mutableStateOf(record.secret) }
    var target by remember(record.id) { mutableStateOf(record.target) }
    var note by remember(record.id) { mutableStateOf(record.note) }
    var showSecret by remember(record.id) { mutableStateOf(false) }
    EditorSheet(title = if (record.name.isBlank()) "新增密码" else "编辑密码", onDismiss = onDismiss) {
        UnderlineField(name, { name = it }, "名称")
        UnderlineField(account, { account = it }, "账号")
        UnderlineField(
            secret,
            { secret = it },
            "密码",
            visualTransformation = if (showSecret) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { showSecret = !showSecret }) {
                    Icon(if (showSecret) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, contentDescription = "显示或隐藏密码")
                }
            }
        )
        UnderlineField(target, { target = it }, "网站或应用")
        UnderlineField(note, { note = it }, "备注")
        PrimaryButton("保存", {
            onSave(record.copy(name = name.trim(), account = account.trim(), secret = secret, target = target.trim(), note = note.trim()))
        }, enabled = name.isNotBlank() && secret.isNotBlank())
    }
}

@Composable
private fun PasswordDetail(record: PasswordRecord, onEdit: () -> Unit, onDelete: () -> Unit, onCopyAccount: () -> Unit, onCopySecret: () -> Unit) {
    var showSecret by remember(record.id) { mutableStateOf(false) }
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(record.name, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            Text("账号：${record.account.ifBlank { "未填写" }}", color = MaterialTheme.colorScheme.onSurface)
            Text("网站/应用：${record.target.ifBlank { "未填写" }}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("密码：${if (showSecret) record.secret else "••••••••"}", color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                IconButton(onClick = { showSecret = !showSecret }) {
                    Icon(if (showSecret) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, contentDescription = "显示或隐藏密码")
                }
            }
            if (record.note.isNotBlank()) Text(record.note, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                FilledTonalButton(onClick = onCopyAccount) {
                    Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(Spacing.xs))
                    Text("账号")
                }
                Spacer(Modifier.width(Spacing.xs))
                FilledTonalButton(onClick = onCopySecret) {
                    Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(Spacing.xs))
                    Text("密码")
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onEdit) { Icon(Icons.Outlined.Edit, contentDescription = "编辑") }
                IconButton(onClick = onDelete) { Icon(Icons.Outlined.DeleteOutline, contentDescription = "删除", tint = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@Composable
private fun PasswordRow(record: PasswordRecord, onClick: () -> Unit) {
    AppCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(Spacing.sm))
            Column(Modifier.weight(1f)) {
                Text(record.name, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(record.account, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
