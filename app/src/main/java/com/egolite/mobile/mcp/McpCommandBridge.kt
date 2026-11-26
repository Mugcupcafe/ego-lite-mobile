package com.egolite.mobile.mcp

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

enum class CommandTransport(val label: String) {
    QUEUE("명령 큐"),
    SAMPLING("MCP Sampling"),
    BUILT_IN_API("내장 API")
}

data class BrowserAiCommand(
    val id: String = "cmd-" + UUID.randomUUID().toString().take(8),
    val targetClient: String,
    val spaceId: String,
    val spaceName: String,
    val tabId: String,
    val tabTitle: String,
    val tabUrl: String,
    val isBackgroundTab: Boolean,
    val prompt: String,
    val transport: CommandTransport = CommandTransport.QUEUE,
    val status: String = "대기 중",
    val createdAtMs: Long = System.currentTimeMillis()
) {
    fun toPayloadJson(): JsonObject = buildJsonObject {
        put("commandId", JsonPrimitive(id))
        put("transport", JsonPrimitive(transport.name.lowercase()))
        put("targetClient", JsonPrimitive(targetClient))
        put("command", JsonPrimitive(prompt))
        put("target", buildJsonObject {
            put("spaceId", JsonPrimitive(spaceId))
            put("spaceName", JsonPrimitive(spaceName))
            put("tabId", JsonPrimitive(tabId))
            put("tabTitle", JsonPrimitive(tabTitle))
            put("tabUrl", JsonPrimitive(tabUrl))
            put("isBackgroundTab", JsonPrimitive(isBackgroundTab))
        })
        put(
            "instruction",
            JsonPrimitive(
                if (isBackgroundTab) {
                    "현재 활성 탭을 방해하지 않도록 target.spaceId($spaceId)의 백그라운드 탭 target.tabId($tabId)를 기준으로 페이지를 확인하고 명령을 수행하세요."
                } else {
                    "target.spaceId($spaceId)의 활성 탭 target.tabId($tabId)에서 명령을 수행하세요."
                }
            )
        )
    }
}

/**
 * 멀티 MCP 세션별 명령 큐 & Sampling / 내장 API 브리지
 * - 여러 MCP 클라이언트(Claude, Gemini 등)가 동시 연결되어도 클라이언트 이름별 큐로 완전 격리
 * - 한 에이전트가 여러 탭을 운영 중일 때도 spaceId + tabId + tabUrl + isBackgroundTab을 자동 포장
 */
object McpCommandBridge {
    private val queuesByClient = ConcurrentHashMap<String, MutableList<BrowserAiCommand>>()
    private val samplingSupportByClient = ConcurrentHashMap<String, Boolean>()

    private val _connectedClients = MutableStateFlow<List<String>>(emptyList())
    val connectedClients: StateFlow<List<String>> = _connectedClients.asStateFlow()

    private val _recentCommands = MutableStateFlow<List<BrowserAiCommand>>(emptyList())
    val recentCommands: StateFlow<List<BrowserAiCommand>> = _recentCommands.asStateFlow()

    private val _pendingCount = MutableStateFlow(0)
    val pendingCount: StateFlow<Int> = _pendingCount.asStateFlow()

    private var lastActiveClientName: String = "Gemini"

    fun normalizeClientName(raw: String?): String {
        val cleaned = raw?.trim().orEmpty()
        if (cleaned.isBlank()) return lastActiveClientName
        val low = cleaned.lowercase()
        return when {
            "claude" in low -> "Claude"
            "gemini" in low -> "Gemini"
            "muse" in low -> "Muse"
            "openai" in low || "chatgpt" in low -> "OpenAI"
            "cursor" in low -> "Cursor"
            else -> cleaned.take(24)
        }
    }

    fun registerClient(rawClientName: String?, supportsSampling: Boolean) {
        val name = normalizeClientName(rawClientName)
        lastActiveClientName = name
        samplingSupportByClient[name] = supportsSampling
        _connectedClients.update { list ->
            if (name in list) list else list + name
        }
    }

    fun supportsSampling(clientName: String): Boolean {
        return samplingSupportByClient[normalizeClientName(clientName)] == true
    }

    fun enqueueCommand(cmd: BrowserAiCommand): String {
        val clientKey = normalizeClientName(cmd.targetClient)
        val normalized = cmd.copy(targetClient = clientKey)

        if (normalized.transport == CommandTransport.SAMPLING && !supportsSampling(clientKey)) {
            val fallback = normalized.copy(
                transport = CommandTransport.QUEUE,
                status = "큐 전환 대기"
            )
            synchronized(queuesByClient) {
                queuesByClient.getOrPut(clientKey) { mutableListOf() }.add(fallback)
                recomputePendingCountLocked()
            }
            pushHistory(fallback)
            return "$clientKey 클라이언트가 Sampling을 미지원해 전용 명령 큐에 적재했습니다."
        }

        if (normalized.transport == CommandTransport.BUILT_IN_API) {
            val apiRecord = normalized.copy(status = "API 요청 준비")
            pushHistory(apiRecord)
            return "내장 API 직접 실행 모드로 '${normalized.tabTitle}' 지시를 기록했습니다. (설정에서 API 키 연동 가능)"
        }

        synchronized(queuesByClient) {
            queuesByClient.getOrPut(clientKey) { mutableListOf() }.add(normalized)
            recomputePendingCountLocked()
        }
        pushHistory(normalized)
        return "$clientKey 전용 큐에 전달됨 (${normalized.tabTitle})"
    }

    fun drainCommandsForClient(rawClientName: String?): List<BrowserAiCommand> {
        val key = normalizeClientName(rawClientName)
        val drained = synchronized(queuesByClient) {
            val list = queuesByClient[key] ?: return emptyList()
            if (list.isEmpty()) return emptyList()
            val copy = list.toList()
            list.clear()
            recomputePendingCountLocked()
            copy
        }
        if (drained.isNotEmpty()) {
            val drainedIds = drained.map { it.id }.toSet()
            _recentCommands.update { hist ->
                hist.map { if (it.id in drainedIds) it.copy(status = "AI 전달됨") else it }
            }
        }
        return drained
    }

    suspend fun waitForCommand(rawClientName: String?, timeoutSeconds: Int): List<BrowserAiCommand> {
        val clampedSec = timeoutSeconds.coerceIn(1, 25)
        val deadline = System.currentTimeMillis() + clampedSec * 1000L
        while (System.currentTimeMillis() < deadline) {
            val ready = drainCommandsForClient(rawClientName)
            if (ready.isNotEmpty()) return ready
            delay(250L)
        }
        return emptyList()
    }

    fun formatCommandsPayload(commands: List<BrowserAiCommand>): String {
        val obj = buildJsonObject {
            put("ok", JsonPrimitive(true))
            put("count", JsonPrimitive(commands.size))
            put("commands", buildJsonArray {
                commands.forEach { add(it.toPayloadJson()) }
            })
        }
        return obj.toString()
    }

    fun buildInlineNoticeIfAny(rawClientName: String?): String? {
        val drained = drainCommandsForClient(rawClientName)
        if (drained.isEmpty()) return null
        return "\n\n[사용자 새 브라우저 지시 (${drained.size}건)]\n${formatCommandsPayload(drained)}"
    }

    private fun pushHistory(cmd: BrowserAiCommand) {
        _recentCommands.update { hist ->
            (listOf(cmd) + hist).take(12)
        }
    }

    private fun recomputePendingCountLocked() {
        _pendingCount.value = queuesByClient.values.sumOf { it.size }
    }
}
