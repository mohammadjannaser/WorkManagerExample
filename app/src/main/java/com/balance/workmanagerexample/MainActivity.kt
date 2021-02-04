package com.balance.workmanagerexample

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.work.*
import com.balance.workmanagerexample.MainActivity.ProgressWorker.Companion.Progress
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity() {

    private lateinit var myContext : Context

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        myContext = this

        // For simple work, which requires no additional configuration, use the static method from
        val myWorkRequest = OneTimeWorkRequest.from(UploadWorker::class.java)

        // For more complex work request.***********************************************************
        val uploadWorkRequest: WorkRequest = OneTimeWorkRequestBuilder<UploadWorker>()
                        // Additional configuration
                        .build()


        // Schedule periodic work ******************************************************************
        val saveRequest = PeriodicWorkRequestBuilder<SaveImageToFileWorker>(1, TimeUnit.HOURS)
                        // Additional configuration
                        .build()


        // Periodic work request.*******************************************************************
        val myUploadWork = PeriodicWorkRequestBuilder<SaveImageToFileWorker>(
                1, TimeUnit.HOURS, // repeatInterval (the period cycle)
                15, TimeUnit.MINUTES) // flexInterval
                .build()


        // Constraint for work request.*************************************************************
        // To create a set of constraints and associate it with some work, create a Constraints
        // instance using the Contraints.Builder()
        // and assign it to your WorkRequest.Builder().
        val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresCharging(false)
                .build()

        val myWorkRequest2: WorkRequest = OneTimeWorkRequestBuilder<SaveImageToFileWorker>()
                        .setConstraints(constraints)
                        .build()


        // Delayed Work *****************************************************************************
        // Here is an example of how to set your work to run at least 10 minutes after it
        // has been enqueued.
        val myWorkRequest3 = OneTimeWorkRequestBuilder<MyWork>()
                .setInitialDelay(10, TimeUnit.MINUTES)
                .build()

        // Back off policy *************************************************************************
        val myWorkRequest4 = OneTimeWorkRequestBuilder<MyWork>()
                .setBackoffCriteria(
                        BackoffPolicy.LINEAR, // 10,20,30,40
                        OneTimeWorkRequest.MIN_BACKOFF_MILLIS, // Minimum 10 sec
                        TimeUnit.MILLISECONDS)
                .build()

        // Tag work ********************************************************************************
        // Every work request has a unique identifier, which can be used to identify that work
        // later in order to cancel the work or observe its progress.
        val myWorkRequest5 = OneTimeWorkRequestBuilder<MyWork>()
                .addTag("cleanup")
                .addTag("tag2")
                .addTag("tag3") // Multiple tags can be assigned at once.
                .build()


        // Create a WorkRequest for your Worker and sending it input *******************************
        val myUploadWork6 = OneTimeWorkRequestBuilder<UploadWork>()
                .setInputData(workDataOf(
                        "IMAGE_URI" to "http://..."
                ))
                .build()

        // second option for sending input data ****************************************************
        val dataBuilder : Data.Builder = Data.Builder()
        dataBuilder.putString("key", "value")
        dataBuilder.putInt("key", 34)
        dataBuilder.putStringArray("key", arrayOf("one", "two", "three"))
        dataBuilder.putIntArray("int_list", intArrayOf(1, 2, 3))
        val data : Data = dataBuilder.build()
        val myUploadWork7 = OneTimeWorkRequestBuilder<UploadWork>().setInputData(data).build()

        /*******************************************************************************************
         * Managing work
         * Once you’ve defined your Worker and your WorkRequest, the last step is to enqueue your
         * work. The simplest way to enqueue work is to call the WorkManager enqueue() method,
         * passing the WorkRequest you want to run.
         ******************************************************************************************/

        // val myWork: WorkRequest = // ... OneTime or PeriodicWork
        // WorkManager.getInstance(myContext).enqueue(myUploadWork6)



        // Unique Work *****************************************************************************
        // Unique work is a powerful concept that guarantees that you only have one instance of
        // work with a particular name at a time.
        // uniqueWorkName - A String used to uniquely identify the work request.
        // existingWorkPolicy - An enum which tells WorkManager what to do if there's already
        // an unfinished chain of work with that unique name. See conflict resolution policy
        // for more information.
        // work - the WorkRequest to schedule.
        // WorkManager.enqueueUniqueWork() // for one time work
        // WorkManager.enqueueUniquePeriodicWork() //for periodic work
        val sendLogsWorkRequest = PeriodicWorkRequestBuilder<SaveImageToFileWorker>(24, TimeUnit.HOURS)
                        .setConstraints(Constraints.Builder()
                                .setRequiresCharging(true)
                                .build()
                        ).addTag("tag_name")
                        .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "sendLogs", // Unique name
                ExistingPeriodicWorkPolicy.KEEP,
                sendLogsWorkRequest
        )

        // Observing your work
        // by id

        val workManager = WorkManager.getInstance(myContext)
        workManager.getWorkInfoById(sendLogsWorkRequest.id) // ListenableFuture<WorkInfo>

        // by name
        workManager.getWorkInfosForUniqueWork("sendLogs") // ListenableFuture<List<WorkInfo>>

        // by tag
        workManager.getWorkInfosByTag("tag_name") // ListenableFuture<List<WorkInfo>>


        workManager.getWorkInfoByIdLiveData(sendLogsWorkRequest.id)
                .observe(this) { workInfo ->
                    if(workInfo?.state == WorkInfo.State.SUCCEEDED) {
                        Log.d("tag", "Work is successfully done.")
                    }

                    val data = workInfo?.progress

                }

        // Complex work queries
        val workQuery = WorkQuery.Builder
                .fromTags(listOf("tag"))
                .addStates(listOf(WorkInfo.State.FAILED, WorkInfo.State.CANCELLED))
                .addUniqueWorkNames(listOf("preProcess", "sendLogs")
                )
                .build()

        val workInfos: ListenableFuture<List<WorkInfo>> = workManager.getWorkInfos(workQuery)


        /*******************************************************************************************
         * Cancelling and stopping work
         * If you no longer need your previously enqueued work to run, you can ask for it
         * to be cancelled. Work can be cancelled by its name, id or by a tag associated with it.
         ******************************************************************************************/
        // by id
        workManager.cancelWorkById(sendLogsWorkRequest.id)

        // by name
        workManager.cancelUniqueWork("sendLogs")

        // by tag
        workManager.cancelAllWorkByTag("tag")

        findViewById<Button>(R.id.open_camera).setOnClickListener { openCamera() }


        /*******************************************************************************************
         * Observing work progress
         ******************************************************************************************/
        WorkManager.getInstance(applicationContext)
                // requestId is the WorkRequest id
                .getWorkInfoByIdLiveData(sendLogsWorkRequest.id)
                .observe(this, { workInfo: WorkInfo? ->
                    if (workInfo != null) {
                        val progress = workInfo.progress
                        val value = progress.getInt(Progress, 0)
                        // Do something with progress information
                    }
                })


        /*******************************************************************************************
         * ArrayCreatingInputMerger
         * For the above example, given that we want to preserve the outputs from all plant name
         * Workers, we should use an ArrayCreatingInputMerger.
         ******************************************************************************************/

        val cache: OneTimeWorkRequest = OneTimeWorkRequestBuilder<SaveDataToRoomDatabase>()
                .setInputMerger(ArrayCreatingInputMerger::class) // The second option OverwritingInputMerger
                .setConstraints(constraints)
                .build()


        /*******************************************************************************************
         *                              enable Logging
         * To determine why your workers aren't running properly, it can be very useful to look
         * at verbose WorkManager logs. To enable logging, you need to use custom initialization.
         * First, disable the default WorkManagerInitializer in your AndroidManifest.xml by
         * creating a new WorkManager provider with the manifest-merge rule remove applied:
         ******************************************************************************************/


        class CoroutineDownloadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

            override suspend fun doWork(): Result  {

               return Result.success()
            }
        }

    }

    class DownloadWorker(private val mContext: Context, parameters: WorkerParameters) : CoroutineWorker(mContext, parameters) {

        private val notificationManager = mContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        override suspend fun doWork(): Result {
            val inputUrl = inputData.getString(KEY_INPUT_URL) ?: return Result.failure()
            val outputFile = inputData.getString(KEY_OUTPUT_FILE_NAME) ?: return Result.failure()
            // Mark the Worker as important
            val progress = "Starting Download"
            setForeground(createForegroundInfo(progress))
            download(inputUrl, outputFile)
            return Result.success()
        }

        private fun download(inputUrl: String, outputFile: String) {
            // Downloads a file and updates bytes read
            // Calls setForegroundInfo() periodically when it needs to update
            // the ongoing Notification
        }
        // Creates an instance of ForegroundInfo which can be used to update the
        // ongoing notification.
        private fun createForegroundInfo(progress: String): ForegroundInfo {
            val id = mContext.getString(R.string.notification_channel_id)
            val title = mContext.getString(R.string.notification_title)
            val cancel = mContext.getString(R.string.cancel_download)
            // This PendingIntent can be used to cancel the worker
            val intent = WorkManager.getInstance(applicationContext).createCancelPendingIntent(getId())

            // Create a Notification channel if necessary
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                createChannel(id)
            }

            val notification = NotificationCompat.Builder(mContext, id)
                    .setContentTitle(title)
                    .setTicker(title)
                    .setContentText(progress)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setOngoing(true)
                    // Add the cancel action to the notification which can
                    // be used to cancel the worker
                    .addAction(android.R.drawable.ic_delete, cancel, intent)
                    .build()

            with(NotificationManagerCompat.from(mContext)) {
                // notificationId is a unique int for each notification that you must define
                notify(DOWNLOAD_NOTIFICATION_ID, notification)
            }

            return ForegroundInfo(1,notification)
        }

        @RequiresApi(Build.VERSION_CODES.O)
        private fun createChannel(channel_id : String) {
            // Create the NotificationChannel, but only on API 26+ because
            // the NotificationChannel class is new and not in the support library
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val name =  applicationContext.getString(R.string.channel_name)
                val descriptionText = applicationContext.getString(R.string.channel_description)
                val importance = NotificationManager.IMPORTANCE_DEFAULT
                val channel = NotificationChannel(mContext.getString(R.string.notification_channel_id), name, importance).apply {
                    description = descriptionText
                }
                // Register the channel with the system
                notificationManager.createNotificationChannel(channel)
            }

        }

        companion object {
            const val KEY_INPUT_URL = "KEY_INPUT_URL"
            const val KEY_OUTPUT_FILE_NAME = "KEY_OUTPUT_FILE_NAME"
            const val DOWNLOAD_NOTIFICATION_ID = 12
        }
    }



    class SleepWorker(context: Context, parameters: WorkerParameters) :
            Worker(context, parameters) {

        override fun doWork(): Result {
            // Sleep on a background thread.
            Thread.sleep(1000)
            return Result.success()
        }
    }

    class SaveDataToRoomDatabase(appContext: Context, workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams){
        override suspend fun doWork(): Result {
            Log.d("tag", "Hey I am doing coroutine work")

            return Result.success()
        }
    }

    class SaveImageToFileWorker(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {
        override fun doWork(): Result {
            doYourScheduleWork()
            return Result.success()
        }

        private fun doYourScheduleWork(){
            Log.d("tag", "I am doing schedule work")
        }
    }

    class UploadWorker(appContext: Context, workerParams: WorkerParameters): Worker(appContext, workerParams) {
        override fun doWork(): Result {

            // Do the work here--in this case, upload the images.
            uploadImages()

            // Indicate whether the work finished successfully with the Result
            return Result.success()
        }
        private fun uploadImages(){
            Log.d("tag", "uploading image to server.")
        }
    }

    class MyWork(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {
        override fun doWork(): Result {
            doYourScheduleWork()
            return Result.success()
        }

        private fun doYourScheduleWork(){
            Log.d("tag", "I am doing schedule work")
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Define the Worker requiring input
    class UploadWork(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {

        override fun doWork(): Result {
            val imageUriInput = inputData.getString("IMAGE_URI") ?: return Result.failure()

            uploadFile(imageUriInput)
            return Result.success()
        }

        private fun uploadFile(imageUriInput: String){
            Log.d("tag", "uploading image : $imageUriInput")
        }

        private fun createOutPutData(){
            val dataBuilder : Data.Builder = Data.Builder()
            dataBuilder.putString("key", "value")
            dataBuilder.putInt("key", 34)
            dataBuilder.putStringArray("key", arrayOf("one", "two", "three"))
            dataBuilder.putIntArray("int_list", intArrayOf(1, 2, 3))
            dataBuilder.build()
            /***************************************************************************************
             * if you have you own data class for instance below class then you can you put.
             * data class MyModel(val id : Int,val name : String)
             **************************************************************************************/

        }

        data class MyModel(val id: Int, val name: String)

    }



    class ProgressWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {

        companion object {
            const val Progress = "Progress"
            private const val delayDuration = 1L
        }

        override suspend fun doWork(): Result {
            val firstUpdate = workDataOf(Progress to 0)
            val lastUpdate = workDataOf(Progress to 100)
            setProgress(firstUpdate)
            delay(delayDuration)
            setProgress(lastUpdate)
            return Result.success()
        }
    }


    private fun openCamera(){


        val getContent = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) {
            // Handle the returned Uri
            Log.d("tag", "bitmap:${it.width} ")
        }
        // Pass in the mime type you'd like to allow the user to select
        // as the input

        val requestPermission =
                registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                    // Do something if permission granted
                    if (isGranted) getContent.launch()

                }

        requestPermission.launch(Manifest.permission.CAMERA)

    }



}

