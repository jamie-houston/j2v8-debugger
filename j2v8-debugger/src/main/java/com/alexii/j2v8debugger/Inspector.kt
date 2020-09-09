package com.alexii.j2v8debugger

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

internal class LoggingInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request: Request = chain.request()
        val t1 = System.nanoTime()
        println(
            String.format(
                "Sending request %s on %s%n%s",
                request.url, chain.connection(), request.headers
            )
        )
        val response: Response = chain.proceed(request)
        val t2 = System.nanoTime()
        println(
            String.format(
                "Received response for %s in %.1fms%n%s",
                response.request.url, (t2 - t1) / 1e6, response.headers
            )
        )
        return response
    }
}

class Inspector {
    init{


        // Stetho Server (to listen for CDP)

        // Note that _devtools_remote is a magic suffix understood by Chrome which causes
        // the discovery process to begin.
//        val server = LocalSocketServer(
//            "main",
//            AddressNameHelper.createCustomAddress("_devtools_remote"),
//            LazySocketHandler(RealSocketHandlerFactory())
//        )

//        val serverManager = ServerManager(server)
//        serverManager.start()
    }

}