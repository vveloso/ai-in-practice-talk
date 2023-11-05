package com.example.gpt_2summarise

import android.content.Context
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.gpt_2summarise.databinding.ActivityGptMainBinding
import com.example.gpt_2summarise.process.SummariseModel
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGptMainBinding
    private lateinit var imm: InputMethodManager

    private val summariser = SummariseModel(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGptMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        binding.btnSummarise.isEnabled = false
        binding.btnSummarise.text = getString(R.string.initialising)
        lifecycleScope.launch {
            summariser.initialise()
            binding.btnSummarise.text = getString(R.string.summarise)
            binding.btnSummarise.isEnabled = true
            binding.btnSummarise.setOnClickListener { onSummarise() }
        }
    }

    private fun onSummarise() {
        val text = binding.editInput.text
        if (text.isNotEmpty()) {
            binding.btnSummarise.isEnabled = false
            binding.btnSummarise.text = getString(R.string.working)
            binding.summaryView.text = ""
            imm.hideSoftInputFromWindow(binding.editInput.windowToken, 0)
            lifecycleScope.launch {
                try {
                    val summary = summariser.getSummary(text.toString())
                    binding.summaryView.text = summary.trim()
                } finally {
                    binding.btnSummarise.text = getString(R.string.summarise)
                    binding.btnSummarise.isEnabled = true
                }
            }
        }
    }

    override fun onDestroy() {
        summariser.close()
        super.onDestroy()
    }
}