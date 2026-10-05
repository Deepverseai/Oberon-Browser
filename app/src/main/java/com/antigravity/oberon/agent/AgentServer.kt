package com.antigravity.oberon.agent

import fi.iki.elonen.NanoHTTPD

class AgentServer(
    private val protocol: AutomationProtocol,
    private val requiredToken: String = "oberon_local_secret_token"
) : NanoHTTPD("127.0.0.1", 8765) {

    companion object {
        const val PORT = 8765
        const val HOST = "127.0.0.1"
    }

    override fun serve(session: IHTTPSession): Response {
        // Enforce Localhost-only verification
        val remoteHost = session.remoteIpAddress
        if (remoteHost != "127.0.0.1" && remoteHost != "localhost") {
            return newFixedLengthResponse(
                Response.Status.FORBIDDEN,
                "text/plain",
                "Forbidden: Agent Server binds strictly to 127.0.0.1"
            )
        }

        // Enforce Security Token Header
        val headers = session.headers
        val token = headers["x-agent-token"]
        if (token != requiredToken) {
            return newFixedLengthResponse(
                Response.Status.UNAUTHORIZED,
                "application/json",
                "{\"status\":\"error\",\"message\":\"Unauthorized: Invalid X-Agent-Token\"}"
            )
        }

        if (session.method == Method.POST) {
            val map = HashMap<String, String>()
            try {
                session.parseBody(map)
                val postData = map["postData"] ?: ""
                val responseJson = protocol.handleCommand(postData)
                return newFixedLengthResponse(
                    Response.Status.OK,
                    "application/json",
                    responseJson
                )
            } catch (e: Exception) {
                return newFixedLengthResponse(
                    Response.Status.INTERNAL_ERROR,
                    "application/json",
                    "{\"status\":\"error\",\"message\":\"${e.message}\"}"
                )
            }
        }

        // GET ping endpoint
        return newFixedLengthResponse(
            Response.Status.OK,
            "application/json",
            "{\"status\":\"ok\",\"service\":\"Oberon Agent Bridge\",\"version\":\"1.0.0\"}"
        )
    }
}
