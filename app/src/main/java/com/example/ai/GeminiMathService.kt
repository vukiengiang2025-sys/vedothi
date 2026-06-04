package com.example.ai

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class GeminiResponse(
    val explanation: String,
    val suggestedFormula: String?,
    val is3D: Boolean,
    val rangeMin: Float,
    val rangeMax: Float,
    val rawJsonError: Boolean = false
)

object GeminiMathService {
    private const val TAG = "GeminiMathService"
    private const val MODEL_NAME = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun getMathRecommendation(userPrompt: String): Result<GeminiResponse> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(Exception("API Key chưa được cấu hình. Vui lòng thêm GEMINI_API_KEY vào bảng điều khiển Secrets trong AI Studio."))
        }

        val systemInstructionText = """
            Bạn là một chuyên gia toán học và giảng viên xuất sắc, chuyên vẽ đồ thị hàm số 2D (Oxy) và 3D (Oxyz).
            Nhiệm vụ của bạn là giải thích định nghĩa toán học của người dùng và đề xuất phương trình hàm số trực quan, ấn tượng nhất có thể vẽ bằng hệ thống vẽ đồ thị.
            Hãy luôn trả về câu trả lời duy nhất ở dạng cấu trúc một đối tượng JSON với schema nghiêm ngặt sau:
            {
              "explanation": "Giải thích chi tiết bằng tiếng Việt về ý nghĩa hàm số, hình dáng đồ thị và hướng dẫn tương tác.",
              "suggestedFormula": "Phương trình hàm số hợp lệ, ví dụ 'sin(x)*cos(y)' hoặc 'exp(-0.2*abs(x))*cos(3*x)'. Định dạng biến chỉ bao gồm 'x' đối với 2D, và cả 'x', 'y' hoặc các hàm số chuẩn (sin, cos, tan, cot, sqrt, abs, exp, ln, log, asin, acos, atan, pi, e).",
              "is3D": true / false,
              "rangeMin": Floats (ví dụ -10.0),
              "rangeMax": Floats (ví dụ 10.0)
            }
            Chú ý: Trả về một chuỗi JSON thuần túy, tuyệt đối không bọc trong thẻ ```json...```. Không dùng ký tự lạ trong công thức gợi ý.
        """.trimIndent()

        // Construct JSON Payload
        val payload = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", userPrompt)
                        })
                    })
                })
            })
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply {
                        put("text", systemInstructionText)
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.3)
            })
        }

        val requestBody = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val urlWithKey = "$BASE_URL?key=$apiKey"

        val request = Request.Builder()
            .url(urlWithKey)
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string()
                Log.d(TAG, "Response Code: ${response.code}, Body: $bodyString")

                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("Yêu cầu gửi lên Gemini thất bại (Mã lỗi: ${response.code}). Vui lòng thử lại."))
                }

                if (bodyString.isNullOrEmpty()) {
                    return@withContext Result.failure(Exception("Nội dung phản hồi trống."))
                }

                // Parse standard Gemini structure
                val rootJson = JSONObject(bodyString)
                val candidates = rootJson.optJSONArray("candidates")
                if (candidates == null || candidates.length() == 0) {
                    return@withContext Result.failure(Exception("Không tìm thấy kết quả từ AI."))
                }

                val content = candidates.getJSONObject(0).get("content") as JSONObject
                val parts = content.getJSONArray("parts")
                if (parts.length() == 0) {
                    return@withContext Result.failure(Exception("Nội dung AI rỗng."))
                }

                val textResponse = parts.getJSONObject(0).optString("text")
                if (textResponse.isNullOrEmpty()) {
                    return@withContext Result.failure(Exception("Không thể nhận văn bản từ AI."))
                }

                // Parse the internal custom JSON
                try {
                    val mathJson = JSONObject(textResponse.trim())
                    val explanation = mathJson.optString("explanation", "Giải thích toán học.")
                    val suggestedFormula = mathJson.optString("suggestedFormula", null)
                    val is3D = mathJson.optBoolean("is3D", false)
                    val rangeMin = mathJson.optDouble("rangeMin", -10.0).toFloat()
                    val rangeMax = mathJson.optDouble("rangeMax", 10.0).toFloat()

                    Result.success(
                        GeminiResponse(
                            explanation = explanation,
                            suggestedFormula = suggestedFormula,
                            is3D = is3D,
                            rangeMin = rangeMin,
                            rangeMax = rangeMax
                        )
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Lỗi phân tích JSON nội bộ: ${e.message}", e)
                    // Fallback to text explanation if JSON formatting fails but we have readable content
                    Result.success(
                        GeminiResponse(
                            explanation = textResponse,
                            suggestedFormula = null,
                            is3D = false,
                            rangeMin = -10f,
                            rangeMax = 10f,
                            rawJsonError = true
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi kết nối mạng: ${e.message}", e)
            Result.failure(Exception("Lỗi mạng: Không thể kết nối với máy chủ AI. Vui lòng kiểm tra kết nối mạng của bạn."))
        }
    }
}
