package com.balance.workmanagerexample

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.work.*
import java.util.concurrent.TimeUnit
import kotlin.contracts.Returns

class MainActivity : AppCompatActivity() {

    private lateinit var myContext : Context

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        myContext = this



        // For simple work, which requires no additional configuration, use the static method from
        val myWorkRequest = OneTimeWorkRequest.from(UploadWorker::class.java)

        // For more complex work request.***********************************************************
        val uploadWorkRequest: WorkRequest =
                OneTimeWorkRequestBuilder<UploadWorker>()
                        // Additional configuration
                        .build()
        WorkManager.getInstance(myContext).enqueue(uploadWorkRequest)


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


        /*******************************************************************************************
         * Managing work
         * Once you’ve defined your Worker and your WorkRequest, the last step is to enqueue your
         * work. The simplest way to enqueue work is to call the WorkManager enqueue() method,
         * passing the WorkRequest you want to run.
         ******************************************************************************************/

        // val myWork: WorkRequest = // ... OneTime or PeriodicWork
        WorkManager.getInstance(myContext).enqueue(myUploadWork6)



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
                        )
                        .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "sendLogs",
                ExistingPeriodicWorkPolicy.KEEP,
                sendLogsWorkRequest
        )


    }


    class SaveImageToFileWorker(appContext : Context, workerParams: WorkerParameters) : Worker(appContext,workerParams) {
        override fun doWork(): Result {
            doYourScheduleWork()
            return Result.success()
        }

        private fun doYourScheduleWork(){
            Log.d("tag","I am doing schedule work")
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
            Log.d("tag","uploading image to server.")
        }
    }

    class MyWork(appContext : Context, workerParams: WorkerParameters) : Worker(appContext,workerParams) {
        override fun doWork(): Result {
            doYourScheduleWork()
            return Result.success()
        }

        private fun doYourScheduleWork(){
            Log.d("tag","I am doing schedule work")
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

        private fun uploadFile(imageUriInput : String){
            Log.d("tag","uploading image : $imageUriInput")
        }

    }



}

