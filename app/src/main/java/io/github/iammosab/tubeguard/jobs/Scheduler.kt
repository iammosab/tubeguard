package io.github.iammosab.tubeguard.jobs

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context

/** The backup to the watchdog: a YouTube check every 3 hours and an update check once a day. */
object Scheduler {
    const val CHECK = 1
    const val UPDATE = 2

    fun schedule(context: Context) {
        val jobs = context.getSystemService(JobScheduler::class.java)
        val service = ComponentName(context, CheckJob::class.java)
        if (jobs.getPendingJob(CHECK) == null) {
            jobs.schedule(JobInfo.Builder(CHECK, service).setPeriodic(3 * HOUR).setPersisted(true).build())
        }
        if (jobs.getPendingJob(UPDATE) == null) {
            jobs.schedule(
                JobInfo.Builder(UPDATE, service).setPeriodic(24 * HOUR).setPersisted(true)
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).build()
            )
        }
    }

    fun pending(context: Context): Boolean =
        context.getSystemService(JobScheduler::class.java).let { it.getPendingJob(CHECK) != null && it.getPendingJob(UPDATE) != null }

    fun cancel(context: Context) {
        context.getSystemService(JobScheduler::class.java).apply { cancel(CHECK); cancel(UPDATE) }
    }

    private const val HOUR = 3_600_000L
}
