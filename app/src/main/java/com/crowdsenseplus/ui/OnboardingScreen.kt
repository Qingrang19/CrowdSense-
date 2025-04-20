package com.crowdsenseplus.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crowdsenseplus.R

/**
 * 引导页面，向用户介绍应用的核心功能和使用场景
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    var currentPage by remember { mutableStateOf(0) }
    val pages = listOf(
        OnboardingPage(
            title = "欢迎使用CrowdSense++",
            description = "群智感知模拟与优化平台",
            content = "CrowdSense++是一个强大的群智感知(Crowd Sensing)模拟平台，帮助研究人员和开发者模拟、分析和优化基于位置的群智感知任务分配。",
            iconResId = R.drawable.ic_launcher_foreground
        ),
        OnboardingPage(
            title = "什么是群智感知？",
            description = "利用移动设备收集和分享数据",
            content = "群智感知是一种利用大量移动设备用户（如智能手机用户）收集环境、交通、噪声等数据的方法。通过合理分配任务，可以高效地获取大范围的感知数据，应用于智慧城市、环境监测等领域。",
            iconResId = R.drawable.ic_launcher_foreground
        ),
        OnboardingPage(
            title = "模拟功能",
            description = "强大的模拟与分析能力",
            content = "• 用户移动轨迹模拟：支持步行、自行车和驾车三种移动方式\n• 任务生成与分配：设置任务数量、执行范围和持续时间\n• 多平台支持：传统MCS、基于雾计算(FOG-MCS)和边缘计算(MEC-MCS)\n• 结果可视化：地图展示和数据分析",
            iconResId = R.drawable.ic_launcher_foreground
        ),
        OnboardingPage(
            title = "开始使用",
            description = "设置参数，运行模拟，分析结果",
            content = "1. 在\"${stringResource(id = R.string.parameter_settings)}\"页面配置模拟参数\n2. 点击\"运行模拟\"按钮开始模拟\n3. 在\"${stringResource(id = R.string.simulation_results)}\"页面查看统计数据\n4. 使用\"${stringResource(id = R.string.map_visualization)}\"直观了解任务分布和用户位置\n5. 保存有价值的模拟结果供后续分析",
            iconResId = R.drawable.ic_launcher_foreground
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.onboarding_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 页面内容
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OnboardingPageContent(
                    page = pages[currentPage],
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 页面指示器
            Row(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                pages.indices.forEach { index ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (currentPage == index) 12.dp else 8.dp)
                            .background(
                                color = if (currentPage == index) 
                                    MaterialTheme.colorScheme.primary 
                                else 
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(50)
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 导航按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (currentPage > 0) {
                    Button(
                        onClick = { currentPage-- },
                        modifier = Modifier.width(120.dp)
                    ) {
                        Text(stringResource(id = R.string.previous_page))
                    }
                } else {
                    Spacer(modifier = Modifier.width(120.dp))
                }

                if (currentPage < pages.size - 1) {
                    Button(
                        onClick = { currentPage++ },
                        modifier = Modifier.width(120.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(id = R.string.next_page))
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = "下一页",
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = onComplete,
                        modifier = Modifier.width(120.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(stringResource(id = R.string.start_using))
                    }
                }
            }
        }
    }
}

/**
 * 引导页面数据类
 */
data class OnboardingPage(
    val title: String,
    val description: String,
    val content: String,
    val iconResId: Int
)

/**
 * 引导页面内容组件
 */
@Composable
fun OnboardingPageContent(page: OnboardingPage, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 图标
        Image(
            painter = painterResource(id = page.iconResId),
            contentDescription = page.title,
            modifier = Modifier
                .size(120.dp)
                .padding(bottom = 24.dp)
        )

        // 标题
        Text(
            text = page.title,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 描述
        Text(
            text = page.description,
            fontSize = 18.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 内容
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Text(
                text = page.content,
                modifier = Modifier.padding(16.dp),
                fontSize = 16.sp,
                lineHeight = 24.sp,
                textAlign = TextAlign.Start
            )
        }
    }
}