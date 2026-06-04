package com.example.math

data class GraphPreset(
    val name: String,
    val formula: String,
    val is3D: Boolean,
    val rangeMinX: Float = -10f,
    val rangeMaxX: Float = 10f,
    val rangeMinY: Float = -10f,
    val rangeMaxY: Float = 10f,
    val description: String = ""
)

object PredefinedFunctions {
    val presets2D = listOf(
        GraphPreset(
            name = "Sóng hình Sin cơ bản",
            formula = "sin(x)",
            is3D = false,
            rangeMinX = -10f,
            rangeMaxX = 10f,
            rangeMinY = -3f,
            rangeMaxY = 3f,
            description = "Hàm lượng giác cơ bản tuần hoàn dao động giữa -1 và 1."
        ),
        GraphPreset(
            name = "Sóng tắt dần (Damped Wave)",
            formula = "exp(-0.15*abs(x)) * cos(2*x)",
            is3D = false,
            rangeMinX = -15f,
            rangeMaxX = 15f,
            rangeMinY = -2f,
            rangeMaxY = 2f,
            description = "Biểu diễn mô hình dao động lí học giảm dần theo thời gian/khoảng cách."
        ),
        GraphPreset(
            name = "Hàm Sinc (Batwing)",
            formula = "sin(x) / (x + 0.001)",
            is3D = false,
            rangeMinX = -15f,
            rangeMaxX = 15f,
            rangeMinY = -1f,
            rangeMaxY = 2f,
            description = "Hàm phân bố lọc tần số trong xử lí tín hiệu số, có dạng sóng giảm dần đối xứng."
        ),
        GraphPreset(
            name = "Đường Cong Trái Tim (Heart f(x))",
            formula = "abs(x)^0.7 + 0.9 * sqrt(max(0, 9 - x^2)) * sin(12 * pi * x)",
            is3D = false,
            rangeMinX = -3.5f,
            rangeMaxX = 3.5f,
            rangeMinY = -1f,
            rangeMaxY = 5f,
            description = "Phương trình trái tim kết hợp sóng sin cao tần tạo hoa văn viền ấn tượng."
        ),
        GraphPreset(
            name = "Giao thoa sóng điện từ",
            formula = "cos(2*x) * sin(3*x)",
            is3D = false,
            rangeMinX = -8f,
            rangeMaxX = 8f,
            rangeMinY = -2f,
            rangeMaxY = 2f,
            description = "Sự chồng chập của các sóng có tần số khác nhau tạo phách giao thoa."
        ),
        GraphPreset(
            name = "Hàm mũ Gaussi (Bell Curve)",
            formula = "3 * exp(-x^2 / 8)",
            is3D = false,
            rangeMinX = -10f,
            rangeMaxX = 10f,
            rangeMinY = -1f,
            rangeMaxY = 4f,
            description = "Đường cong chuông phân phối chuẩn Gauss phổ biến trong thống kê."
        )
    )

    val presets3D = listOf(
        GraphPreset(
            name = "Mặt sóng Sombrero (Sombrero Hat)",
            formula = "3 * sin(sqrt(x^2 + y^2)) / (sqrt(x^2 + y^2) + 0.01)",
            is3D = true,
            rangeMinX = -10f,
            rangeMaxX = 10f,
            rangeMinY = -10f,
            rangeMaxY = 10f,
            description = "Đồ thị 3D kinh điển hình dáng mũ rộng vành Mexico với sóng lan tỏa."
        ),
        GraphPreset(
            name = "Mặt Yên Ngựa (Saddle Surface)",
            formula = "x^2 / 8 - y^2 / 8",
            is3D = true,
            rangeMinX = -6f,
            rangeMaxX = 6f,
            rangeMinY = -6f,
            rangeMaxY = 6f,
            description = "Điểm yên ngựa nổi tiếng tại gốc tọa độ, dốc xuống theo một chiều và dốc lên theo chiều kia."
        ),
        GraphPreset(
            name = "Đồi Cát Sa Mạc (Waves)",
            formula = "sin(x) * cos(y)",
            is3D = true,
            rangeMinX = -6f,
            rangeMaxX = 6f,
            rangeMinY = -6f,
            rangeMaxY = 6f,
            description = "Đồi sóng nhấp nhô vô tận mô tả địa hình cát bay hoang mạc."
        ),
        GraphPreset(
            name = "Đỉnh Núi Gaussian (Gaussian Dome)",
            formula = "4 * exp(-(x^2 + y^2) / 6)",
            is3D = true,
            rangeMinX = -5f,
            rangeMaxX = 5f,
            rangeMinY = -5f,
            rangeMaxY = 5f,
            description = "Đại diện phân phối chuẩn 2 chiều thành hình đỉnh vòm tròn mượt mà."
        ),
        GraphPreset(
            name = "Mặt Phễu (Funnel)",
            formula = "-2 / (sqrt(x^2 + y^2) + 0.5)",
            is3D = true,
            rangeMinX = -5f,
            rangeMaxX = 5f,
            rangeMinY = -5f,
            rangeMaxY = 5f,
            description = "Biểu diễn trường trọng lực, hút điểm ở lõi sâu thẳm vô tận."
        ),
        GraphPreset(
            name = "Sóng Giao Thoa Tròn (Ripple)",
            formula = "1.5 * cos(sqrt(x^2 + y^2) * 1.5)",
            is3D = true,
            rangeMinX = -8f,
            rangeMaxX = 8f,
            rangeMinY = -8f,
            rangeMaxY = 8f,
            description = "Mô hình sóng đồng tâm lan tỏa khi ném viên sỏi xuống mặt hồ lặng."
        )
    )
}
