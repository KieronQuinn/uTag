package com.kieronquinn.app.utag.model

enum class ChaserRegion(private val id: Int, val url: String) {
    NA03D(1, "chaser-na03d-useast2.samsungiots.com"),
    NA03S(3, "chaser-na03s-useast2.samsungiots.com"),
    EU02S(5, "chaser-eu02s-euwest1.samsungiots.com"),
    NA03A(7, "chaser-na03a-useast2.samsungiots.com"),
    NA03(10, "chaser-na03-useast2.samsungiotcloud.com"),
    EU02(11, "chaser-eu02-euwest1.samsungiotcloud.com"),
    AP03(12, "chaser-ap03-apnortheast2.samsungiotcloud.com");

    companion object {
        fun getRegion(id: Int): ChaserRegion? {
            return entries.firstOrNull { it.id == id }
        }
    }
}