package io.github.iammosab.tubeguard.jobs

import android.app.job.JobParameters
import android.app.job.JobService
import io.github.iammosab.tubeguard.App

class CheckJob : JobService() {
    override fun onStartJob(params: JobParameters): Boolean {
        val app = App.of(this)
        when (params.jobId) {
            Scheduler.CHECK -> app.checkNow("periodic")
        }
        app.background { jobFinished(params, false) }
        return true
    }

    override fun onStopJob(params: JobParameters) = true
}
