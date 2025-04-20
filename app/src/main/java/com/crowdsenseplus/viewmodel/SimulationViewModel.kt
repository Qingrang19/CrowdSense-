package com.crowdsenseplus.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crowdsenseplus.model.SimulationModel
import com.crowdsenseplus.repository.SimulationRepository
import kotlinx.coroutines.launch

/**
 * 模拟器ViewModel，负责连接UI和数据层
 */
class SimulationViewModel : ViewModel() {
    private val repository = SimulationRepository()
    private val simulationModel = SimulationModel()
    
    // 模拟状态
    private val _simulationState = MutableLiveData<SimulationState>(SimulationState.Idle)
    val simulationState: LiveData<SimulationState> = _simulationState
    
    // 模拟参数
    private val _parameters = MutableLiveData(SimulationModel.SimulationParameters())
    val parameters: LiveData<SimulationModel.SimulationParameters> = _parameters
    
    // 模拟结果
    private val _results = MutableLiveData<List<SimulationModel.SimulationResult>>(emptyList())
    val results: LiveData<List<SimulationModel.SimulationResult>> = _results
    
    // 用户数据
    private val _userMovements = MutableLiveData<List<SimulationModel.UserMovementEvent>>(emptyList())
    val userMovements: LiveData<List<SimulationModel.UserMovementEvent>> = _userMovements
    
    // 任务数据
    private val _tasks = MutableLiveData<List<SimulationModel.Task>>(emptyList())
    val tasks: LiveData<List<SimulationModel.Task>> = _tasks
    
    /**
     * 更新模拟参数
     */
    fun updateParameters(params: SimulationModel.SimulationParameters) {
        _parameters.value = params
        simulationModel.setParameters(params)
    }
    
    /**
     * 运行完整模拟
     */
    fun runSimulation(context: Context, cityName: String) {
        viewModelScope.launch {
            try {
                _simulationState.value = SimulationState.GeneratingUsers
                
                // 生成用户移动数据
                val usersGenerated = simulationModel.generateUserMovements(context, cityName)
                if (!usersGenerated) {
                    _simulationState.value = SimulationState.Error("生成用户移动数据失败")
                    return@launch
                }
                _userMovements.value = simulationModel.getUserMovements()
                
                // 生成任务
                _simulationState.value = SimulationState.GeneratingTasks
                val tasksGenerated = simulationModel.generateTasks(context)
                if (!tasksGenerated) {
                    _simulationState.value = SimulationState.Error("生成任务失败")
                    return@launch
                }
                _tasks.value = simulationModel.getTasks()
                
                // 计算候选者
                _simulationState.value = SimulationState.Computing
                val simulationResults = simulationModel.computeCandidatesForTasks()
                _results.value = simulationResults
                
                // 保存结果
                simulationModel.saveResultsToFile(context)
                
                _simulationState.value = SimulationState.Completed
            } catch (e: Exception) {
                _simulationState.value = SimulationState.Error(e.message ?: "未知错误")
            }
        }
    }
    
    /**
     * 保存模拟结果
     */
    fun saveSimulation(context: Context) {
        viewModelScope.launch {
            try {
                _simulationState.value = SimulationState.Saving
                repository.saveSimulation(context, 
                    simulationModel.getUserMovements(),
                    simulationModel.getTasks(),
                    simulationModel.getResults())
                _simulationState.value = SimulationState.Saved
            } catch (e: Exception) {
                _simulationState.value = SimulationState.Error("保存模拟失败: ${e.message}")
            }
        }
    }
    
    /**
     * 加载保存的模拟
     */
    fun loadSavedSimulation(context: Context, simulationId: String) {
        viewModelScope.launch {
            try {
                _simulationState.value = SimulationState.Loading
                val simulation = repository.loadSimulation(context, simulationId)
                _userMovements.value = simulation.userMovements
                _tasks.value = simulation.tasks
                _results.value = simulation.results
                _simulationState.value = SimulationState.Completed
            } catch (e: Exception) {
                _simulationState.value = SimulationState.Error("加载模拟失败: ${e.message}")
            }
        }
    }
    
    /**
     * 获取保存的模拟列表
     */
    suspend fun getSavedSimulations(context: Context): List<String> {
        return repository.getSavedSimulations(context)
    }
}

/**
 * 模拟状态
 */
sealed class SimulationState {
    object Idle : SimulationState()
    object GeneratingUsers : SimulationState()
    object GeneratingTasks : SimulationState()
    object Computing : SimulationState()
    object Completed : SimulationState()
    object Saving : SimulationState()
    object Saved : SimulationState()
    object Loading : SimulationState()
    data class Error(val message: String) : SimulationState()
}