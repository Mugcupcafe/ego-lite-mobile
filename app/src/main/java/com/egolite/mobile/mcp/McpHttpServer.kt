package com.egolite.mobile.mcp

import com.egolite.mobile.browser.BrowserScripts
import com.egolite.mobile.browser.SpaceManager
import com.egolite.mobile.model.AiSpaceStatus
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.request.header
import io.ktor.server.request.receiveParameters
import io.ktor.server.request.receiveText
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

class McpHttpServer(
    private val port: Int,
    private val onLog: (String) -> Unit
) {
    private var server: ApplicationEngine? = null
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }

    fun start() {
        stop()
        server = embeddedServer(CIO, host = "127.0.0.1", port = port) {
            install(ContentNegotiation) {
                json(json)
            }
            install(CORS) {
                anyHost()
                allowHeader(HttpHeaders.Authorization)
                allowHeader(HttpHeaders.ContentType)
                allowHeader("Mcp-Session-Id")
                allowMethod(HttpMethod.Get)
                allowMethod(HttpMethod.Post)
                allowMethod(HttpMethod.Delete)
                allowMethod(HttpMethod.Options)
            }

            routing {
                get("/health") {
                    call.respondText("ok", ContentType.Text.Plain)
                }

                // OAuth 2.1 Discovery Endpoints (Gemini Web/App)
                get("/.well-known/oauth-protected-resource") {
                    call.respond(OAuth21Manager.protectedResourceMetadata())
                }
                get("/.well-known/oauth-protected-resource/mcp") {
                    call.respond(OAuth21Manager.protectedResourceMetadata())
                }
                get("/.well-known/oauth-authorization-server") {
                    call.respond(OAuth21Manager.authorizationServerMetadata())
                }

                post("/oauth/register") {
                    val bodyText = call.receiveText()
                    val obj = runCatching { json.parseToJsonElement(bodyText).jsonObject }.getOrNull()
                    val clientName = obj?.get("client_name")?.jsonPrimitive?.contentOrNull.orEmpty()
                    val redirectUri = obj?.get("redirect_uris")?.jsonArray?.firstOrNull()?.jsonPrimitive?.contentOrNull.orEmpty()
                    val reg = OAuth21Manager.registerClient(clientName, redirectUri)
                    onLog("OAuth 2.1 클라이언트 등록: $clientName")
                    call.respond(HttpStatusCode.Created, reg)
                }

                get("/oauth/authorize") {
                    val clientId = call.request.queryParameters["client_id"].orEmpty()
                    val redirectUri = call.request.queryParameters["redirect_uri"].orEmpty()
                    val state = call.request.queryParameters["state"].orEmpty()
                    val challenge = call.request.queryParameters["code_challenge"].orEmpty()
                    val method = call.request.queryParameters["code_challenge_method"] ?: "S256"

                    val code = OAuth21Manager.issueAuthCode(clientId, redirectUri, challenge, method)
                    val sep = if (redirectUri.contains("?")) "&" else "?"
                    val target = buildString {
                        append(redirectUri)
                        append(sep)
                        append("code=").append(code)
                        if (state.isNotBlank()) append("&state=").append(state)
                    }
                    onLog("OAuth 2.1 인가 코드 발급 ($clientId)")
                    call.respondRedirect(target)
                }

                post("/oauth/token") {
                    val params = call.receiveParameters()
                    val code = params["code"].orEmpty()
                    val clientId = params["client_id"].orEmpty()
                    val verifier = params["code_verifier"]
                    val tokenJson = OAuth21Manager.exchangeCodeForToken(code, clientId, verifier)
                    if (tokenJson == null) {
                        call.respond(HttpStatusCode.BadRequest, buildJsonObject { put("error", "invalid_grant") })
                    } else {
                        onLog("OAuth 2.1 액세스 토큰 발급 완료")
                        call.respond(tokenJson)
                    }
                }

                // Streamable HTTP MCP Endpoint (POST /mcp)
                post("/mcp") {
                    val authHeader = call.request.header(HttpHeaders.Authorization)
                    if (!OAuth21Manager.validateAuthorizationHeader(authHeader)) {
                        val metaUrl = "${OAuth21Manager.publicBaseUrl.trimEnd('/')}/.well-known/oauth-protected-resource"
                        call.response.header(
                            HttpHeaders.WWWAuthenticate,
                            "Bearer resource_metadata=\"$metaUrl\""
                        )
                        call.respond(HttpStatusCode.Unauthorized, buildJsonObject {
                            put("error", "unauthorized")
                        })
                        return@post
                    }

                    val rawBody = call.receiveText()
                    val req = runCatching { json.parseToJsonElement(rawBody).jsonObject }.getOrNull()
                    if (req == null) {
                        call.respond(HttpStatusCode.BadRequest, rpcError(null, -32700, "Invalid JSON"))
                        return@post
                    }

                    val id = req["id"]
                    val method = req["method"]?.jsonPrimitive?.contentOrNull.orEmpty()
                    val params = req["params"]?.jsonObject

                    if (method.startsWith("notifications/")) {
                        call.respond(HttpStatusCode.Accepted)
                        return@post
                    }

                    val responseObj = when (method) {
                        "initialize" -> rpcResult(id, buildJsonObject {
                            put("protocolVersion", "2025-03-26")
                            put("capabilities", buildJsonObject {
                                put("tools", buildJsonObject { put("listChanged", false) })
                            })
                            put("serverInfo", buildJsonObject {
                                put("name", "ego-lite-mobile")
                                put("version", "0.2.2")
                            })
                        })
                        "ping" -> rpcResult(id, buildJsonObject {})
                        "tools/list" -> rpcResult(id, buildToolsList())
                        "tools/call" -> handleToolCall(id, params)
                        else -> rpcError(id, -32601, "Method not found: $method")
                    }

                    call.respond(HttpStatusCode.OK, responseObj)
                }
            }
        }.start(wait = false)
        onLog("Streamable HTTP MCP 서버 시작 (127.0.0.1:$port/mcp)")
    }

    fun stop() {
        server?.stop(500, 1000)
        server = null
    }

    private fun buildToolsList(): JsonObject = buildJsonObject {
        put("tools", buildJsonArray {
            // Tool 1: Code Mode JS Batch Execution
            add(buildJsonObject {
                put("name", "browser_exec")
                put("description", "Code Mode: Execute a batched JavaScript snippet inside an isolated AI Space WebView (with helpers: clickRef, fillRef, scrollByY). Optionally navigate to url first.")
                put("inputSchema", buildJsonObject {
                    put("type", "object")
                    put("properties", buildJsonObject {
                        put("space_id", buildJsonObject { put("type", "string"); put("description", "Optional AI Space ID") })
                        put("url", buildJsonObject { put("type", "string"); put("description", "Optional URL to navigate to before executing script") })
                        put("script", buildJsonObject { put("type", "string"); put("description", "JavaScript body returning JSON-serializable result") })
                        put("timeout_ms", buildJsonObject { put("type", "integer"); put("description", "Execution timeout in ms (default 15000)") })
                    })
                })
            })
            // Tool 2: Observe (Snapshot / Firecrawl-style Markdown / Screenshot)
            add(buildJsonObject {
                put("name", "browser_observe")
                put("description", "Inspect the active AI Space page. Use mode='markdown' for token-efficient reading, 'snapshot' for interactive @N refs, or 'screenshot' for visual capture.")
                put("inputSchema", buildJsonObject {
                    put("type", "object")
                    put("properties", buildJsonObject {
                        put("space_id", buildJsonObject { put("type", "string") })
                        put("url", buildJsonObject { put("type", "string"); put("description", "Optional URL to load before observing") })
                        put("mode", buildJsonObject {
                            put("type", "string")
                            put("enum", buildJsonArray {
                                add(JsonPrimitive("snapshot"))
                                add(JsonPrimitive("markdown"))
                                add(JsonPrimitive("screenshot"))
                            })
                        })
                    })
                    put("required", buildJsonArray { add(JsonPrimitive("mode")) })
                })
            })
            // Tool 3: last30days Lightweight Research
            add(buildJsonObject {
                put("name", "research_last30days")
                put("description", "Gather pre-filtered last-30-days community discussions and engagement signals (Reddit, Hacker News, Polymarket, Web, optional logged-in X WebView).")
                put("inputSchema", buildJsonObject {
                    put("type", "object")
                    put("properties", buildJsonObject {
                        put("topic", buildJsonObject { put("type", "string") })
                        put("mode", buildJsonObject {
                            put("type", "string")
                            put("enum", buildJsonArray {
                                add(JsonPrimitive("quick"))
                                add(JsonPrimitive("deep"))
                            })
                        })
                        put("sources", buildJsonObject {
                            put("type", "array")
                            put("items", buildJsonObject { put("type", "string") })
                        })
                    })
                    put("required", buildJsonArray { add(JsonPrimitive("topic")) })
                })
            })
        })
    }

    private suspend fun handleToolCall(id: JsonElement?, params: JsonObject?): JsonObject {
        val toolName = params?.get("name")?.jsonPrimitive?.contentOrNull.orEmpty()
        val args = params?.get("arguments")?.jsonObject ?: buildJsonObject {}
        onLog("MCP 툴 호출: $toolName")

        return try {
            when (toolName) {
                "browser_exec" -> {
                    val space = SpaceManager.resolveAiSpace(args["space_id"]?.jsonPrimitive?.contentOrNull)
                    if (space.aiStatus == AiSpaceStatus.TAKEOVER_PAUSED) {
                        return toolTextResult(id, """{"ok":false,"error":"User has taken over this AI Space. Ask user to resume."}""")
                    }
                    SpaceManager.setAiStatus(space.id, AiSpaceStatus.WORKING, "browser_exec 실행 중")
                    try {
                        val url = args["url"]?.jsonPrimitive?.contentOrNull
                        if (!url.isNullOrBlank()) {
                            SpaceManager.navigateAndWait(space.id, url)
                        }
                        val script = args["script"]?.jsonPrimitive?.contentOrNull.orEmpty()
                        val timeout = args["timeout_ms"]?.jsonPrimitive?.longOrNull ?: 15000L
                        val out = if (script.isNotBlank()) {
                            SpaceManager.evaluateJsInSpace(space.id, BrowserScripts.wrapCodeModeScript(script), timeout)
                        } else {
                            """{"ok":true,"url":"${space.activeTab.url}"}"""
                        }
                        SpaceManager.setAiStatus(space.id, AiSpaceStatus.DONE, "실행 완료")
                        toolTextResult(id, out)
                    } catch (e: Exception) {
                        SpaceManager.setAiStatus(space.id, AiSpaceStatus.IDLE, "오류: ${e.message}")
                        toolTextResult(id, """{"ok":false,"error":"${e.message}"}""")
                    }
                }

                "browser_observe" -> {
                    val space = SpaceManager.resolveAiSpace(args["space_id"]?.jsonPrimitive?.contentOrNull)
                    val mode = args["mode"]?.jsonPrimitive?.contentOrNull ?: "markdown"
                    SpaceManager.setAiStatus(space.id, AiSpaceStatus.WORKING, "화면 분석($mode)")
                    try {
                        val url = args["url"]?.jsonPrimitive?.contentOrNull
                        if (!url.isNullOrBlank()) {
                            SpaceManager.navigateAndWait(space.id, url)
                        }
                        val result = when (mode) {
                            "snapshot" -> toolTextResult(id, SpaceManager.evaluateJsInSpace(space.id, BrowserScripts.SNAPSHOT_JS))
                            "markdown" -> toolTextResult(id, SpaceManager.evaluateJsInSpace(space.id, BrowserScripts.MARKDOWN_EXTRACT_JS))
                            "screenshot" -> {
                                val b64 = SpaceManager.captureScreenshotInSpace(space.id)
                                rpcResult(id, buildJsonObject {
                                    put("content", buildJsonArray {
                                        add(buildJsonObject {
                                            put("type", "image")
                                            put("data", b64)
                                            put("mimeType", "image/jpeg")
                                        })
                                    })
                                })
                            }
                            else -> toolTextResult(id, """{"ok":false,"error":"Unknown mode: $mode"}""")
                        }
                        SpaceManager.setAiStatus(space.id, AiSpaceStatus.DONE, "관찰 완료")
                        result
                    } catch (e: Exception) {
                        SpaceManager.setAiStatus(space.id, AiSpaceStatus.IDLE, "오류: ${e.message}")
                        toolTextResult(id, """{"ok":false,"error":"${e.message}"}""")
                    }
                }

                "research_last30days" -> {
                    val topic = args["topic"]?.jsonPrimitive?.contentOrNull.orEmpty()
                    val mode = args["mode"]?.jsonPrimitive?.contentOrNull ?: "quick"
                    val sources = args["sources"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList()
                    val data = Last30DaysEngine.research(topic, mode, sources)
                    toolTextResult(id, data.toString())
                }

                else -> rpcError(id, -32602, "Unknown tool: $toolName")
            }
        } catch (e: Exception) {
            rpcError(id, -32000, e.message ?: "Internal server error")
        }
    }

    private fun toolTextResult(id: JsonElement?, text: String): JsonObject = rpcResult(id, buildJsonObject {
        put("content", buildJsonArray {
            add(buildJsonObject {
                put("type", "text")
                put("text", text)
            })
        })
    })

    private fun rpcResult(id: JsonElement?, result: JsonObject): JsonObject = buildJsonObject {
        put("jsonrpc", "2.0")
        if (id != null) put("id", id)
        put("result", result)
    }

    private fun rpcError(id: JsonElement?, code: Int, message: String): JsonObject = buildJsonObject {
        put("jsonrpc", "2.0")
        if (id != null) put("id", id)
        put("error", buildJsonObject {
            put("code", code)
            put("message", message)
        })
    }
}
