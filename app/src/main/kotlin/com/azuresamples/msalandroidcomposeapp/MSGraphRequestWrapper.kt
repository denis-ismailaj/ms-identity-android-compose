package com.azuresamples.msalandroidcomposeapp

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import org.json.JSONObject

object MSGraphRequestWrapper {

    private val httpClient: HttpClient by lazy {
        HttpClient {
            expectSuccess = true
        }
    }

    // See: https://docs.microsoft.com/en-us/graph/deployments#microsoft-graph-and-graph-explorer-service-root-endpoints
    const val MS_GRAPH_ROOT_ENDPOINT = "https://graph.microsoft.com/"

    /**
     * Use Ktor to make an HTTP request with
     * 1) a given MSGraph resource URL
     * 2) an access token
     * to obtain MSGraph data.
     */
    suspend fun callGraphAPI(
        graphResourceUrl: String,
        accessToken: String
    ): JSONObject {
        Log.d(TAG, "Sending request to graph")

        val response: HttpResponse = httpClient.get(graphResourceUrl) {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
        }

        return JSONObject(response.bodyAsText())
    }

    private val TAG = this::class.java.simpleName
}
