package com.example.basicworkmanager

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    companion object {
        const val KEY_COUNT_VALUE = "key_count"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val button = findViewById<Button>(R.id.button)
        button.setOnClickListener {
            setOneTimeWorkRequest()
//            setPeriodicWorkRequest()
        }
    }

    //일회성 요청
    private fun setOneTimeWorkRequest(){
        val workManager = WorkManager.getInstance(applicationContext)

        //데이터 객체
        val data = Data.Builder()
            .putInt(KEY_COUNT_VALUE, 60000)
            .build()

        //제약 조건
        val constraints = Constraints.Builder()
            .setRequiresCharging(true) //충전중일때
            .setRequiredNetworkType(NetworkType.CONNECTED) //네트워크 연결상태일때
            .build()

        val uploadRequest = OneTimeWorkRequestBuilder<UploadWorker>()
            .setConstraints(constraints)
            .setInputData(data)
            .build()

        val filteringRequest = OneTimeWorkRequestBuilder<FilteringWorker>()
            .build()

        val compressingRequest = OneTimeWorkRequestBuilder<CompressingWorker>()
            .build()

        val downloadingRequest = OneTimeWorkRequestBuilder<DownloadingWorker>()
            .build()

        //request가 하나일 때는 이렇게하고
//        workManager
//            .enqueue(uploadRequest)

        //request가 여러개일 때는 이렇게 해라
//        workManager
//            .beginWith(filteringRequest)
//            .then(compressingRequest)
//            .then(uploadRequest)
//            .enqueue()

        //병렬 작업할 때는 이렇게 해라
        val paralleWorks = mutableListOf<OneTimeWorkRequest>()
        paralleWorks.add(filteringRequest)
        paralleWorks.add(downloadingRequest)
        workManager
            .beginWith(paralleWorks)
            .then(compressingRequest)
            .then(uploadRequest)
            .enqueue()

        workManager.getWorkInfoByIdLiveData(uploadRequest.id)
            .observe(this){
                val textView = findViewById<TextView>(R.id.textView)
                textView.text = it.state.name
                if(it.state.isFinished){
                    val data = it.outputData
                    val message = data.getString(UploadWorker.KEY_WORKER)
                    Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
                }
            }
    }

    //정기적으로 작업을 요청할 수 있는 최소 기간은 15분이다.
    private fun setPeriodicWorkRequest(){
        val periodicWorkRequest = PeriodicWorkRequest.Builder(DownloadingWorker::class.java, 16, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(applicationContext).enqueue(periodicWorkRequest)
    }
}