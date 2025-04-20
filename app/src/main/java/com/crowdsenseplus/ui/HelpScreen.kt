package com.crowdsenseplus.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.crowdsenseplus.R

/**
 * 帮助页面，提供应用功能的详细说明和使用指南
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.help_screen_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(id = R.string.back))
                    }
                },
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            HelpSection(
                title = "什么是群智感知？",
                content = "群智感知(Crowd Sensing)是一种利用大量移动设备用户收集环境数据的方法。通过智能手机等移动设备，用户可以感知并上传周围环境的各种数据，如空气质量、噪声水平、交通状况等。\n\n这种方法可以高效地获取大范围的感知数据，应用于智慧城市、环境监测、交通管理等多个领域。"
            )
            
            HelpSection(
                title = "CrowdSense++的功能",
                content = "CrowdSense++是一个群智感知模拟平台，提供以下核心功能：\n\n• 用户移动轨迹模拟：支持步行、自行车和驾车三种移动方式\n• 任务生成与分配：设置任务数量、执行范围和持续时间\n• 多平台支持：传统MCS、基于雾计算(FOG-MCS)和边缘计算(MEC-MCS)\n• 结果可视化：地图展示和数据分析"
            )
            
            HelpSection(
                title = "参数设置说明",
                content = "• 模拟天数：设置模拟的总天数\n• 用户数量：参与群智感知的用户总数\n• 移动方式：用户的移动方式（步行/自行车/驾车）\n• 任务数量：需要完成的感知任务数量\n• 执行范围：任务可被执行的地理范围（米）\n• 任务持续时间：每个任务的持续时间（分钟）\n• 时间槽持续时间：时间划分的最小单位（分钟）\n• 平台类型：选择不同的计算架构"
            )
            
            HelpSection(
                title = "平台类型说明",
                content = "• MCS（移动群智感知）：传统的中心化群智感知架构\n• FOG-MCS（雾计算群智感知）：利用网络边缘的雾节点进行数据处理和任务分配\n• MEC-MCS（移动边缘计算群智感知）：在移动网络边缘进行计算，降低延迟"
            )
            
            HelpSection(
                title = "如何使用模拟结果",
                content = "模拟结果页面显示每个任务的候选用户数量，这表示在任务执行时间和范围内可以执行该任务的用户数量。\n\n通过地图可视化功能，您可以直观地查看任务分布和用户位置，了解任务覆盖情况。\n\n您可以保存有价值的模拟结果，以便后续分析或比较不同参数设置下的效果。"
            )
            
            HelpSection(
                title = "应用场景示例",
                content = "• 智慧城市：收集城市噪声、空气质量等环境数据\n• 交通监测：实时获取交通流量和道路状况\n• 公共安全：紧急情况下的人群分布监测\n• 大型活动：活动场所的人流密度和分布监测\n• 环境研究：收集特定区域的环境参数"
            )
            
            HelpSection(
                title = "优化建议",
                content = "• 根据实际场景调整用户数量和任务数量\n• 考虑不同移动方式对数据收集的影响\n• 合理设置任务执行范围，过大或过小都会影响效果\n• 比较不同平台类型的性能差异，选择最适合的架构\n• 保存多组模拟结果进行对比分析"
            )
        }
    }
}

/**
 * 帮助部分组件
 */
@Composable
fun HelpSection(title: String, content: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = content,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                textAlign = TextAlign.Start
            )
        }
    }
}