package com.crowdsenseplus

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import android.content.Context
import android.util.Log
import com.crowdsenseplus.model.SimulationModel
import com.crowdsenseplus.ui.HelpScreen
import com.crowdsenseplus.ui.OnboardingScreen
import com.crowdsenseplus.ui.theme.CrowdSenseTheme
import com.crowdsenseplus.viewmodel.SimulationState
import com.crowdsenseplus.viewmodel.SimulationViewModel
import com.crowdsenseplus.utils.MapCrashHandler
import com.crowdsenseplus.utils.MapErrorLogger
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // 初始化地图崩溃处理器，用于捕获和记录地图相关崩溃
        try {
            MapCrashHandler.initialize(applicationContext)
            Log.i("MainActivity", "地图崩溃处理器初始化成功")
        } catch (e: Exception) {
            Log.e("MainActivity", "地图崩溃处理器初始化失败: ${e.message}")
        }
        
        setContent {
            CrowdSenseTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // 检查是否是首次启动应用
                    val sharedPrefs = getSharedPreferences("crowdsense_prefs", Context.MODE_PRIVATE)
                    val isFirstLaunch = sharedPrefs.getBoolean("is_first_launch", true)
                    
                    var showOnboarding by remember { mutableStateOf(isFirstLaunch) }
                    
                    if (showOnboarding) {
                        OnboardingScreen(onComplete = {
                            // 标记已完成引导
                            sharedPrefs.edit().putBoolean("is_first_launch", false).apply()
                            showOnboarding = false
                        })
                    } else {
                        CrowdSenseApp()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrowdSenseApp() {
    val viewModel: SimulationViewModel = viewModel()
    val simulationState by viewModel.simulationState.observeAsState(SimulationState.Idle)
    val parameters by viewModel.parameters.observeAsState(SimulationModel.SimulationParameters())
    val results by viewModel.results.observeAsState(emptyList())
    
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf(
        context.getString(R.string.parameter_settings),
        context.getString(R.string.simulation_results),
        context.getString(R.string.map_visualization),
        context.getString(R.string.saved_simulations)
    )
    val scope = rememberCoroutineScope()
    var cityName by remember { mutableStateOf("Pamplona") }
    var savedSimulations by remember { mutableStateOf(emptyList<String>()) }
    var showHelp by remember { mutableStateOf(false) }
    
    // 加载保存的模拟列表
    LaunchedEffect(Unit) {
        savedSimulations = viewModel.getSavedSimulations(context)
    }
    
    // 显示帮助页面
    if (showHelp) {
        HelpScreen(onNavigateBack = { showHelp = false })
        return
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CrowdSense++") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(onClick = { showHelp = true }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = stringResource(id = R.string.help)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            // 标签栏
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }
            
            // 内容区域
            Box(modifier = Modifier.fillMaxSize()) {
                when (selectedTab) {
                    0 -> ParametersScreen(
                        parameters = parameters,
                        cityName = cityName,
                        onCityNameChange = { cityName = it },
                        onParametersChange = { viewModel.updateParameters(it) },
                        onRunSimulation = { viewModel.runSimulation(context, cityName) },
                        simulationState = simulationState,
                        onShowHelp = { showHelp = true }
                    )
                    1 -> ResultsScreen(
                        results = results,
                        simulationState = simulationState,
                        onSaveSimulation = { viewModel.saveSimulation(context) }
                    )
                    2 -> {
                        // 地图可视化选择
                        var useStaticMap by remember { mutableStateOf(true) } // 默认使用静态地图避免崩溃
                        
                        Column(modifier = Modifier.fillMaxSize()) {
                            // 地图类型选择开关
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("地图类型：", style = MaterialTheme.typography.bodyMedium)
                                Spacer(modifier = Modifier.width(8.dp))
                                
                                Row(
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Button(
                                        onClick = { useStaticMap = true },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (useStaticMap) 
                                                MaterialTheme.colorScheme.primary 
                                            else 
                                                MaterialTheme.colorScheme.surfaceVariant
                                        ),
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    ) {
                                        Text("静态地图")
                                    }
                                    
                                    Button(
                                        onClick = { useStaticMap = false },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (!useStaticMap) 
                                                MaterialTheme.colorScheme.primary 
                                            else 
                                                MaterialTheme.colorScheme.surfaceVariant
                                        ),
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    ) {
                                        Text("高德地图")
                                    }
                                }
                                
                                Spacer(modifier = Modifier.weight(1f))
                                
                                if (!useStaticMap) {
                                    Text(
                                        "⚠️ 高德地图可能导致应用崩溃",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                            
                            // 地图内容区域
                            Box(modifier = Modifier.weight(1f)) {
                                if (useStaticMap) {
                                    com.crowdsenseplus.ui.StaticMapVisualizationScreen(
                                        userMovements = viewModel.userMovements.value ?: emptyList(),
                                        tasks = viewModel.tasks.value ?: emptyList(),
                                        results = results
                                    )
                                } else {
                                    com.crowdsenseplus.ui.MapVisualizationScreen(
                                        userMovements = viewModel.userMovements.value ?: emptyList(),
                                        tasks = viewModel.tasks.value ?: emptyList(),
                                        results = results
                                    )
                                }
                            }
                        }
                    }
                    3 -> SavedSimulationsScreen(
                        simulations = savedSimulations,
                        onLoadSimulation = { viewModel.loadSavedSimulation(context, it) },
                        onRefresh = {
                            scope.launch {
                                savedSimulations = viewModel.getSavedSimulations(context)
                            }
                        }
                    )
                }
                
                // 显示加载状态
                if (simulationState is SimulationState.GeneratingUsers ||
                    simulationState is SimulationState.GeneratingTasks ||
                    simulationState is SimulationState.Computing ||
                    simulationState is SimulationState.Loading ||
                    simulationState is SimulationState.Saving) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = when (simulationState) {
                                    is SimulationState.GeneratingUsers -> "正在生成用户移动数据..."
                                    is SimulationState.GeneratingTasks -> "正在生成任务数据..."
                                    is SimulationState.Computing -> "正在计算候选者..."
                                    is SimulationState.Loading -> "正在加载模拟数据..."
                                    is SimulationState.Saving -> "正在保存模拟数据..."
                                    else -> "处理中..."
                                },
                                color = Color.White
                            )
                        }
                    }
                }
                
                // 显示错误信息
                if (simulationState is SimulationState.Error) {
                    val errorMessage = (simulationState as SimulationState.Error).message
                    LaunchedEffect(errorMessage) {
                        Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                    }
                }
                
                // 显示保存成功信息
                if (simulationState is SimulationState.Saved) {
                    LaunchedEffect(Unit) {
                        Toast.makeText(context, "模拟数据已保存", Toast.LENGTH_SHORT).show()
                        scope.launch {
                            savedSimulations = viewModel.getSavedSimulations(context)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ParametersScreen(
    parameters: SimulationModel.SimulationParameters,
    cityName: String,
    onCityNameChange: (String) -> Unit,
    onParametersChange: (SimulationModel.SimulationParameters) -> Unit,
    onRunSimulation: () -> Unit,
    simulationState: SimulationState,
    onShowHelp: () -> Unit
) {
    var days by remember { mutableStateOf(parameters.days.toString()) }
    var users by remember { mutableStateOf(parameters.numberOfUsers.toString()) }
    var tasks by remember { mutableStateOf(parameters.numberOfTasks.toString()) }
    var range by remember { mutableStateOf(parameters.executionRange.toString()) }
    var duration by remember { mutableStateOf(parameters.taskDuration.toString()) }
    var timeslot by remember { mutableStateOf(parameters.timeslotDuration.toString()) }
    var locomotionType by remember { mutableStateOf(parameters.locomotionType) }
    var platformType by remember { mutableStateOf(parameters.platformType) }
    
    // 更新参数
    fun updateParameters() {
        try {
            onParametersChange(
                SimulationModel.SimulationParameters(
                    days = days.toIntOrNull() ?: parameters.days,
                    numberOfUsers = users.toIntOrNull() ?: parameters.numberOfUsers,
                    locomotionType = locomotionType,
                    numberOfTasks = tasks.toIntOrNull() ?: parameters.numberOfTasks,
                    executionRange = range.toIntOrNull() ?: parameters.executionRange,
                    taskDuration = duration.toIntOrNull() ?: parameters.taskDuration,
                    timeslotDuration = timeslot.toIntOrNull() ?: parameters.timeslotDuration,
                    platformType = platformType
                )
            )
        } catch (e: Exception) {
            // 参数转换错误处理
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "模拟参数设置",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            
            IconButton(onClick = onShowHelp) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_info),
                    contentDescription = "参数说明",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        // 功能简介卡片
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "群智感知模拟器",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "设置参数模拟用户移动和任务分配，分析候选用户数量和分布情况。",
                    fontSize = 14.sp
                )
            }
        }
        
        // 城市名称
        OutlinedTextField(
            value = cityName,
            onValueChange = onCityNameChange,
            label = { Text("城市名称") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        // 模拟天数
        OutlinedTextField(
            value = days,
            onValueChange = { 
                days = it 
                updateParameters()
            },
            label = { Text("模拟天数") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        // 用户数量
        OutlinedTextField(
            value = users,
            onValueChange = { 
                users = it 
                updateParameters()
            },
            label = { Text("用户数量") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        // 移动方式
        Text("移动方式")
        Row(modifier = Modifier.fillMaxWidth()) {
            RadioButton(
                selected = locomotionType == 1,
                onClick = { 
                    locomotionType = 1 
                    updateParameters()
                }
            )
            Text("步行", modifier = Modifier.padding(start = 8.dp, end = 16.dp))
            
            RadioButton(
                selected = locomotionType == 2,
                onClick = { 
                    locomotionType = 2 
                    updateParameters()
                }
            )
            Text("自行车", modifier = Modifier.padding(start = 8.dp, end = 16.dp))
            
            RadioButton(
                selected = locomotionType == 3,
                onClick = { 
                    locomotionType = 3 
                    updateParameters()
                }
            )
            Text("驾车", modifier = Modifier.padding(start = 8.dp))
        }
        
        // 任务数量
        OutlinedTextField(
            value = tasks,
            onValueChange = { 
                tasks = it 
                updateParameters()
            },
            label = { Text("任务数量") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        // 执行范围
        OutlinedTextField(
            value = range,
            onValueChange = { 
                range = it 
                updateParameters()
            },
            label = { Text("执行范围(米)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        // 任务持续时间
        OutlinedTextField(
            value = duration,
            onValueChange = { 
                duration = it 
                updateParameters()
            },
            label = { Text("任务持续时间(分钟)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        // 时间槽持续时间
        OutlinedTextField(
            value = timeslot,
            onValueChange = { 
                timeslot = it 
                updateParameters()
            },
            label = { Text("时间槽持续时间(分钟)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        // 平台类型
        Text("平台类型")
        Row(modifier = Modifier.fillMaxWidth()) {
            RadioButton(
                selected = platformType == 1,
                onClick = { 
                    platformType = 1 
                    updateParameters()
                }
            )
            Text("MCS", modifier = Modifier.padding(start = 8.dp, end = 16.dp))
            
            RadioButton(
                selected = platformType == 2,
                onClick = { 
                    platformType = 2 
                    updateParameters()
                }
            )
            Text("FOG-MCS", modifier = Modifier.padding(start = 8.dp, end = 16.dp))
            
            RadioButton(
                selected = platformType == 3,
                onClick = { 
                    platformType = 3 
                    updateParameters()
                }
            )
            Text("MEC-MCS", modifier = Modifier.padding(start = 8.dp))
        }
        
        // 运行模拟按钮
        Button(
            onClick = onRunSimulation,
            modifier = Modifier.fillMaxWidth(),
            enabled = simulationState !is SimulationState.GeneratingUsers &&
                     simulationState !is SimulationState.GeneratingTasks &&
                     simulationState !is SimulationState.Computing
        ) {
            Text("运行模拟")
        }
    }
}

@Composable
fun ResultsScreen(
    results: List<SimulationModel.SimulationResult>,
    simulationState: SimulationState,
    onSaveSimulation: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "模拟结果",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            
            Button(
                onClick = onSaveSimulation,
                enabled = results.isNotEmpty() && 
                         simulationState !is SimulationState.Saving &&
                         simulationState !is SimulationState.Loading
            ) {
                Text("保存模拟")
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 结果说明卡片
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "结果解释",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "候选用户数表示在任务执行时间和范围内可以执行该任务的用户数量。数值越高表示任务可能被执行的概率越大。",
                    fontSize = 14.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        if (results.isEmpty() && simulationState !is SimulationState.Computing) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "暂无模拟结果，请先运行模拟",
                    color = Color.Gray
                )
            }
        } else {
            // 结果统计信息
            if (results.isNotEmpty()) {
                val avgCandidates = results.map { it.candidates }.average()
                val maxCandidates = results.maxOf { it.candidates }
                val minCandidates = results.minOf { it.candidates }
                
                Row(modifier = Modifier.fillMaxWidth()) {
                    StatisticCard(
                        title = "平均候选用户",
                        value = String.format("%.1f", avgCandidates),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    StatisticCard(
                        title = "最大候选用户",
                        value = maxCandidates.toString(),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    StatisticCard(
                        title = "最小候选用户",
                        value = minCandidates.toString(),
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // 结果表头
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(8.dp)
            ) {
                Text(
                    text = "任务ID",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "候选用户数",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "状态",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
            }
            
            // 结果列表
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(results) { result ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${result.taskId}",
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${result.candidates}",
                            modifier = Modifier.weight(1f)
                        )
                        // 状态指示
                        val status = when {
                            result.candidates == 0 -> "无覆盖"
                            result.candidates < 5 -> "低覆盖"
                            result.candidates < 15 -> "中覆盖"
                            else -> "高覆盖"
                        }
                        val statusColor = when {
                            result.candidates == 0 -> Color.Red
                            result.candidates < 5 -> Color(0xFFFFA000) // 橙色
                            result.candidates < 15 -> Color(0xFF4CAF50) // 绿色
                            else -> Color(0xFF2196F3) // 蓝色
                        }
                        Text(
                            text = status,
                            color = statusColor,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Divider()
                }
            }
        }
    }
}

@Composable
fun StatisticCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.padding(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun SavedSimulationsScreen(
    simulations: List<String>,
    onLoadSimulation: (String) -> Unit,
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "保存的模拟",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            
            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, contentDescription = "刷新")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (simulations.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "暂无保存的模拟",
                    color = Color.Gray
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(simulations) { simulation ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(simulation)
                            Button(onClick = { onLoadSimulation(simulation) }) {
                                Text("加载")
                            }
                        }
                    }
                }
            }
        }
    }
}