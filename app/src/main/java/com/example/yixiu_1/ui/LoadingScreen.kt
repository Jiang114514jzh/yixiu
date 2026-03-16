import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// 骨架屏卡片组件
@Composable
fun SkeletonHistoryCard() {
    // 1. 创建无限循环的“呼吸”透明度动画
    val infiniteTransition = rememberInfiniteTransition(label = "skeleton_transition")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeleton_alpha"
    )

    // 2. 绘制与真实卡片形状相似的灰色色块
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha), // 应用呼吸透明度
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 模拟标题和状态
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Box(modifier = Modifier.height(20.dp).width(120.dp).background(Color.LightGray, RoundedCornerShape(4.dp)))
                Box(modifier = Modifier.height(24.dp).width(60.dp).background(Color.LightGray, RoundedCornerShape(4.dp)))
            }
            Spacer(modifier = Modifier.height(16.dp))
            // 模拟内容描述 (两行)
            Box(modifier = Modifier.height(16.dp).fillMaxWidth(0.9f).background(Color(0xFFEEEEEE), RoundedCornerShape(4.dp)))
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.height(16.dp).fillMaxWidth(0.6f).background(Color(0xFFEEEEEE), RoundedCornerShape(4.dp)))

            Spacer(modifier = Modifier.height(24.dp))

            // 模拟底部时间和单号
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Box(modifier = Modifier.height(14.dp).width(80.dp).background(Color(0xFFEEEEEE), RoundedCornerShape(4.dp)))
                Box(modifier = Modifier.height(14.dp).width(100.dp).background(Color(0xFFEEEEEE), RoundedCornerShape(4.dp)))
            }
        }
    }
}