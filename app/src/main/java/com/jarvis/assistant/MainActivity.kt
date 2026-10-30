package com.jarvis.assistant

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jarvis.assistant.ai.*
import com.jarvis.assistant.databinding.ActivityMainBinding
import com.jarvis.assistant.memory.MemoryManager
import com.jarvis.assistant.services.JarvisAccessibilityService
import com.jarvis.assistant.services.JarvisForegroundService
import com.jarvis.assistant.ui.AvatarView
import com.jarvis.assistant.ui.MemoryAdapter
import com.jarvis.assistant.voice.VoiceRecognizer
import com.jarvis.assistant.voice.VoiceSpeaker
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), VoiceRecognizer.Listener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: SharedPreferences

    private lateinit var geminiClient: GeminiClient
    private lateinit var voiceSpeaker: VoiceSpeaker
    private lateinit var voiceRecognizer: VoiceRecognizer
    private lateinit var memoryManager: MemoryManager

    private lateinit var appLauncherTool: AppLauncherTool
    private lateinit var youtubeTool: YouTubeTool
    private lateinit var imageGenTool: ImageGenTool

    private val conversationHistory = mutableListOf<Pair<String, String>>()
    private val RECORD_AUDIO_PERMISSION_CODE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = getSharedPreferences("jarvis_prefs", Context.MODE_PRIVATE)

        initToolsAndEngines()
        setupListeners()
        checkPermissions()
        startBackgroundService()
    }

    private fun initToolsAndEngines() {
        val apiKey = prefs.getString("gemini_api_key", "") ?: ""
        val model = prefs.getString("gemini_model", "gemini-2.0-flash") ?: "gemini-2.0-flash"

        geminiClient = GeminiClient(apiKey, model)
        memoryManager = MemoryManager(this)

        appLauncherTool = AppLauncherTool(this)
        youtubeTool = YouTubeTool(this)
        imageGenTool = ImageGenTool(this)

        voiceSpeaker = VoiceSpeaker(this) { isSpeaking ->
            runOnUiThread {
                binding.visualizerView.setState(listening = false, speaking = isSpeaking)
                if (isSpeaking) {
                    binding.avatarView.setState(AvatarView.State.SPEAKING)
                    binding.tvStatus.text = getString(R.string.status_speaking)
                    binding.tvStatus.setTextColor(ContextCompat.getColor(this, R.color.status_speaking))
                } else {
                    binding.avatarView.setState(AvatarView.State.IDLE)
                    binding.tvStatus.text = getString(R.string.status_ready)
                    binding.tvStatus.setTextColor(ContextCompat.getColor(this, R.color.status_online))
                }
            }
        }

        voiceRecognizer = VoiceRecognizer(this, this)
    }

    private fun setupListeners() {
        binding.btnMic.setOnClickListener {
            if (voiceRecognizer.isCurrentlyListening()) {
                voiceRecognizer.stopListening()
                binding.avatarView.setState(AvatarView.State.IDLE)
                binding.tvStatus.text = getString(R.string.status_ready)
                binding.visualizerView.setState(listening = false, speaking = false)
            } else {
                startListening()
            }
        }

        binding.btnSend.setOnClickListener {
            val text = binding.etTextInput.text.toString().trim()
            if (text.isNotEmpty()) {
                binding.etTextInput.setText("")
                processUserPrompt(text)
            }
        }

        binding.etTextInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                binding.btnSend.performClick()
                true
            } else false
        }

        binding.btnSettings.setOnClickListener {
            showSettingsDialog()
        }

        binding.btnMemory.setOnClickListener {
            showMemoryDialog()
        }

        binding.btnAccessibility.setOnClickListener {
            if (!JarvisAccessibilityService.isServiceEnabled(this)) {
                Toast.makeText(this, "Enable Jarvis Accessibility to allow complete phone control", Toast.LENGTH_LONG).show()
                JarvisAccessibilityService.openAccessibilitySettings(this)
            } else {
                Toast.makeText(this, "Accessibility Service is already active! Jarvis has full phone control.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                RECORD_AUDIO_PERMISSION_CODE
            )
        }
    }

    private fun startListening() {
        voiceSpeaker.stop()
        binding.avatarView.setState(AvatarView.State.LISTENING)
        binding.tvStatus.text = getString(R.string.status_listening)
        binding.tvStatus.setTextColor(ContextCompat.getColor(this, R.color.status_listening))
        binding.visualizerView.setState(listening = true, speaking = false)
        voiceRecognizer.startListening()
    }

    override fun onReady() {
        binding.tvStatus.text = getString(R.string.status_listening)
    }

    override fun onRmsChanged(rmsdB: Float) {
        binding.visualizerView.setRms(rmsdB)
    }

    override fun onResults(text: String) {
        processUserPrompt(text)
    }

    override fun onError(errorMsg: String) {
        binding.avatarView.setState(AvatarView.State.IDLE)
        binding.tvStatus.text = getString(R.string.status_ready)
        binding.tvStatus.setTextColor(ContextCompat.getColor(this, R.color.status_online))
        binding.visualizerView.setState(listening = false, speaking = false)
        Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show()
    }

    private fun processUserPrompt(query: String) {
        binding.tvUserPrompt.text = "You: $query"
        binding.avatarView.setState(AvatarView.State.THINKING)
        binding.tvStatus.text = getString(R.string.status_thinking)
        binding.tvStatus.setTextColor(ContextCompat.getColor(this, R.color.status_thinking))
        binding.visualizerView.setState(listening = false, speaking = false)

        lifecycleScope.launch {
            // Auto extract and memorize facts/habits from user speech
            memoryManager.autoLearnFromText(query)

            // Inject stored long-term memory into prompt
            val memoryContext = memoryManager.buildMemoryContext()
            val systemPrompt = PromptTemplates.buildSystemPrompt(memoryContext)

            val response = geminiClient.sendUserMessage(
                userMessage = query,
                systemInstruction = systemPrompt,
                chatHistory = conversationHistory
            )

            handleGeminiResponse(query, response)
        }
    }

    private fun handleGeminiResponse(userQuery: String, response: GeminiResponse) {
        var finalSpokenText = response.text ?: ""

        val fnCall = response.functionCall
        if (fnCall != null) {
            when (fnCall.name) {
                "open_app" -> {
                    val appName = fnCall.args["app_name"] ?: ""
                    val result = appLauncherTool.openApp(appName)
                    if (finalSpokenText.isBlank()) finalSpokenText = result.message
                }
                "play_youtube" -> {
                    val ytQuery = fnCall.args["query"] ?: ""
                    val message = youtubeTool.playOrSearch(ytQuery)
                    if (finalSpokenText.isBlank()) finalSpokenText = message
                }
                "generate_image" -> {
                    val prompt = fnCall.args["prompt"] ?: ""
                    val message = imageGenTool.generateImage(prompt)
                    if (finalSpokenText.isBlank()) finalSpokenText = message
                }
                "remember_fact" -> {
                    val category = fnCall.args["category"] ?: "FACT"
                    val key = fnCall.args["key"] ?: "fact_${System.currentTimeMillis() % 1000}"
                    val value = fnCall.args["value"] ?: ""
                    lifecycleScope.launch {
                        memoryManager.saveFact(category, key, value)
                    }
                    if (finalSpokenText.isBlank()) {
                        finalSpokenText = "Got it Boss, I have permanently committed that to memory."
                    }
                }
                "control_phone" -> {
                    val action = fnCall.args["action"] ?: ""
                    val executed = JarvisAccessibilityService.executeAction(action)
                    if (finalSpokenText.isBlank()) {
                        finalSpokenText = if (executed) {
                            "Executed $action for you, Boss."
                        } else {
                            "Boss, please enable Accessibility Service in Settings so I can navigate the phone."
                        }
                    }
                }
            }
        }

        if (finalSpokenText.isBlank()) {
            finalSpokenText = "Command executed, Boss."
        }

        binding.tvJarvisResponse.text = "Jarvis: $finalSpokenText"
        voiceSpeaker.speak(finalSpokenText)

        conversationHistory.add("user" to userQuery)
        conversationHistory.add("model" to finalSpokenText)
    }

    private fun showSettingsDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_settings, null)
        val dialog = AlertDialog.Builder(this)
            .setView(view)
            .create()

        val etApiKey = view.findViewById<EditText>(R.id.etApiKey)
        val rgModel = view.findViewById<RadioGroup>(R.id.rgModel)
        val rb2 = view.findViewById<RadioButton>(R.id.rbGemini2Flash)
        val rb15 = view.findViewById<RadioButton>(R.id.rbGemini15Flash)
        val btnAccessibility = view.findViewById<Button>(R.id.btnEnableAccessibility)
        val btnSave = view.findViewById<Button>(R.id.btnSaveSettings)
        val btnCancel = view.findViewById<Button>(R.id.btnCancelSettings)

        etApiKey.setText(prefs.getString("gemini_api_key", ""))
        val curModel = prefs.getString("gemini_model", "gemini-2.0-flash")
        if (curModel == "gemini-1.5-flash") {
            rb15.isChecked = true
        } else {
            rb2.isChecked = true
        }

        btnAccessibility.setOnClickListener {
            JarvisAccessibilityService.openAccessibilitySettings(this)
        }

        btnSave.setOnClickListener {
            val key = etApiKey.text.toString().trim()
            val selectedModel = if (rb15.isChecked) "gemini-1.5-flash" else "gemini-2.0-flash"

            prefs.edit()
                .putString("gemini_api_key", key)
                .putString("gemini_model", selectedModel)
                .apply()

            geminiClient.updateApiKey(key)
            geminiClient.updateModel(selectedModel)

            Toast.makeText(this, "Settings saved successfully!", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        btnCancel.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun showMemoryDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_memory, null)
        val dialog = AlertDialog.Builder(this)
            .setView(view)
            .create()

        val rv = view.findViewById<RecyclerView>(R.id.rvMemories)
        val tvEmpty = view.findViewById<TextView>(R.id.tvEmptyMemory)
        val btnClear = view.findViewById<Button>(R.id.btnClearAllMemory)
        val btnClose = view.findViewById<Button>(R.id.btnCloseMemory)

        rv.layoutManager = LinearLayoutManager(this)
        lifecycleScope.launch {
            val memories = memoryManager.getAllMemories().toMutableList()
            if (memories.isEmpty()) {
                tvEmpty.visibility = View.VISIBLE
                rv.visibility = View.GONE
            } else {
                tvEmpty.visibility = View.GONE
                rv.visibility = View.VISIBLE
            }

            val adapter = MemoryAdapter(memories) { itemToDelete ->
                lifecycleScope.launch {
                    memoryManager.deleteMemory(itemToDelete.id)
                    memories.remove(itemToDelete)
                    rv.adapter?.notifyDataSetChanged()
                    if (memories.isEmpty()) {
                        tvEmpty.visibility = View.VISIBLE
                        rv.visibility = View.GONE
                    }
                }
            }
            rv.adapter = adapter
        }

        btnClear.setOnClickListener {
            lifecycleScope.launch {
                memoryManager.clearAll()
                rv.adapter = MemoryAdapter(emptyList()) {}
                tvEmpty.visibility = View.VISIBLE
                rv.visibility = View.GONE
                Toast.makeText(this@MainActivity, "Memory wiped clean.", Toast.LENGTH_SHORT).show()
            }
        }

        btnClose.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun startBackgroundService() {
        try {
            JarvisForegroundService.startService(this)
        } catch (e: Exception) {
            // Foreground service optional
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        voiceRecognizer.stopListening()
        voiceSpeaker.shutdown()
    }
}
