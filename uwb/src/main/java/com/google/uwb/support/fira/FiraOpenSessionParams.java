/*
 * Copyright (C) 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.uwb.support.fira;

import static com.google.commoncompat.base.Preconditions.checkArgument;
import static com.google.commoncompat.base.Preconditions.checkNotNull;
import static com.google.uwb.support.Constants.EXTENDED_ADDRESS_BYTE_LENGTH;
import static com.google.uwb.support.Constants.SHORT_ADDRESS_BYTE_LENGTH;

import static java.util.Objects.requireNonNull;

import android.os.PersistableBundle;
import androidx.core.uwb.UwbAddress;

import androidx.annotation.FloatRange;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.google.commoncompat.primitives.Longs;
import com.google.uwb.support.base.RequiredParam;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * UWB parameters used to open a FiRa session.
 *
 * <p>This is passed as a bundle to the service API {@link UwbManager#openRangingSession}.
 */
public class FiraOpenSessionParams extends FiraParams {
    private final FiraProtocolVersion mProtocolVersion;

    private final int mSessionId;
    @SessionType private final int mSessionType;
    @RangingDeviceType private final int mDeviceType;
    @RangingDeviceRole private final int mDeviceRole;
    @RangingRoundUsage private final int mRangingRoundUsage;
    @MultiNodeMode private final int mMultiNodeMode;

    private final UwbAddress mDeviceAddress;

    // Dest address list
    private final List<UwbAddress> mDestAddressList;

    // FiRa 1.0: Relative time (in milli-seconds).
    // FiRa 2.0: Relative time (in milli-seconds).
    private final long mInitiationTime;
    // FiRa 2.0: Absolute time in UWB time domain, as specified in CR-272 (in micro-seconds).
    private final long mAbsoluteInitiationTime;
    private final int mSlotDurationRstu;
    private final int mSlotsPerRangingRound;
    private final int mRangingIntervalMs;
    private final int mBlockStrideLength;
    private final int mHoppingMode;

    @IntRange(from = 0, to = 65535)
    private final int mMaxRangingRoundRetries;

    private final int mSessionPriority;
    @MacAddressMode final int mMacAddressMode;
    private final boolean mHasRangingResultReportMessage;
    private final boolean mHasControlMessage;
    private final boolean mHasRangingControlPhase;
    @MeasurementReportType private final int mMeasurementReportType;
    @MeasurementReportPhase private final int mMeasurementReportPhase;

    @IntRange(from = 1, to = 10)
    private final int mInBandTerminationAttemptCount;

    @UwbChannel private final int mChannelNumber;
    private final int mPreambleCodeIndex;
    @RframeConfig private final int mRframeConfig;
    @PrfMode private final int mPrfMode;
    private final byte[] mCapSize;
    @SchedulingMode private final int mScheduledMode;
    @PreambleDuration private final int mPreambleDuration;
    @SfdIdValue private final int mSfdId;
    @StsSegmentCountValue private final int mStsSegmentCount;
    @StsLength private final int mStsLength;

    // 16-byte or 32-byte long array
    @Nullable private final byte[] mSessionKey;

    // 16-byte or 32-byte long array
    @Nullable private final byte[] mSubSessionKey;

    @PsduDataRate private final int mPsduDataRate;
    @BprfPhrDataRate private final int mBprfPhrDataRate;
    @MacFcsType private final int mFcsType;
    private final boolean mIsTxAdaptivePayloadPowerEnabled;
    @StsConfig private final int mStsConfig;
    private final int mSubSessionId;
    @AoaType private final int mAoaType;

    // 2-byte long array
    @Nullable private final byte[] mVendorId;

    // 6-byte long array
    @Nullable private final byte[] mStaticStsIV;

    private final boolean mIsRssiReportingEnabled;
    private final boolean mIsDiagnosticsEnabled;
    private final byte mDiagramsFrameReportsFieldsFlags;
    private final byte mAntennaMode;
    private final boolean mIsKeyRotationEnabled;
    private final int mKeyRotationRate;
    @AoaResultRequestMode private final int mAoaResultRequest;
    @RangeDataNtfConfig private final int mRangeDataNtfConfig;
    private final int mRangeDataNtfProximityNear;
    private final int mRangeDataNtfProximityFar;
    private double mRangeDataNtfAoaAzimuthLower;
    private double mRangeDataNtfAoaAzimuthUpper;
    private double mRangeDataNtfAoaElevationLower;
    private double mRangeDataNtfAoaElevationUpper;
    private final boolean mHasTimeOfFlightReport;
    private final boolean mHasAngleOfArrivalAzimuthReport;
    private final boolean mHasAngleOfArrivalElevationReport;
    private final boolean mHasAngleOfArrivalFigureOfMeritReport;
    private final int mNumOfMsrmtFocusOnRange;
    private final int mNumOfMsrmtFocusOnAoaAzimuth;
    private final int mNumOfMsrmtFocusOnAoaElevation;
    private final Long mRangingErrorStreakTimeoutMs;
    private final int mLinkLayerMode;
    private final int mDataRepetitionCount;
    @RangingTimeStruct
    private final int mRangingTimeStruct;
    private final int mMinFramesPerRr;
    private final int mMtuSize;
    private final int mInterFrameInterval;
    private final int mDlTdoaBlockStriding;
    private final int mUlTdoaTxIntervalMs;
    private final int mUlTdoaRandomWindowMs;
    @UlTdoaDeviceIdType private final int mUlTdoaDeviceIdType;
    @Nullable private final byte[] mUlTdoaDeviceId;
    @UlTdoaTxTimestampType private final int mUlTdoaTxTimestampType;
    @FilterType private final int mFilterType;
    private final int mMaxNumberOfMeasurements;
    private final boolean mSessionDataTransferStatusNtfConfig;
    @Nullable private final int mReferenceTimeBase;
    @Nullable private final int mReferenceSessionHandle;
    @Nullable private final int mSessionOffsetInMicroSeconds;
    private final int mApplicationDataEndpoint;

    private static final int BUNDLE_VERSION_1 = 1;
    private static final int BUNDLE_VERSION_CURRENT = BUNDLE_VERSION_1;

    private static final String KEY_PROTOCOL_VERSION = "protocol_version";
    private static final String KEY_SESSION_ID = "session_id";
    private static final String KEY_SESSION_TYPE = "session_type";
    private static final String KEY_DEVICE_TYPE = "device_type";
    private static final String KEY_DEVICE_ROLE = "device_role";
    private static final String KEY_RANGING_ROUND_USAGE = "ranging_round_usage";
    private static final String KEY_MULTI_NODE_MODE = "multi_node_mode";
    private static final String KEY_DEVICE_ADDRESS = "device_address";
    private static final String KEY_DEST_ADDRESS_LIST = "dest_address_list";
    private static final String KEY_INITIATION_TIME_MS = "initiation_time_ms";
    private static final String KEY_ABSOLUTE_INITIATION_TIME_US = "absolute_initiation_time_us";
    private static final String KEY_SLOT_DURATION_RSTU = "slot_duration_rstu";
    private static final String KEY_SLOTS_PER_RANGING_ROUND = "slots_per_ranging_round";
    private static final String KEY_RANGING_INTERVAL_MS = "ranging_interval_ms";
    private static final String KEY_BLOCK_STRIDE_LENGTH = "block_stride_length";
    private static final String KEY_HOPPING_MODE = "hopping_mode";
    private static final String KEY_MAX_RANGING_ROUND_RETRIES = "max_ranging_round_retries";
    private static final String KEY_SESSION_PRIORITY = "session_priority";
    private static final String KEY_MAC_ADDRESS_MODE = "mac_address_mode";
    private static final String KEY_IN_BAND_TERMINATION_ATTEMPT_COUNT =
            "in_band_termination_attempt_count";
    private static final String KEY_CHANNEL_NUMBER = "channel_number";
    private static final String KEY_PREAMBLE_CODE_INDEX = "preamble_code_index";
    private static final String KEY_RFRAME_CONFIG = "rframe_config";
    private static final String KEY_PRF_MODE = "prf_mode";
    private static final String KEY_CAP_SIZE_RANGE = "cap_size_range";
    private static final String KEY_SCHEDULED_MODE = "scheduled_mode";
    private static final String KEY_PREAMBLE_DURATION = "preamble_duration";
    private static final String KEY_SFD_ID = "sfd_id";
    private static final String KEY_STS_SEGMENT_COUNT = "sts_segment_count";
    private static final String KEY_STS_LENGTH = "sts_length";
    private static final String KEY_SESSION_KEY = "session_key";
    private static final String KEY_SUBSESSION_KEY = "subsession_key";
    private static final String KEY_PSDU_DATA_RATE = "psdu_data_rate";
    private static final String KEY_BPRF_PHR_DATA_RATE = "bprf_phr_data_rate";
    private static final String KEY_FCS_TYPE = "fcs_type";
    private static final String KEY_IS_TX_ADAPTIVE_PAYLOAD_POWER_ENABLED =
            "is_tx_adaptive_payload_power_enabled";
    private static final String KEY_STS_CONFIG = "sts_config";
    private static final String KEY_SUB_SESSION_ID = "sub_session_id";
    private static final String KEY_VENDOR_ID = "vendor_id";
    private static final String KEY_STATIC_STS_IV = "static_sts_iv";
    private static final String KEY_IS_RSSI_REPORTING_ENABLED = "is_rssi_reporting_enabled";
    private static final String KEY_IS_DIAGNOSTICS_ENABLED = "is_diagnostics_enabled";
    private static final String KEY_DIAGRAMS_FRAME_REPORTS_FIELDS_FLAGS =
            "diagrams_frame_reports_fields_flags";
    private static final String KEY_IS_KEY_ROTATION_ENABLED = "is_key_rotation_enabled";
    private static final String KEY_KEY_ROTATION_RATE = "key_rotation_rate";
    private static final String KEY_AOA_RESULT_REQUEST = "aoa_result_request";
    private static final String KEY_RANGE_DATA_NTF_CONFIG = "range_data_ntf_config";
    private static final String KEY_RANGE_DATA_NTF_PROXIMITY_NEAR = "range_data_ntf_proximity_near";
    private static final String KEY_RANGE_DATA_NTF_PROXIMITY_FAR = "range_data_ntf_proximity_far";
    private static final String KEY_RANGE_DATA_NTF_AOA_AZIMUTH_LOWER =
            "range_data_ntf_aoa_azimuth_lower";
    private static final String KEY_RANGE_DATA_NTF_AOA_AZIMUTH_UPPER =
            "range_data_ntf_aoa_azimuth_upper";
    private static final String KEY_RANGE_DATA_NTF_AOA_ELEVATION_LOWER =
            "range_data_ntf_aoa_elevation_lower";
    private static final String KEY_RANGE_DATA_NTF_AOA_ELEVATION_UPPER =
            "range_data_ntf_aoa_elevation_upper";
    private static final String KEY_HAS_TIME_OF_FLIGHT_REPORT = "has_time_of_flight_report";
    private static final String KEY_HAS_ANGLE_OF_ARRIVAL_AZIMUTH_REPORT =
            "has_angle_of_arrival_azimuth_report";
    private static final String KEY_HAS_ANGLE_OF_ARRIVAL_ELEVATION_REPORT =
            "has_angle_of_arrival_elevation_report";
    private static final String KEY_HAS_ANGLE_OF_ARRIVAL_FIGURE_OF_MERIT_REPORT =
            "has_angle_of_arrival_figure_of_merit_report";
    // key value not the same as constant name to maintain backwards compatibility.
    private static final String KEY_HAS_RANGING_RESULT_REPORT_MESSAGE = "has_result_report_phase";
    private static final String KEY_HAS_CONTROL_MESSAGE = "has_control_message";
    private static final String KEY_HAS_RANGING_CONTROL_PHASE = "has_ranging_control_phase";
    private static final String KEY_MEASUREMENT_REPORT_TYPE = "measurement_report_type";
    private static final String KEY_MEASUREMENT_REPORT_PHASE = "measurement_report_phase";
    private static final String KEY_AOA_TYPE = "aoa_type";
    private static final String KEY_NUM_OF_MSRMT_FOCUS_ON_RANGE =
            "num_of_msrmt_focus_on_range";
    private static final String KEY_NUM_OF_MSRMT_FOCUS_ON_AOA_AZIMUTH =
            "num_of_msrmt_focus_on_aoa_azimuth";
    private static final String KEY_NUM_OF_MSRMT_FOCUS_ON_AOA_ELEVATION =
            "num_of_msrmt_focus_on_aoa_elevation";
    private static final String RANGING_ERROR_STREAK_TIMEOUT_MS =
            "ranging_error_streak_timeout_ms";
    private static final String KEY_LINK_LAYER_MODE =
            "link_layer_mode";
    private static final String KEY_DATA_REPETITION_COUNT = "data_repetition_count";
    private static final String KEY_RANGING_TIME_STRUCT = "ranging_time_struct";
    private static final String KEY_MIN_FRAMES_PER_RR =
            "min_frames_per_rr";
    private static final String KEY_MTU_SIZE =
            "mtu_size";
    private static final String KEY_INTER_FRAME_INTERVAL =
            "inter_frame_interval";
    private static final String KEY_DLTDOA_BLOCK_STRIDING = "dltdoa_block_striding";
    private static final String UL_TDOA_TX_INTERVAL = "ul_tdoa_tx_interval";
    private static final String UL_TDOA_RANDOM_WINDOW = "ul_tdoa_random_window";
    private static final String UL_TDOA_DEVICE_ID_TYPE = "ul_tdoa_device_id_type";
    private static final String UL_TDOA_DEVICE_ID = "ul_tdoa_device_id";
    private static final String UL_TDOA_TX_TIMESTAMP_TYPE = "ul_tdoa_tx_timestamp_type";
    private static final String KEY_FILTER_TYPE = "filter_type";
    private static final String KEY_MAX_NUMBER_OF_MEASUREMENTS = "max_number_of_measurements";
    private static final String KEY_SESSION_DATA_TRANSFER_STATUS_NTF_CONFIG =
            "session_data_transfer_status_ntf_config";
    private static final String KEY_REFERENCE_TIME_BASE = "reference_time_base";
    private static final String KEY_REFERENCE_SESSION_HANDLE = "reference_session_handle";
    private static final String KEY_SESSION_OFFSET_IN_MICRO_SECONDS =
                "session_offset_in_micro_seconds";
    private static final String KEY_APPLICATION_DATA_ENDPOINT = "application_data_endpoint";
    private static final String KEY_ANTENNA_MODE = "antenna_mode";

    private FiraOpenSessionParams(
            FiraProtocolVersion protocolVersion,
            int sessionId,
            @SessionType int sessionType,
            @RangingDeviceType int deviceType,
            @RangingDeviceRole int deviceRole,
            @RangingRoundUsage int rangingRoundUsage,
            @MultiNodeMode int multiNodeMode,
            UwbAddress deviceAddress,
            List<UwbAddress> destAddressList,
            long initiationTime,
            long absoluteInitiationTime,
            int slotDurationRstu,
            int slotsPerRangingRound,
            int rangingIntervalMs,
            int blockStrideLength,
            int hoppingMode,
            @IntRange(from = 0, to = 65535) int maxRangingRoundRetries,
            int sessionPriority,
            @MacAddressMode int macAddressMode,
            boolean hasRangingResultReportMessage,
            boolean hasControlMessage,
            boolean hasRangingControlPhase,
            @MeasurementReportType int measurementReportType,
            @MeasurementReportPhase int measurementReportPhase,
            @IntRange(from = 1, to = 10) int inBandTerminationAttemptCount,
            @UwbChannel int channelNumber,
            int preambleCodeIndex,
            @RframeConfig int rframeConfig,
            @PrfMode int prfMode,
            byte[] capSize,
            @SchedulingMode int scheduledMode,
            @PreambleDuration int preambleDuration,
            @SfdIdValue int sfdId,
            @StsSegmentCountValue int stsSegmentCount,
            @StsLength int stsLength,
            @Nullable byte[] sessionKey,
            @Nullable byte[] subsessionKey,
            @PsduDataRate int psduDataRate,
            @BprfPhrDataRate int bprfPhrDataRate,
            @MacFcsType int fcsType,
            boolean isTxAdaptivePayloadPowerEnabled,
            @StsConfig int stsConfig,
            int subSessionId,
            @Nullable byte[] vendorId,
            @Nullable byte[] staticStsIV,
            boolean isRssiReportingEnabled,
            boolean isDiagnosticsEnabled,
            byte diagramsFrameReportsFieldsFlags,
            @AntennaMode byte antennaMode,
            boolean isKeyRotationEnabled,
            int keyRotationRate,
            @AoaResultRequestMode int aoaResultRequest,
            @RangeDataNtfConfig int rangeDataNtfConfig,
            int rangeDataNtfProximityNear,
            int rangeDataNtfProximityFar,
            double rangeDataNtfAoaAzimuthLower,
            double rangeDataNtfAoaAzimuthUpper,
            double rangeDataNtfAoaElevationLower,
            double rangeDataNtfAoaElevationUpper,
            boolean hasTimeOfFlightReport,
            boolean hasAngleOfArrivalAzimuthReport,
            boolean hasAngleOfArrivalElevationReport,
            boolean hasAngleOfArrivalFigureOfMeritReport,
            @AoaType int aoaType,
            int numOfMsrmtFocusOnRange,
            int numOfMsrmtFocusOnAoaAzimuth,
            int numOfMsrmtFocusOnAoaElevation,
            Long rangingErrorStreakTimeoutMs,
            int linkLayerMode,
            int dataRepetitionCount,
            @RangingTimeStruct int rangingTimeStruct,
            int minFramePerRr,
            int mtuSize,
            int interFrameInterval,
            int dlTdoaBlockStriding,
            int ulTdoaTxIntervalMs,
            int ulTdoaRandomWindowMs,
            int ulTdoaDeviceIdType,
            @Nullable byte[] ulTdoaDeviceId,
            int ulTdoaTxTimestampType,
            int filterType,
            int maxNumberOfMeasurements,
            boolean sessionDataTransferStatusNtfConfig,
            int referenceTimeBase,
            int referenceSessionHandle,
            int sessionOffsetInMicroSecond,
            int applicationDataEndpoint) {
        mProtocolVersion = protocolVersion;
        mSessionId = sessionId;
        mSessionType = sessionType;
        mDeviceType = deviceType;
        mDeviceRole = deviceRole;
        mRangingRoundUsage = rangingRoundUsage;
        mMultiNodeMode = multiNodeMode;
        mDeviceAddress = deviceAddress;
        mDestAddressList = destAddressList;
        mInitiationTime = initiationTime;
        mAbsoluteInitiationTime = absoluteInitiationTime;
        mSlotDurationRstu = slotDurationRstu;
        mSlotsPerRangingRound = slotsPerRangingRound;
        mRangingIntervalMs = rangingIntervalMs;
        mBlockStrideLength = blockStrideLength;
        mHoppingMode = hoppingMode;
        mMaxRangingRoundRetries = maxRangingRoundRetries;
        mSessionPriority = sessionPriority;
        mMacAddressMode = macAddressMode;
        mHasRangingResultReportMessage = hasRangingResultReportMessage;
        mHasControlMessage = hasControlMessage;
        mHasRangingControlPhase = hasRangingControlPhase;
        mMeasurementReportType = measurementReportType;
        mMeasurementReportPhase = measurementReportPhase;
        mInBandTerminationAttemptCount = inBandTerminationAttemptCount;
        mChannelNumber = channelNumber;
        mPreambleCodeIndex = preambleCodeIndex;
        mRframeConfig = rframeConfig;
        mPrfMode = prfMode;
        mCapSize = capSize;
        mScheduledMode = scheduledMode;
        mPreambleDuration = preambleDuration;
        mSfdId = sfdId;
        mStsSegmentCount = stsSegmentCount;
        mStsLength = stsLength;
        mSessionKey = sessionKey;
        mSubSessionKey = subsessionKey;
        mPsduDataRate = psduDataRate;
        mBprfPhrDataRate = bprfPhrDataRate;
        mFcsType = fcsType;
        mIsTxAdaptivePayloadPowerEnabled = isTxAdaptivePayloadPowerEnabled;
        mStsConfig = stsConfig;
        mSubSessionId = subSessionId;
        mVendorId = vendorId;
        mStaticStsIV = staticStsIV;
        mIsRssiReportingEnabled = isRssiReportingEnabled;
        mIsDiagnosticsEnabled = isDiagnosticsEnabled;
        mDiagramsFrameReportsFieldsFlags = diagramsFrameReportsFieldsFlags;
        mAntennaMode = antennaMode;
        mIsKeyRotationEnabled = isKeyRotationEnabled;
        mKeyRotationRate = keyRotationRate;
        mAoaResultRequest = aoaResultRequest;
        mRangeDataNtfConfig = rangeDataNtfConfig;
        mRangeDataNtfProximityNear = rangeDataNtfProximityNear;
        mRangeDataNtfProximityFar = rangeDataNtfProximityFar;
        mRangeDataNtfAoaAzimuthLower = rangeDataNtfAoaAzimuthLower;
        mRangeDataNtfAoaAzimuthUpper = rangeDataNtfAoaAzimuthUpper;
        mRangeDataNtfAoaElevationLower = rangeDataNtfAoaElevationLower;
        mRangeDataNtfAoaElevationUpper = rangeDataNtfAoaElevationUpper;
        mHasTimeOfFlightReport = hasTimeOfFlightReport;
        mHasAngleOfArrivalAzimuthReport = hasAngleOfArrivalAzimuthReport;
        mHasAngleOfArrivalElevationReport = hasAngleOfArrivalElevationReport;
        mHasAngleOfArrivalFigureOfMeritReport = hasAngleOfArrivalFigureOfMeritReport;
        mAoaType = aoaType;
        mNumOfMsrmtFocusOnRange = numOfMsrmtFocusOnRange;
        mNumOfMsrmtFocusOnAoaAzimuth = numOfMsrmtFocusOnAoaAzimuth;
        mNumOfMsrmtFocusOnAoaElevation = numOfMsrmtFocusOnAoaElevation;
        mRangingErrorStreakTimeoutMs = rangingErrorStreakTimeoutMs;
        mLinkLayerMode = linkLayerMode;
        mDataRepetitionCount = dataRepetitionCount;
        mRangingTimeStruct = rangingTimeStruct;
        mMinFramesPerRr = minFramePerRr;
        mMtuSize = mtuSize;
        mInterFrameInterval = interFrameInterval;
        mDlTdoaBlockStriding = dlTdoaBlockStriding;
        mUlTdoaTxIntervalMs = ulTdoaTxIntervalMs;
        mUlTdoaRandomWindowMs = ulTdoaRandomWindowMs;
        mUlTdoaDeviceIdType = ulTdoaDeviceIdType;
        mUlTdoaDeviceId = ulTdoaDeviceId;
        mUlTdoaTxTimestampType = ulTdoaTxTimestampType;
        mFilterType = filterType;
        mMaxNumberOfMeasurements = maxNumberOfMeasurements;
        mSessionDataTransferStatusNtfConfig = sessionDataTransferStatusNtfConfig;
        mReferenceTimeBase = referenceTimeBase;
        mReferenceSessionHandle = referenceSessionHandle;
        mSessionOffsetInMicroSeconds = sessionOffsetInMicroSecond;
        mApplicationDataEndpoint = applicationDataEndpoint;
    }

    @Override
    protected int getBundleVersion() {
        return BUNDLE_VERSION_CURRENT;
    }

    public int getSessionId() {
        return mSessionId;
    }

    @SessionType
    public int getSessionType() {
        return mSessionType;
    }

    @RangingDeviceType
    public int getDeviceType() {
        return mDeviceType;
    }

    @RangingDeviceRole
    public int getDeviceRole() {
        return mDeviceRole;
    }

    @RangingRoundUsage
    public int getRangingRoundUsage() {
        return mRangingRoundUsage;
    }

    @MultiNodeMode
    public int getMultiNodeMode() {
        return mMultiNodeMode;
    }

    public UwbAddress getDeviceAddress() {
        return mDeviceAddress;
    }

    public List<UwbAddress> getDestAddressList() {
        return mDestAddressList != null ? Collections.unmodifiableList(mDestAddressList) : null;
    }

    public long getInitiationTime() {
        return mInitiationTime;
    }

    public long getAbsoluteInitiationTime() {
        return mAbsoluteInitiationTime;
    }

    public int getSlotDurationRstu() {
        return mSlotDurationRstu;
    }

    public int getSlotsPerRangingRound() {
        return mSlotsPerRangingRound;
    }

    public int getRangingIntervalMs() {
        return mRangingIntervalMs;
    }

    public int getBlockStrideLength() {
        return mBlockStrideLength;
    }

    public int getHoppingMode() {
        return mHoppingMode;
    }

    @IntRange(from = 0, to = 65535)
    public int getMaxRangingRoundRetries() {
        return mMaxRangingRoundRetries;
    }

    public int getSessionPriority() {
        return mSessionPriority;
    }

    @MacAddressMode
    public int getMacAddressMode() {
        return mMacAddressMode;
    }

    public boolean hasRangingResultReportMessage() {
        return mHasRangingResultReportMessage;
    }

    public boolean hasControlMessage() {
        return mHasControlMessage;
    }

    public boolean hasRangingControlPhase() {
        return mHasRangingControlPhase;
    }

    @MeasurementReportType
    public int getMeasurementReportType() {
        return mMeasurementReportType;
    }

    @MeasurementReportPhase
    public int getMeasurementReportPhase() {
        return mMeasurementReportPhase;
    }

    @IntRange(from = 1, to = 10)
    public int getInBandTerminationAttemptCount() {
        return mInBandTerminationAttemptCount;
    }

    @UwbChannel
    public int getChannelNumber() {
        return mChannelNumber;
    }

    public int getPreambleCodeIndex() {
        return mPreambleCodeIndex;
    }

    @RframeConfig
    public int getRframeConfig() {
        return mRframeConfig;
    }

    @PrfMode
    public int getPrfMode() {
        return mPrfMode;
    }

    public byte[] getCapSize() {
        return mCapSize;
    }

    @SchedulingMode
    public int getScheduledMode() {
        return mScheduledMode;
    }

    @PreambleDuration
    public int getPreambleDuration() {
        return mPreambleDuration;
    }

    @SfdIdValue
    public int getSfdId() {
        return mSfdId;
    }

    @StsSegmentCountValue
    public int getStsSegmentCount() {
        return mStsSegmentCount;
    }

    @StsLength
    public int getStsLength() {
        return mStsLength;
    }

    @Nullable
    public byte[] getSessionKey() {
        return mSessionKey;
    }

    @Nullable
    public byte[] getSubsessionKey() {
        return mSubSessionKey;
    }

    @PsduDataRate
    public int getPsduDataRate() {
        return mPsduDataRate;
    }

    @BprfPhrDataRate
    public int getBprfPhrDataRate() {
        return mBprfPhrDataRate;
    }

    @MacFcsType
    public int getFcsType() {
        return mFcsType;
    }

    public boolean isTxAdaptivePayloadPowerEnabled() {
        return mIsTxAdaptivePayloadPowerEnabled;
    }

    @StsConfig
    public int getStsConfig() {
        return mStsConfig;
    }

    public int getSubSessionId() {
        return mSubSessionId;
    }

    @Nullable
    public byte[] getVendorId() {
        return mVendorId;
    }

    @Nullable
    public byte[] getStaticStsIV() {
        return mStaticStsIV;
    }

    public boolean isRssiReportingEnabled() {
        return mIsRssiReportingEnabled;
    }

    public boolean isDiagnosticsEnabled() {
        return mIsDiagnosticsEnabled;
    }

    public byte getDiagramsFrameReportsFieldsFlags() {
        return mDiagramsFrameReportsFieldsFlags;
    }

    @AntennaMode
    public byte getAntennaMode() {
        return mAntennaMode;
    }

    public boolean isKeyRotationEnabled() {
        return mIsKeyRotationEnabled;
    }

    public int getKeyRotationRate() {
        return mKeyRotationRate;
    }

    @AoaResultRequestMode
    public int getAoaResultRequest() {
        return mAoaResultRequest;
    }

    @RangeDataNtfConfig
    public int getRangeDataNtfConfig() {
        return mRangeDataNtfConfig;
    }

    public int getRangeDataNtfProximityNear() {
        return mRangeDataNtfProximityNear;
    }

    public int getRangeDataNtfProximityFar() {
        return mRangeDataNtfProximityFar;
    }

    public double getRangeDataNtfAoaAzimuthLower() {
        return mRangeDataNtfAoaAzimuthLower;
    }

    public double getRangeDataNtfAoaAzimuthUpper() {
        return mRangeDataNtfAoaAzimuthUpper;
    }

    public double getRangeDataNtfAoaElevationLower() {
        return mRangeDataNtfAoaElevationLower;
    }

    public double getRangeDataNtfAoaElevationUpper() {
        return mRangeDataNtfAoaElevationUpper;
    }

    public boolean hasTimeOfFlightReport() {
        return mHasTimeOfFlightReport;
    }

    public boolean hasAngleOfArrivalAzimuthReport() {
        return mHasAngleOfArrivalAzimuthReport;
    }

    public boolean hasAngleOfArrivalElevationReport() {
        return mHasAngleOfArrivalElevationReport;
    }

    public boolean hasAngleOfArrivalFigureOfMeritReport() {
        return mHasAngleOfArrivalFigureOfMeritReport;
    }

    @AoaType
    public int getAoaType() {
        return mAoaType;
    }

    public int getNumOfMsrmtFocusOnRange() {
        return mNumOfMsrmtFocusOnRange;
    }

    public int getNumOfMsrmtFocusOnAoaAzimuth() {
        return mNumOfMsrmtFocusOnAoaAzimuth;
    }

    public int getNumOfMsrmtFocusOnAoaElevation() {
        return mNumOfMsrmtFocusOnAoaElevation;
    }

    public long getRangingErrorStreakTimeoutMs() {
        return mRangingErrorStreakTimeoutMs;
    }

    public int getLinkLayerMode() {
        return mLinkLayerMode;
    }

    public int getDataRepetitionCount() {
        return mDataRepetitionCount;
    }

    @RangingTimeStruct
    public int getRangingTimeStruct() {
        return mRangingTimeStruct;
    }

    public int getMinFramesPerRr() {
        return mMinFramesPerRr;
    }

    public int getMtuSize() {
        return mMtuSize;
    }

    public int getInterFrameInterval() {
        return mInterFrameInterval;
    }

    public int getDlTdoaBlockStriding() {
        return mDlTdoaBlockStriding;
    }

    public int getUlTdoaTxIntervalMs() {
        return mUlTdoaTxIntervalMs;
    }

    public int getUlTdoaRandomWindowMs() {
        return mUlTdoaRandomWindowMs;
    }

    public int getUlTdoaDeviceIdType() {
        return mUlTdoaDeviceIdType;
    }

    @Nullable
    public byte[] getUlTdoaDeviceId() {
        return mUlTdoaDeviceId;
    }

    public int getUlTdoaTxTimestampType() {
        return mUlTdoaTxTimestampType;
    }

    @FilterType
    public int getFilterType() {
        return mFilterType;
    }

    public int getMaxNumberOfMeasurements() { return mMaxNumberOfMeasurements; }

    public boolean getSessionDataTransferStatusNtfConfig() {
        return mSessionDataTransferStatusNtfConfig;
    }

    public int getReferenceTimeBase() {
        return mReferenceTimeBase;
    }

    public int getReferenceSessionHandle() {
        return mReferenceSessionHandle;
    }

    public int getSessionOffsetInMicroSeconds() {
        return mSessionOffsetInMicroSeconds;
    }

    public int getApplicationDataEndpoint() {
        return mApplicationDataEndpoint;
    }

    @Nullable
    private static int[] byteArrayToIntArray(@Nullable byte[] bytes) {
        if (bytes == null) {
            return null;
        }

        int[] values = new int[bytes.length];
        for (int i = 0; i < values.length; i++) {
            values[i] = bytes[i];
        }
        return values;
    }

    @Nullable
    private static byte[] intArrayToByteArray(@Nullable int[] values) {
        if (values == null) {
            return null;
        }
        byte[] bytes = new byte[values.length];
        for (int i = 0; i < values.length; i++) {
            bytes[i] = (byte) values[i];
        }
        return bytes;
    }

    @Override
    public PersistableBundle toBundle() {
        PersistableBundle bundle = super.toBundle();
        bundle.putString(KEY_PROTOCOL_VERSION, mProtocolVersion.toString());
        bundle.putInt(KEY_SESSION_ID, mSessionId);
        bundle.putInt(KEY_SESSION_TYPE, mSessionType);
        bundle.putInt(KEY_DEVICE_TYPE, mDeviceType);
        bundle.putInt(KEY_DEVICE_ROLE, mDeviceRole);
        bundle.putInt(KEY_RANGING_ROUND_USAGE, mRangingRoundUsage);
        bundle.putInt(KEY_MULTI_NODE_MODE, mMultiNodeMode);
        // Always store address as long in bundle.
        bundle.putLong(KEY_DEVICE_ADDRESS, uwbAddressToLong(mDeviceAddress));

        if (mScheduledMode != CONTENTION_BASED_RANGING
                && mDestAddressList != null) {
            // Dest Address list needs to be converted to long array.
            long[] destAddressList = new long[mDestAddressList.size()];
            int i = 0;
            for (UwbAddress destAddress : mDestAddressList) {
                destAddressList[i++] = uwbAddressToLong(destAddress);
            }
            bundle.putLongArray(KEY_DEST_ADDRESS_LIST, destAddressList);
        }

        if (mRangingRoundUsage == RANGING_ROUND_USAGE_DL_TDOA
                && mDeviceRole == RANGING_DEVICE_DT_TAG) {
            bundle.putInt(KEY_DLTDOA_BLOCK_STRIDING, mDlTdoaBlockStriding);
        }

        bundle.putLong(KEY_INITIATION_TIME_MS, mInitiationTime);
        bundle.putLong(KEY_ABSOLUTE_INITIATION_TIME_US, mAbsoluteInitiationTime);
        bundle.putInt(KEY_SLOT_DURATION_RSTU, mSlotDurationRstu);
        bundle.putInt(KEY_SLOTS_PER_RANGING_ROUND, mSlotsPerRangingRound);
        bundle.putInt(KEY_RANGING_INTERVAL_MS, mRangingIntervalMs);
        bundle.putInt(KEY_BLOCK_STRIDE_LENGTH, mBlockStrideLength);
        bundle.putInt(KEY_HOPPING_MODE, mHoppingMode);
        bundle.putInt(KEY_MAX_RANGING_ROUND_RETRIES, mMaxRangingRoundRetries);
        bundle.putInt(KEY_SESSION_PRIORITY, mSessionPriority);
        bundle.putInt(KEY_MAC_ADDRESS_MODE, mMacAddressMode);
        bundle.putBoolean(KEY_HAS_RANGING_RESULT_REPORT_MESSAGE, mHasRangingResultReportMessage);
        bundle.putBoolean(KEY_HAS_CONTROL_MESSAGE, mHasControlMessage);
        bundle.putBoolean(KEY_HAS_RANGING_CONTROL_PHASE, mHasRangingControlPhase);
        bundle.putInt(KEY_MEASUREMENT_REPORT_TYPE, mMeasurementReportType);
        bundle.putInt(KEY_MEASUREMENT_REPORT_PHASE, mMeasurementReportPhase);
        bundle.putInt(KEY_IN_BAND_TERMINATION_ATTEMPT_COUNT, mInBandTerminationAttemptCount);
        bundle.putInt(KEY_CHANNEL_NUMBER, mChannelNumber);
        bundle.putInt(KEY_PREAMBLE_CODE_INDEX, mPreambleCodeIndex);
        bundle.putInt(KEY_RFRAME_CONFIG, mRframeConfig);
        bundle.putInt(KEY_PRF_MODE, mPrfMode);
        bundle.putInt(KEY_SCHEDULED_MODE, mScheduledMode);
        if (mScheduledMode == CONTENTION_BASED_RANGING) {
            bundle.putIntArray(KEY_CAP_SIZE_RANGE, byteArrayToIntArray(mCapSize));
        }
        bundle.putInt(KEY_PREAMBLE_DURATION, mPreambleDuration);
        bundle.putInt(KEY_SFD_ID, mSfdId);
        bundle.putInt(KEY_STS_SEGMENT_COUNT, mStsSegmentCount);
        bundle.putInt(KEY_STS_LENGTH, mStsLength);
        bundle.putInt(KEY_PSDU_DATA_RATE, mPsduDataRate);
        bundle.putInt(KEY_BPRF_PHR_DATA_RATE, mBprfPhrDataRate);
        bundle.putInt(KEY_FCS_TYPE, mFcsType);
        bundle.putBoolean(
                KEY_IS_TX_ADAPTIVE_PAYLOAD_POWER_ENABLED, mIsTxAdaptivePayloadPowerEnabled);
        bundle.putInt(KEY_STS_CONFIG, mStsConfig);
        if (mStsConfig == STS_CONFIG_DYNAMIC_FOR_CONTROLEE_INDIVIDUAL_KEY
            || mStsConfig == STS_CONFIG_PROVISIONED_FOR_CONTROLEE_INDIVIDUAL_KEY) {
            bundle.putInt(KEY_SUB_SESSION_ID, mSubSessionId);
        }
        if (mSessionKey != null) {
            bundle.putIntArray(KEY_SESSION_KEY, byteArrayToIntArray(mSessionKey));
        }
        if (mSubSessionKey != null) {
            bundle.putIntArray(KEY_SUBSESSION_KEY, byteArrayToIntArray(mSubSessionKey));
        }
        bundle.putIntArray(KEY_VENDOR_ID, byteArrayToIntArray(mVendorId));
        bundle.putIntArray(KEY_STATIC_STS_IV, byteArrayToIntArray(mStaticStsIV));
        bundle.putBoolean(KEY_IS_RSSI_REPORTING_ENABLED, mIsRssiReportingEnabled);
        bundle.putBoolean(KEY_IS_DIAGNOSTICS_ENABLED, mIsDiagnosticsEnabled);
        bundle.putInt(KEY_DIAGRAMS_FRAME_REPORTS_FIELDS_FLAGS, mDiagramsFrameReportsFieldsFlags);
        bundle.putInt(KEY_ANTENNA_MODE, mAntennaMode);
        bundle.putBoolean(KEY_IS_KEY_ROTATION_ENABLED, mIsKeyRotationEnabled);
        bundle.putInt(KEY_KEY_ROTATION_RATE, mKeyRotationRate);
        bundle.putInt(KEY_AOA_RESULT_REQUEST, mAoaResultRequest);
        bundle.putInt(KEY_RANGE_DATA_NTF_CONFIG, mRangeDataNtfConfig);
        bundle.putInt(KEY_RANGE_DATA_NTF_PROXIMITY_NEAR, mRangeDataNtfProximityNear);
        bundle.putInt(KEY_RANGE_DATA_NTF_PROXIMITY_FAR, mRangeDataNtfProximityFar);
        bundle.putDouble(KEY_RANGE_DATA_NTF_AOA_AZIMUTH_LOWER, mRangeDataNtfAoaAzimuthLower);
        bundle.putDouble(KEY_RANGE_DATA_NTF_AOA_AZIMUTH_UPPER, mRangeDataNtfAoaAzimuthUpper);
        bundle.putDouble(KEY_RANGE_DATA_NTF_AOA_ELEVATION_LOWER, mRangeDataNtfAoaElevationLower);
        bundle.putDouble(KEY_RANGE_DATA_NTF_AOA_ELEVATION_UPPER, mRangeDataNtfAoaElevationUpper);
        bundle.putBoolean(KEY_HAS_TIME_OF_FLIGHT_REPORT, mHasTimeOfFlightReport);
        bundle.putBoolean(KEY_HAS_ANGLE_OF_ARRIVAL_AZIMUTH_REPORT, mHasAngleOfArrivalAzimuthReport);
        bundle.putBoolean(
                KEY_HAS_ANGLE_OF_ARRIVAL_ELEVATION_REPORT, mHasAngleOfArrivalElevationReport);
        bundle.putBoolean(
                KEY_HAS_ANGLE_OF_ARRIVAL_FIGURE_OF_MERIT_REPORT,
                mHasAngleOfArrivalFigureOfMeritReport);
        bundle.putInt(KEY_AOA_TYPE, mAoaType);
        bundle.putInt(KEY_NUM_OF_MSRMT_FOCUS_ON_RANGE, mNumOfMsrmtFocusOnRange);
        bundle.putInt(KEY_NUM_OF_MSRMT_FOCUS_ON_AOA_AZIMUTH, mNumOfMsrmtFocusOnAoaAzimuth);
        bundle.putInt(KEY_NUM_OF_MSRMT_FOCUS_ON_AOA_ELEVATION, mNumOfMsrmtFocusOnAoaElevation);
        bundle.putLong(RANGING_ERROR_STREAK_TIMEOUT_MS, mRangingErrorStreakTimeoutMs);
        bundle.putInt(KEY_LINK_LAYER_MODE, mLinkLayerMode);
        bundle.putInt(KEY_DATA_REPETITION_COUNT, mDataRepetitionCount);
        bundle.putInt(KEY_RANGING_TIME_STRUCT, mRangingTimeStruct);
        bundle.putInt(KEY_MIN_FRAMES_PER_RR, mMinFramesPerRr);
        bundle.putInt(KEY_MTU_SIZE, mMtuSize);
        bundle.putInt(KEY_INTER_FRAME_INTERVAL, mInterFrameInterval);
        bundle.putInt(UL_TDOA_TX_INTERVAL, mUlTdoaTxIntervalMs);
        bundle.putInt(UL_TDOA_RANDOM_WINDOW, mUlTdoaRandomWindowMs);
        bundle.putInt(UL_TDOA_DEVICE_ID_TYPE, mUlTdoaDeviceIdType);
        bundle.putIntArray(UL_TDOA_DEVICE_ID, byteArrayToIntArray(mUlTdoaDeviceId));
        bundle.putInt(UL_TDOA_TX_TIMESTAMP_TYPE, mUlTdoaTxTimestampType);
        bundle.putInt(KEY_FILTER_TYPE, mFilterType);
        bundle.putInt(KEY_MAX_NUMBER_OF_MEASUREMENTS, mMaxNumberOfMeasurements);
        bundle.putBoolean(
                KEY_SESSION_DATA_TRANSFER_STATUS_NTF_CONFIG, mSessionDataTransferStatusNtfConfig);
        if (mDeviceType == FiraParams.RANGING_DEVICE_TYPE_CONTROLLER) {
            bundle.putInt(KEY_REFERENCE_TIME_BASE, mReferenceTimeBase);
            bundle.putInt(KEY_REFERENCE_SESSION_HANDLE, mReferenceSessionHandle);
            bundle.putInt(KEY_SESSION_OFFSET_IN_MICRO_SECONDS, mSessionOffsetInMicroSeconds);
        }
        bundle.putInt(KEY_APPLICATION_DATA_ENDPOINT, mApplicationDataEndpoint);
        return bundle;
    }

    public static FiraOpenSessionParams fromBundle(PersistableBundle bundle) {
        if (!isCorrectProtocol(bundle)) {
            throw new IllegalArgumentException("Invalid protocol");
        }

        switch (getBundleVersion(bundle)) {
            case BUNDLE_VERSION_1:
                return parseBundleVersion1(bundle);

            default:
                throw new IllegalArgumentException("unknown bundle version");
        }
    }

    private static FiraOpenSessionParams parseBundleVersion1(PersistableBundle bundle) {
        int macAddressMode = bundle.getInt(KEY_MAC_ADDRESS_MODE);
        int addressByteLength = 2;
        if (macAddressMode == MAC_ADDRESS_MODE_8_BYTES) {
            addressByteLength = 8;
        }
        UwbAddress deviceAddress =
                longToUwbAddress(bundle.getLong(KEY_DEVICE_ADDRESS), addressByteLength);

        Builder builder = new Builder()
                .setProtocolVersion(
                        FiraProtocolVersion.fromString(
                                requireNonNull(bundle.getString(KEY_PROTOCOL_VERSION))))
                .setSessionId(bundle.getInt(KEY_SESSION_ID))
                .setSessionType(bundle.getInt(KEY_SESSION_TYPE, FiraParams.SESSION_TYPE_RANGING))
                .setDeviceType(bundle.getInt(KEY_DEVICE_TYPE))
                .setDeviceRole(bundle.getInt(KEY_DEVICE_ROLE))
                .setRangingRoundUsage(bundle.getInt(KEY_RANGING_ROUND_USAGE))
                .setMultiNodeMode(bundle.getInt(KEY_MULTI_NODE_MODE))
                .setDeviceAddress(deviceAddress)
                // Changed from int to long. Look for int value, if long value not found to
                // maintain backwards compatibility.
                .setInitiationTime(bundle.getLong(KEY_INITIATION_TIME_MS))
                .setAbsoluteInitiationTime(bundle.getLong(KEY_ABSOLUTE_INITIATION_TIME_US))
                .setSlotDurationRstu(bundle.getInt(KEY_SLOT_DURATION_RSTU))
                .setSlotsPerRangingRound(bundle.getInt(KEY_SLOTS_PER_RANGING_ROUND))
                .setRangingIntervalMs(bundle.getInt(KEY_RANGING_INTERVAL_MS))
                .setBlockStrideLength(bundle.getInt(KEY_BLOCK_STRIDE_LENGTH))
                .setHoppingMode(bundle.getInt(KEY_HOPPING_MODE))
                .setMaxRangingRoundRetries(bundle.getInt(KEY_MAX_RANGING_ROUND_RETRIES))
                .setSessionPriority(bundle.getInt(KEY_SESSION_PRIORITY))
                .setMacAddressMode(bundle.getInt(KEY_MAC_ADDRESS_MODE))
                .setHasRangingResultReportMessage(
                        bundle.getBoolean(KEY_HAS_RANGING_RESULT_REPORT_MESSAGE))
                .setHasControlMessage(
                        bundle.getBoolean(KEY_HAS_CONTROL_MESSAGE, true))
                .setHasRangingControlPhase(
                        bundle.getBoolean(KEY_HAS_RANGING_CONTROL_PHASE, false))
                .setMeasurementReportType(bundle.getInt(KEY_MEASUREMENT_REPORT_TYPE))
                .setMeasurementReportPhase(bundle.getInt(KEY_MEASUREMENT_REPORT_PHASE))
                .setInBandTerminationAttemptCount(
                        bundle.getInt(KEY_IN_BAND_TERMINATION_ATTEMPT_COUNT))
                .setChannelNumber(bundle.getInt(KEY_CHANNEL_NUMBER))
                .setPreambleCodeIndex(bundle.getInt(KEY_PREAMBLE_CODE_INDEX))
                .setRframeConfig(bundle.getInt(KEY_RFRAME_CONFIG))
                .setPrfMode(bundle.getInt(KEY_PRF_MODE))
                .setCapSize(intArrayToByteArray(bundle.getIntArray(KEY_CAP_SIZE_RANGE)))
                .setScheduledMode(bundle.getInt(KEY_SCHEDULED_MODE, TIME_SCHEDULED_RANGING))
                .setPreambleDuration(bundle.getInt(KEY_PREAMBLE_DURATION))
                .setSfdId(bundle.getInt(KEY_SFD_ID))
                .setStsSegmentCount(bundle.getInt(KEY_STS_SEGMENT_COUNT))
                .setStsLength(bundle.getInt(KEY_STS_LENGTH))
                .setSessionKey(intArrayToByteArray(bundle.getIntArray(KEY_SESSION_KEY)))
                .setSubsessionKey(intArrayToByteArray(bundle.getIntArray(KEY_SUBSESSION_KEY)))
                .setPsduDataRate(bundle.getInt(KEY_PSDU_DATA_RATE))
                .setBprfPhrDataRate(bundle.getInt(KEY_BPRF_PHR_DATA_RATE))
                .setFcsType(bundle.getInt(KEY_FCS_TYPE))
                .setIsTxAdaptivePayloadPowerEnabled(
                        bundle.getBoolean(KEY_IS_TX_ADAPTIVE_PAYLOAD_POWER_ENABLED))
                .setStsConfig(bundle.getInt(KEY_STS_CONFIG))
                .setSubSessionId(bundle.getInt(KEY_SUB_SESSION_ID))
                .setVendorId(intArrayToByteArray(bundle.getIntArray(KEY_VENDOR_ID)))
                .setStaticStsIV(intArrayToByteArray(bundle.getIntArray(KEY_STATIC_STS_IV)))
                .setIsRssiReportingEnabled(bundle.getBoolean(KEY_IS_RSSI_REPORTING_ENABLED))
                .setIsDiagnosticsEnabled(bundle.getBoolean(KEY_IS_DIAGNOSTICS_ENABLED, false))
                .setDiagramsFrameReportsFieldsFlags((byte)
                        bundle.getInt(KEY_DIAGRAMS_FRAME_REPORTS_FIELDS_FLAGS, 0))
                .setAntennaMode((byte) bundle.getInt(KEY_ANTENNA_MODE, ANTENNA_MODE_OMNI))
                .setIsKeyRotationEnabled(bundle.getBoolean(KEY_IS_KEY_ROTATION_ENABLED))
                .setKeyRotationRate(bundle.getInt(KEY_KEY_ROTATION_RATE))
                .setAoaResultRequest(bundle.getInt(KEY_AOA_RESULT_REQUEST))
                .setRangeDataNtfConfig(bundle.getInt(KEY_RANGE_DATA_NTF_CONFIG))
                .setRangeDataNtfProximityNear(bundle.getInt(KEY_RANGE_DATA_NTF_PROXIMITY_NEAR))
                .setRangeDataNtfProximityFar(bundle.getInt(KEY_RANGE_DATA_NTF_PROXIMITY_FAR))
                .setRangeDataNtfAoaAzimuthLower(
                        bundle.getDouble(KEY_RANGE_DATA_NTF_AOA_AZIMUTH_LOWER,
                                RANGE_DATA_NTF_AOA_AZIMUTH_LOWER_DEFAULT))
                .setRangeDataNtfAoaAzimuthUpper(
                        bundle.getDouble(KEY_RANGE_DATA_NTF_AOA_AZIMUTH_UPPER,
                                RANGE_DATA_NTF_AOA_AZIMUTH_UPPER_DEFAULT))
                .setRangeDataNtfAoaElevationLower(
                        bundle.getDouble(KEY_RANGE_DATA_NTF_AOA_ELEVATION_LOWER,
                                RANGE_DATA_NTF_AOA_ELEVATION_LOWER_DEFAULT))
                .setRangeDataNtfAoaElevationUpper(
                        bundle.getDouble(KEY_RANGE_DATA_NTF_AOA_ELEVATION_UPPER,
                                RANGE_DATA_NTF_AOA_ELEVATION_UPPER_DEFAULT))
                .setHasTimeOfFlightReport(bundle.getBoolean(KEY_HAS_TIME_OF_FLIGHT_REPORT))
                .setHasAngleOfArrivalAzimuthReport(
                        bundle.getBoolean(KEY_HAS_ANGLE_OF_ARRIVAL_AZIMUTH_REPORT))
                .setHasAngleOfArrivalElevationReport(
                        bundle.getBoolean(KEY_HAS_ANGLE_OF_ARRIVAL_ELEVATION_REPORT))
                .setHasAngleOfArrivalFigureOfMeritReport(
                        bundle.getBoolean(KEY_HAS_ANGLE_OF_ARRIVAL_FIGURE_OF_MERIT_REPORT))
                .setAoaType(bundle.getInt(KEY_AOA_TYPE))
                .setMeasurementFocusRatio(
                        bundle.getInt(KEY_NUM_OF_MSRMT_FOCUS_ON_RANGE),
                        bundle.getInt(KEY_NUM_OF_MSRMT_FOCUS_ON_AOA_AZIMUTH),
                        bundle.getInt(KEY_NUM_OF_MSRMT_FOCUS_ON_AOA_ELEVATION))
                .setRangingErrorStreakTimeoutMs(bundle
                        .getLong(RANGING_ERROR_STREAK_TIMEOUT_MS, 10_000L))
                .setLinkLayerMode(bundle.getInt(KEY_LINK_LAYER_MODE, 0))
                .setDataRepetitionCount(bundle.getInt(KEY_DATA_REPETITION_COUNT, 0))
                .setRangingTimeStruct(bundle.getInt(KEY_RANGING_TIME_STRUCT,
                    BLOCK_BASED_SCHEDULING))
                .setMinFramePerRr(bundle.getInt(KEY_MIN_FRAMES_PER_RR, 1))
                .setMtuSize(bundle.getInt(KEY_MTU_SIZE, 1048))
                .setInterFrameInterval(bundle.getInt(KEY_INTER_FRAME_INTERVAL, 1))
                .setDlTdoaBlockStriding(bundle.getInt(KEY_DLTDOA_BLOCK_STRIDING))
                .setUlTdoaTxIntervalMs(bundle.getInt(UL_TDOA_TX_INTERVAL))
                .setUlTdoaRandomWindowMs(bundle.getInt(UL_TDOA_RANDOM_WINDOW))
                .setUlTdoaDeviceIdType(bundle.getInt(UL_TDOA_DEVICE_ID_TYPE))
                .setUlTdoaDeviceId(intArrayToByteArray(bundle.getIntArray(UL_TDOA_DEVICE_ID)))
                .setUlTdoaTxTimestampType(bundle.getInt(UL_TDOA_TX_TIMESTAMP_TYPE))
                .setFilterType(bundle.getInt(KEY_FILTER_TYPE, FILTER_TYPE_DEFAULT))
                .setMaxNumberOfMeasurements(bundle.getInt(
                        KEY_MAX_NUMBER_OF_MEASUREMENTS, MAX_NUMBER_OF_MEASUREMENTS_DEFAULT))
                .setSessionDataTransferStatusNtfConfig(bundle.getBoolean(
                        KEY_SESSION_DATA_TRANSFER_STATUS_NTF_CONFIG))
                .setSessionTimeBase(bundle.getInt(KEY_REFERENCE_TIME_BASE),
                        bundle.getInt(KEY_REFERENCE_SESSION_HANDLE),
                        bundle.getInt(KEY_SESSION_OFFSET_IN_MICRO_SECONDS))
                .setApplicationDataEndpoint(bundle.getInt(
                        KEY_APPLICATION_DATA_ENDPOINT, APPLICATION_DATA_ENDPOINT_DEFAULT));

        if (builder.isTimeScheduledTwrSession()) {
            long[] destAddresses = bundle.getLongArray(KEY_DEST_ADDRESS_LIST);
            if (destAddresses != null) {
                List<UwbAddress> destAddressList = new ArrayList<>();
                for (long address : destAddresses) {
                    destAddressList.add(longToUwbAddress(address, addressByteLength));
                }
                builder.setDestAddressList(destAddressList);
            }
        }
        return builder.build();
    }

    public FiraProtocolVersion getProtocolVersion() {
        return mProtocolVersion;
    }

    /** Returns a builder from the params. */
    public Builder toBuilder() {
        return new Builder(this);
    }

    /** Builder */
    public static final class Builder {
        private final RequiredParam<FiraProtocolVersion> mProtocolVersion = new RequiredParam<>();

        private final RequiredParam<Integer> mSessionId = new RequiredParam<>();
        @SessionType
        private int mSessionType = FiraParams.SESSION_TYPE_RANGING;
        private final RequiredParam<Integer> mDeviceType = new RequiredParam<>();
        private final RequiredParam<Integer> mDeviceRole = new RequiredParam<>();

        /** UCI spec default: DS-TWR with deferred mode */
        @RangingRoundUsage
        private int mRangingRoundUsage = RANGING_ROUND_USAGE_DS_TWR_DEFERRED_MODE;

        private final RequiredParam<Integer> mMultiNodeMode = new RequiredParam<>();
        private UwbAddress mDeviceAddress = null;
        private List<UwbAddress> mDestAddressList = null;

        /** UCI spec default: 0ms */
        private long mInitiationTime = 0;
        private long mAbsoluteInitiationTime = 0;

        /** UCI spec default: 2400 RSTU (2 ms). */
        private int mSlotDurationRstu = 2400;

        /** UCI spec default: 25 slots per ranging round. */
        private int mSlotsPerRangingRound = SLOTS_PER_RR;

        /** UCI spec default: RANGING_INTERVAL(Fira 2.0: RANGING_DURATION) 200 ms */
        private int mRangingIntervalMs = 200;

        /** UCI spec default: no block striding. */
        private int mBlockStrideLength = 0;

        /** UCI spec default: no hopping. */
        private int mHoppingMode = HOPPING_MODE_DISABLE;

        /** UCI spec default: Termination is disabled and ranging round attempt is infinite */
        @IntRange(from = 0, to = 65535)
        private int mMaxRangingRoundRetries = 0;

        /** UCI spec default: priority 50 */
        private int mSessionPriority = 50;

        /** UCI spec default: 2-byte short address */
        @MacAddressMode private int mMacAddressMode = MAC_ADDRESS_MODE_2_BYTES;

        /** UCI spec default: RANGING_ROUND_CONTROL bit 0 default 1 */
        private boolean mHasRangingResultReportMessage = true;

        /** UCI spec default: RANGING_ROUND_CONTROL bit 1 default 1 */
        private boolean mHasControlMessage = true;

        /** UCI spec default: RANGING_ROUND_CONTROL bit 2 default 0 */
        private boolean mHasRangingControlPhase = false;

        /** UCI spec default: RANGING_ROUND_CONTROL bit 7 default 0 */
        @MeasurementReportType
        private int mMeasurementReportType = MEASUREMENT_REPORT_TYPE_INITIATOR_TO_RESPONDER;

        /** UCI spec default: RANGING_ROUND_CONTROL bit 6 default 0 */
        @MeasurementReportPhase
        private int mMeasurementReportPhase = MEASUREMENT_REPORT_PHASE_NOTSET;

        /** UCI spec default: in-band termination signal will be sent once. */
        @IntRange(from = 1, to = 10)
        private int mInBandTerminationAttemptCount = 1;

        /** UCI spec default: Channel 9, which is the only mandatory channel. */
        @UwbChannel private int mChannelNumber = UWB_CHANNEL_9;

        /** UCI spec default: index 10 */
        @UwbPreambleCodeIndex private int mPreambleCodeIndex = UWB_PREAMBLE_CODE_INDEX_10;

        /** UCI spec default: SP3 */
        private int mRframeConfig = RFRAME_CONFIG_SP3;

        /** UCI spec default: BPRF */
        @PrfMode private int mPrfMode = PRF_MODE_BPRF;

        /** UCI spec default: Octet [0] = SLOTS_PER_RR-1 Octet [1] = 0x05 */
        private byte[] mCapSize = {(SLOTS_PER_RR - 1) , MIN_CAP_SIZE};

        /** UCI spec default: Time scheduled ranging */
        @SchedulingMode private int mScheduledMode = TIME_SCHEDULED_RANGING;

        /** UCI spec default: 64 symbols */
        @PreambleDuration private int mPreambleDuration = PREAMBLE_DURATION_T64_SYMBOLS;

        /** UCI spec default: ID 2 */
        @SfdIdValue private int mSfdId = SFD_ID_VALUE_2;

        /** UCI spec default: one STS segment */
        @StsSegmentCountValue private int mStsSegmentCount = STS_SEGMENT_COUNT_VALUE_1;

        /** UCI spec default: 64 symbols */
        @StsLength private int mStsLength = STS_LENGTH_64_SYMBOLS;

        /** PROVISIONED STS only. 128-bit or 256-bit long */
        @Nullable private byte[] mSessionKey = null;

        /** PROVISIONED STS only. 128-bit or 256-bit long */
        @Nullable private byte[] mSubsessionKey = null;

        /** UCI spec default: 6.81Mb/s */
        @PsduDataRate private int mPsduDataRate = PSDU_DATA_RATE_6M81;

        /** UCI spec default: 850kb/s */
        @BprfPhrDataRate private int mBprfPhrDataRate = BPRF_PHR_DATA_RATE_850K;

        /** UCI spec default: CRC-16 */
        @MacFcsType private int mFcsType = MAC_FCS_TYPE_CRC_16;

        /** UCI spec default: adaptive payload power for TX disabled */
        private boolean mIsTxAdaptivePayloadPowerEnabled = false;

        /** UCI spec default: static STS */
        @StsConfig private int mStsConfig = STS_CONFIG_STATIC;

        /**
         * Per UCI spec, only required when STS config is
         * STS_CONFIG_DYNAMIC_FOR_CONTROLEE_INDIVIDUAL_KEY.
         */
        private final RequiredParam<Integer> mSubSessionId = new RequiredParam<>();

        /** STATIC STS only. For Key generation. 16-bit long */
        @Nullable private byte[] mVendorId = null;

        /** STATIC STS only. For Key generation. 48-bit long */
        @Nullable private byte[] mStaticStsIV = null;

        /** UCI spec default: RSSI reporting disabled */
        private boolean mIsRssiReportingEnabled = false;

        /** Diagnostics is Disabled by default */
        private boolean mIsDiagnosticsEnabled = false;

        /** All fields are set to 0 by default */
        private byte mDiagramsFrameReportsFieldsFlags = 0;

        /** Defaults to omni mode **/
        @AntennaMode private byte mAntennaMode = ANTENNA_MODE_OMNI;

        /** UCI spec default: no key rotation */
        private boolean mIsKeyRotationEnabled = false;

        /** UCI spec default: 0 */
        private int mKeyRotationRate = 0;

        /** UCI spec default: AoA enabled. */
        @AoaResultRequestMode
        private int mAoaResultRequest = AOA_RESULT_REQUEST_MODE_REQ_AOA_RESULTS;

        /** UCI spec default: Ranging notification enabled. */
        @RangeDataNtfConfig private int mRangeDataNtfConfig = RANGE_DATA_NTF_CONFIG_ENABLE;

        /** UCI spec default: 0 (No low-bound filtering) */
        private int mRangeDataNtfProximityNear = RANGE_DATA_NTF_PROXIMITY_NEAR_DEFAULT;

        /** UCI spec default: 20000 cm (or 200 meters) */
        private int mRangeDataNtfProximityFar = RANGE_DATA_NTF_PROXIMITY_FAR_DEFAULT;

        /** UCI spec default: -180 (No low-bound filtering) */
        private double mRangeDataNtfAoaAzimuthLower = RANGE_DATA_NTF_AOA_AZIMUTH_LOWER_DEFAULT;

        /** UCI spec default: +180 (No upper-bound filtering) */
        private double mRangeDataNtfAoaAzimuthUpper = RANGE_DATA_NTF_AOA_AZIMUTH_UPPER_DEFAULT;

        /** UCI spec default: -90 (No low-bound filtering) */
        private double mRangeDataNtfAoaElevationLower = RANGE_DATA_NTF_AOA_ELEVATION_LOWER_DEFAULT;

        /** UCI spec default: +90 (No upper-bound filtering) */
        private double mRangeDataNtfAoaElevationUpper = RANGE_DATA_NTF_AOA_ELEVATION_UPPER_DEFAULT;

        /** UCI spec default: RESULT_REPORT_CONFIG bit 0 is 1 */
        private boolean mHasTimeOfFlightReport = true;

        /** UCI spec default: RESULT_REPORT_CONFIG bit 1 is 0 */
        private boolean mHasAngleOfArrivalAzimuthReport = false;

        /** UCI spec default: RESULT_REPORT_CONFIG bit 2 is 0 */
        private boolean mHasAngleOfArrivalElevationReport = false;

        /** UCI spec default: RESULT_REPORT_CONFIG bit 3 is 0 */
        private boolean mHasAngleOfArrivalFigureOfMeritReport = false;

        /** Not defined in UCI, we use Azimuth-only as default */
        @AoaType private int mAoaType = AOA_TYPE_AZIMUTH;

        /** Interleaving ratios are not set by default */
        private int mNumOfMsrmtFocusOnRange = 0;
        private int mNumOfMsrmtFocusOnAoaAzimuth = 0;
        private int mNumOfMsrmtFocusOnAoaElevation = 0;

        /** Ranging result error streak timeout in Milliseconds*/
        private long mRangingErrorStreakTimeoutMs = 10_000L;

        /** UCI spec default: 0 */
        private int mLinkLayerMode = 0;

        /** UCI spec default: 0x00(No repetition) */
        private int mDataRepetitionCount = 0;

        /** UCI spec default: 0x01 */
        private int mRangingTimeStruct = BLOCK_BASED_SCHEDULING;

        /** UCI spec default: 1 */
        public int mMinFramesPerRr = 1;

        /** No UCI spec default*/
        public int mMtuSize = 1048;

        /** UCI spec default: 1 */
        public int mInterFrameInterval = 1;

        /** UCI spec default: no dltdoa block striding. */
        private int mDlTdoaBlockStriding = 0;

        /** Ul-TDoA Tx Interval in Milliseconds */
        private int mUlTdoaTxIntervalMs = 2000;

        /** Ul-TDoA Random Window in Milliseconds */
        private int mUlTdoaRandomWindowMs = 0;

        /** Ul-TDoA Device ID type */
        @UlTdoaDeviceIdType private int mUlTdoaDeviceIdType = UL_TDOA_DEVICE_ID_NONE;

        /** Ul-TDoA Device ID */
        @Nullable private byte[] mUlTdoaDeviceId;

        /** Ul-TDoA Tx Timestamp Type */
        @UlTdoaTxTimestampType private int mUlTdoaTxTimestampType = TX_TIMESTAMP_NONE;

        /** AoA/distance filtering type */
        @FilterType private int mFilterType = FILTER_TYPE_DEFAULT;

        private int mMaxNumberOfMeasurements = MAX_NUMBER_OF_MEASUREMENTS_DEFAULT;

        /** UCI spec default: 0x00(Disable) */
        private boolean mSessionDataTransferStatusNtfConfig = false;

        /** UCI spec default: 9 Octets of SESSION_TIME_BASE as per UCI spec*/
        private int mReferenceTimeBase = 0;

        private int mReferenceSessionHandle = 0;

        private int mSessionOffsetInMicroSeconds = 0;

        private int mApplicationDataEndpoint = APPLICATION_DATA_ENDPOINT_DEFAULT;

        public Builder() {}

        public Builder(@NonNull Builder builder) {
            mProtocolVersion.set(builder.mProtocolVersion.get());
            mSessionId.set(builder.mSessionId.get());
            mSessionType = builder.mSessionType;
            mDeviceType.set(builder.mDeviceType.get());
            mDeviceRole.set(builder.mDeviceRole.get());
            mRangingRoundUsage = builder.mRangingRoundUsage;
            mMultiNodeMode.set(builder.mMultiNodeMode.get());
            mDeviceAddress = builder.mDeviceAddress;
            mDestAddressList = builder.mDestAddressList;
            mInitiationTime = builder.mInitiationTime;
            mAbsoluteInitiationTime = builder.mAbsoluteInitiationTime;
            mSlotDurationRstu = builder.mSlotDurationRstu;
            mSlotsPerRangingRound = builder.mSlotsPerRangingRound;
            mRangingIntervalMs = builder.mRangingIntervalMs;
            mBlockStrideLength = builder.mBlockStrideLength;
            mHoppingMode = builder.mHoppingMode;
            mMaxRangingRoundRetries = builder.mMaxRangingRoundRetries;
            mSessionPriority = builder.mSessionPriority;
            mMacAddressMode = builder.mMacAddressMode;
            mHasRangingResultReportMessage = builder.mHasRangingResultReportMessage;
            mHasControlMessage = builder.mHasControlMessage;
            mHasRangingControlPhase = builder.mHasRangingControlPhase;
            mMeasurementReportType = builder.mMeasurementReportType;
            mMeasurementReportPhase = builder.mMeasurementReportPhase;
            mInBandTerminationAttemptCount = builder.mInBandTerminationAttemptCount;
            mChannelNumber = builder.mChannelNumber;
            mPreambleCodeIndex = builder.mPreambleCodeIndex;
            mRframeConfig = builder.mRframeConfig;
            mPrfMode = builder.mPrfMode;
            mScheduledMode = builder.mScheduledMode;
            if (builder.mScheduledMode == CONTENTION_BASED_RANGING) {
                mCapSize = builder.mCapSize;
            }
            mPreambleDuration = builder.mPreambleDuration;
            mSfdId = builder.mSfdId;
            mStsSegmentCount = builder.mStsSegmentCount;
            mStsLength = builder.mStsLength;
            mSessionKey = builder.mSessionKey;
            mSubsessionKey = builder.mSubsessionKey;
            mPsduDataRate = builder.mPsduDataRate;
            mBprfPhrDataRate = builder.mBprfPhrDataRate;
            mFcsType = builder.mFcsType;
            mIsTxAdaptivePayloadPowerEnabled = builder.mIsTxAdaptivePayloadPowerEnabled;
            mStsConfig = builder.mStsConfig;
            if (builder.mSubSessionId.isSet()) mSubSessionId.set(builder.mSubSessionId.get());
            mVendorId = builder.mVendorId;
            mStaticStsIV = builder.mStaticStsIV;
            mIsRssiReportingEnabled = builder.mIsRssiReportingEnabled;
            mIsDiagnosticsEnabled = builder.mIsDiagnosticsEnabled;
            mDiagramsFrameReportsFieldsFlags = builder.mDiagramsFrameReportsFieldsFlags;
            mAntennaMode = builder.mAntennaMode;
            mIsKeyRotationEnabled = builder.mIsKeyRotationEnabled;
            mKeyRotationRate = builder.mKeyRotationRate;
            mAoaResultRequest = builder.mAoaResultRequest;
            mRangeDataNtfConfig = builder.mRangeDataNtfConfig;
            mRangeDataNtfProximityNear = builder.mRangeDataNtfProximityNear;
            mRangeDataNtfProximityFar = builder.mRangeDataNtfProximityFar;
            mRangeDataNtfAoaAzimuthLower = builder.mRangeDataNtfAoaAzimuthLower;
            mRangeDataNtfAoaAzimuthUpper = builder.mRangeDataNtfAoaAzimuthUpper;
            mRangeDataNtfAoaElevationLower = builder.mRangeDataNtfAoaElevationLower;
            mRangeDataNtfAoaElevationUpper = builder.mRangeDataNtfAoaElevationUpper;
            mHasTimeOfFlightReport = builder.mHasTimeOfFlightReport;
            mHasAngleOfArrivalAzimuthReport = builder.mHasAngleOfArrivalAzimuthReport;
            mHasAngleOfArrivalElevationReport = builder.mHasAngleOfArrivalElevationReport;
            mHasAngleOfArrivalFigureOfMeritReport = builder.mHasAngleOfArrivalFigureOfMeritReport;
            mAoaType = builder.mAoaType;
            mNumOfMsrmtFocusOnRange = builder.mNumOfMsrmtFocusOnRange;
            mNumOfMsrmtFocusOnAoaAzimuth = builder.mNumOfMsrmtFocusOnAoaAzimuth;
            mNumOfMsrmtFocusOnAoaElevation = builder.mNumOfMsrmtFocusOnAoaElevation;
            mRangingErrorStreakTimeoutMs = builder.mRangingErrorStreakTimeoutMs;
            mLinkLayerMode = builder.mLinkLayerMode;
            mDataRepetitionCount = builder.mDataRepetitionCount;
            mRangingTimeStruct = builder.mRangingTimeStruct;
            mMinFramesPerRr = builder.mMinFramesPerRr;
            mMtuSize = builder.mMtuSize;
            mInterFrameInterval = builder.mInterFrameInterval;
            mDlTdoaBlockStriding = builder.mDlTdoaBlockStriding;
            mUlTdoaTxIntervalMs = builder.mUlTdoaTxIntervalMs;
            mUlTdoaRandomWindowMs = builder.mUlTdoaRandomWindowMs;
            mUlTdoaDeviceIdType = builder.mUlTdoaDeviceIdType;
            mUlTdoaDeviceId = builder.mUlTdoaDeviceId;
            mUlTdoaTxTimestampType = builder.mUlTdoaTxTimestampType;
            mMaxNumberOfMeasurements = builder.mMaxNumberOfMeasurements;
            mSessionDataTransferStatusNtfConfig = builder.mSessionDataTransferStatusNtfConfig;
            mReferenceTimeBase = builder.mReferenceTimeBase;
            mReferenceSessionHandle = builder.mReferenceSessionHandle;
            mSessionOffsetInMicroSeconds = builder.mSessionOffsetInMicroSeconds;
            mApplicationDataEndpoint = builder.mApplicationDataEndpoint;
        }

        public Builder(@NonNull FiraOpenSessionParams params) {
            mProtocolVersion.set(params.mProtocolVersion);
            mSessionId.set(params.mSessionId);
            mSessionType = params.mSessionType;
            mDeviceType.set(params.mDeviceType);
            mDeviceRole.set(params.mDeviceRole);
            mRangingRoundUsage = params.mRangingRoundUsage;
            mMultiNodeMode.set(params.mMultiNodeMode);
            mDeviceAddress = params.mDeviceAddress;
            mDestAddressList = params.mDestAddressList;
            mInitiationTime = params.mInitiationTime;
            mAbsoluteInitiationTime = params.mAbsoluteInitiationTime;
            mSlotDurationRstu = params.mSlotDurationRstu;
            mSlotsPerRangingRound = params.mSlotsPerRangingRound;
            mRangingIntervalMs = params.mRangingIntervalMs;
            mBlockStrideLength = params.mBlockStrideLength;
            mHoppingMode = params.mHoppingMode;
            mMaxRangingRoundRetries = params.mMaxRangingRoundRetries;
            mSessionPriority = params.mSessionPriority;
            mMacAddressMode = params.mMacAddressMode;
            mHasRangingResultReportMessage = params.mHasRangingResultReportMessage;
            mHasControlMessage = params.mHasControlMessage;
            mHasRangingControlPhase = params.mHasRangingControlPhase;
            mMeasurementReportType = params.mMeasurementReportType;
            mMeasurementReportPhase = params.mMeasurementReportPhase;
            mInBandTerminationAttemptCount = params.mInBandTerminationAttemptCount;
            mChannelNumber = params.mChannelNumber;
            mPreambleCodeIndex = params.mPreambleCodeIndex;
            mRframeConfig = params.mRframeConfig;
            mPrfMode = params.mPrfMode;
            mScheduledMode = params.mScheduledMode;
            if (params.mScheduledMode == CONTENTION_BASED_RANGING) {
                mCapSize = params.mCapSize;
            }
            mPreambleDuration = params.mPreambleDuration;
            mSfdId = params.mSfdId;
            mStsSegmentCount = params.mStsSegmentCount;
            mStsLength = params.mStsLength;
            mSessionKey = params.mSessionKey;
            mSubsessionKey = params.mSubSessionKey;
            mPsduDataRate = params.mPsduDataRate;
            mBprfPhrDataRate = params.mBprfPhrDataRate;
            mFcsType = params.mFcsType;
            mIsTxAdaptivePayloadPowerEnabled = params.mIsTxAdaptivePayloadPowerEnabled;
            mStsConfig = params.mStsConfig;
            mSubSessionId.set(params.mSubSessionId);
            mVendorId = params.mVendorId;
            mStaticStsIV = params.mStaticStsIV;
            mIsRssiReportingEnabled = params.mIsRssiReportingEnabled;
            mIsDiagnosticsEnabled = params.mIsDiagnosticsEnabled;
            mDiagramsFrameReportsFieldsFlags = params.mDiagramsFrameReportsFieldsFlags;
            mAntennaMode = params.mAntennaMode;
            mIsKeyRotationEnabled = params.mIsKeyRotationEnabled;
            mKeyRotationRate = params.mKeyRotationRate;
            mAoaResultRequest = params.mAoaResultRequest;
            mRangeDataNtfConfig = params.mRangeDataNtfConfig;
            mRangeDataNtfProximityNear = params.mRangeDataNtfProximityNear;
            mRangeDataNtfProximityFar = params.mRangeDataNtfProximityFar;
            mRangeDataNtfAoaAzimuthLower = params.mRangeDataNtfAoaAzimuthLower;
            mRangeDataNtfAoaAzimuthUpper = params.mRangeDataNtfAoaAzimuthUpper;
            mRangeDataNtfAoaElevationLower = params.mRangeDataNtfAoaElevationLower;
            mRangeDataNtfAoaElevationUpper = params.mRangeDataNtfAoaElevationUpper;
            mHasTimeOfFlightReport = params.mHasTimeOfFlightReport;
            mHasAngleOfArrivalAzimuthReport = params.mHasAngleOfArrivalAzimuthReport;
            mHasAngleOfArrivalElevationReport = params.mHasAngleOfArrivalElevationReport;
            mHasAngleOfArrivalFigureOfMeritReport = params.mHasAngleOfArrivalFigureOfMeritReport;
            mAoaType = params.mAoaType;
            mNumOfMsrmtFocusOnRange = params.mNumOfMsrmtFocusOnRange;
            mNumOfMsrmtFocusOnAoaAzimuth = params.mNumOfMsrmtFocusOnAoaAzimuth;
            mNumOfMsrmtFocusOnAoaElevation = params.mNumOfMsrmtFocusOnAoaElevation;
            mRangingErrorStreakTimeoutMs = params.mRangingErrorStreakTimeoutMs;
            mLinkLayerMode = params.mLinkLayerMode;
            mDataRepetitionCount = params.mDataRepetitionCount;
            mRangingTimeStruct = params.mRangingTimeStruct;
            mMinFramesPerRr = params.mMinFramesPerRr;
            mMtuSize = params.mMtuSize;
            mInterFrameInterval = params.mInterFrameInterval;
            mDlTdoaBlockStriding = params.mDlTdoaBlockStriding;
            mUlTdoaTxIntervalMs = params.mUlTdoaTxIntervalMs;
            mUlTdoaRandomWindowMs = params.mUlTdoaRandomWindowMs;
            mUlTdoaDeviceIdType = params.mUlTdoaDeviceIdType;
            mUlTdoaDeviceId = params.mUlTdoaDeviceId;
            mUlTdoaTxTimestampType = params.mUlTdoaTxTimestampType;
            mFilterType = params.mFilterType;
            mMaxNumberOfMeasurements = params.mMaxNumberOfMeasurements;
            mSessionDataTransferStatusNtfConfig = params.mSessionDataTransferStatusNtfConfig;
            mReferenceTimeBase = params.mReferenceTimeBase;
            mReferenceSessionHandle = params.mReferenceSessionHandle;
            mSessionOffsetInMicroSeconds = params.mSessionOffsetInMicroSeconds;
            mApplicationDataEndpoint = params.mApplicationDataEndpoint;
        }

        public Builder setProtocolVersion(FiraProtocolVersion version) {
            mProtocolVersion.set(version);
            return this;
        }

        public Builder setSessionId(int sessionId) {
            mSessionId.set(sessionId);
            return this;
        }

        /** @param sessionId must be non-negative and fit in 32 bit unsigned integer. */
        public Builder setSessionId(long sessionId) {
            return setSessionId(asUnsigned(sessionId));
        }

        public Builder setSessionType(@SessionType int sessionType) {
            mSessionType = sessionType;
            return this;
        }

        public Builder setDeviceType(@RangingDeviceType int deviceType) {
            mDeviceType.set(deviceType);
            return this;
        }

        public Builder setDeviceRole(@RangingDeviceRole int deviceRole) {
            mDeviceRole.set(deviceRole);
            return this;
        }

        public Builder setRangingRoundUsage(
                @RangingRoundUsage int rangingRoundUsage) {
            mRangingRoundUsage = rangingRoundUsage;
            return this;
        }

        public Builder setMultiNodeMode(@MultiNodeMode int multiNodeMode) {
            mMultiNodeMode.set(multiNodeMode);
            return this;
        }

        public Builder setDeviceAddress(UwbAddress deviceAddress) {
            mDeviceAddress = deviceAddress;
            return this;
        }

        public Builder setDestAddressList(List<UwbAddress> destAddressList) {
            mDestAddressList = destAddressList;
            return this;
        }

        /**
         * Sets the UWB initiation time.
         *
         * @param initiationTime UWB initiation time:
         *        FiRa 1.0: Relative time (in milli-seconds).
         *        FiRa 2.0: Relative time (in milli-seconds).
         *            For a FiRa 2.0 device, the UWB Service will query the absolute UWBS timestamp
         *            and add the relative time (in milli-seconds) configured here, to compute the
         *            absolute time that will be configured in the UWB_INITIATION_TIME parameter.
         */
        public Builder setInitiationTime(long initiationTime) {
            mInitiationTime = initiationTime;
            return this;
        }

        /**
         * Sets the Absolute UWB initiation time.
         *
         * @param absoluteInitiationTime Absolute UWB initiation time (in micro-seconds). This is
         *        applicable only for FiRa 2.0+ devices, as specified in CR-272.
         */
        public Builder setAbsoluteInitiationTime(
                long absoluteInitiationTime) {
            mAbsoluteInitiationTime = absoluteInitiationTime;
            return this;
        }

        public Builder setSlotDurationRstu(int slotDurationRstu) {
            mSlotDurationRstu = slotDurationRstu;
            return this;
        }

        public Builder setSlotsPerRangingRound(int slotsPerRangingRound) {
            mSlotsPerRangingRound = slotsPerRangingRound;
            return this;
        }

        public Builder setRangingIntervalMs(int rangingIntervalMs) {
            mRangingIntervalMs = rangingIntervalMs;
            return this;
        }

        public Builder setBlockStrideLength(int blockStrideLength) {
            mBlockStrideLength = blockStrideLength;
            return this;
        }

        public Builder setHoppingMode(int hoppingMode) {
            this.mHoppingMode = hoppingMode;
            return this;
        }

        public Builder setMaxRangingRoundRetries(
                @IntRange(from = 0, to = 65535) int maxRangingRoundRetries) {
            mMaxRangingRoundRetries = maxRangingRoundRetries;
            return this;
        }

        public Builder setSessionPriority(int sessionPriority) {
            mSessionPriority = sessionPriority;
            return this;
        }

        public Builder setMacAddressMode(int macAddressMode) {
            this.mMacAddressMode = macAddressMode;
            return this;
        }

        public Builder setHasRangingResultReportMessage(
                boolean hasRangingResultReportMessage) {
            mHasRangingResultReportMessage = hasRangingResultReportMessage;
            return this;
        }

        public Builder setHasControlMessage(boolean hasControlMessage) {
            mHasControlMessage = hasControlMessage;
            return this;
        }

        public Builder setHasRangingControlPhase(
                boolean hasRangingControlPhase) {
            mHasRangingControlPhase = hasRangingControlPhase;
            return this;
        }

        public Builder setMeasurementReportType(
                @MeasurementReportType int measurementReportType) {
            mMeasurementReportType = measurementReportType;
            return this;
        }

        public Builder setMeasurementReportPhase(
                @MeasurementReportPhase int measurementReportPhase) {
            mMeasurementReportPhase = measurementReportPhase;
            return this;
        }

        public Builder setInBandTerminationAttemptCount(
                @IntRange(from = 1, to = 10) int inBandTerminationAttemptCount) {
            mInBandTerminationAttemptCount = inBandTerminationAttemptCount;
            return this;
        }

        public Builder setChannelNumber(@UwbChannel int channelNumber) {
            mChannelNumber = channelNumber;
            return this;
        }

        public Builder setPreambleCodeIndex(
                @UwbPreambleCodeIndex int preambleCodeIndex) {
            mPreambleCodeIndex = preambleCodeIndex;
            return this;
        }

        public Builder setRframeConfig(@RframeConfig int rframeConfig) {
            mRframeConfig = rframeConfig;
            return this;
        }

        public Builder setPrfMode(@PrfMode int prfMode) {
            mPrfMode = prfMode;
            return this;
        }

        public Builder setCapSize(byte[] capSize) {
            mCapSize = capSize;
            return this;
        }

        public Builder setScheduledMode(@SchedulingMode int scheduledMode) {
            mScheduledMode = scheduledMode;
            return this;
        }

        public Builder setPreambleDuration(
                @PreambleDuration int preambleDuration) {
            mPreambleDuration = preambleDuration;
            return this;
        }

        public Builder setSfdId(@SfdIdValue int sfdId) {
            mSfdId = sfdId;
            return this;
        }

        public Builder setStsSegmentCount(
                @StsSegmentCountValue int stsSegmentCount) {
            mStsSegmentCount = stsSegmentCount;
            return this;
        }

        public Builder setStsLength(@StsLength int stsLength) {
            mStsLength = stsLength;
            return this;
        }

        /** set session key */
        public Builder setSessionKey(@Nullable byte[] sessionKey) {
            mSessionKey = sessionKey;
            return this;
        }

        /** set subsession key */
        public Builder setSubsessionKey(@Nullable byte[] subsessionKey) {
            mSubsessionKey = subsessionKey;
            return this;
        }

        public Builder setPsduDataRate(@PsduDataRate int psduDataRate) {
            mPsduDataRate = psduDataRate;
            return this;
        }

        public Builder setBprfPhrDataRate(
                @BprfPhrDataRate int bprfPhrDataRate) {
            mBprfPhrDataRate = bprfPhrDataRate;
            return this;
        }

        public Builder setFcsType(@MacFcsType int fcsType) {
            mFcsType = fcsType;
            return this;
        }

        public Builder setIsTxAdaptivePayloadPowerEnabled(
                boolean isTxAdaptivePayloadPowerEnabled) {
            mIsTxAdaptivePayloadPowerEnabled = isTxAdaptivePayloadPowerEnabled;
            return this;
        }

        public Builder setStsConfig(@StsConfig int stsConfig) {
            mStsConfig = stsConfig;
            return this;
        }

        public Builder setSubSessionId(int subSessionId) {
            mSubSessionId.set(subSessionId);
            return this;
        }

        /** @param subSessionId must be non-negative and fit in 32 bit unsigned integer. */
        public Builder setSubSessionId(long subSessionId) {
            return setSubSessionId(asUnsigned(subSessionId));
        }

        public Builder setVendorId(@Nullable byte[] vendorId) {
            mVendorId = vendorId;
            return this;
        }

        public Builder setStaticStsIV(@Nullable byte[] staticStsIV) {
            mStaticStsIV = staticStsIV;
            return this;
        }

        /** Set whether rssi reporting is enabled */
        public Builder
                setIsRssiReportingEnabled(boolean isRssiReportingEnabled) {
            mIsRssiReportingEnabled = isRssiReportingEnabled;
            return this;
        }

        /** Set whether diagnostics is enabled */
        public Builder setIsDiagnosticsEnabled(boolean isDiagnosticsEnabled) {
            mIsDiagnosticsEnabled = isDiagnosticsEnabled;
            return this;
        }

        /** Set the activated field
         *  b0: Activate RSSIs field
         *  b1: Activate AoAs field
         *  b2: Activate CIRs field
         *  b3 - b7: RFU
         */
        public Builder
                setDiagramsFrameReportsFieldsFlags(byte diagramsFrameReportsFieldsFlags) {
            mDiagramsFrameReportsFieldsFlags = diagramsFrameReportsFieldsFlags;
            return this;
        }

        /** Set the antenna mode **/
        public Builder setAntennaMode(@AntennaMode byte antennaMode) {
            mAntennaMode = antennaMode;
            return this;
        }

        public Builder setIsKeyRotationEnabled(boolean isKeyRotationEnabled) {
            mIsKeyRotationEnabled = isKeyRotationEnabled;
            return this;
        }

        public Builder setKeyRotationRate(int keyRotationRate) {
            mKeyRotationRate = keyRotationRate;
            return this;
        }

        public Builder setAoaResultRequest(
                @AoaResultRequestMode int aoaResultRequest) {
            mAoaResultRequest = aoaResultRequest;
            return this;
        }

        public Builder setRangeDataNtfConfig(
                @RangeDataNtfConfig int rangeDataNtfConfig) {
            mRangeDataNtfConfig = rangeDataNtfConfig;
            return this;
        }

        public Builder setRangeDataNtfProximityNear(
                @IntRange(from = RANGE_DATA_NTF_PROXIMITY_NEAR_DEFAULT,
                        to = RANGE_DATA_NTF_PROXIMITY_FAR_DEFAULT)
                        int rangeDataNtfProximityNear) {
            mRangeDataNtfProximityNear = rangeDataNtfProximityNear;
            return this;
        }

        public Builder setRangeDataNtfProximityFar(
                @IntRange(from = RANGE_DATA_NTF_PROXIMITY_NEAR_DEFAULT,
                        to = RANGE_DATA_NTF_PROXIMITY_FAR_DEFAULT)
                        int rangeDataNtfProximityFar) {
            mRangeDataNtfProximityFar = rangeDataNtfProximityFar;
            return this;
        }

        public Builder setRangeDataNtfAoaAzimuthLower(
                @FloatRange(from = RANGE_DATA_NTF_AOA_AZIMUTH_LOWER_DEFAULT,
                        to = RANGE_DATA_NTF_AOA_AZIMUTH_UPPER_DEFAULT)
                        double rangeDataNtfAoaAzimuthLower) {
            mRangeDataNtfAoaAzimuthLower = rangeDataNtfAoaAzimuthLower;
            return this;
        }

        public Builder setRangeDataNtfAoaAzimuthUpper(
                @FloatRange(from = RANGE_DATA_NTF_AOA_AZIMUTH_LOWER_DEFAULT,
                        to = RANGE_DATA_NTF_AOA_AZIMUTH_UPPER_DEFAULT)
                        double rangeDataNtfAoaAzimuthUpper) {
            mRangeDataNtfAoaAzimuthUpper = rangeDataNtfAoaAzimuthUpper;
            return this;
        }

        public Builder setRangeDataNtfAoaElevationLower(
                @FloatRange(from = RANGE_DATA_NTF_AOA_ELEVATION_LOWER_DEFAULT,
                        to = RANGE_DATA_NTF_AOA_ELEVATION_UPPER_DEFAULT)
                        double rangeDataNtfAoaElevationLower) {
            mRangeDataNtfAoaElevationLower = rangeDataNtfAoaElevationLower;
            return this;
        }

        public Builder setRangeDataNtfAoaElevationUpper(
                @FloatRange(from = RANGE_DATA_NTF_AOA_ELEVATION_LOWER_DEFAULT,
                        to = RANGE_DATA_NTF_AOA_ELEVATION_UPPER_DEFAULT)
                        double rangeDataNtfAoaElevationUpper) {
            mRangeDataNtfAoaElevationUpper = rangeDataNtfAoaElevationUpper;
            return this;
        }

        public Builder setHasTimeOfFlightReport(
                boolean hasTimeOfFlightReport) {
            mHasTimeOfFlightReport = hasTimeOfFlightReport;
            return this;
        }

        public Builder setHasAngleOfArrivalAzimuthReport(
                boolean hasAngleOfArrivalAzimuthReport) {
            mHasAngleOfArrivalAzimuthReport = hasAngleOfArrivalAzimuthReport;
            return this;
        }

        public Builder setHasAngleOfArrivalElevationReport(
                boolean hasAngleOfArrivalElevationReport) {
            mHasAngleOfArrivalElevationReport = hasAngleOfArrivalElevationReport;
            return this;
        }

        public Builder setHasAngleOfArrivalFigureOfMeritReport(
                boolean hasAngleOfArrivalFigureOfMeritReport) {
            mHasAngleOfArrivalFigureOfMeritReport = hasAngleOfArrivalFigureOfMeritReport;
            return this;
        }

        public Builder setAoaType(int aoaType) {
            mAoaType = aoaType;
            return this;
        }

        public Builder setRangingErrorStreakTimeoutMs(
                long rangingErrorStreakTimeoutMs) {
            mRangingErrorStreakTimeoutMs = rangingErrorStreakTimeoutMs;
            return this;
        }

        public Builder setLinkLayerMode(int linkLayerMode) {
            mLinkLayerMode = linkLayerMode;
            return this;
        }

        public Builder setDataRepetitionCount(int dataRepetitionCount) {
            mDataRepetitionCount = dataRepetitionCount;
            return this;
        }

        public Builder setRangingTimeStruct(
                @RangingTimeStruct int rangingTimeStruct) {
            mRangingTimeStruct = rangingTimeStruct;
            return this;
        }

        public Builder setMinFramePerRr(int minFramePerRr) {
            mMinFramesPerRr = minFramePerRr;
            return this;
        }

        public Builder setMtuSize(int mtuSize) {
            mMtuSize = mtuSize;
            return this;
        }

        public Builder setInterFrameInterval(int interFrameInterval) {
            mInterFrameInterval = interFrameInterval;
            return this;
        }

        public Builder setDlTdoaBlockStriding(int dlTdoaBlockStriding) {
            mDlTdoaBlockStriding = dlTdoaBlockStriding;
            return this;
        }

        public Builder setUlTdoaTxIntervalMs(
                int ulTdoaTxIntervalMs) {
            mUlTdoaTxIntervalMs = ulTdoaTxIntervalMs;
            return this;
        }

        public Builder setUlTdoaRandomWindowMs(
                int ulTdoaRandomWindowMs) {
            mUlTdoaRandomWindowMs = ulTdoaRandomWindowMs;
            return this;
        }

        public Builder setUlTdoaDeviceIdType(
                int ulTdoaDeviceIdType) {
            mUlTdoaDeviceIdType = ulTdoaDeviceIdType;
            return this;
        }

        public Builder setUlTdoaDeviceId(
                byte[] ulTdoaDeviceId) {
            mUlTdoaDeviceId = ulTdoaDeviceId;
            return this;
        }

        public Builder setUlTdoaTxTimestampType(
                int ulTdoatxTimestampType) {
            mUlTdoaTxTimestampType = ulTdoatxTimestampType;
            return this;
        }

       /**
        * After the session has been started, the device starts by
        * performing numOfMsrmtFocusOnRange range-only measurements (no
        * AoA), then it proceeds with numOfMsrmtFocusOnAoaAzimuth AoA
        * azimuth measurements followed by numOfMsrmtFocusOnAoaElevation
        * AoA elevation measurements.
        * If this is not invoked, the focus of each measurement is left
        * to the UWB vendor.
        *
        * Only valid when {@link #setAoaResultRequest(int)} is set to
        * {@link FiraParams#AOA_RESULT_REQUEST_MODE_REQ_AOA_RESULTS_INTERLEAVED}.
        */
        public Builder setMeasurementFocusRatio(
                int numOfMsrmtFocusOnRange,
                int numOfMsrmtFocusOnAoaAzimuth,
                int numOfMsrmtFocusOnAoaElevation) {
            mNumOfMsrmtFocusOnRange = numOfMsrmtFocusOnRange;
            mNumOfMsrmtFocusOnAoaAzimuth = numOfMsrmtFocusOnAoaAzimuth;
            mNumOfMsrmtFocusOnAoaElevation = numOfMsrmtFocusOnAoaElevation;
            return this;
        }

        public Builder setMaxNumberOfMeasurements(
                int maxNumberOfMeasurements) {
            mMaxNumberOfMeasurements = maxNumberOfMeasurements;
            return this;
        }

        public Builder setSessionDataTransferStatusNtfConfig(
                boolean sessionDataTransferStatusNtfConfig) {
            mSessionDataTransferStatusNtfConfig = sessionDataTransferStatusNtfConfig;
            return this;
        }


        public Builder setSessionTimeBase(int referenceTimeBase,
                int referenceSessionHandle, int sessionOffsetInMicroSecond) {
            mReferenceTimeBase = referenceTimeBase;
            mReferenceSessionHandle = referenceSessionHandle;
            mSessionOffsetInMicroSeconds = sessionOffsetInMicroSecond;
            return this;
        }

        /**
         * @param referenceSessionHandle must be non-negative and fit in 32 bit unsigned integer.
         */
        public Builder setSessionTimeBase(
                int referenceTimeBase, long referenceSessionHandle,
                int sessionOffsetInMicroSecond) {
            return setSessionTimeBase(referenceTimeBase, asUnsigned(referenceSessionHandle),
                    sessionOffsetInMicroSecond);
        }

        public Builder setApplicationDataEndpoint(
                int applicationDataEndpoint) {
            mApplicationDataEndpoint = applicationDataEndpoint;
            return this;
        }

        private void checkAddress() {
            checkArgument(
                    mMacAddressMode == MAC_ADDRESS_MODE_2_BYTES
                            || mMacAddressMode == MAC_ADDRESS_MODE_8_BYTES);
            int addressByteLength = SHORT_ADDRESS_BYTE_LENGTH;
            if (mMacAddressMode == MAC_ADDRESS_MODE_8_BYTES) {
                addressByteLength = EXTENDED_ADDRESS_BYTE_LENGTH;
            }

            // Make sure address length matches the address mode
            checkArgument(mDeviceAddress != null && mDeviceAddress.getAddress().length == addressByteLength);
            if (isTimeScheduledTwrSession()
                    && mStsConfig != STS_CONFIG_PROVISIONED_FOR_CONTROLEE_INDIVIDUAL_KEY) {
                checkNotNull(mDestAddressList);
                for (UwbAddress destAddress : mDestAddressList) {
                    checkArgument(destAddress != null
                            && destAddress.getAddress().length == addressByteLength);
                }
            }
        }

        private void checkStsConfig() {
            if (mStsConfig == STS_CONFIG_STATIC) {
                // These two fields are used by Static STS only.
                checkArgument(mVendorId != null && mVendorId.length == 2);
                checkArgument(mStaticStsIV != null && mStaticStsIV.length == 6);
            }

            if ((mStsConfig == STS_CONFIG_DYNAMIC_FOR_CONTROLEE_INDIVIDUAL_KEY ||
                 mStsConfig == STS_CONFIG_PROVISIONED_FOR_CONTROLEE_INDIVIDUAL_KEY) &&
                 (mDeviceType.get() == RANGING_DEVICE_TYPE_CONTROLEE)) {
                // Sub Session ID is used for dynamic/Provisional individual key and
                // for controlee device.
                checkArgument(mSubSessionId.isSet());
            } else {
                mSubSessionId.set(0);
            }

            if (mStsConfig == STS_CONFIG_PROVISIONED && mSessionKey != null) {
                checkArgument(mSessionKey.length == 16 || mSessionKey.length == 32);
            }

            if (mStsConfig == STS_CONFIG_PROVISIONED_FOR_CONTROLEE_INDIVIDUAL_KEY
                && mDeviceType.get() == RANGING_DEVICE_TYPE_CONTROLEE && mSubsessionKey != null) {
                checkArgument(mSessionKey != null &&
                        (mSessionKey.length == 16 || mSessionKey.length == 32));
                checkArgument(mSubsessionKey.length == 16 || mSubsessionKey.length == 32);
            }
        }

        private void checkInterleavingRatio() {
            if (mAoaResultRequest != AOA_RESULT_REQUEST_MODE_REQ_AOA_RESULTS_INTERLEAVED) {
                checkArgument(mNumOfMsrmtFocusOnRange == 0);
                checkArgument(mNumOfMsrmtFocusOnAoaAzimuth == 0);
                checkArgument(mNumOfMsrmtFocusOnAoaElevation == 0);
            } else {
                // at-least one of the ratio params should be set for interleaving mode.
                checkArgument(mNumOfMsrmtFocusOnRange > 0
                        || mNumOfMsrmtFocusOnAoaAzimuth > 0
                        || mNumOfMsrmtFocusOnAoaElevation > 0);
            }
        }

        private void checkRangeDataNtfConfig() {
            if (mRangeDataNtfConfig == RANGE_DATA_NTF_CONFIG_DISABLE) {
                checkArgument(mRangeDataNtfProximityNear
                        == RANGE_DATA_NTF_PROXIMITY_NEAR_DEFAULT);
                checkArgument(mRangeDataNtfProximityFar
                        == RANGE_DATA_NTF_PROXIMITY_FAR_DEFAULT);
                checkArgument(mRangeDataNtfAoaAzimuthLower
                        == RANGE_DATA_NTF_AOA_AZIMUTH_LOWER_DEFAULT);
                checkArgument(mRangeDataNtfAoaAzimuthUpper
                        == RANGE_DATA_NTF_AOA_AZIMUTH_UPPER_DEFAULT);
                checkArgument(mRangeDataNtfAoaElevationLower
                        == RANGE_DATA_NTF_AOA_ELEVATION_LOWER_DEFAULT);
                checkArgument(mRangeDataNtfAoaElevationUpper
                        == RANGE_DATA_NTF_AOA_ELEVATION_UPPER_DEFAULT);
            } else if (mRangeDataNtfConfig == RANGE_DATA_NTF_CONFIG_ENABLE_PROXIMITY_LEVEL_TRIG
                    || mRangeDataNtfConfig == RANGE_DATA_NTF_CONFIG_ENABLE_PROXIMITY_EDGE_TRIG) {
                checkArgument(
                        mRangeDataNtfProximityNear != RANGE_DATA_NTF_PROXIMITY_NEAR_DEFAULT
                        || mRangeDataNtfProximityFar != RANGE_DATA_NTF_PROXIMITY_FAR_DEFAULT);
                checkArgument(mRangeDataNtfAoaAzimuthLower
                        == RANGE_DATA_NTF_AOA_AZIMUTH_LOWER_DEFAULT);
                checkArgument(mRangeDataNtfAoaAzimuthUpper
                        == RANGE_DATA_NTF_AOA_AZIMUTH_UPPER_DEFAULT);
                checkArgument(mRangeDataNtfAoaElevationLower
                        == RANGE_DATA_NTF_AOA_ELEVATION_LOWER_DEFAULT);
                checkArgument(mRangeDataNtfAoaElevationUpper
                        == RANGE_DATA_NTF_AOA_ELEVATION_UPPER_DEFAULT);
            } else if (mRangeDataNtfConfig == RANGE_DATA_NTF_CONFIG_ENABLE_AOA_LEVEL_TRIG
                    || mRangeDataNtfConfig == RANGE_DATA_NTF_CONFIG_ENABLE_AOA_EDGE_TRIG) {
                checkArgument(mRangeDataNtfProximityNear
                        == RANGE_DATA_NTF_PROXIMITY_NEAR_DEFAULT);
                checkArgument(mRangeDataNtfProximityFar
                        == RANGE_DATA_NTF_PROXIMITY_FAR_DEFAULT);
                checkArgument(mRangeDataNtfAoaAzimuthLower
                            != RANGE_DATA_NTF_AOA_AZIMUTH_LOWER_DEFAULT
                        || mRangeDataNtfAoaAzimuthUpper
                            != RANGE_DATA_NTF_AOA_AZIMUTH_UPPER_DEFAULT
                        || mRangeDataNtfAoaElevationLower
                            != RANGE_DATA_NTF_AOA_ELEVATION_LOWER_DEFAULT
                        || mRangeDataNtfAoaElevationUpper
                            != RANGE_DATA_NTF_AOA_ELEVATION_UPPER_DEFAULT);
            } else if (mRangeDataNtfConfig == RANGE_DATA_NTF_CONFIG_ENABLE_PROXIMITY_AOA_LEVEL_TRIG
                    || mRangeDataNtfConfig
                    == RANGE_DATA_NTF_CONFIG_ENABLE_PROXIMITY_AOA_EDGE_TRIG) {
                checkArgument(
                        mRangeDataNtfProximityNear != RANGE_DATA_NTF_PROXIMITY_NEAR_DEFAULT
                        || mRangeDataNtfProximityFar != RANGE_DATA_NTF_PROXIMITY_FAR_DEFAULT
                        || mRangeDataNtfAoaAzimuthLower
                            != RANGE_DATA_NTF_AOA_AZIMUTH_LOWER_DEFAULT
                        || mRangeDataNtfAoaAzimuthUpper
                            != RANGE_DATA_NTF_AOA_AZIMUTH_UPPER_DEFAULT
                        || mRangeDataNtfAoaElevationLower
                            != RANGE_DATA_NTF_AOA_ELEVATION_LOWER_DEFAULT
                        || mRangeDataNtfAoaElevationUpper
                            != RANGE_DATA_NTF_AOA_ELEVATION_UPPER_DEFAULT);
            }
        }
        private void checkDlTdoaParameters() {
            if (mDeviceRole.get() == RANGING_DEVICE_DT_TAG) {
                checkArgument(mStsConfig == STS_CONFIG_STATIC
                            && mMultiNodeMode.get() == MULTI_NODE_MODE_ONE_TO_MANY
                            && mRframeConfig == RFRAME_CONFIG_SP1);
            }
        }

        /** Sets the type of filtering used by the session. Defaults to FILTER_TYPE_DEFAULT */
        public Builder setFilterType(@FilterType int filterType) {
            this.mFilterType = filterType;
            return this;
        }

        /**
         * Returns true when (RangingRoundUsage = 1, 2, 3, 4) and
         * SCHEDULED_MODE == 0x01 (TIME_SCHEDULED_RANGING)
         **/
        public boolean isTimeScheduledTwrSession() {
            if (mScheduledMode == FiraParams.TIME_SCHEDULED_RANGING) {
                if (mRangingRoundUsage == RANGING_ROUND_USAGE_SS_TWR_DEFERRED_MODE
                        || mRangingRoundUsage == RANGING_ROUND_USAGE_DS_TWR_DEFERRED_MODE
                        || mRangingRoundUsage == RANGING_ROUND_USAGE_SS_TWR_NON_DEFERRED_MODE
                        || mRangingRoundUsage == RANGING_ROUND_USAGE_DS_TWR_NON_DEFERRED_MODE) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Reinterprets the least significant 32 bits of the input as an unsigned integer and
         * returns the result as a signed integer.
         *
         * @param x input treated as unsigned 32 bit integer.
         * @return a (signed) integer interpretation of the input's underlying bytes.
         */
        @VisibleForTesting
        public static int asUnsigned(long x) {
            checkArgument(x >= 0, "Input was negative");
            checkArgument(x < 1L << 32, "Input does not fit in an unsigned 32 bit integer");

            return ByteBuffer.wrap(Longs.toByteArray(x)).getInt(4);
        }

        public FiraOpenSessionParams build() {
            checkAddress();
            checkStsConfig();
            checkInterleavingRatio();
            checkRangeDataNtfConfig();
            checkDlTdoaParameters();
            return new FiraOpenSessionParams(
                    mProtocolVersion.get(),
                    mSessionId.get(),
                    mSessionType,
                    mDeviceType.get(),
                    mDeviceRole.get(),
                    mRangingRoundUsage,
                    mMultiNodeMode.get(),
                    mDeviceAddress,
                    mDestAddressList,
                    mInitiationTime,
                    mAbsoluteInitiationTime,
                    mSlotDurationRstu,
                    mSlotsPerRangingRound,
                    mRangingIntervalMs,
                    mBlockStrideLength,
                    mHoppingMode,
                    mMaxRangingRoundRetries,
                    mSessionPriority,
                    mMacAddressMode,
                    mHasRangingResultReportMessage,
                    mHasControlMessage,
                    mHasRangingControlPhase,
                    mMeasurementReportType,
                    mMeasurementReportPhase,
                    mInBandTerminationAttemptCount,
                    mChannelNumber,
                    mPreambleCodeIndex,
                    mRframeConfig,
                    mPrfMode,
                    mCapSize,
                    mScheduledMode,
                    mPreambleDuration,
                    mSfdId,
                    mStsSegmentCount,
                    mStsLength,
                    mSessionKey,
                    mSubsessionKey,
                    mPsduDataRate,
                    mBprfPhrDataRate,
                    mFcsType,
                    mIsTxAdaptivePayloadPowerEnabled,
                    mStsConfig,
                    mSubSessionId.get(),
                    mVendorId,
                    mStaticStsIV,
                    mIsRssiReportingEnabled,
                    mIsDiagnosticsEnabled,
                    mDiagramsFrameReportsFieldsFlags,
                    mAntennaMode,
                    mIsKeyRotationEnabled,
                    mKeyRotationRate,
                    mAoaResultRequest,
                    mRangeDataNtfConfig,
                    mRangeDataNtfProximityNear,
                    mRangeDataNtfProximityFar,
                    mRangeDataNtfAoaAzimuthLower,
                    mRangeDataNtfAoaAzimuthUpper,
                    mRangeDataNtfAoaElevationLower,
                    mRangeDataNtfAoaElevationUpper,
                    mHasTimeOfFlightReport,
                    mHasAngleOfArrivalAzimuthReport,
                    mHasAngleOfArrivalElevationReport,
                    mHasAngleOfArrivalFigureOfMeritReport,
                    mAoaType,
                    mNumOfMsrmtFocusOnRange,
                    mNumOfMsrmtFocusOnAoaAzimuth,
                    mNumOfMsrmtFocusOnAoaElevation,
                    mRangingErrorStreakTimeoutMs,
                    mLinkLayerMode,
                    mDataRepetitionCount,
                    mRangingTimeStruct,
                    mMinFramesPerRr,
                    mMtuSize,
                    mInterFrameInterval,
                    mDlTdoaBlockStriding,
                    mUlTdoaTxIntervalMs,
                    mUlTdoaRandomWindowMs,
                    mUlTdoaDeviceIdType,
                    mUlTdoaDeviceId,
                    mUlTdoaTxTimestampType,
                    mFilterType,
                    mMaxNumberOfMeasurements,
                    mSessionDataTransferStatusNtfConfig,
                    mReferenceTimeBase,
                    mReferenceSessionHandle,
                    mSessionOffsetInMicroSeconds,
                    mApplicationDataEndpoint);
        }
    }
}
