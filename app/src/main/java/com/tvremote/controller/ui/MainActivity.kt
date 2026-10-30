package com.tvremote.controller.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tvremote.controller.data.TvDevice
import com.tvremote.controller.protocol.KeyCodes
import com.tvremote.controller.protocol.PairingCode
import com.tvremote.controller.transport.TvWebSocketClient

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { HomeScreen() } }
    }
}

@Composable
fun HomeScreen() {
    var code by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Not connected") }
    var client by remember { mutableStateOf<TvWebSocketClient?>(null) }

    fun connect(ip: String, port: Int) {
        val ws = TvWebSocketClient("ws://$ip:$port")
        ws.listener = object : TvWebSocketClient.Listener {
            override fun onConnected() { status = "Connected to $ip:$port" }
            override fun onDisconnected(reason: String) { status = "Disconnected: $reason" }
            override fun onReply(json: String) { status = "TV: $json" }
        }
        client = ws
        ws.connect()
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("TV Remote Controller", style = MaterialTheme.typography.headlineSmall)
        Text(status, style = MaterialTheme.typography.bodyMedium)

        OutlinedTextField(
            value = code,
            onValueChange = { code = it.uppercase() },
            label = { Text("6-digit code shown on TV") },
            singleLine = true
        )

        Button(onClick = {
            val result = PairingCode.decode(code)
            if (result == null) status = "Invalid code"
            else { status = "Connecting to ${result.ip}:${result.port} ..."; connect(result.ip, result.port) }
        }) { Text("Pair & Connect") }

        Spacer(Modifier.height(16.dp))

        val keys = listOf("HOME" to KeyCodes.HOME, "BACK" to KeyCodes.BACK,
            "UP" to KeyCodes.DPAD_UP, "DOWN" to KeyCodes.DPAD_DOWN,
            "LEFT" to KeyCodes.DPAD_LEFT, "RIGHT" to KeyCodes.DPAD_RIGHT,
            "OK" to KeyCodes.DPAD_CENTER, "VOL+" to KeyCodes.VOLUME_UP,
            "VOL-" to KeyCodes.VOLUME_DOWN, "POWER" to KeyCodes.POWER)
        keys.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (label, keyCode) ->
                    Button(onClick = { client?.sendKey(keyCode) }) { Text(label) }
                }
            }
        }
    }
}
