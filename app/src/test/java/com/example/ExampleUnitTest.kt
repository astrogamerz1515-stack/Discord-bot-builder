package com.example

import com.example.ui.editor.intellisense.CompletionKind
import com.example.ui.editor.intellisense.IntelliSenseEngine
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun intelliSense_interactionDotCompletions() {
    val code = "async (interaction) => { interaction."
    val completions = IntelliSenseEngine.computeCompletions(
      code = code,
      cursorIndex = code.length,
      filePath = "index.js"
    )

    assertTrue("Expected interaction completions", completions.isNotEmpty())
    val labels = completions.map { it.label }
    assertTrue("Should suggest reply", labels.any { it.contains("reply") })
  }

  @Test
  fun intelliSense_embedBuilderCompletions() {
    val code = "const embed = new EmbedBuilder().set"
    val completions = IntelliSenseEngine.computeCompletions(
      code = code,
      cursorIndex = code.length,
      filePath = "index.js"
    )

    assertTrue("Expected embed setter completions", completions.isNotEmpty())
    val labels = completions.map { it.label }
    assertTrue("Should contain setTitle or setDescription", labels.any { it.contains("setTitle") })
  }

  @Test
  fun intelliSense_signatureHelpActiveParameter() {
    val code = "await interaction.reply({ content: 'hi' "
    val signature = IntelliSenseEngine.computeSignatureHelp(
      code = code,
      cursorIndex = code.length
    )

    assertNotNull("Should detect signature help inside reply()", signature)
    assertEquals("interaction.reply", signature?.functionName)
  }

  @Test
  fun intelliSense_applyCompletionReplacesTokenCleanly() {
    val initialCode = "await interaction.rep"
    val completions = IntelliSenseEngine.computeCompletions(
      code = initialCode,
      cursorIndex = initialCode.length,
      filePath = "index.js"
    )

    val replyItem = completions.firstOrNull { it.label.contains("reply") }
    assertNotNull("Reply completion item should exist", replyItem)

    val (updatedCode, newCursor) = IntelliSenseEngine.applyCompletion(
      currentCode = initialCode,
      cursorIndex = initialCode.length,
      item = replyItem!!
    )

    assertTrue("Code should now call reply", updatedCode.contains("reply"))
    assertTrue("Cursor should be past the inserted text", newCursor > initialCode.length - 3)
  }
}

