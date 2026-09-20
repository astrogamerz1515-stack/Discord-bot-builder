package com.example.ui.deploy

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.BotStudioViewModel
import com.example.ui.theme.DiscordBackground
import com.example.ui.theme.DiscordBlurple
import com.example.ui.theme.DiscordDarker
import com.example.ui.theme.DiscordElevated
import com.example.ui.theme.DiscordGreen
import com.example.ui.theme.DiscordHover
import com.example.ui.theme.DiscordSurface
import com.example.ui.theme.DiscordTextMuted
import com.example.ui.theme.DiscordTextPrimary
import com.example.ui.theme.DiscordTextSecondary
import com.example.ui.theme.DiscordYellow

@Composable
fun DeploymentScreen(viewModel: BotStudioViewModel) {
    val context = LocalContext.current
    val project by viewModel.currentProject.collectAsState()
    val files by viewModel.projectFiles.collectAsState()

    val lang = project?.language ?: "JavaScript"

    val dockerfileContent = remember(lang) {
        when {
            lang.contains("Python", ignoreCase = true) -> """
FROM python:3.11-slim
WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY . .
CMD ["python", "bot.py"]
""".trimIndent()

            lang.contains("Rust", ignoreCase = true) -> """
FROM rust:1.76 as builder
WORKDIR /app
COPY . .
RUN cargo build --release

FROM debian:bookworm-slim
COPY --from=builder /app/target/release/bot /usr/local/bin/bot
CMD ["bot"]
""".trimIndent()

            lang.contains("Go", ignoreCase = true) -> """
FROM golang:1.22-alpine as builder
WORKDIR /app
COPY . .
RUN go build -o bot .

FROM alpine:latest
COPY --from=builder /app/bot /usr/local/bin/bot
CMD ["bot"]
""".trimIndent()

            else -> """
FROM node:20-alpine
WORKDIR /app
COPY package*.json ./
RUN npm ci --only=production
COPY . .
CMD ["node", "index.js"]
""".trimIndent()
        }
    }

    val procfileContent = remember(lang) {
        when {
            lang.contains("Python", ignoreCase = true) -> "worker: python bot.py"
            lang.contains("Rust", ignoreCase = true) -> "worker: ./target/release/bot"
            lang.contains("Go", ignoreCase = true) -> "worker: ./bot"
            else -> "worker: node index.js"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DiscordBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Column {
            Text(
                text = "Deploy & Cloud Hosting",
                color = DiscordTextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Host your bot 24/7 on Railway, Render, Fly.io or VPS",
                color = DiscordTextSecondary,
                fontSize = 12.sp
            )
        }

        // Export project summary
        Card(
            colors = CardDefaults.cardColors(containerColor = DiscordSurface),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Project Summary", color = DiscordTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("• Name: ${project?.name}", color = DiscordTextSecondary, fontSize = 13.sp)
                Text("• Language: ${project?.language}", color = DiscordTextSecondary, fontSize = 13.sp)
                Text("• Files: ${files.size} source files", color = DiscordTextSecondary, fontSize = 13.sp)

                Button(
                    onClick = {
                        val fullCode = files.joinToString("\n\n" + "=".repeat(40) + "\n\n") {
                            "// FILE: ${it.filePath}\n${it.content}"
                        }
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Discord Bot Project", fullCode))
                        Toast.makeText(context, "Full project code bundle copied to clipboard!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DiscordBlurple),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy Full Project Code Bundle", fontSize = 13.sp)
                }
            }
        }

        // Generated Dockerfile
        Card(
            colors = CardDefaults.cardColors(containerColor = DiscordSurface),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Dockerfile (for Docker / Railway / Fly.io)", color = DiscordTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Dockerfile", dockerfileContent))
                            Toast.makeText(context, "Dockerfile copied to clipboard!", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = DiscordTextSecondary, modifier = Modifier.size(16.dp))
                    }
                }

                Surface(
                    color = DiscordDarker,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = dockerfileContent,
                        color = DiscordTextPrimary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }

        // Generated Procfile
        Card(
            colors = CardDefaults.cardColors(containerColor = DiscordSurface),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Procfile (for Heroku / Render Worker)", color = DiscordTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Procfile", procfileContent))
                            Toast.makeText(context, "Procfile copied to clipboard!", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = DiscordTextSecondary, modifier = Modifier.size(16.dp))
                    }
                }

                Surface(
                    color = DiscordDarker,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = procfileContent,
                        color = DiscordTextPrimary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }

        // 24/7 Hosting Guide
        Card(
            colors = CardDefaults.cardColors(containerColor = DiscordSurface),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Recommended 24/7 Cloud Hosts", color = DiscordTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("1. Railway.app - Deploy via GitHub with Dockerfile or Nixpacks. Add DISCORD_TOKEN in variables.", color = DiscordTextSecondary, fontSize = 12.sp)
                Text("2. Render.com - Set up a Background Worker service. Worker type does not sleep unlike Web service.", color = DiscordTextSecondary, fontSize = 12.sp)
                Text("3. Fly.io - Ultra low latency edge hosting. Deploy via 'fly launch'.", color = DiscordTextSecondary, fontSize = 12.sp)
                Text("4. Self-hosted VPS - Ubuntu server running PM2: 'pm2 start index.js --name bot'", color = DiscordTextSecondary, fontSize = 12.sp)
            }
        }
    }
}
