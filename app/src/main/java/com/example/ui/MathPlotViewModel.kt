package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.GeminiMathService
import com.example.math.GraphPreset
import com.example.math.PredefinedFunctions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MathUiState(
    // Formulas
    val formula2D: String = "sin(x)",
    // Updated default Sombrero formulation that doesn't trigger divisions by zero easily
    val formula3D: String = "3 * sin(sqrt(x^2 + y^2)) / (sqrt(x^2 + y^2) + 0.1)",
    
    // 2D Engine Settings
    val scale2D: Float = 45f,
    val centerX2D: Double = 0.0,
    val centerY2D: Double = 0.0,

    // 3D Engine Settings
    val scale3D: Float = 35f,
    val theta3D: Float = -0.7f, // Default isometric-like angles
    val phi3D: Float = 0.5f,
    val rangeMin3D: Float = -10f,
    val rangeMax3D: Float = 10f,

    // Layout configuration
    val currentTab: Int = 0, // 0 = 2D, 1 = 3D, 2 = Presets, 3 = AI Assistant
    
    // History & Favorites
    val plotHistory: List<String> = listOf("sin(x)", "3 * sin(sqrt(x^2 + y^2)) / (sqrt(x^2 + y^2) + 0.1)"),

    // Gemini AI helper states
    val aiPrompt: String = "",
    val aiLoading: Boolean = false,
    val aiError: String? = null,
    val aiExplanationResult: String? = null,
    val aiSuggestedFormula: String? = null,
    val aiIs3D: Boolean = false,
    val aiRangeMin: Float = -10f,
    val aiRangeMax: Float = 10f
)

class MathPlotViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(MathUiState())
    val uiState: StateFlow<MathUiState> = _uiState.asStateFlow()

    fun updateTab(tab: Int) {
        _uiState.update { it.copy(currentTab = tab) }
    }

    fun updateFormula2D(newExpr: String) {
        _uiState.update { it.copy(formula2D = newExpr) }
    }

    fun updateFormula3D(newExpr: String) {
        _uiState.update { it.copy(formula3D = newExpr) }
    }

    fun updateTransform2D(newScale: Float, dCX: Double, dCY: Double) {
        _uiState.update {
            it.copy(
                scale2D = newScale,
                centerX2D = it.centerX2D + dCX,
                centerY2D = it.centerY2D + dCY
            )
        }
    }

    fun resetView2D() {
        _uiState.update {
            it.copy(
                scale2D = 45f,
                centerX2D = 0.0,
                centerY2D = 0.0
            )
        }
    }

    fun updateTransform3D(newScale: Float, newTheta: Float, newPhi: Float) {
        _uiState.update {
            it.copy(
                scale3D = newScale,
                theta3D = newTheta,
                phi3D = newPhi.coerceIn(0.1f, 1.4f) // Clamping elevation so we don't flip upside down
            )
        }
    }

    fun update3DRange(minX: Float, maxX: Float) {
        if (minX < maxX) {
            _uiState.update {
                it.copy(
                    rangeMin3D = minX,
                    rangeMax3D = maxX
                )
            }
        }
    }

    fun resetView3D() {
        _uiState.update {
            it.copy(
                scale3D = 35f,
                theta3D = -0.7f,
                phi3D = 0.5f,
                rangeMin3D = -10f,
                rangeMax3D = 10f
            )
        }
    }

    fun selectPreset(preset: GraphPreset) {
        if (preset.is3D) {
            _uiState.update {
                it.copy(
                    formula3D = preset.formula,
                    rangeMin3D = preset.rangeMinX,
                    rangeMax3D = preset.rangeMaxX,
                    currentTab = 1, // Switch to 3D plot tab immediately
                    plotHistory = (listOf(preset.formula) + it.plotHistory).distinct().take(15)
                )
            }
            resetView3D()
        } else {
            _uiState.update {
                it.copy(
                    formula2D = preset.formula,
                    currentTab = 0, // Switch to 2D plot tab immediately
                    plotHistory = (listOf(preset.formula) + it.plotHistory).distinct().take(15)
                )
            }
            resetView2D()
        }
    }

    fun applyFromAI(formula: String, is3D: Boolean, min: Float, max: Float) {
        if (is3D) {
            _uiState.update {
                it.copy(
                    formula3D = formula,
                    rangeMin3D = min,
                    rangeMax3D = max,
                    currentTab = 1,
                    plotHistory = (listOf(formula) + it.plotHistory).distinct().take(15)
                )
            }
            resetView3D()
        } else {
            _uiState.update {
                it.copy(
                    formula2D = formula,
                    currentTab = 0,
                    plotHistory = (listOf(formula) + it.plotHistory).distinct().take(15)
                )
            }
            resetView2D()
        }
    }

    fun updateAiPrompt(prompt: String) {
        _uiState.update { it.copy(aiPrompt = prompt) }
    }

    fun executeAiRecommendation() {
        val prompt = _uiState.value.aiPrompt.trim()
        if (prompt.isEmpty()) return

        _uiState.update {
            it.copy(
                aiLoading = true,
                aiError = null,
                aiExplanationResult = null,
                aiSuggestedFormula = null
            )
        }

        viewModelScope.launch {
            val result = GeminiMathService.getMathRecommendation(prompt)
            result.onSuccess { response ->
                _uiState.update {
                    it.copy(
                        aiLoading = false,
                        aiPrompt = "",
                        aiExplanationResult = response.explanation,
                        aiSuggestedFormula = response.suggestedFormula,
                        aiIs3D = response.is3D,
                        aiRangeMin = response.rangeMin,
                        aiRangeMax = response.rangeMax
                    )
                }
            }.onFailure { exception ->
                _uiState.update {
                    it.copy(
                        aiLoading = false,
                        aiError = exception.message ?: "Có lỗi xảy ra khi gọi trợ lý AI."
                    )
                }
            }
        }
    }

    fun clearAiState() {
        _uiState.update {
            it.copy(
                aiError = null,
                aiExplanationResult = null,
                aiSuggestedFormula = null
            )
        }
    }

    fun addToHistory(formula: String) {
        _uiState.update {
            it.copy(
                plotHistory = (listOf(formula) + it.plotHistory).distinct().take(15)
            )
        }
    }
}
