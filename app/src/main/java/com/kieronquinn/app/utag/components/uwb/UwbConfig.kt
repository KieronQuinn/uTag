package com.kieronquinn.app.utag.components.uwb

import androidx.core.uwb.UwbAddress
import com.google.uwb.support.fira.FiraOpenSessionParams
import com.google.uwb.support.fira.FiraParams
import com.google.uwb.support.fira.FiraProtocolVersion
import com.kieronquinn.app.utag.utils.LengthByteArray
import java.util.Random

/**
 *  UWB Config generator for the SmartTags. Uses a random address, a specified preamble code index
 *  and channel 9 (this seems to be hardcoded in for some reason, so we're matching it).
 */
class UwbConfig(@FiraParams.UwbPreambleCodeIndex private val preambleCodeIndex: Int) {

    companion object {
        private val random = Random()
        private val DUMMY_ADDRESS = UwbAddress(byteArrayOf(0, 0))

        private fun generateSessionId(): Int {
            var randomSessionId = random.nextInt()
            if(randomSessionId == 0) {
                randomSessionId += 1
            }
            return randomSessionId
        }

        private fun generateRandomAddress(): Short {
            return (random.nextInt() and 0xFFFF).toShort()
        }
    }

    /**
     *  Fira session params for the SmartTag to communicate over UWB. Only fields that are not the
     *  defaults are specified here, most are the same as those in the builder. Values which are
     *  different were obtained by dumping the session open bundle from a device, as well as
     *  experimenting to match the bundle produced by AndroidX UWB.
     */
    private val params = FiraOpenSessionParams.Builder().apply {
        val address = generateRandomAddress()
        setIsTxAdaptivePayloadPowerEnabled(false)
        setDeviceRole(FiraParams.RANGING_DEVICE_ROLE_RESPONDER)
        setDeviceType(FiraParams.RANGING_DEVICE_TYPE_CONTROLEE)
        setProtocolVersion(
            FiraProtocolVersion(
                1,
                1
            )
        )
        setSlotsPerRangingRound(6)
        setVendorId(byteArrayOf(0, 0))
        setRangingIntervalMs(240)
        setDestAddressList(listOf(UwbAddress(address.toBigEndianBytes())))
        //Device address gets overridden later
        setDeviceAddress(DUMMY_ADDRESS)
        setSfdId(FiraParams.SFD_ID_VALUE_2)
        setFilterType(FiraParams.FILTER_TYPE_NONE)
        setInBandTerminationAttemptCount(3)
        setAoaResultRequest(FiraParams.AOA_RESULT_REQUEST_MODE_REQ_AOA_RESULTS)
        setChannelNumber(FiraParams.UWB_CHANNEL_9)
        setStaticStsIV(byteArrayOf(0, 0, 0, 0, 0, 0))
        setPreambleCodeIndex(preambleCodeIndex)
        setSessionId(generateSessionId())
        setHoppingMode(1)
        setHasRangingResultReportMessage(false)
        setMultiNodeMode(FiraParams.MULTI_NODE_MODE_UNICAST)
    }.build()

    /**
     *  Converts [params] into the data blob expected by a SmartTag. The expected indexes, lengths
     *  and formats of this array were reversed from SmartThings.
     */
    fun getBytes(address: UwbAddress): ByteArray {
        return LengthByteArray(35).apply {
            set(8, 1.toByte(), 1)
            set(9, 1.toByte(), 1)
            set(10, 1.toByte(), 1)
            set(11, 1.toByte(), 1)
            set(12, params.rangingRoundUsage.toByte(), 2)
            set(22, params.multiNodeMode.toByte(), 2)
            set(16, params.fcsType.toByte(), 2)
            set(18, 3.toByte(), 2)
            set(20, params.prfMode.toByte(), 2)
            set(22, params.stsConfig.toByte(), 1)
            set(23, params.isKeyRotationEnabled.toByte(), 1)
            set(24, params.macAddressMode.toByte(), 2)
            set(26, params.rframeConfig.toByte(), 2)
            set(28, params.preambleDuration.toByte(), 1)
            set(29, params.psduDataRate.toByte(), 1)
            set(30, params.sfdId.toByte(), 2)
            set(32, params.stsSegmentCount.toByte(), 2)
            set(34, params.hoppingMode.toByte(), 1)
            set(40, params.sessionId.toBigEndianBytes(), 4)
            setShort(72, params.destAddressList.first().address)
            setShort(88, address.address)
            set(104, params.channelNumber.toByte())
            //DIFFERENT TO PARAMS, SLOT DURATION 2000 != DEFAULT OF 2400 IN PARAMS
            setShort(112, 2000.toShort().toBigEndianBytes())
            setShort(128, params.rangingIntervalMs.toShort().toBigEndianBytes())
            set(144, 3.toByte())
            set(152, preambleCodeIndex.toByte())
            set(160, 1.toByte())
            set(168, 5.toByte())
            setShort(176, 0.toShort().toBigEndianBytes())
            set(192, params.staticStsIV as ByteArray, 6)
            set(240, params.slotsPerRangingRound.toByte())
            set(248, byteArrayOf(0, 0, 0, 0), 4)
        }.getBytes()
    }

    fun getParams(): FiraOpenSessionParams {
        return params
    }

    /**
     *  Converts an integer to its bytes representation in big-endian order
     */
    private fun Int.toBigEndianBytes(): ByteArray {
        return byteArrayOf(
            (this shr 24).toByte(),
            (this shr 16).toByte(),
            (this shr 8).toByte(),
            this.toByte()
        )
    }

    /**
     *  Converts a short into its bytes representation in big-endian order
     */
    private fun Short.toBigEndianBytes(): ByteArray {
        return byteArrayOf(
            (this.toInt() shr 8).toByte(),
            (this.toInt() and 0xFF).toByte()
        )
    }

    /**
     *  Convenience method which converts [Boolean] -> [Int] -> [Byte]
     */
    private fun Boolean.toByte(): Byte {
        return (if(this) 1 else 0).toByte()
    }

}
