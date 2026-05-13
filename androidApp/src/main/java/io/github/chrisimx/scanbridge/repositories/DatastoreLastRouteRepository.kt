package io.github.chrisimx.scanbridge.repositories

import android.content.Context
import com.google.protobuf.StringValue
import io.github.chrisimx.scanbridge.LastRouteRepository
import io.github.chrisimx.scanbridge.datastore.lastRouteStore
import io.github.chrisimx.scanbridge.proto.copy
import io.github.chrisimx.scanbridge.proto.lastRouteOrNull
import kotlinx.coroutines.flow.firstOrNull

class DatastoreLastRouteRepository(context: Context) : LastRouteRepository {
    val lastRouteStore = context.lastRouteStore
    override suspend fun getLastRoute(): String? = lastRouteStore.data.firstOrNull()?.lastRouteOrNull?.value

    override suspend fun setLastRoute(route: String?) {
        if (route != null) {
            lastRouteStore.updateData {
                it.copy {
                    lastRoute = StringValue.of(route)
                }
            }
        } else {
            lastRouteStore.updateData {
                lastRouteStore.updateData {
                    it.copy {
                        clearLastRoute()
                    }
                }
            }
        }
    }
}
